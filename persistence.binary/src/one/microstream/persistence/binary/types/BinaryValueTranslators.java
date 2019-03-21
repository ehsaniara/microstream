package one.microstream.persistence.binary.types;

import one.microstream.X;
import one.microstream.memory.XMemory;
import one.microstream.persistence.types.PersistenceObjectIdResolver;
import one.microstream.persistence.types.PersistenceTypeDescriptionMember;
import one.microstream.typing.TypeMapping;

public final class BinaryValueTranslators
{
	///////////////////////////////////////////////////////////////////////////
	// static methods //
	///////////////////
	
	public static BinaryValueSetter provideReferenceValueBinaryTranslator(
		final PersistenceTypeDescriptionMember sourceMember,
		final PersistenceTypeDescriptionMember targetMember
	)
	{
		// all references are stored as OID primitive values (long)
		if(targetMember == null)
		{
			return BinaryValueTranslators::skip_long;
		}
		
		if(!targetMember.isReference())
		{
			throwUnhandledTypeCompatibilityException(sourceMember.typeName(), targetMember.typeName());
		}
		
		return BinaryValueTranslators::copy_longTo_long;
	}
	
	private static void throwUnhandledTypeCompatibilityException(
		final String sourceType,
		final String targetType
	)
	{
		// (18.09.2018 TM)EXCP: proper exception
		throw new RuntimeException(
			"Cannot convert between primitive and reference values: "
			+ sourceType + " <-> " + targetType+ "."
		);
	}
	
	/**
	 * The default mapping only covers primitive types, because for arbitrary Object types, it cannot be
	 * safely assumed that instances of those types are unshared and that implicitely replacing one instance
	 * with another will never cause erronous behavior (e.g. identity comparisons suddenly yielding different
	 * results than would be expected based on the stored instances).<p>
	 * However, arbitrary mappings can be added to suit the needs of specific programs.
	 * 
	 * @return a default mapping of primitive-to-primitive binary value translators.
	 */
	public static final TypeMapping<BinaryValueSetter> createDefaultValueTranslators(final boolean switchByteOrder)
	{
		return switchByteOrder
			? createDefaultValueTranslatorsSwitchingByteOrder()
			: createDefaultValueTranslatorsDirectByteOrder()
		;
	}
	
	public static final TypeMapping<BinaryValueSetter> createDefaultValueTranslatorsDirectByteOrder()
	{
		final TypeMapping<BinaryValueSetter> mapping = TypeMapping.New();
		registerPrimitivesToPrimitives(mapping);
		registerPrimitivesToWrappers(mapping);
		registerWrappersToPrimitives(mapping);
		registerWrappersToWrappers(mapping);
		registerCommonValueTypes(mapping);
		
		return mapping;
	}
	
	public static final TypeMapping<BinaryValueSetter> createDefaultValueTranslatorsSwitchingByteOrder()
	{
		/* note:
		 * these translators must do a double-switch since the actual binary-to-instance logic will
		 * switch the bytes, too.
		 * For the same reason, non-converting translators can just pass the value through without any switching.
		 */
		final TypeMapping<BinaryValueSetter> mapping = TypeMapping.New();
		registerPrimitivesToPrimitivesSwitchingByteOrder(mapping);
		registerPrimitivesToWrappersSwitchingByteOrder(mapping);
		registerWrappersToPrimitivesSwitchingByteOrder(mapping);
		registerWrappersToWrappersSwitchingByteOrder(mapping);
		registerCommonValueTypesSwitchingByteOrder(mapping);
		
		return mapping;
	}
	
