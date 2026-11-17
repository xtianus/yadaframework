package net.yadaframework.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import net.yadaframework.exceptions.YadaInternalException;
import net.yadaframework.exceptions.YadaInvalidUsageException;
import net.yadaframework.exceptions.YadaInvalidValueException;
import net.yadaframework.persistence.YadaSavedPropertyCodec;
import net.yadaframework.persistence.YadaSavedPropertyKey;
import net.yadaframework.persistence.entity.YadaSavedProperty;

/**
 * Stores and retrieves scoped saved properties through their typed keys.
 */
@Repository
@Transactional(readOnly = true)
public class YadaSavedPropertyDao {
	private static final String FIND_QUERY = "select savedProperty from YadaSavedProperty savedProperty where savedProperty.applicationName = :applicationName and savedProperty.name = :name";

	@PersistenceContext
	private EntityManager entityManager;

	/**
	 * Finds and decodes the value stored for a typed key.
	 * @param key the typed property key
	 * @return the decoded value, or an empty optional when no row exists
	 * @param <T> the property value type
	 */
	public <T> Optional<T> findValue(YadaSavedPropertyKey<T> key) {
		Optional<YadaSavedProperty> savedProperty = findEntity(key);
		if (savedProperty.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(decode(savedProperty.get(), key));
	}

	/**
	 * Gets a stored value or the key default when no row exists.
	 * @param key the typed property key
	 * @return the stored or default value
	 * @param <T> the property value type
	 * @throws YadaInvalidUsageException when neither a row nor a default exists
	 */
	public <T> T getValue(YadaSavedPropertyKey<T> key) {
		Optional<T> storedValue = findValue(key);
		if (storedValue.isPresent()) {
			return storedValue.get();
		}
		return key.getDefaultValue().orElseThrow(() -> new YadaInvalidUsageException(
			"No saved property or default exists for application '{}' and name '{}'", key.getApplicationName(), key.getName()));
	}

	/**
	 * Inserts a missing property or updates the value of its managed row.
	 * @param key the typed property key
	 * @param value the non-null value to store
	 * @param <T> the property value type
	 * @throws YadaInvalidValueException when the value is null or the stored logical type differs
	 */
	@Transactional(readOnly = false)
	public <T> void setValue(YadaSavedPropertyKey<T> key, T value) {
		if (value == null) {
			throw new YadaInvalidValueException("Null saved property value is not allowed for application '{}' and name '{}'", key.getApplicationName(), key.getName());
		}
		Optional<YadaSavedProperty> savedProperty = findEntity(key);
		if (savedProperty.isPresent()) {
			YadaSavedProperty managedProperty = savedProperty.get();
			validateType(managedProperty, key);
			managedProperty.setValue(serialize(key, value));
			return;
		}
		String serializedValue = serialize(key, value);
		entityManager.persist(new YadaSavedProperty(key.getApplicationName(), key.getName(), key.getCodec().getType(), serializedValue));
	}

	/**
	 * Deletes the managed row identified by a key.
	 * @param key the property key
	 * @return true when a row was removed, otherwise false
	 */
	@Transactional(readOnly = false)
	public boolean delete(YadaSavedPropertyKey<?> key) {
		Optional<YadaSavedProperty> savedProperty = findEntity(key);
		if (savedProperty.isEmpty()) {
			return false;
		}
		entityManager.remove(savedProperty.get());
		return true;
	}

	/**
	 * Loads a row by its complete normalized key and detects duplicate storage corruption.
	 * @param key the property key
	 * @return the managed row, if present
	 */
	private Optional<YadaSavedProperty> findEntity(YadaSavedPropertyKey<?> key) {
		List<YadaSavedProperty> matches = entityManager.createQuery(FIND_QUERY, YadaSavedProperty.class)
			.setParameter("applicationName", key.getApplicationName())
			.setParameter("name", key.getName())
			.getResultList();
		if (matches.size() > 1) {
			throw new YadaInternalException("Multiple saved properties exist for application '{}' and name '{}'", key.getApplicationName(), key.getName());
		}
		return matches.stream().findFirst();
	}

	/**
	 * Decodes one row after validating its logical and Java types.
	 * @param savedProperty the stored row
	 * @param key the typed property key
	 * @return the decoded value
	 * @param <T> the property value type
	 */
	private <T> T decode(YadaSavedProperty savedProperty, YadaSavedPropertyKey<T> key) {
		validateType(savedProperty, key);
		YadaSavedPropertyCodec<T> codec = key.getCodec();
		Object decodedValue;
		try {
			decodedValue = codec.deserialize(savedProperty.getValue());
		} catch (RuntimeException exception) {
			throw new YadaInvalidValueException(exception, "Malformed saved property storage for application '{}' and name '{}'", key.getApplicationName(), key.getName());
		}
		if (!codec.getJavaType().isInstance(decodedValue)) {
			throw new YadaInvalidValueException("Saved property storage has an invalid Java type for application '{}' and name '{}'", key.getApplicationName(), key.getName());
		}
		return codec.getJavaType().cast(decodedValue);
	}

	/**
	 * Verifies that a stored row retains the logical type declared by its key.
	 * @param savedProperty the stored row
	 * @param key the property key
	 */
	private void validateType(YadaSavedProperty savedProperty, YadaSavedPropertyKey<?> key) {
		if (savedProperty.getType() != key.getCodec().getType()) {
			throw new YadaInvalidValueException(
				"Saved property type mismatch for application '{}' and name '{}': stored {}, requested {}",
				key.getApplicationName(), key.getName(), savedProperty.getType(), key.getCodec().getType());
		}
	}

	/**
	 * Serializes a value and rejects codecs that return null text.
	 * @param key the typed property key
	 * @param value the value to serialize
	 * @return the canonical storage text
	 * @param <T> the property value type
	 */
	private <T> String serialize(YadaSavedPropertyKey<T> key, T value) {
		String serializedValue = key.getCodec().serialize(value);
		if (serializedValue == null) {
			throw new YadaInvalidValueException("Saved property codec returned null for application '{}' and name '{}'", key.getApplicationName(), key.getName());
		}
		return serializedValue;
	}
}
