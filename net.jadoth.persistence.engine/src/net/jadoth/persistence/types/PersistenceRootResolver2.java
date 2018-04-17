package net.jadoth.persistence.types;

import static net.jadoth.Jadoth.keyValue;
import static net.jadoth.Jadoth.mayNull;
import static net.jadoth.Jadoth.notNull;

import java.lang.reflect.Field;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.jadoth.collections.EqConstHashTable;
import net.jadoth.collections.EqHashTable;
import net.jadoth.collections.types.XGettingMap;
import net.jadoth.collections.types.XImmutableTable;
import net.jadoth.memory.Memory;
import net.jadoth.util.KeyValue;

public interface PersistenceRootResolver2
{
	public interface Builder
	{
		public Builder registerRoot(String identifier, Supplier<?> instanceSupplier);
		
		public Builder registerMapping(String sourceIdentifier, String targetIdentifier);
		
		public PersistenceRootResolver2 build();
		
		public final class Implementation implements PersistenceRootResolver2.Builder
		{
			///////////////////////////////////////////////////////////////////////////
			// instance fields //
			////////////////////
			
			private final BiFunction<String, Supplier<?>, PersistenceRootEntry> entryProvider     ;
			private final EqHashTable<String, String>                           refactoringMapping = EqHashTable.New();
			private final EqHashTable<String, PersistenceRootEntry>             rootEntries        = EqHashTable.New();
			
			
			
			///////////////////////////////////////////////////////////////////////////
			// constructors //
			/////////////////
			
			Implementation(final BiFunction<String, Supplier<?>, PersistenceRootEntry> entryProvider)
			{
				super();
				this.entryProvider = entryProvider;
			}
			
			
			
			///////////////////////////////////////////////////////////////////////////
			// methods //
			////////////
			
			@Override
			public final Builder registerRoot(final String identifier, final Supplier<?> instanceSupplier)
			{
				final PersistenceRootEntry entry = this.entryProvider.apply(identifier, instanceSupplier);
				this.addEntry(identifier, entry);
				return this;
			}
			
			private void addEntry(final String identifier, final PersistenceRootEntry entry)
			{
				if(this.rootEntries.add(identifier, entry))
				{
					return;
				}
				throw new RuntimeException(); // (17.04.2018 TM)EXCP: proper exception
			}
			
			@Override
			public final Builder registerMapping(final String sourceIdentifier, final String targetIdentifier)
			{
				if(!this.refactoringMapping.add(sourceIdentifier, targetIdentifier))
				{
					throw new RuntimeException(); // (17.04.2018 TM)EXCP: proper exception
				}
				return this;
			}
			
			@Override
			public final PersistenceRootResolver2 build()
			{
				final EqHashTable<String, PersistenceRootEntry> effectiveEntries = EqHashTable.New(this.rootEntries);
				final BiFunction<String, Supplier<?>, PersistenceRootEntry> entryProvider = this.entryProvider;
				
				for(final KeyValue<String, String> kv : this.refactoringMapping)
				{
					final String sourceIdentifier = kv.key();
					if(kv.value() == null)
					{
						effectiveEntries.add(sourceIdentifier, entryProvider.apply(sourceIdentifier, null));
						continue;
					}
					
					final PersistenceRootEntry targetEntry = effectiveEntries.get(kv.value());
					if(targetEntry == null)
					{
						throw new RuntimeException(); // (17.04.2018 TM)EXCP: proper exception
					}
					this.addEntry(sourceIdentifier, targetEntry);
				}
				
				return new PersistenceRootResolver2.Implementation(null, null);
			}
		}
	}
	
	
	
	public default Result resolveRootInstance(final String identifier)
	{
		final Field field;
		try
		{
			field = resolveField(identifier);
		}
		catch(final ReflectiveOperationException e)
		{
			throw new IllegalArgumentException(e);
		}
		
		return PersistenceRootResolver2.createResult(getStaticReference(field), identifier);
	}

	public default String deriveIdentifier(final Field field)
	{
		return buildFieldIdentifier(field.getDeclaringClass().getName(), field.getName());
	}
	
	public static String buildFieldIdentifier(final String className, final String fieldName)
	{
		return className + fieldIdentifierDelimiter() + fieldName;
	}

	/**
	 * Iterates all entries that are explicitely known to this instance (e.g. custom mapped override entries).
	 *
	 * @param procedure
	 */
	public default void iterateIdentifierMappings(final Consumer<? super KeyValue<String, ?>> procedure)
	{
		// no entries in stateless default implementation
	}
	
	
	public static Object getStaticReference(final Field field)
	{
		return Memory.getStaticReference(field);
	}

	public static Result createResult(
		final Object resolvedRootInstance,
		final String identifier
	)
	{
		return PersistenceRootResolver2.createResult(resolvedRootInstance, identifier, identifier);
	}
	