	private static void registerPrimitivesToPrimitives(final TypeMapping<BinaryValueSetter> mapping)
	{
		mapping
		.register(byte.class, byte   .class, BinaryValueTranslators::copy_byteTo_byte   )
		.register(byte.class, boolean.class, BinaryValueTranslators::copy_byteTo_boolean)
		.register(byte.class, short  .class, BinaryValueTranslators::copy_byteTo_short  )
		.register(byte.class, char   .class, BinaryValueTranslators::copy_byteTo_char   )
		.register(byte.class, int    .class, BinaryValueTranslators::copy_byteTo_int    )
		.register(byte.class, float  .class, BinaryValueTranslators::copy_byteTo_float  )
		.register(byte.class, long   .class, BinaryValueTranslators::copy_byteTo_long   )
		.register(byte.class, double .class, BinaryValueTranslators::copy_byteTo_double )
		
		.register(boolean.class, byte   .class, BinaryValueTranslators::copy_booleanTo_byte   )
		.register(boolean.class, boolean.class, BinaryValueTranslators::copy_booleanTo_boolean)
		.register(boolean.class, short  .class, BinaryValueTranslators::copy_booleanTo_short  )
		.register(boolean.class, char   .class, BinaryValueTranslators::copy_booleanTo_char   )
		.register(boolean.class, int    .class, BinaryValueTranslators::copy_booleanTo_int    )
		.register(boolean.class, float  .class, BinaryValueTranslators::copy_booleanTo_float  )
		.register(boolean.class, long   .class, BinaryValueTranslators::copy_booleanTo_long   )
		.register(boolean.class, double .class, BinaryValueTranslators::copy_booleanTo_double )
		
		.register(short.class, byte   .class, BinaryValueTranslators::copy_shortTo_byte   )
		.register(short.class, boolean.class, BinaryValueTranslators::copy_shortTo_boolean)
		.register(short.class, short  .class, BinaryValueTranslators::copy_shortTo_short  )
		.register(short.class, char   .class, BinaryValueTranslators::copy_shortTo_char   )
		.register(short.class, int    .class, BinaryValueTranslators::copy_shortTo_int    )
		.register(short.class, float  .class, BinaryValueTranslators::copy_shortTo_float  )
		.register(short.class, long   .class, BinaryValueTranslators::copy_shortTo_long   )
		.register(short.class, double .class, BinaryValueTranslators::copy_shortTo_double )
		
		.register(char.class, byte   .class, BinaryValueTranslators::copy_charTo_byte   )
		.register(char.class, boolean.class, BinaryValueTranslators::copy_charTo_boolean)
		.register(char.class, short  .class, BinaryValueTranslators::copy_charTo_short  )
		.register(char.class, char   .class, BinaryValueTranslators::copy_charTo_char   )
		.register(char.class, int    .class, BinaryValueTranslators::copy_charTo_int    )
		.register(char.class, float  .class, BinaryValueTranslators::copy_charTo_float  )
		.register(char.class, long   .class, BinaryValueTranslators::copy_charTo_long   )
		.register(char.class, double .class, BinaryValueTranslators::copy_charTo_double )
		
		.register(int.class, byte   .class, BinaryValueTranslators::copy_intTo_byte   )
		.register(int.class, boolean.class, BinaryValueTranslators::copy_intTo_boolean)
		.register(int.class, short  .class, BinaryValueTranslators::copy_intTo_short  )
		.register(int.class, char   .class, BinaryValueTranslators::copy_intTo_char   )
		.register(int.class, int    .class, BinaryValueTranslators::copy_intTo_int    )
		.register(int.class, float  .class, BinaryValueTranslators::copy_intTo_float  )
		.register(int.class, long   .class, BinaryValueTranslators::copy_intTo_long   )
		.register(int.class, double .class, BinaryValueTranslators::copy_intTo_double )
		
		.register(float.class, byte   .class, BinaryValueTranslators::copy_floatTo_byte   )
		.register(float.class, boolean.class, BinaryValueTranslators::copy_floatTo_boolean)
		.register(float.class, short  .class, BinaryValueTranslators::copy_floatTo_short  )
		.register(float.class, char   .class, BinaryValueTranslators::copy_floatTo_char   )
		.register(float.class, int    .class, BinaryValueTranslators::copy_floatTo_int    )
		.register(float.class, float  .class, BinaryValueTranslators::copy_floatTo_float  )
		.register(float.class, long   .class, BinaryValueTranslators::copy_floatTo_long   )
		.register(float.class, double .class, BinaryValueTranslators::copy_floatTo_double )
		
		.register(long.class, byte   .class, BinaryValueTranslators::copy_longTo_byte   )
		.register(long.class, boolean.class, BinaryValueTranslators::copy_longTo_boolean)
		.register(long.class, short  .class, BinaryValueTranslators::copy_longTo_short  )
		.register(long.class, char   .class, BinaryValueTranslators::copy_longTo_char   )
		.register(long.class, int    .class, BinaryValueTranslators::copy_longTo_int    )
		.register(long.class, float  .class, BinaryValueTranslators::copy_longTo_float  )
		.register(long.class, long   .class, BinaryValueTranslators::copy_longTo_long   )
		.register(long.class, double .class, BinaryValueTranslators::copy_longTo_double )
		
		.register(double.class, byte   .class, BinaryValueTranslators::copy_doubleTo_byte   )
		.register(double.class, boolean.class, BinaryValueTranslators::copy_doubleTo_boolean)
		.register(double.class, short  .class, BinaryValueTranslators::copy_doubleTo_short  )
		.register(double.class, char   .class, BinaryValueTranslators::copy_doubleTo_char   )
		.register(double.class, int    .class, BinaryValueTranslators::copy_doubleTo_int    )
		.register(double.class, float  .class, BinaryValueTranslators::copy_doubleTo_float  )
		.register(double.class, long   .class, BinaryValueTranslators::copy_doubleTo_long   )
		.register(double.class, double .class, BinaryValueTranslators::copy_doubleTo_double )
		;
	}
	
	private static void registerPrimitivesToWrappers(final TypeMapping<BinaryValueSetter> mapping)
	{
		mapping
		.register(byte   .class, Byte     .class, BinaryValueTranslators::copy_byteAsByte      )
		.register(boolean.class, Boolean  .class, BinaryValueTranslators::copy_booleanAsBoolean)
		.register(short  .class, Short    .class, BinaryValueTranslators::copy_shortAsShort    )
		.register(char   .class, Character.class, BinaryValueTranslators::copy_charAsCharacter )
		.register(int    .class, Integer  .class, BinaryValueTranslators::copy_intAsInteger    )
		.register(float  .class, Float    .class, BinaryValueTranslators::copy_floatAsFloat    )
		.register(long   .class, Long     .class, BinaryValueTranslators::copy_longAsLong      )
		.register(double .class, Double   .class, BinaryValueTranslators::copy_doubleAsDouble  )
		;
	}
	
	private static void registerWrappersToPrimitives(final TypeMapping<BinaryValueSetter> mapping)
	{
		mapping
		.register(Byte     .class, byte   .class, BinaryValueTranslators::copyByteTo_byte      )
		.register(Boolean  .class, boolean.class, BinaryValueTranslators::copyBooleanTo_boolean)
		.register(Short    .class, short  .class, BinaryValueTranslators::copyShortTo_short    )
		.register(Character.class, char   .class, BinaryValueTranslators::copyCharacterTo_char )
		.register(Integer  .class, int    .class, BinaryValueTranslators::copyIntegerTo_int    )
		.register(Float    .class, float  .class, BinaryValueTranslators::copyFloatTo_float    )
		.register(Long     .class, long   .class, BinaryValueTranslators::copyLongTo_long      )
		.register(Double   .class, double .class, BinaryValueTranslators::copyDoubleTo_double  )
		;
	}
	
	private static void registerWrappersToWrappers(final TypeMapping<BinaryValueSetter> mapping)
	{
		// (28.09.2018 TM)TODO: Legacy Type Mapping: Defaults for primitive wrapper -> primitive wrapper
	}
	
	private static void registerCommonValueTypes(final TypeMapping<BinaryValueSetter> mapping)
	{
		/* (28.09.2018 TM)TODO: Legacy Type Mapping: Defaults for common value type translation
		 * (types that have no references to other - non-unshared - instances)
		 * 
		 * Obvious:
		 * String?
		 * BigInteger?
		 * BigDecimal?
		 * 
		 * Potentially:
		 * Date stuffs?
		 * File stuffs?
		 * StringBuilder stuffs?
		 * Primitive arrays?? (char[] is obvious, but then why not the other 7?)
		 * 
		 * Hm... writing and mapping converters for 20 types (8 primitives, 8 primitive wrappers plus the above)
		 * among each other would yield a whopping 400 methods.
		 * With primitive arrays, it would be near 800.
		 * With primitive wrapper arrays, near 1300.
		 * Hm...
		 */
	}
	
