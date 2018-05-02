package net.jadoth.config;

import static net.jadoth.Jadoth.notNull;

import java.util.function.Consumer;

import net.jadoth.collections.BulkList;
import net.jadoth.collections.EqConstHashTable;
import net.jadoth.collections.EqHashEnum;
import net.jadoth.collections.EqHashTable;
import net.jadoth.collections.KeyValue;
import net.jadoth.collections.types.XGettingCollection;
import net.jadoth.collections.types.XGettingMap;
import net.jadoth.collections.types.XGettingTable;
import net.jadoth.meta.JadothConsole;
import net.jadoth.util.chars.VarString;


public abstract class AbstractConfig implements Config
{
	///////////////////////////////////////////////////////////////////////////
	// static methods //
	///////////////////

	static final EqHashTable<String, String> toTable(final EqHashEnum<ConfigFile> configFiles)
	{
		final EqHashTable<String, String> newConfig = EqHashTable.New();
		configFiles.iterate(new Consumer<ConfigFile>()
		{
			@Override
			public void accept(final ConfigFile e)
			{
				newConfig.addAll(e.table());
			}
		});
		return newConfig;
	}

	static final EqHashTable<String, String> toTable(final XGettingCollection<KeyValue<String, String>> entries)
	{
		// collect entries in new hashtable
		final EqHashTable<String, String> newConfig = EqHashTable.New();
		newConfig.addAll(entries);
		return newConfig;
	}

	public static char variableStarter()
	{
		return '{';
	}

	public static char variableTerminator()
	{
		return '}';
	}



	///////////////////////////////////////////////////////////////////////////
	// instance fields //
	////////////////////

	final String                            identifier        ;

	// may be anything. Unresolvable Strings are passed through.
	final char                              variableStarter   ;
	final char                              variableTerminator;

	final String                            stringStarter     ;
	final String                            stringTerminator  ;
	final XGettingMap<String, String>       customVariables   ;
	final EqHashTable<String, ConfigFile>   configFiles        = EqHashTable.New();
	final XGettingTable<String, ConfigFile> viewConfigFiles    = this.configFiles.view();



	///////////////////////////////////////////////////////////////////////////
	// constructors //
	/////////////////

	AbstractConfig(
		final String                      identifier        ,
		final XGettingMap<String, String> customVariables   ,
		final Character                   variableStarter   ,
		final Character                   variableTerminator
	)
	{
		super();
		this.identifier = notNull(identifier);

		this.variableStarter    = variableStarter    != null ? variableStarter    : variableStarter()   ;
		this.variableTerminator = variableTerminator != null ? variableTerminator : variableTerminator();
		this.customVariables    = customVariables   ; // may be null
		this.stringStarter      = String.valueOf(this.variableStarter);
		this.stringTerminator   = String.valueOf(this.variableTerminator);
	}

	AbstractConfig(
		final String                      identifier     ,
		final XGettingMap<String, String> customVariables
	)
	{
		this(identifier, customVariables, null, null);
	}

	AbstractConfig(final String identifier)
	{
		this(identifier, null);
	}



	///////////////////////////////////////////////////////////////////////////
	// declared methods //
	/////////////////////

	public final XGettingTable<String, ConfigFile> files()
	{
		return this.viewConfigFiles;
	}

	@Override
	public abstract XGettingTable<String, String> table();

	@Override
	public final String identifier()
	{
		return this.identifier;
	}

	private String parseRawValue(final String originalKey, final String rawValue)
	{
		if(rawValue.isEmpty())
		{
			return rawValue;
		}

		// strictly read-only, so accessing the string instance's storage array is safe
		final char[]           chars              = rawValue.toCharArray() ;
		final int              length             = chars.length           ;
		final char             variableStarter    = this.variableStarter   ;
		final char             variableTerminator = this.variableTerminator;
		final String           stringStrt         = this.stringStarter     ;
		final String           stringTerm         = this.stringTerminator  ;

		// lazily initialized to make the normal case (no variable at all) as efficient as possible
		BulkList<String> elements = null;

		int lastOffset = 0;
		int i = 0;
		while(i < length)
		{
			if(chars[i] == variableStarter || chars[i] == variableTerminator)
			{
				if(elements == null)
				{
					elements = BulkList.New();
				}

				if(i != lastOffset)
				{
					elements.add(new String(chars, lastOffset, i - lastOffset));
				}
				elements.add(chars[i] == variableStarter ? stringStrt : stringTerm);
				lastOffset = i + 1;
			}
			i++;
		}

		// if no potential variable has been found at all, abort here and return the raw value directly.
		if(elements == null)
		{
			return rawValue;
		}

		if(lastOffset != length)
		{
			elements.add(new String(chars, lastOffset, length - lastOffset));
		}

		return this.resolveParsedElements(elements.toArray(String.class), length, originalKey);
	}

