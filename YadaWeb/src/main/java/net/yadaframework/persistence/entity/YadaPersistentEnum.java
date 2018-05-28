package net.yadaframework.persistence.entity;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.MapKeyColumn;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.i18n.LocaleContextHolder;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Objects;

import net.yadaframework.core.YadaLocalEnum;
import net.yadaframework.exceptions.YadaInvalidValueException;

/**
 * Needed to store a localized enum in the database, on which to perform localized search and sort operations.
 * To retrieve the localized value call {@link YadaPersistentEnum#getLocalText()}
 * For every Enum class in Java there is a number of rows in the database holding the enum class name and its ordinals.
 * For each such row there are a number of rows holding the localized values.
 * E.g. the "ProcessState" enum with values "RUNNING" and "STOPPED" produces the following rows:<p>
 * Table YadaPersistentEnum (logical view)
 * <table border="1">
 * <tr>
 * 		<th>enumClassName</th><th>enumName</th><th>enumOrdinal</th><th>langToText</th>
 * </tr>
 * <tr>
 * 		<td>ProcessState</td><td>RUNNING</td><td>0</td><td><table><tr><td>en_US</td><td>Executing</td></tr><tr><td>it_IT</td><td>In esecuzione</td></tr></table></td>
 * </tr>
 * <tr>
 * 		<td>ProcessState</td><td>STOPPED</td><td>1</td><td><table><tr><td>en_US</td><td>Idle</td></tr><tr><td>it_IT</td><td>Fermo</td></tr></table></td>
 * </tr>
 * </table>
 * </p>
 */
@JsonSerialize(using = YadaPersistentEnum.YadaPersistentEnumSerializer.class)
@Entity
@Table(
		uniqueConstraints = @UniqueConstraint(columnNames={"enumClassName", "enumOrdinal"})
)
@Inheritance(strategy = InheritanceType.JOINED)
public class YadaPersistentEnum<E extends Enum<E>> {
	
	@Deprecated // TODO delete because not used anymore for datatables
	static class YadaPersistentEnumSerializer extends JsonSerializer<YadaPersistentEnum<?>> {
		@Override
		public void serialize(YadaPersistentEnum<?> value, JsonGenerator generator, SerializerProvider serializers) throws IOException, JsonProcessingException {
			generator.writeString(value.getLocalText());
		}
		
	}
	
	@SuppressWarnings("unused")
	private final transient Logger log = LoggerFactory.getLogger(getClass());
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long id;

	@Column(nullable=false, length=191)
	private String enumClassName; 	// ProcessState
	private int enumOrdinal;		// 2
	@Column(nullable=false)
	private String enumName;		// "RUNNING"
	
	@ElementCollection(fetch = FetchType.EAGER)
	@MapKeyColumn(name="language", length=32) // th_TH_TH_#u-nu-thai
	@Column(name="localText", length=128)
	Map<String, String> langToText = new HashMap<>(); // Language code to localized text: { "it_IT" = "In esecuzione", "en_US" = "Running" }

	/**
	 * Check this instance with a normal enum for equality
	 * @param enumValue
	 * @return
	 */
	@Transient
	public boolean equals(Enum<? extends YadaLocalEnum<?>> enumValue) {
		return enumValue.getClass().getName().equals(this.enumClassName) && enumValue.ordinal() == this.enumOrdinal;
	}
	
	@Transient
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof YadaPersistentEnum == false || obj==null) {
			return false;
		}
		YadaPersistentEnum<E> other = (YadaPersistentEnum<E>) obj;
		return (this.enumClassName.equals(other.enumClassName) && this.enumOrdinal == other.enumOrdinal);
	}

	@Transient
	@Override
	public int hashCode() {
		return Objects.hashCode(this.enumClassName, this.enumOrdinal);
	}
	
	/**
	 * Sets the attributes of a normal enum into this instance, but not the localized text
	 * @param enumValue
	 */
	@Transient
	public void setEnum(E enumValue) {
		this.enumClassName = enumValue.getClass().getName();
		this.enumOrdinal = enumValue.ordinal();
		this.enumName = enumValue.name();
	}
	
	/**
	 * Returns the localized value in the current locale
	 * @return
	 */
	@Transient
	public String getLocalText() {
		Locale locale = LocaleContextHolder.getLocale();
		return langToText.get(locale.getLanguage());
	}
	
	/**
	 * Convert the localised enum back to a normal Enum instance. Same as getEnum()
	 * @return
	 * @throws YadaInvalidValueException 
	 * @see #getEnum()
	 */
	public E toEnum() throws YadaInvalidValueException {
		return getEnum();
	}
	
	/**
	 * Convert the localised enum back to a normal Enum instance. Same as toEnum()
	 * @return
	 * @throws YadaInvalidValueException 
	 * @see #toEnum()
	 */
		public E getEnum() throws YadaInvalidValueException {
		try {
			Class<E> enumClassClass = (Class<E>) Class.forName(this.enumClassName);
			return (E) Enum.valueOf(enumClassClass, enumName);
		} catch (ClassNotFoundException e) {
			throw new YadaInvalidValueException(e);
		}
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public int getEnumOrdinal() {
		return enumOrdinal;
	}

	public void setEnumOrdinal(int enumOrdinal) {
		this.enumOrdinal = enumOrdinal;
	}

	public String getEnumName() {
		return enumName;
	}

	public void setEnumName(String enumName) {
		this.enumName = enumName;
	}

	public Map<String, String> getLangToText() {
		return langToText;
	}

	public void setLangToText(Map<String, String> langToText) {
		this.langToText = langToText;
	}

	public String getEnumClassName() {
		return enumClassName;
	}

	public void setEnumClassName(String enumClassName) {
		this.enumClassName = enumClassName;
	}

	
}
