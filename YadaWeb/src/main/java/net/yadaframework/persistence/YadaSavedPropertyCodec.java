package net.yadaframework.persistence;

/**
 * Converts a typed saved-property value to and from canonical text.
 * @param <T> the Java value type
 */
public interface YadaSavedPropertyCodec<T> {

	/**
	 * Returns the Java type accepted and produced by this codec.
	 * @return the Java value type
	 */
	Class<T> getJavaType();

	/**
	 * Returns the logical storage type used by this codec.
	 * @return the logical storage type
	 */
	YadaSavedPropertyTypeEnum getType();

	/**
	 * Serializes a typed value to canonical text.
	 * @param value the value to serialize
	 * @return the canonical text
	 */
	String serialize(T value);

	/**
	 * Deserializes canonical text to its typed value.
	 * @param value the text to deserialize
	 * @return the typed value
	 */
	T deserialize(String value);
}
