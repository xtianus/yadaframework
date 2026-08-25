package net.yadaframework.persistence.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
	void constructorNormalizesScopeAndRetainsValues() {
		YadaSavedProperty defaultProperty = new YadaSavedProperty(null, "feature.enabled", YadaSavedPropertyTypeEnum.BOOLEAN, "true");
		YadaSavedProperty scopedProperty = new YadaSavedProperty("  my-application  ", "counter", YadaSavedPropertyTypeEnum.LONG, "42");

		assertEquals("default", defaultProperty.getApplicationName());
		assertEquals("feature.enabled", defaultProperty.getName());
		assertSame(YadaSavedPropertyTypeEnum.BOOLEAN, defaultProperty.getType());
		assertEquals("true", defaultProperty.getValue());
		assertEquals("my-application", scopedProperty.getApplicationName());
		assertEquals("counter", scopedProperty.getName());
		assertSame(YadaSavedPropertyTypeEnum.LONG, scopedProperty.getType());
		assertEquals("42", scopedProperty.getValue());
	}

	/**
	 * Verifies setter normalization and semantic value retention.
	 */
	@Test
	void settersNormalizeScopeAndRetainValues() {
		YadaSavedProperty savedProperty = new YadaSavedProperty();

		savedProperty.setApplicationName(" \t ");
		savedProperty.setName("Display.Name");
		savedProperty.setType(YadaSavedPropertyTypeEnum.STRING);
		savedProperty.setValue("  retained value  ");

		assertEquals("default", savedProperty.getApplicationName());
		assertEquals("Display.Name", savedProperty.getName());
		assertSame(YadaSavedPropertyTypeEnum.STRING, savedProperty.getType());
		assertEquals("  retained value  ", savedProperty.getValue());

		savedProperty.setApplicationName("  another-application  ");
		assertEquals("another-application", savedProperty.getApplicationName());
	}

	/**
	 * Verifies that lifecycle validation accepts a complete row.
	 */
	@Test
	void completeStatePassesLifecycleValidation() {
		YadaSavedProperty savedProperty = new YadaSavedProperty(null, "feature.enabled", YadaSavedPropertyTypeEnum.BOOLEAN, "false");

		savedProperty.normalizeAndValidate();

		assertEquals("default", savedProperty.getApplicationName());
	}

	/**
	 * Verifies that lifecycle validation rejects incomplete rows.
	 */
	@Test
	void incompleteStateFailsLifecycleValidation() {
		YadaSavedProperty savedProperty = new YadaSavedProperty();

		assertThrows(YadaInvalidValueException.class, savedProperty::normalizeAndValidate);
		savedProperty.setName(" \t ");
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
}