	public static Result createResult(
		final Object resolvedRootInstance,
		final String providedIdentifier  ,
		final String resolvedIdentifier
	)
	{
		// if an explicit delete entry was found, the instance and the resolved identifier are null.
		return new Result.Implementation(
			mayNull(resolvedRootInstance),
			notNull(providedIdentifier)  ,
			mayNull(resolvedIdentifier)
		);
	}

	public interface Result
	{
		public Object resolvedRootInstance();
		
		public String providedIdentifier();
		
		public String resolvedIdentifier();
		
		public default boolean hasChanged()
		{
			// it is assumed that for the unchanged case, the same identifier String is passed twice.
			return this.providedIdentifier() == this.resolvedIdentifier();
		}
		
		public final class Implementation implements PersistenceRootResolver2.Result
		{
			private final Object resolvedRootInstance;
			private final String providedIdentifier  ;
			private final String resolvedIdentifier  ;
			
			Implementation(
				final Object resolvedRootInstance,
				final String providedIdentifier  ,
				final String resolvedIdentifier
			)
			{
				super();
				this.resolvedRootInstance = resolvedRootInstance;
				this.providedIdentifier   = providedIdentifier  ;
				this.resolvedIdentifier   = resolvedIdentifier  ;
			}

			@Override
			public final Object resolvedRootInstance()
			{
				return this.resolvedRootInstance;
			}

			@Override
			public String providedIdentifier()
			{
				return this.providedIdentifier;
			}

			@Override
			public String resolvedIdentifier()
			{
				return this.resolvedIdentifier;
			}
			
		}
		
	}

	
	
	public static char fieldIdentifierDelimiter()
	{
		return '#';
	}
	
	public static int getFieldIdentifierDelimiterIndex(final String identifier)
	{
		final int index = identifier.lastIndexOf(fieldIdentifierDelimiter());
		if(index < 0)
		{
			throw new IllegalArgumentException(); // (16.10.2013 TM)TODO: proper Exception
		}
		
		return index;
	}
	
	public static String getClassName(final String identifier)
	{
		return identifier.substring(0, PersistenceRootResolver2.getFieldIdentifierDelimiterIndex(identifier));
	}
	
	public static String getFieldName(final String identifier)
	{
		return identifier.substring(PersistenceRootResolver2.getFieldIdentifierDelimiterIndex(identifier) + 1);
	}

	public static Field resolveField(final String identifier)
		throws ClassNotFoundException, NoSuchFieldException
	{
		return resolveField(
			PersistenceRootResolver2.getClassName(identifier),
			PersistenceRootResolver2.getFieldName(identifier)
		);
	}
	
	public static Field resolveField(final String className, final String fieldName)
		throws ClassNotFoundException, NoSuchFieldException
	{
		// ReflectiveOperationExceptions have to be passed to the calling context for retryin
		final Class<?> declaringClass = Class.forName(className);
		return declaringClass.getDeclaredField(fieldName);
	}
	
	
	public static PersistenceRootResolver2 New()
	{
		return new Stateless();
	}
	
	public static PersistenceRootResolver2 New(final String identifier, final Object instance)
	{
		// hardcoded table implementation to ensure value-equality.
		return new Implementation(
			EqConstHashTable.New(
				keyValue(identifier, instance)
			),
			PersistenceRefactoringMapping.Provider.New()
		);
	}
	
	public static PersistenceRootResolver2 New(final XGettingMap<String, ?> identifierMapping)
	{
		// hardcoded table implementation to ensure value-equality.
		return new Implementation(
			EqConstHashTable.New(identifierMapping),
			PersistenceRefactoringMapping.Provider.New()
		);
	}
	
	public static PersistenceRootResolver2 New(
		final String                                 identifier         ,
		final Object                                 instance           ,
		final PersistenceRefactoringMapping.Provider refactoringMappingProvider
	)
	{
		// hardcoded table implementation to ensure value-equality.
		return new Implementation(
			EqConstHashTable.New(
				keyValue(identifier, instance)
			),
			refactoringMappingProvider
		);
	}
	
	public static PersistenceRootResolver2 New(
		final XGettingMap<String, ?>                 identifierMappings        ,
		final PersistenceRefactoringMapping.Provider refactoringMappingProvider
	)
	{
		// hardcoded table implementation to ensure value-equality.
		return new Implementation(
			EqConstHashTable.New(identifierMappings) ,
			refactoringMappingProvider
		);
	}
	
	
	public final class Stateless implements PersistenceRootResolver2
	{
		/*
		 * A stateless class with all-default-methods interface(s) contains no source code.
		 * In other words: since default methods, java is missing a mechanism
		 * to create (stateless) instances of interfaces.
		 */
	}
	
