package net.yadaframework.persistence;

import java.util.Objects;
import java.util.Optional;

import net.yadaframework.exceptions.YadaInvalidValueException;

/**
 * Declares an immutable, scoped, typed saved-property key.
 * @param <T> the property value type
 */
public final class YadaSavedPropertyKey<T> {
	public static final String DEFAULT_APPLICATION_NAME = "default";
	private static final int MAX_APPLICATION_NAME_LENGTH = 64;
	private static final int MAX_NAME_LENGTH = 191;

	private final String applicationName;
	private final String name;
	private final YadaSavedPropertyCodec<T> codec;
	private final Optional<T> defaultValue;

	/**
	 * Creates a validated saved-property key.
	 * @param applicationName the application scope
	 * @param name the property name
	 * @param codec the property codec
	 * @param defaultValue the optional default value
	 */
	private YadaSavedPropertyKey(String applicationName, String name, YadaSavedPropertyCodec<T> codec, Optional<T> defaultValue) {
		this.applicationName = normalizeApplicationName(applicationName);
		this.name = normalizeName(name);
		this.codec = Objects.requireNonNull(codec, "codec must not be null");
		this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue must not be null");
	}

	/**
	 * Creates a key without a default value.
	 * @param applicationName the application scope, or blank for the default scope
	 * @param name the property name
	 * @param codec the property codec
	 * @return the typed key
	 * @param <T> the property value type
	 */
	public static <T> YadaSavedPropertyKey<T> of(String applicationName, String name, YadaSavedPropertyCodec<T> codec) {
		return new YadaSavedPropertyKey<>(applicationName, name, codec, Optional.empty());
	}

	/**
	 * Creates a key with a non-null default value.
	 * @param applicationName the application scope, or blank for the default scope
	 * @param name the property name
	 * @param codec the property codec
	 * @param defaultValue the value used when no property is stored
	 * @return the typed key
	 * @param <T> the property value type
	 */
	public static <T> YadaSavedPropertyKey<T> withDefault(String applicationName, String name, YadaSavedPropertyCodec<T> codec, T defaultValue) {
		return new YadaSavedPropertyKey<>(applicationName, name, codec, Optional.of(Objects.requireNonNull(defaultValue, "defaultValue must not be null")));
	}

	/**
	 * Normalizes a nullable application scope and enforces its storage limit.
	 * @param applicationName the application scope
	 * @return the trimmed scope or {@link #DEFAULT_APPLICATION_NAME}
	 */
	public static String normalizeApplicationName(String applicationName) {
		String normalized = applicationName == null ? "" : applicationName.strip();
		if (normalized.isEmpty()) {
			return DEFAULT_APPLICATION_NAME;
		}
		if (normalized.length() > MAX_APPLICATION_NAME_LENGTH) {
			throw new YadaInvalidValueException("Saved property application name exceeds {} characters: {}", MAX_APPLICATION_NAME_LENGTH, normalized);
		}
		return normalized;
	}

	/**
	 * Normalizes and validates a property name.
	 * @param name the property name
	 * @return the trimmed property name
	 */
	private static String normalizeName(String name) {
		String normalized = name == null ? "" : name.strip();
		if (normalized.isEmpty()) {
			throw new YadaInvalidValueException("Saved property name must not be blank");
		}
		if (normalized.length() > MAX_NAME_LENGTH) {
			throw new YadaInvalidValueException("Saved property name exceeds {} characters: {}", MAX_NAME_LENGTH, normalized);
		}
		return normalized;
	}

	/**
	 * Returns the normalized application scope.
	 * @return the normalized application scope
	 */
	public String getApplicationName() {
		return applicationName;
	}

	/**
	 * Returns the case-sensitive property name.
	 * @return the property name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the codec that defines the property's type and representation.
	 * @return the property codec
	 */
	public YadaSavedPropertyCodec<T> getCodec() {
		return codec;
	}

	/**
	 * Returns the optional default value.
	 * @return the declared default, if present
	 */
	public Optional<T> getDefaultValue() {
		return defaultValue;
	}
}
