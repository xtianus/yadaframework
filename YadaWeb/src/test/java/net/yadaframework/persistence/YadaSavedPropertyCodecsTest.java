package net.yadaframework.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.yadaframework.exceptions.YadaInvalidValueException;

/**
 * Verifies canonical saved-property codecs and their strict parsing behavior.
 */
class YadaSavedPropertyCodecsTest {

	/**
	 * Verifies that strings round-trip without transformation.
	 */
	@Test
	void stringCodecRoundTripsCanonicalText() {
		YadaSavedPropertyCodec<String> codec = YadaSavedPropertyCodecs.stringCodec();

		assertCodecMetadata(codec, String.class, YadaSavedPropertyTypeEnum.STRING);
		assertEquals("Saved value", codec.serialize("Saved value"));
		assertEquals("Saved value", codec.deserialize("Saved value"));
	}

	/**
	 * Verifies that booleans use strict lowercase canonical text.
	 */
	@Test
	void booleanCodecUsesLowercaseCanonicalText() {
		YadaSavedPropertyCodec<Boolean> codec = YadaSavedPropertyCodecs.booleanCodec();

		assertCodecMetadata(codec, Boolean.class, YadaSavedPropertyTypeEnum.BOOLEAN);
		assertEquals("true", codec.serialize(Boolean.TRUE));
		assertEquals("false", codec.serialize(Boolean.FALSE));
		assertEquals(Boolean.TRUE, codec.deserialize("true"));
		assertEquals(Boolean.FALSE, codec.deserialize("false"));
	}

	/**
	 * Verifies that long values round-trip in base-ten form.
	 */
	@Test
	void longCodecRoundTripsCanonicalText() {
		YadaSavedPropertyCodec<Long> codec = YadaSavedPropertyCodecs.longCodec();

		assertCodecMetadata(codec, Long.class, YadaSavedPropertyTypeEnum.LONG);
		assertEquals("-9223372036854775808", codec.serialize(Long.MIN_VALUE));
		assertEquals(Long.MIN_VALUE, codec.deserialize("-9223372036854775808"));
	}

	/**
	 * Verifies that decimals use plain notation without an exponent.
	 */
	@Test
	void decimalCodecUsesPlainCanonicalText() {
		YadaSavedPropertyCodec<BigDecimal> codec = YadaSavedPropertyCodecs.decimalCodec();
		BigDecimal value = new BigDecimal("1234567890.0012300");

		assertCodecMetadata(codec, BigDecimal.class, YadaSavedPropertyTypeEnum.DECIMAL);
		assertEquals(value.toPlainString(), codec.serialize(value));
		assertEquals(value, codec.deserialize(value.toPlainString()));
	}

	/**
	 * Verifies that instants use the ISO instant formatter.
	 */
	@Test
	void instantCodecUsesIsoInstantCanonicalText() {
		YadaSavedPropertyCodec<Instant> codec = YadaSavedPropertyCodecs.instantCodec();
		Instant value = Instant.parse("2026-08-25T10:15:30.123456Z");
		String expected = DateTimeFormatter.ISO_INSTANT.format(value);

		assertCodecMetadata(codec, Instant.class, YadaSavedPropertyTypeEnum.INSTANT);
		assertEquals(expected, codec.serialize(value));
		assertEquals(value, codec.deserialize(expected));
	}

	/**
	 * Verifies that enum constants use their names rather than ordinals.
	 */
	@Test
	void enumCodecUsesConstantName() {
		YadaSavedPropertyCodec<YadaSavedPropertyTestEnum> codec = YadaSavedPropertyCodecs.enumCodec(YadaSavedPropertyTestEnum.class);

		assertCodecMetadata(codec, YadaSavedPropertyTestEnum.class, YadaSavedPropertyTypeEnum.ENUM);
		assertEquals("SECOND_VALUE", codec.serialize(YadaSavedPropertyTestEnum.SECOND_VALUE));
		assertEquals(YadaSavedPropertyTestEnum.SECOND_VALUE, codec.deserialize("SECOND_VALUE"));
		assertFalse(codec.serialize(YadaSavedPropertyTestEnum.SECOND_VALUE).matches("\\d+"));
	}