	private static int to_int(final boolean value)
	{
		return value
			? 1
			: 0
		;
	}
	
	public static long skip_byte(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		return sourceAddress + XMemory.byteSize_byte();
	}
	
	public static long skip_boolean(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		return sourceAddress + XMemory.byteSize_boolean();
	}
	
	public static long skip_short(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		return sourceAddress + XMemory.byteSize_short();
	}
	
	public static long skip_char(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		return sourceAddress + XMemory.byteSize_char();
	}
	
	public static long skip_int(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		return sourceAddress + XMemory.byteSize_int();
	}
	
	public static long skip_float(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		return sourceAddress + Float.BYTES;
	}
	
	public static long skip_long(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		return sourceAddress + XMemory.byteSize_long();
	}
	
	public static long skip_double(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		return sourceAddress + XMemory.byteSize_double();
	}
		
	
	
	public static long copy_byteTo_byte(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_byte(target, targetOffset, XMemory.get_byte(sourceAddress));
		return sourceAddress + XMemory.byteSize_byte();
	}

	public static long copy_byteTo_boolean(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_boolean(target, targetOffset, 0 != XMemory.get_byte(sourceAddress));
		return sourceAddress + XMemory.byteSize_byte();
	}

	public static long copy_byteTo_short(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_short(target, targetOffset, XMemory.get_byte(sourceAddress));
		return sourceAddress + XMemory.byteSize_byte();
	}

	public static long copy_byteTo_char(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_char(target, targetOffset, (char)XMemory.get_byte(sourceAddress));
		return sourceAddress + XMemory.byteSize_byte();
	}

	public static long copy_byteTo_int(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_int(target, targetOffset, XMemory.get_byte(sourceAddress));
		return sourceAddress + XMemory.byteSize_byte();
	}

	public static long copy_byteTo_float(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_float(target, targetOffset, XMemory.get_byte(sourceAddress));
		return sourceAddress + XMemory.byteSize_byte();
	}

	public static long copy_byteTo_long(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_long(target, targetOffset, XMemory.get_byte(sourceAddress));
		return sourceAddress + XMemory.byteSize_byte();
	}

	public static long copy_byteTo_double(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_double(target, targetOffset, XMemory.get_byte(sourceAddress));
		return sourceAddress + XMemory.byteSize_byte();
	}



	public static long copy_booleanTo_byte(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_byte(target, targetOffset, (byte)to_int(XMemory.get_boolean(sourceAddress)));
		return sourceAddress + XMemory.byteSize_boolean();
	}

	public static long copy_booleanTo_boolean(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_boolean(target, targetOffset, XMemory.get_boolean(sourceAddress));
		return sourceAddress + XMemory.byteSize_boolean();
	}

	public static long copy_booleanTo_short(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_short(target, targetOffset, (short)to_int(XMemory.get_boolean(sourceAddress)));
		return sourceAddress + XMemory.byteSize_boolean();
	}

	public static long copy_booleanTo_char(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_char(target, targetOffset, (char)to_int(XMemory.get_boolean(sourceAddress)));
		return sourceAddress + XMemory.byteSize_boolean();
	}

	public static long copy_booleanTo_int(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_int(target, targetOffset, to_int(XMemory.get_boolean(sourceAddress)));
		return sourceAddress + XMemory.byteSize_boolean();
	}

	public static long copy_booleanTo_float(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_float(target, targetOffset, to_int(XMemory.get_boolean(sourceAddress)));
		return sourceAddress + XMemory.byteSize_boolean();
	}

	public static long copy_booleanTo_long(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_long(target, targetOffset, to_int(XMemory.get_boolean(sourceAddress)));
		return sourceAddress + XMemory.byteSize_boolean();
	}

	public static long copy_booleanTo_double(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_double(target, targetOffset, to_int(XMemory.get_boolean(sourceAddress)));
		return sourceAddress + XMemory.byteSize_boolean();
	}



	public static long copy_shortTo_byte(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_byte(target, targetOffset, (byte)XMemory.get_short(sourceAddress));
		return sourceAddress + XMemory.byteSize_short();
	}

	public static long copy_shortTo_boolean(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_boolean(target, targetOffset, 0 != XMemory.get_short(sourceAddress));
		return sourceAddress + XMemory.byteSize_short();
	}

	public static long copy_shortTo_short(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_short(target, targetOffset, XMemory.get_short(sourceAddress));
		return sourceAddress + XMemory.byteSize_short();
	}

	public static long copy_shortTo_char(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_char(target, targetOffset, (char)XMemory.get_short(sourceAddress));
		return sourceAddress + XMemory.byteSize_short();
	}

	public static long copy_shortTo_int(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_int(target, targetOffset, XMemory.get_short(sourceAddress));
		return sourceAddress + XMemory.byteSize_short();
	}

	public static long copy_shortTo_float(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_float(target, targetOffset, XMemory.get_short(sourceAddress));
		return sourceAddress + XMemory.byteSize_short();
	}

	public static long copy_shortTo_long(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_long(target, targetOffset, XMemory.get_short(sourceAddress));
		return sourceAddress + XMemory.byteSize_short();
	}

	public static long copy_shortTo_double(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_double(target, targetOffset, XMemory.get_short(sourceAddress));
		return sourceAddress + XMemory.byteSize_short();
	}



	public static long copy_charTo_byte(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_byte(target, targetOffset, (byte)XMemory.get_char(sourceAddress));
		return sourceAddress + XMemory.byteSize_char();
	}

	public static long copy_charTo_boolean(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_boolean(target, targetOffset, 0 != XMemory.get_char(sourceAddress));
		return sourceAddress + XMemory.byteSize_char();
	}

	public static long copy_charTo_short(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_short(target, targetOffset, (short)XMemory.get_char(sourceAddress));
		return sourceAddress + XMemory.byteSize_char();
	}

	public static long copy_charTo_char(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_char(target, targetOffset, XMemory.get_char(sourceAddress));
		return sourceAddress + XMemory.byteSize_char();
	}

	public static long copy_charTo_int(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_int(target, targetOffset, XMemory.get_char(sourceAddress));
		return sourceAddress + XMemory.byteSize_char();
	}

