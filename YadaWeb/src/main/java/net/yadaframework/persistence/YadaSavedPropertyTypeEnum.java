package net.yadaframework.persistence;

/**
 * Identifies the internal logical format used to store a saved property as canonical text.
 * This discriminator is independent from localized and persistent JPA enums.
 * New logical formats require a new enum constant but no database column change.
 */
public enum YadaSavedPropertyTypeEnum {
	STRING,
	BOOLEAN,
	LONG,
	DECIMAL,
	INSTANT,
	ENUM,
	JSON
}