	/**
	 * Verifies that malformed scalar and enum values use the framework exception.
	 */
	@Test
	void malformedValuesRaiseYadaInvalidValueException() {
		assertMalformed(YadaSavedPropertyCodecs.booleanCodec(), "TRUE", YadaSavedPropertyTypeEnum.BOOLEAN);
		assertMalformed(YadaSavedPropertyCodecs.longCodec(), "12.5", YadaSavedPropertyTypeEnum.LONG);
		assertMalformed(YadaSavedPropertyCodecs.decimalCodec(), "not-a-number", YadaSavedPropertyTypeEnum.DECIMAL);
		assertMalformed(YadaSavedPropertyCodecs.instantCodec(), "25 August 2026", YadaSavedPropertyTypeEnum.INSTANT);
		assertMalformed(YadaSavedPropertyCodecs.enumCodec(YadaSavedPropertyTestEnum.class), "MISSING", YadaSavedPropertyTypeEnum.ENUM);
	}

	/**
	 * Verifies explicitly typed JSON round trips and malformed JSON handling.
	 */
	@Test
	void jsonCodecUsesCallerSuppliedMapperAndExplicitType() {
		ObjectMapper objectMapper = new ObjectMapper();
		YadaSavedPropertyCodec<YadaSavedPropertyJsonFixture> codec = YadaSavedPropertyCodecs.jsonCodec(YadaSavedPropertyJsonFixture.class, objectMapper);
		YadaSavedPropertyJsonFixture value = new YadaSavedPropertyJsonFixture();
		value.name = "fixture";
		value.count = 7;

		assertCodecMetadata(codec, YadaSavedPropertyJsonFixture.class, YadaSavedPropertyTypeEnum.JSON);
		String encoded = codec.serialize(value);
		YadaSavedPropertyJsonFixture decoded = codec.deserialize(encoded);
		assertEquals(value.name, decoded.name);
		assertEquals(value.count, decoded.count);
		assertMalformed(codec, "{malformed", YadaSavedPropertyTypeEnum.JSON);
	}

	/**
	 * Verifies that encoded values never include Java target class names.
	 */
	@Test
	void encodedValuesDoNotStoreJavaClassNames() {
		YadaSavedPropertyJsonFixture fixture = new YadaSavedPropertyJsonFixture();
		fixture.name = "safe";
		fixture.count = 1;
		List<String> encodedValues = List.of(
			YadaSavedPropertyCodecs.stringCodec().serialize("value"),
			YadaSavedPropertyCodecs.booleanCodec().serialize(Boolean.TRUE),
			YadaSavedPropertyCodecs.longCodec().serialize(1L),
			YadaSavedPropertyCodecs.decimalCodec().serialize(BigDecimal.ONE),
			YadaSavedPropertyCodecs.instantCodec().serialize(Instant.EPOCH),
			YadaSavedPropertyCodecs.enumCodec(YadaSavedPropertyTestEnum.class).serialize(YadaSavedPropertyTestEnum.FIRST_VALUE),
			YadaSavedPropertyCodecs.jsonCodec(YadaSavedPropertyJsonFixture.class, new ObjectMapper()).serialize(fixture));

		for (String encodedValue : encodedValues) {
			assertFalse(encodedValue.contains(YadaSavedPropertyTestEnum.class.getName()));
			assertFalse(encodedValue.contains(YadaSavedPropertyJsonFixture.class.getName()));
			assertFalse(encodedValue.contains(YadaSavedPropertyJsonFixture.class.getSimpleName()));
		}
	}

	/**
	 * Verifies a codec's declared Java and logical types.
	 * @param codec the codec to inspect
	 * @param javaType the expected Java type
	 * @param type the expected logical type
	 */
	private <T> void assertCodecMetadata(YadaSavedPropertyCodec<T> codec, Class<T> javaType, YadaSavedPropertyTypeEnum type) {
		assertSame(javaType, codec.getJavaType());
		assertSame(type, codec.getType());
	}

	/**
	 * Verifies that malformed text reports its logical type and original text.
	 * @param codec the codec to exercise
	 * @param value the malformed text
	 * @param type the expected logical type
	 */
	private void assertMalformed(YadaSavedPropertyCodec<?> codec, String value, YadaSavedPropertyTypeEnum type) {
		YadaInvalidValueException exception = assertThrows(YadaInvalidValueException.class, () -> codec.deserialize(value));
		assertTrue(exception.getMessage().contains(type.name()));
		assertTrue(exception.getMessage().contains(value));
	}

	/**
	 * Enum fixture for verifying name-based enum storage.
	 */
	enum YadaSavedPropertyTestEnum {
		FIRST_VALUE,
		SECOND_VALUE
	}

	/**
	 * Explicit JSON target used to verify restricted deserialization.
	 */
	public static class YadaSavedPropertyJsonFixture {
		public String name;
		public int count;
	}
}