	public static long copy_charTo_float(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_float(target, targetOffset, XMemory.get_char(sourceAddress));
		return sourceAddress + XMemory.byteSize_char();
	}

	public static long copy_charTo_long(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_long(target, targetOffset, XMemory.get_char(sourceAddress));
		return sourceAddress + XMemory.byteSize_char();
	}

	public static long copy_charTo_double(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_double(target, targetOffset, XMemory.get_char(sourceAddress));
		return sourceAddress + XMemory.byteSize_char();
	}



	public static long copy_intTo_byte(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_byte(target, targetOffset, (byte)XMemory.get_int(sourceAddress));
		return sourceAddress + XMemory.byteSize_int();
	}

	public static long copy_intTo_boolean(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_boolean(target, targetOffset, 0 != XMemory.get_int(sourceAddress));
		return sourceAddress + XMemory.byteSize_int();
	}

	public static long copy_intTo_short(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_short(target, targetOffset, (short)XMemory.get_int(sourceAddress));
		return sourceAddress + XMemory.byteSize_int();
	}

	public static long copy_intTo_char(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_char(target, targetOffset, (char)XMemory.get_int(sourceAddress));
		return sourceAddress + XMemory.byteSize_int();
	}

	public static long copy_intTo_int(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_int(target, targetOffset, XMemory.get_int(sourceAddress));
		return sourceAddress + XMemory.byteSize_int();
	}
	
	public static long copy_intTo_float(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_float(target, targetOffset, XMemory.get_int(sourceAddress));
		return sourceAddress + XMemory.byteSize_int();
	}

	public static long copy_intTo_long(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_long(target, targetOffset, XMemory.get_int(sourceAddress));
		return sourceAddress + XMemory.byteSize_int();
	}

	public static long copy_intTo_double(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_double(target, targetOffset, XMemory.get_int(sourceAddress));
		return sourceAddress + XMemory.byteSize_int();
	}



	public static long copy_floatTo_byte(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_byte(target, targetOffset, (byte)XMemory.get_float(sourceAddress));
		return sourceAddress + Float.BYTES;
	}

	public static long copy_floatTo_boolean(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_boolean(target, targetOffset, 0 != XMemory.get_float(sourceAddress));
		return sourceAddress + Float.BYTES;
	}

	public static long copy_floatTo_short(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_short(target, targetOffset, (short)XMemory.get_float(sourceAddress));
		return sourceAddress + Float.BYTES;
	}

	public static long copy_floatTo_char(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_char(target, targetOffset, (char)XMemory.get_float(sourceAddress));
		return sourceAddress + Float.BYTES;
	}

	public static long copy_floatTo_int(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_int(target, targetOffset, (int)XMemory.get_float(sourceAddress));
		return sourceAddress + Float.BYTES;
	}

	public static long copy_floatTo_float(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_float(target, targetOffset, XMemory.get_float(sourceAddress));
		return sourceAddress + Float.BYTES;
	}

	public static long copy_floatTo_long(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_long(target, targetOffset, (long)XMemory.get_float(sourceAddress));
		return sourceAddress + Float.BYTES;
	}

	public static long copy_floatTo_double(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_double(target, targetOffset, XMemory.get_float(sourceAddress));
		return sourceAddress + Float.BYTES;
	}



	public static long copy_longTo_byte(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_byte(target, targetOffset, (byte)XMemory.get_long(sourceAddress));
		return sourceAddress + XMemory.byteSize_long();
	}

	public static long copy_longTo_boolean(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_boolean(target, targetOffset, 0 != XMemory.get_long(sourceAddress));
		return sourceAddress + XMemory.byteSize_long();
	}

	public static long copy_longTo_short(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_short(target, targetOffset, (short)XMemory.get_long(sourceAddress));
		return sourceAddress + XMemory.byteSize_long();
	}

	public static long copy_longTo_char(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_char(target, targetOffset, (char)XMemory.get_long(sourceAddress));
		return sourceAddress + XMemory.byteSize_long();
	}

	public static long copy_longTo_int(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_int(target, targetOffset, (int)XMemory.get_long(sourceAddress));
		return sourceAddress + XMemory.byteSize_long();
	}

	public static long copy_longTo_float(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_float(target, targetOffset, XMemory.get_long(sourceAddress));
		return sourceAddress + XMemory.byteSize_long();
	}

	public static long copy_longTo_long(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_long(target, targetOffset, XMemory.get_long(sourceAddress));
		return sourceAddress + XMemory.byteSize_long();
	}

	public static long copy_longTo_double(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_double(target, targetOffset, XMemory.get_long(sourceAddress));
		return sourceAddress + XMemory.byteSize_long();
	}



	public static long copy_doubleTo_byte(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_byte(target, targetOffset, (byte)XMemory.get_double(sourceAddress));
		return sourceAddress + XMemory.byteSize_double();
	}

	public static long copy_doubleTo_boolean(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_boolean(target, targetOffset, 0 != XMemory.get_double(sourceAddress));
		return sourceAddress + XMemory.byteSize_double();
	}

	public static long copy_doubleTo_short(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_short(target, targetOffset, (short)XMemory.get_double(sourceAddress));
		return sourceAddress + XMemory.byteSize_double();
	}

	public static long copy_doubleTo_char(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_char(target, targetOffset, (char)XMemory.get_double(sourceAddress));
		return sourceAddress + XMemory.byteSize_double();
	}

	public static long copy_doubleTo_int(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_int(target, targetOffset, (int)XMemory.get_double(sourceAddress));
		return sourceAddress + XMemory.byteSize_double();
	}

	public static long copy_doubleTo_float(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_float(target, targetOffset, (float)XMemory.get_double(sourceAddress));
		return sourceAddress + XMemory.byteSize_double();
	}

	public static long copy_doubleTo_long(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_long(target, targetOffset, (long)XMemory.get_double(sourceAddress));
		return sourceAddress + XMemory.byteSize_double();
	}

	public static long copy_doubleTo_double(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_double(target, targetOffset, XMemory.get_double(sourceAddress));
		return sourceAddress + XMemory.byteSize_double();
	}
	
