package net.yadaframework.persistence.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import net.yadaframework.exceptions.YadaInvalidValueException;
import net.yadaframework.persistence.YadaSavedPropertyTypeEnum;

/**
 * Verifies saved-property entity defaults, normalization, state, and validation.
 */
class YadaSavedPropertyTest {

	/**
	 * Verifies the JPA constructor defaults and diagnostic fields.
	 */
	@Test
	void defaultConstructorExposesInitialState() {
		YadaSavedProperty savedProperty = new YadaSavedProperty();

		assertEquals("default", savedProperty.getApplicationName());
		assertNull(savedProperty.getName());
		assertNull(savedProperty.getType());
		assertNull(savedProperty.getValue());
		assertNull(savedProperty.getId());
		assertEquals(0, savedProperty.getVersion());
	}

	/**
	 * Verifies constructor normalization while retaining semantic values.
	 */
	@Test
	void constructorNormalizesIdentifiersAndRetainsValues() {
		YadaSavedProperty defaultProperty = new YadaSavedProperty(null, "  Feature.Enabled  ", YadaSavedPropertyTypeEnum.BOOLEAN, "true");
		YadaSavedProperty scopedProperty = new YadaSavedProperty("  My-Application  ", "  Usage.Counter  ", YadaSavedPropertyTypeEnum.LONG, "42");

		assertEquals("default", defaultProperty.getApplicationName());
		assertEquals("feature.enabled", defaultProperty.getName());
		assertSame(YadaSavedPropertyTypeEnum.BOOLEAN, defaultProperty.getType());
		assertEquals("true", defaultProperty.getValue());
		assertEquals("my-application", scopedProperty.getApplicationName());
		assertEquals("usage.counter", scopedProperty.getName());
		assertSame(YadaSavedPropertyTypeEnum.LONG, scopedProperty.getType());
		assertEquals("42", scopedProperty.getValue());
	}

	/**
	 * Verifies identifier setter normalization and semantic value retention.
	 */
	@Test
	void settersNormalizeIdentifiersAndRetainValues() {
		YadaSavedProperty savedProperty = new YadaSavedProperty();

		savedProperty.setApplicationName(" \t ");
		savedProperty.setName("  Display.Name  ");
		savedProperty.setType(YadaSavedPropertyTypeEnum.STRING);
		savedProperty.setValue("  retained value  ");

		assertEquals("default", savedProperty.getApplicationName());
		assertEquals("display.name", savedProperty.getName());
		assertSame(YadaSavedPropertyTypeEnum.STRING, savedProperty.getType());
		assertEquals("  retained value  ", savedProperty.getValue());

		savedProperty.setApplicationName("  Another-Application  ");
		assertEquals("another-application", savedProperty.getApplicationName());
	}

	/**
	 * Verifies that the lifecycle callback itself re-normalizes identifier state that bypassed setters.
	 * The callback is invoked directly because this is a plain unit test: it cannot verify that Hibernate
	 * includes a field first modified inside {@code @PreUpdate} in the generated UPDATE statement, since
	 * dirty state is computed before the callback runs. The callback is therefore only a safety net for
	 * state populated without setters; every framework write path normalizes through the setters instead.
	 * @throws ReflectiveOperationException when an expected persistence field cannot be changed
	 */
	@Test
	void lifecycleRenormalizesIdentifiersThatBypassedSetters() throws ReflectiveOperationException {
		YadaSavedProperty savedProperty = new YadaSavedProperty(null, "feature.enabled", YadaSavedPropertyTypeEnum.BOOLEAN, "false");
		setField(savedProperty, "applicationName", "  My-Application  ");
		setField(savedProperty, "name", "  Feature.Enabled  ");

		savedProperty.normalizeAndValidate();

		assertEquals("my-application", savedProperty.getApplicationName());
		assertEquals("feature.enabled", savedProperty.getName());
	}

	/**
	 * Verifies that setters and lifecycle validation reject incomplete rows.
	 * @throws ReflectiveOperationException when an expected persistence field cannot be changed
	 */
	@Test
	void incompleteStateFailsLifecycleValidation() throws ReflectiveOperationException {
		YadaSavedProperty savedProperty = new YadaSavedProperty();

		assertThrows(YadaInvalidValueException.class, savedProperty::normalizeAndValidate);
		assertThrows(YadaInvalidValueException.class, () -> savedProperty.setName(" \t "));
		setField(savedProperty, "name", " \t ");
		savedProperty.setType(YadaSavedPropertyTypeEnum.STRING);
		savedProperty.setValue("");
		assertThrows(YadaInvalidValueException.class, savedProperty::normalizeAndValidate);
		savedProperty.setName("name");
		savedProperty.setType(null);
		assertThrows(YadaInvalidValueException.class, savedProperty::normalizeAndValidate);
		savedProperty.setType(YadaSavedPropertyTypeEnum.STRING);
		savedProperty.setValue(null);
		assertThrows(YadaInvalidValueException.class, savedProperty::normalizeAndValidate);
	}

	/**
	 * Changes a persistence field to simulate state populated without entity setters.
	 * @param savedProperty the entity to change
	 * @param fieldName the persistence field name
	 * @param value the raw field value
	 * @throws ReflectiveOperationException when the field cannot be changed
	 */
	private void setField(YadaSavedProperty savedProperty, String fieldName, String value) throws ReflectiveOperationException {
		Field field = YadaSavedProperty.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(savedProperty, value);
	}
}