	public final class Implementation implements PersistenceRootResolver2
	{
		///////////////////////////////////////////////////////////////////////////
		// instance fields //
		////////////////////

		final XImmutableTable<String, ?>             identifierMappings        ;
		final PersistenceRefactoringMapping.Provider refactoringMappingProvider;
		
		transient XGettingMap<String, String> refactoringMappings;



		///////////////////////////////////////////////////////////////////////////
		// constructors //
		/////////////////

		Implementation(
			final XImmutableTable<String, ?>             identifierMappings        ,
			final PersistenceRefactoringMapping.Provider refactoringMappingProvider
		)
		{
			super();
			this.identifierMappings         = identifierMappings        ;
			this.refactoringMappingProvider = refactoringMappingProvider;
		}



		///////////////////////////////////////////////////////////////////////////
		// methods //
		////////////
		
		private synchronized XGettingMap<String, String> refactoringMappings()
		{
			if(this.refactoringMappings == null)
			{
				this.refactoringMappings = this.refactoringMappingProvider.provideRefactoringMapping().entries();
			}
			
			return this.refactoringMappings;
		}
		
		@Override
		public final void iterateIdentifierMappings(final Consumer<? super KeyValue<String, ?>> procedure)
		{
			this.identifierMappings.iterate(procedure);
		}
		
		final Result tryResolveRootInstance(final String originalIdentifier, final String effectiveIdentifier)
			throws ClassNotFoundException, NoSuchFieldException
		{
			final Object overrideInstance = this.identifierMappings.get(effectiveIdentifier);
			if(overrideInstance != null)
			{
				return PersistenceRootResolver2.createResult(overrideInstance, effectiveIdentifier);
			}
			
			final String className = PersistenceRootResolver2.getClassName(effectiveIdentifier);
			final String fieldName = PersistenceRootResolver2.getFieldName(effectiveIdentifier);
			
			return PersistenceRootResolver2.createResult(
				PersistenceRootResolver2.getStaticReference(
					PersistenceRootResolver2.resolveField(className, fieldName)
				),
				originalIdentifier,
				effectiveIdentifier
			);
		}
		
		@Override
		public final Result resolveRootInstance(final String identifier)
		{
			/*
			 * Mapping lookups take precedence over the direct resolving attempt.
			 * This is important to enable refactorings that switch names.
			 * E.g.:
			 * A -> B
			 * C -> A
			 * However, this also increases the responsibility of the developer who defines the mapping:
			 * The mapping has to be removed after the first usage, otherwise the new instance under the old name
			 * is mapped to the old name's new name, as well. (In the example: after two executions, both instances
			 * would be mapped to B, which is an error. However, the source of the error is not a bug,
			 * but an outdated mapping rule defined by the using developer).
			 */
			final XGettingMap<String, String> refactoringMappings = this.refactoringMappings();
			
			// mapping variant #1: completely mapped identifier (className#fieldName or arbitrary name, e.g. "root")
			if(refactoringMappings.keys().contains(identifier))
			{
				final String mappedIdentifier = refactoringMappings.get(identifier);
				if(mappedIdentifier == null)
				{
					// an identifier explicitely mapped to null means the element has been deleted.
					return PersistenceRootResolver2.createResult(null, identifier, null);
				}
				
				try
				{
					return tryResolveRootInstance(identifier, mappedIdentifier);
				}
				catch(final ReflectiveOperationException e)
				{
					// check next case.
				}
			}
			
			/*
			 * identifier mappings must be checked before trying to extract a reflection class name in case
			 * they are arbitrary strings (e.g. "root") instead of reflective identifiers.
			 */
			final Object overrideInstance = this.identifierMappings.get(identifier);
			if(overrideInstance != null)
			{
				return PersistenceRootResolver2.createResult(overrideInstance, identifier);
			}

			// mapping variant #2: only mapped className (fieldName remains the same)
			final String className = PersistenceRootResolver2.getClassName(identifier);
			if(refactoringMappings.keys().contains(className))
			{
				final String mappedClassName = refactoringMappings.get(className);
				if(mappedClassName == null)
				{
					// a className explicitely mapped to null means it has been deleted.
					return PersistenceRootResolver2.createResult(null, identifier, null);
				}
				
				final String fieldName        = PersistenceRootResolver2.getFieldName(identifier);
				final String mappedIdentifier = PersistenceRootResolver2.buildFieldIdentifier(mappedClassName, fieldName);
				try
				{
					return tryResolveRootInstance(identifier, mappedIdentifier);
				}
				catch(final ReflectiveOperationException e)
				{
					// check next case
				}
			}
			
			// no mapping could be found, so the only remaining option is to resolve the identifier in a general way.
			return PersistenceRootResolver2.super.resolveRootInstance(identifier);
		}

	}

}