	public static long copy_byteAsByte(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.setObject(target, targetOffset, Byte.valueOf(XMemory.get_byte(sourceAddress)));
		return sourceAddress + XMemory.byteSize_byte();
	}
	
	public static long copy_booleanAsBoolean(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.setObject(target, targetOffset, Boolean.valueOf(XMemory.get_boolean(sourceAddress)));
		return sourceAddress + XMemory.byteSize_boolean();
	}
	
	public static long copy_shortAsShort(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.setObject(target, targetOffset, Short.valueOf(XMemory.get_short(sourceAddress)));
		return sourceAddress + XMemory.byteSize_short();
	}
	
	public static long copy_charAsCharacter(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.setObject(target, targetOffset, Character.valueOf(XMemory.get_char(sourceAddress)));
		return sourceAddress + XMemory.byteSize_char();
	}
	
	public static long copy_intAsInteger(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.setObject(target, targetOffset, Integer.valueOf(XMemory.get_int(sourceAddress)));
		return sourceAddress + XMemory.byteSize_int();
	}
	
	public static long copy_floatAsFloat(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.setObject(target, targetOffset, Float.valueOf(XMemory.get_float(sourceAddress)));
		return sourceAddress + XMemory.byteSize_float();
	}
	
	public static long copy_longAsLong(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.setObject(target, targetOffset, Long.valueOf(XMemory.get_long(sourceAddress)));
		return sourceAddress + XMemory.byteSize_long();
	}
	
	public static long copy_doubleAsDouble(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.setObject(target, targetOffset, Double.valueOf(XMemory.get_double(sourceAddress)));
		return sourceAddress + XMemory.byteSize_double();
	}
	
	public static long copyByteTo_byte(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_byte(target, targetOffset, X.unbox((Byte)idResolver.lookupObject(XMemory.get_long(sourceAddress))));
		return sourceAddress + Binary.objectIdByteLength();
	}
	
	public static long copyBooleanTo_boolean(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_boolean(target, targetOffset, X.unbox((Boolean)idResolver.lookupObject(XMemory.get_long(sourceAddress))));
		return sourceAddress + Binary.objectIdByteLength();
	}
	
	public static long copyShortTo_short(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_short(target, targetOffset, X.unbox((Short)idResolver.lookupObject(XMemory.get_long(sourceAddress))));
		return sourceAddress + Binary.objectIdByteLength();
	}
	
	public static long copyCharacterTo_char(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_char(target, targetOffset, X.unbox((Character)idResolver.lookupObject(XMemory.get_long(sourceAddress))));
		return sourceAddress + Binary.objectIdByteLength();
	}
	
	public static long copyIntegerTo_int(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_int(target, targetOffset, X.unbox((Integer)idResolver.lookupObject(XMemory.get_long(sourceAddress))));
		return sourceAddress + Binary.objectIdByteLength();
	}
	
	public static long copyFloatTo_float(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_float(target, targetOffset, X.unbox((Float)idResolver.lookupObject(XMemory.get_long(sourceAddress))));
		return sourceAddress + Binary.objectIdByteLength();
	}
	
	public static long copyLongTo_long(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_long(target, targetOffset, X.unbox((Long)idResolver.lookupObject(XMemory.get_long(sourceAddress))));
		return sourceAddress + Binary.objectIdByteLength();
	}
	
	public static long copyDoubleTo_double(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_double(target, targetOffset, X.unbox((Double)idResolver.lookupObject(XMemory.get_long(sourceAddress))));
		return sourceAddress + Binary.objectIdByteLength();
	}
	
	
	
	
	///////////////////////////////////////////////////////////////////////////
	// constructors //
	/////////////////
	
	private BinaryValueTranslators()
	{
		// static only
		throw new UnsupportedOperationException();
	}
	
	
	
	
	
