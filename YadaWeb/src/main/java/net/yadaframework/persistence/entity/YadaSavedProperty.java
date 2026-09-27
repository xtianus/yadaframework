package net.yadaframework.persistence.entity;

import org.hibernate.Length;
import org.hibernate.annotations.ColumnDefault;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import net.yadaframework.exceptions.YadaInvalidValueException;
import net.yadaframework.persistence.YadaSavedPropertyKey;
import net.yadaframework.persistence.YadaSavedPropertyTypeEnum;

/**
 * Stores a scoped, typed saved property as canonical text.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "UK_YadaSavedProperty_applicationName_name", columnNames = {"applicationName", "name"}))
public class YadaSavedProperty {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 64)
	@ColumnDefault("'default'")
	private String applicationName = YadaSavedPropertyKey.DEFAULT_APPLICATION_NAME;

	@Column(nullable = false, length = 191)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private YadaSavedPropertyTypeEnum type;

	@Column(nullable = false, length = Length.LONG32)
	private String value;

	@Version
	private long version;

	/**
	 * Creates an empty entity for JPA and initializes its default scope.
	 */
	public YadaSavedProperty() {
	}

	/**
	 * Creates a saved property with its complete semantic state.
	 * @param applicationName the application scope, or blank for the default scope
	 * @param name the property name
	 * @param type the logical storage type
	 * @param value the canonical text value
	 */
	public YadaSavedProperty(String applicationName, String name, YadaSavedPropertyTypeEnum type, String value) {
		setApplicationName(applicationName);
		setName(name);
		this.type = type;
		this.value = value;
	}

	/**
	 * Normalizes the identifiers and rejects incomplete persistent state.
	 */
	@PrePersist
	@PreUpdate
	void normalizeAndValidate() {
		applicationName = YadaSavedPropertyKey.normalizeApplicationName(applicationName);
		name = YadaSavedPropertyKey.normalizeName(name);
		if (type == null) {
			throw new YadaInvalidValueException("Saved property type must not be null");
		}
		if (value == null) {
			throw new YadaInvalidValueException("Saved property value must not be null");
		}
	}

	/**
	 * Returns the generated technical identifier.
	 * @return the generated identifier, or null before persistence
	 */
	public Long getId() {
		return id;
	}

	/**
	 * Returns the normalized application scope.
	 * @return the application scope
	 */
	public String getApplicationName() {
		return applicationName;
	}

	/**
	 * Sets and normalizes the application scope.
	 * @param applicationName the application scope, or blank for the default scope
	 */
	public void setApplicationName(String applicationName) {
		this.applicationName = YadaSavedPropertyKey.normalizeApplicationName(applicationName);
	}

	/**
	 * Returns the property name.
	 * @return the property name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets and normalizes the property name.
	 * @param name the property name
	 */
	public void setName(String name) {
		this.name = YadaSavedPropertyKey.normalizeName(name);
	}

	/**
	 * Returns the logical storage type.
	 * @return the logical storage type
	 */
	public YadaSavedPropertyTypeEnum getType() {
		return type;
	}

	/**
	 * Sets the logical storage type.
	 * @param type the logical storage type
	 */
	public void setType(YadaSavedPropertyTypeEnum type) {
		this.type = type;
	}

	/**
	 * Returns the canonical text value.
	 * @return the canonical text value
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Sets the canonical text value.
	 * @param value the canonical text value
	 */
	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * Returns the optimistic-lock version.
	 * @return the optimistic-lock version
	 */
	public long getVersion() {
		return version;
	}
}