	@Override
	public String getValue(final String key)
	{
		final String rawValue = this.getRawValue(key);
		if(rawValue == null)
		{
			return null;
		}

		return this.parseRawValue(key, rawValue);
	}

	private String resolveParsedElements(final String[] elements, final int originalValueLength, final String originalKey)
	{
		final XGettingMap<String, String>   customVariables = this.customVariables   ;
		final String                        stringStrt      = this.stringStarter     ;
		final String                        stringTerm      = this.stringTerminator  ;
		final XGettingTable<String, String> table           = this.table()           ;
		final int                           length          = elements.length        ;
		final int                           startBound      = length - 2             ;

		final VarString result = VarString.New(originalValueLength);

		int i = 0;
		while(i < startBound)
		{
			if(elements[i] != stringStrt || elements[i + 2] != stringTerm)
			{
				result.add(elements[i++]);
				continue;
			}

			final String variable = elements[i + 1];

			// custom variable explicitely allow equal names as they serve as a kind of loop stopper.
			String resolved = customVariables == null
				? null
				: customVariables.get(variable)
			;

			if(resolved == null)
			{
				// check for self-referencing
				if(originalKey.equals(variable))
				{
					throw new RuntimeException("Reference loop for key \"" + originalKey+"\"");
				}

				final String dereferenced = table.get(variable);
				if(dereferenced != null)
				{
					// the dereferenced value might contain a variable itself, so it has to be resolved recursively
					resolved = this.parseRawValue(originalKey, dereferenced);
				}
			}
			if(resolved != null)
			{
				// if the variable has been successfully resolved, use the resolved value and discard the actual string.
				result.add(resolved);
			}
			else
			{
				// if the variable could not have been resolved, replicate the actual string in the result
				result.add(stringStrt).add(variable).add(stringTerm);
			}

			// must not only skip the current element (loop's increment), but also the variable and terminator.
			i += 3;
		}

		// add the rest of the elements (2 at the most) that cannot be a variable
		while(i < length)
		{
			result.add(elements[i]);
			i++;
		}

		return result.toString();
	}

	@Override
	public final <T> T get(final ConfigEntry<T> entry)
	{
		try
		{
			return entry.parse(this.getValue(entry.key()));
		}
		catch(final Exception e)
		{
			// (18.11.2013 TM)EXCP: proper exception
			throw new RuntimeException("Exception for entry " + entry.key(), e);
		}
	}


	void updateFile(final ConfigFile newConfigFile)
	{
		ConfigFile file = this.configFiles.get(newConfigFile.name());
		if(file == null)
		{
			this.configFiles.add(newConfigFile.name(), file = newConfigFile);
		}
		else
		{
			file.table().putAll(newConfigFile.table()); // put to override old entries
		}
	}

	void updateFiles(final EqHashTable<String, ConfigFile> newConfigFiles)
	{
		newConfigFiles.values().iterate(e ->
			this.updateFile(e)
		);
	}

	EqConstHashTable<String, String> compileEntries()
	{
		final EqHashTable<String, String> table = EqHashTable.New();
		this.configFiles.values().iterate(e ->
			table.addAll(e.table())
		);
		return table.immure();
	}



	///////////////////////////////////////////////////////////////////////////
	// override methods //
	/////////////////////

	@Override
	public String toString()
	{
		final VarString vs = VarString.New();

		this.configFiles.values().iterate(new Consumer<ConfigFile>()
		{
			@Override
			public void accept(final ConfigFile e)
			{
				vs.add(e.name()).blank().add('(').add(e.name).add(')').lf();
				JadothConsole.assembleTable(vs, e.table(), "---", "---\n", "\n---", null, null);
				vs.lf().add("|||||||||||||||||||||||||||||||||||||||||||||||||||||||||||").lf();
			}
		});
		return vs.toString();
	}

}