	private static void registerPrimitivesToPrimitivesSwitchingByteOrder(
		final TypeMapping<BinaryValueSetter> mapping
	)
	{
		/* note:
		 * these translators must do a double-switch since the actual binary-to-instance logic will
		 * switch the bytes, too.
		 * For the same reason, non-converting translators can just pass the value through without any switching.
		 */
		mapping
		.register(byte.class, byte   .class, BinaryValueTranslators::copy_byteTo_byte            )
		.register(byte.class, boolean.class, BinaryValueTranslators::copy_byteTo_boolean         )
		.register(byte.class, short  .class, BinaryValueTranslators::switchingCopy_byteTo_short  )
		.register(byte.class, char   .class, BinaryValueTranslators::switchingCopy_byteTo_char   )
		.register(byte.class, int    .class, BinaryValueTranslators::switchingCopy_byteTo_int    )
		.register(byte.class, float  .class, BinaryValueTranslators::switchingCopy_byteTo_float  )
		.register(byte.class, long   .class, BinaryValueTranslators::switchingCopy_byteTo_long   )
		.register(byte.class, double .class, BinaryValueTranslators::switchingCopy_byteTo_double )
		
		.register(boolean.class, byte   .class, BinaryValueTranslators::copy_booleanTo_byte            )
		.register(boolean.class, boolean.class, BinaryValueTranslators::copy_booleanTo_boolean         )
		.register(boolean.class, short  .class, BinaryValueTranslators::switchingCopy_booleanTo_short  )
		.register(boolean.class, char   .class, BinaryValueTranslators::switchingCopy_booleanTo_char   )
		.register(boolean.class, int    .class, BinaryValueTranslators::switchingCopy_booleanTo_int    )
		.register(boolean.class, float  .class, BinaryValueTranslators::switchingCopy_booleanTo_float  )
		.register(boolean.class, long   .class, BinaryValueTranslators::switchingCopy_booleanTo_long   )
		.register(boolean.class, double .class, BinaryValueTranslators::switchingCopy_booleanTo_double )
		
		.register(short.class, byte   .class, BinaryValueTranslators::switchingCopy_shortTo_byte   )
		.register(short.class, boolean.class, BinaryValueTranslators::switchingCopy_shortTo_boolean)
		.register(short.class, short  .class, BinaryValueTranslators::copy_shortTo_short           )
		.register(short.class, char   .class, BinaryValueTranslators::switchingCopy_shortTo_char   )
		.register(short.class, int    .class, BinaryValueTranslators::switchingCopy_shortTo_int    )
		.register(short.class, float  .class, BinaryValueTranslators::switchingCopy_shortTo_float  )
		.register(short.class, long   .class, BinaryValueTranslators::switchingCopy_shortTo_long   )
		.register(short.class, double .class, BinaryValueTranslators::switchingCopy_shortTo_double )
		
		.register(char.class, byte   .class, BinaryValueTranslators::switchingCopy_charTo_byte   )
		.register(char.class, boolean.class, BinaryValueTranslators::switchingCopy_charTo_boolean)
		.register(char.class, short  .class, BinaryValueTranslators::switchingCopy_charTo_short  )
		.register(char.class, char   .class, BinaryValueTranslators::copy_charTo_char            )
		.register(char.class, int    .class, BinaryValueTranslators::switchingCopy_charTo_int    )
		.register(char.class, float  .class, BinaryValueTranslators::switchingCopy_charTo_float  )
		.register(char.class, long   .class, BinaryValueTranslators::switchingCopy_charTo_long   )
		.register(char.class, double .class, BinaryValueTranslators::switchingCopy_charTo_double )
		
		.register(int.class, byte   .class, BinaryValueTranslators::switchingCopy_intTo_byte   )
		.register(int.class, boolean.class, BinaryValueTranslators::switchingCopy_intTo_boolean)
		.register(int.class, short  .class, BinaryValueTranslators::switchingCopy_intTo_short  )
		.register(int.class, char   .class, BinaryValueTranslators::switchingCopy_intTo_char   )
		.register(int.class, int    .class, BinaryValueTranslators::copy_intTo_int             )
		.register(int.class, float  .class, BinaryValueTranslators::switchingCopy_intTo_float  )
		.register(int.class, long   .class, BinaryValueTranslators::switchingCopy_intTo_long   )
		.register(int.class, double .class, BinaryValueTranslators::switchingCopy_intTo_double )
		
		.register(float.class, byte   .class, BinaryValueTranslators::switchingCopy_floatTo_byte   )
		.register(float.class, boolean.class, BinaryValueTranslators::switchingCopy_floatTo_boolean)
		.register(float.class, short  .class, BinaryValueTranslators::switchingCopy_floatTo_short  )
		.register(float.class, char   .class, BinaryValueTranslators::switchingCopy_floatTo_char   )
		.register(float.class, int    .class, BinaryValueTranslators::switchingCopy_floatTo_int    )
		.register(float.class, float  .class, BinaryValueTranslators::copy_floatTo_float           )
		.register(float.class, long   .class, BinaryValueTranslators::switchingCopy_floatTo_long   )
		.register(float.class, double .class, BinaryValueTranslators::switchingCopy_floatTo_double )
		
		.register(long.class, byte   .class, BinaryValueTranslators::switchingCopy_longTo_byte   )
		.register(long.class, boolean.class, BinaryValueTranslators::switchingCopy_longTo_boolean)
		.register(long.class, short  .class, BinaryValueTranslators::switchingCopy_longTo_short  )
		.register(long.class, char   .class, BinaryValueTranslators::switchingCopy_longTo_char   )
		.register(long.class, int    .class, BinaryValueTranslators::switchingCopy_longTo_int    )
		.register(long.class, float  .class, BinaryValueTranslators::switchingCopy_longTo_float  )
		.register(long.class, long   .class, BinaryValueTranslators::copy_longTo_long            )
		.register(long.class, double .class, BinaryValueTranslators::switchingCopy_longTo_double )
		
		.register(double.class, byte   .class, BinaryValueTranslators::switchingCopy_doubleTo_byte   )
		.register(double.class, boolean.class, BinaryValueTranslators::switchingCopy_doubleTo_boolean)
		.register(double.class, short  .class, BinaryValueTranslators::switchingCopy_doubleTo_short  )
		.register(double.class, char   .class, BinaryValueTranslators::switchingCopy_doubleTo_char   )
		.register(double.class, int    .class, BinaryValueTranslators::switchingCopy_doubleTo_int    )
		.register(double.class, float  .class, BinaryValueTranslators::switchingCopy_doubleTo_float  )
		.register(double.class, long   .class, BinaryValueTranslators::switchingCopy_doubleTo_long   )
		.register(double.class, double .class, BinaryValueTranslators::copy_doubleTo_double          )
		;
	}
	
	private static void registerPrimitivesToWrappersSwitchingByteOrder(final TypeMapping<BinaryValueSetter> mapping)
	{
		// (10.02.2019 TM)TODO: copy from non-switching variant
	}
	
	private static void registerWrappersToPrimitivesSwitchingByteOrder(final TypeMapping<BinaryValueSetter> mapping)
	{
		// (10.02.2019 TM)TODO: copy from non-switching variant
	}
	
	private static void registerWrappersToWrappersSwitchingByteOrder(final TypeMapping<BinaryValueSetter> mapping)
	{
		// (10.02.2019 TM)TODO: copy from non-switching variant
	}
	
	private static void registerCommonValueTypesSwitchingByteOrder(final TypeMapping<BinaryValueSetter> mapping)
	{
		// (10.02.2019 TM)TODO: copy from non-switching variant
	}
	
	public static long switchingCopy_byteTo_short(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_short(target, targetOffset, Short.reverseBytes(XMemory.get_byte(sourceAddress)));
		return sourceAddress + XMemory.byteSize_byte();
	}

	public static long switchingCopy_byteTo_char(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_char(target, targetOffset, Character.reverseBytes((char)XMemory.get_byte(sourceAddress)));
		return sourceAddress + XMemory.byteSize_byte();
	}

	public static long switchingCopy_byteTo_int(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_int(target, targetOffset, Integer.reverseBytes(XMemory.get_byte(sourceAddress)));
		return sourceAddress + XMemory.byteSize_byte();
	}

	public static long switchingCopy_byteTo_float(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_int(target, targetOffset, Integer.reverseBytes(Float.floatToRawIntBits(XMemory.get_byte(sourceAddress))));
		return sourceAddress + XMemory.byteSize_byte();
	}

	public static long switchingCopy_byteTo_long(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_long(target, targetOffset, Long.reverseBytes(XMemory.get_byte(sourceAddress)));
		return sourceAddress + XMemory.byteSize_byte();
	}

