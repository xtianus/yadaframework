package net.yadaframework.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.function.Function;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.yadaframework.exceptions.YadaInvalidValueException;

/**
 * Provides canonical codecs for the supported saved-property logical types.
 */
public final class YadaSavedPropertyCodecs {
	private static final YadaSavedPropertyCodec<String> STRING_CODEC = codec(String.class, YadaSavedPropertyTypeEnum.STRING, Function.identity(), Function.identity());
	private static final YadaSavedPropertyCodec<Boolean> BOOLEAN_CODEC = codec(Boolean.class, YadaSavedPropertyTypeEnum.BOOLEAN, String::valueOf, YadaSavedPropertyCodecs::parseBoolean);
	private static final YadaSavedPropertyCodec<Long> LONG_CODEC = codec(Long.class, YadaSavedPropertyTypeEnum.LONG, String::valueOf, Long::valueOf);
	private static final YadaSavedPropertyCodec<BigDecimal> DECIMAL_CODEC = codec(BigDecimal.class, YadaSavedPropertyTypeEnum.DECIMAL, BigDecimal::toPlainString, BigDecimal::new);
	private static final YadaSavedPropertyCodec<Instant> INSTANT_CODEC = codec(Instant.class, YadaSavedPropertyTypeEnum.INSTANT, DateTimeFormatter.ISO_INSTANT::format, value -> Instant.from(DateTimeFormatter.ISO_INSTANT.parse(value)));

	/**
	 * Prevents utility-class instantiation.
	 */
	private YadaSavedPropertyCodecs() {
	}

	/**
	 * Returns the canonical string codec.
	 * @return the string codec
	 */
	public static YadaSavedPropertyCodec<String> stringCodec() {
		return STRING_CODEC;
	}

	/**
	 * Returns the strict lowercase boolean codec.
	 * @return the boolean codec
	 */
	public static YadaSavedPropertyCodec<Boolean> booleanCodec() {
		return BOOLEAN_CODEC;
	}

	/**
	 * Returns the base-ten long codec.
	 * @return the long codec
	 */
	public static YadaSavedPropertyCodec<Long> longCodec() {
		return LONG_CODEC;
	}

	/**
	 * Returns the plain-text decimal codec.
	 * @return the decimal codec
	 */
	public static YadaSavedPropertyCodec<BigDecimal> decimalCodec() {
		return DECIMAL_CODEC;
	}

	/**
	 * Returns the ISO instant codec.
	 * @return the instant codec
	 */
	public static YadaSavedPropertyCodec<Instant> instantCodec() {
		return INSTANT_CODEC;
	}

	/**
	 * Creates a name-based codec restricted to one enum type.
	 * @param enumType the enum class
	 * @return the enum codec
	 * @param <E> the enum type
	 */
	public static <E extends Enum<E>> YadaSavedPropertyCodec<E> enumCodec(Class<E> enumType) {
		Class<E> checkedType = Objects.requireNonNull(enumType, "enumType must not be null");
		return codec(checkedType, YadaSavedPropertyTypeEnum.ENUM, Enum::name, value -> Enum.valueOf(checkedType, value));
	}

	/**
	 * Creates a JSON codec restricted to an explicit target class and caller-supplied mapper.
	 * @param javaType the target class
	 * @param objectMapper the mapper used for JSON conversion
	 * @return the explicitly typed JSON codec
	 * @param <T> the JSON value type
	 */
	public static <T> YadaSavedPropertyCodec<T> jsonCodec(Class<T> javaType, ObjectMapper objectMapper) {
		Class<T> checkedType = Objects.requireNonNull(javaType, "javaType must not be null");
		ObjectMapper checkedMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
		return codec(checkedType, YadaSavedPropertyTypeEnum.JSON, value -> serializeJson(value, checkedMapper), value -> deserializeJson(value, checkedType, checkedMapper));
	}

	/**
	 * Creates a codec from canonical serializer and deserializer functions.
	 * @param javaType the Java value type
	 * @param type the logical storage type
	 * @param serializer the canonical serializer
	 * @param deserializer the strict deserializer
	 * @return the codec
	 * @param <T> the Java value type
	 */
	private static <T> YadaSavedPropertyCodec<T> codec(Class<T> javaType, YadaSavedPropertyTypeEnum type, Function<T, String> serializer, Function<String, T> deserializer) {
		return new YadaSavedPropertyCodec<>() {
			/**
			 * {@inheritDoc}
			 */
			@Override
			public Class<T> getJavaType() {
				return javaType;
			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			public YadaSavedPropertyTypeEnum getType() {
				return type;
			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			public String serialize(T value) {
				return serializer.apply(value);
			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			public T deserialize(String value) {
				try {
					return deserializer.apply(value);
				} catch (RuntimeException exception) {
					throw new YadaInvalidValueException(exception, "Invalid {} saved property value: {}", type, value);
				}
			}
		};
	}

	/**
	 * Parses only the two lowercase canonical boolean values.
	 * @param value the text to parse
	 * @return the parsed boolean
	 */
	private static Boolean parseBoolean(String value) {
		if ("true".equals(value)) {
			return Boolean.TRUE;
		}
		if ("false".equals(value)) {
			return Boolean.FALSE;
		}
		throw new IllegalArgumentException("Boolean text must be true or false");
	}

	/**
	 * Serializes one value with the caller-supplied JSON mapper.
	 * @param value the value to serialize
	 * @param objectMapper the JSON mapper
	 * @return the JSON text
	 * @param <T> the JSON value type
	 */
	private static <T> String serializeJson(T value, ObjectMapper objectMapper) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (JsonProcessingException exception) {
			throw new YadaInvalidValueException(exception, "Unable to serialize JSON saved property value");
		}
	}

	/**
	 * Deserializes JSON to the explicitly declared target class.
	 * @param value the JSON text
	 * @param javaType the target class
	 * @param objectMapper the JSON mapper
	 * @return the typed JSON value
	 * @param <T> the JSON value type
	 */
	private static <T> T deserializeJson(String value, Class<T> javaType, ObjectMapper objectMapper) {
		try {
			return objectMapper.readValue(value, javaType);
		} catch (JsonProcessingException exception) {
			throw new IllegalArgumentException(exception);
		}
	}
}