	public static long switchingCopy_byteTo_double(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_long(target, targetOffset, Long.reverseBytes(Double.doubleToRawLongBits(XMemory.get_byte(sourceAddress))));
		return sourceAddress + XMemory.byteSize_byte();
	}


	
	public static long switchingCopy_booleanTo_short(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_short(target, targetOffset, Short.reverseBytes((short)to_int(XMemory.get_boolean(sourceAddress))));
		return sourceAddress + XMemory.byteSize_boolean();
	}

	public static long switchingCopy_booleanTo_char(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_char(target, targetOffset, Character.reverseBytes((char)to_int(XMemory.get_boolean(sourceAddress))));
		return sourceAddress + XMemory.byteSize_boolean();
	}

	public static long switchingCopy_booleanTo_int(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_int(target, targetOffset, Integer.reverseBytes(to_int(XMemory.get_boolean(sourceAddress))));
		return sourceAddress + XMemory.byteSize_boolean();
	}

	public static long switchingCopy_booleanTo_float(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_int(target, targetOffset, Integer.reverseBytes(Float.floatToRawIntBits(to_int(XMemory.get_boolean(sourceAddress)))));
		return sourceAddress + XMemory.byteSize_boolean();
	}

	public static long switchingCopy_booleanTo_long(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_long(target, targetOffset, Long.reverseBytes(to_int(XMemory.get_boolean(sourceAddress))));
		return sourceAddress + XMemory.byteSize_boolean();
	}

	public static long switchingCopy_booleanTo_double(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_long(target, targetOffset, Long.reverseBytes(Double.doubleToRawLongBits(to_int(XMemory.get_boolean(sourceAddress)))));
		return sourceAddress + XMemory.byteSize_boolean();
	}



	public static long switchingCopy_shortTo_byte(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_byte(target, targetOffset, (byte)Short.reverseBytes(XMemory.get_short(sourceAddress)));
		return sourceAddress + XMemory.byteSize_short();
	}

	public static long switchingCopy_shortTo_boolean(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_boolean(target, targetOffset, 0 != Short.reverseBytes(XMemory.get_short(sourceAddress)));
		return sourceAddress + XMemory.byteSize_short();
	}

	public static long switchingCopy_shortTo_char(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_char(target, targetOffset, Character.reverseBytes((char)Short.reverseBytes(XMemory.get_short(sourceAddress))));
		return sourceAddress + XMemory.byteSize_short();
	}

	public static long switchingCopy_shortTo_int(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_int(target, targetOffset, Integer.reverseBytes(Short.reverseBytes(XMemory.get_short(sourceAddress))));
		return sourceAddress + XMemory.byteSize_short();
	}

	public static long switchingCopy_shortTo_float(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_int(target, targetOffset, Integer.reverseBytes(Float.floatToRawIntBits(Short.reverseBytes(XMemory.get_short(sourceAddress)))));
		return sourceAddress + XMemory.byteSize_short();
	}

	public static long switchingCopy_shortTo_long(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_long(target, targetOffset, Long.reverseBytes(Short.reverseBytes(XMemory.get_short(sourceAddress))));
		return sourceAddress + XMemory.byteSize_short();
	}

	public static long switchingCopy_shortTo_double(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_long(target, targetOffset, Long.reverseBytes(Double.doubleToRawLongBits(Short.reverseBytes(XMemory.get_short(sourceAddress)))));
		return sourceAddress + XMemory.byteSize_short();
	}



	public static long switchingCopy_charTo_byte(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_byte(target, targetOffset, (byte)Character.reverseBytes(XMemory.get_char(sourceAddress)));
		return sourceAddress + XMemory.byteSize_char();
	}

	public static long switchingCopy_charTo_boolean(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_boolean(target, targetOffset, 0 != Character.reverseBytes(XMemory.get_char(sourceAddress)));
		return sourceAddress + XMemory.byteSize_char();
	}

	public static long switchingCopy_charTo_short(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_short(target, targetOffset, Short.reverseBytes((short)Character.reverseBytes(XMemory.get_char(sourceAddress))));
		return sourceAddress + XMemory.byteSize_char();
	}

	public static long switchingCopy_charTo_int(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_int(target, targetOffset, Integer.reverseBytes(Character.reverseBytes(XMemory.get_char(sourceAddress))));
		return sourceAddress + XMemory.byteSize_char();
	}

	public static long switchingCopy_charTo_float(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_int(target, targetOffset, Integer.reverseBytes(Float.floatToRawIntBits(Character.reverseBytes(XMemory.get_char(sourceAddress)))));
		return sourceAddress + XMemory.byteSize_char();
	}

	public static long switchingCopy_charTo_long(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_long(target, targetOffset, Long.reverseBytes(Character.reverseBytes(XMemory.get_char(sourceAddress))));
		return sourceAddress + XMemory.byteSize_char();
	}

	public static long switchingCopy_charTo_double(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_long(target, targetOffset, Long.reverseBytes(Double.doubleToRawLongBits(Character.reverseBytes(XMemory.get_char(sourceAddress)))));
		return sourceAddress + XMemory.byteSize_char();
	}



	public static long switchingCopy_intTo_byte(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_byte(target, targetOffset, (byte)Integer.reverseBytes(XMemory.get_int(sourceAddress)));
		return sourceAddress + XMemory.byteSize_int();
	}

	public static long switchingCopy_intTo_boolean(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_boolean(target, targetOffset, 0 != Integer.reverseBytes(XMemory.get_int(sourceAddress)));
		return sourceAddress + XMemory.byteSize_int();
	}

	public static long switchingCopy_intTo_short(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_short(target, targetOffset, Short.reverseBytes((short)Integer.reverseBytes(XMemory.get_int(sourceAddress))));
		return sourceAddress + XMemory.byteSize_int();
	}

	public static long switchingCopy_intTo_char(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_char(target, targetOffset, Character.reverseBytes((char)Integer.reverseBytes(XMemory.get_int(sourceAddress))));
		return sourceAddress + XMemory.byteSize_int();
	}

	public static long switchingCopy_intTo_float(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_int(target, targetOffset, Integer.reverseBytes(Float.floatToRawIntBits(Integer.reverseBytes(XMemory.get_int(sourceAddress)))));
		return sourceAddress + XMemory.byteSize_int();
	}

	public static long switchingCopy_intTo_long(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_long(target, targetOffset, Long.reverseBytes(Integer.reverseBytes(XMemory.get_int(sourceAddress))));
		return sourceAddress + XMemory.byteSize_int();
	}

	public static long switchingCopy_intTo_double(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_long(target, targetOffset, Long.reverseBytes(Double.doubleToRawLongBits(Integer.reverseBytes(XMemory.get_int(sourceAddress)))));
		return sourceAddress + XMemory.byteSize_int();
	}



	public static long switchingCopy_floatTo_byte(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_byte(target, targetOffset, (byte)Float.intBitsToFloat(Integer.reverseBytes(XMemory.get_int(sourceAddress))));
		return sourceAddress + Float.BYTES;
	}

	public static long switchingCopy_floatTo_boolean(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_boolean(target, targetOffset, 0 != Float.intBitsToFloat(Integer.reverseBytes(XMemory.get_int(sourceAddress))));
		return sourceAddress + Float.BYTES;
	}

	public static long switchingCopy_floatTo_short(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_short(target, targetOffset, Short.reverseBytes((short)Float.intBitsToFloat(Integer.reverseBytes(XMemory.get_int(sourceAddress)))));
		return sourceAddress + Float.BYTES;
	}

	public static long switchingCopy_floatTo_char(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_char(target, targetOffset, Character.reverseBytes((char)Float.intBitsToFloat(Integer.reverseBytes(XMemory.get_int(sourceAddress)))));
		return sourceAddress + Float.BYTES;
	}

	public static long switchingCopy_floatTo_int(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_int(target, targetOffset, Integer.reverseBytes((int)Float.intBitsToFloat(Integer.reverseBytes(XMemory.get_int(sourceAddress)))));
		return sourceAddress + Float.BYTES;
	}

	public static long switchingCopy_floatTo_long(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_long(target, targetOffset, Long.reverseBytes((long)Float.intBitsToFloat(Integer.reverseBytes(XMemory.get_int(sourceAddress)))));
		return sourceAddress + Float.BYTES;
	}

	public static long switchingCopy_floatTo_double(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_long(target, targetOffset, Long.reverseBytes(Double.doubleToRawLongBits(Float.intBitsToFloat(Integer.reverseBytes(XMemory.get_int(sourceAddress))))));
		return sourceAddress + Float.BYTES;
	}



	public static long switchingCopy_longTo_byte(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_byte(target, targetOffset, (byte)Long.reverseBytes(XMemory.get_long(sourceAddress)));
		return sourceAddress + XMemory.byteSize_long();
	}

	public static long switchingCopy_longTo_boolean(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_boolean(target, targetOffset, 0 != Long.reverseBytes(XMemory.get_long(sourceAddress)));
		return sourceAddress + XMemory.byteSize_long();
	}

	public static long switchingCopy_longTo_short(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_short(target, targetOffset, Short.reverseBytes((short)Long.reverseBytes(XMemory.get_long(sourceAddress))));
		return sourceAddress + XMemory.byteSize_long();
	}

	public static long switchingCopy_longTo_char(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_char(target, targetOffset, Character.reverseBytes((char)Long.reverseBytes(XMemory.get_long(sourceAddress))));
		return sourceAddress + XMemory.byteSize_long();
	}

	public static long switchingCopy_longTo_int(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_int(target, targetOffset, Integer.reverseBytes((int)Long.reverseBytes(XMemory.get_long(sourceAddress))));
		return sourceAddress + XMemory.byteSize_long();
	}

	public static long switchingCopy_longTo_float(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_int(target, targetOffset, Integer.reverseBytes(Float.floatToRawIntBits(Long.reverseBytes(XMemory.get_long(sourceAddress)))));
		return sourceAddress + XMemory.byteSize_long();
	}

	public static long switchingCopy_longTo_double(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_long(target, targetOffset, Long.reverseBytes(Double.doubleToRawLongBits(Long.reverseBytes(XMemory.get_long(sourceAddress)))));
		return sourceAddress + XMemory.byteSize_long();
	}



	public static long switchingCopy_doubleTo_byte(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_byte(target, targetOffset, (byte)Double.longBitsToDouble(Long.reverseBytes(XMemory.get_long(sourceAddress))));
		return sourceAddress + XMemory.byteSize_double();
	}

	public static long switchingCopy_doubleTo_boolean(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_boolean(target, targetOffset, 0 != Double.longBitsToDouble(Long.reverseBytes(XMemory.get_long(sourceAddress))));
		return sourceAddress + XMemory.byteSize_double();
	}

	public static long switchingCopy_doubleTo_short(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_short(target, targetOffset, Short.reverseBytes((short)Double.longBitsToDouble(Long.reverseBytes(XMemory.get_long(sourceAddress)))));
		return sourceAddress + XMemory.byteSize_double();
	}

	public static long switchingCopy_doubleTo_char(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_char(target, targetOffset, Character.reverseBytes((char)Double.longBitsToDouble(Long.reverseBytes(XMemory.get_long(sourceAddress)))));
		return sourceAddress + XMemory.byteSize_double();
	}

	public static long switchingCopy_doubleTo_int(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_int(target, targetOffset, Integer.reverseBytes((int)Double.longBitsToDouble(Long.reverseBytes(XMemory.get_long(sourceAddress)))));
		return sourceAddress + XMemory.byteSize_double();
	}

	public static long switchingCopy_doubleTo_float(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_int(target, targetOffset, Integer.reverseBytes(Float.floatToRawIntBits((float)Double.longBitsToDouble(Long.reverseBytes(XMemory.get_long(sourceAddress))))));
		return sourceAddress + XMemory.byteSize_double();
	}

	public static long switchingCopy_doubleTo_long(
		final long                        sourceAddress,
		final Object                      target       ,
		final long                        targetOffset ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		XMemory.set_long(target, targetOffset, Long.reverseBytes((long)Double.longBitsToDouble(Long.reverseBytes(XMemory.get_long(sourceAddress)))));
		return sourceAddress + XMemory.byteSize_double();
	}
	
}
