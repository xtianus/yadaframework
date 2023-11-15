package net.yadaframework.security.persistence.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.hibernate.validator.constraints.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import net.yadaframework.core.YadaRegistrationType;
import net.yadaframework.persistence.entity.YadaClause;

/**
 * Data to submit during user registration.
 * Subclasses must not define a id field.
 */
@Entity
// Subclasses must not have an id but should still be tagged as @Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class YadaRegistrationRequest implements Serializable {
	private static final long serialVersionUID = 1L;
	private final transient Logger log = LoggerFactory.getLogger(getClass());

	// For optimistic locking
	@Version
	private long version;

	@Id
	@GeneratedValue(strategy= GenerationType.IDENTITY)
	private Long id;

	@NotNull(message="{validation.registration.email.missing}")
	@Email(message="{validation.registration.email.invalid}")
	@Size(min=6, max=64, message="La lunghezza dell''email deve essere compresa tra {min} e {max} caratteri")
	// Non è NaturalId perchè posso avere una richiesta di cambio email e contemporaneamente una di cambio password
	// @NaturalId
	@Column(nullable=false, length=64)
	private String email;

	@NotNull(message="Specificare una password")
	// TODO parametrizzare questa validazione con i valori della configurazione (se si può)
	// @Size(min=3, max=32, message="La lunghezza della password deve essere compresa tra {min} e {max} caratteri")
	//
	@Column(nullable=false, length=128)
	private String password;

	@Column(columnDefinition="TIMESTAMP NULL")
	@Temporal(TemporalType.TIMESTAMP)
	private Date timestamp; // Creazione

	private long token;

	@Column(length=255)
	private String destinationUrl;

	private YadaRegistrationType registrationType;

//	@Column(length=512)
//	private String generic1; // Campo generico
//	@Column(length=512)
//	private String generic2; // Campo generico
//	@Lob
//	private String generic3; // Campo generico molto grande

	@OneToOne
	private YadaUserCredentials yadaUserCredentials; // Usato per il cambio di email

	@OneToOne
	private YadaClause trattamentoDati; // Usato per visualizzare la clausola nel form, e poi per sapere quale clausola è stata accettata in fase di creazione utente

	// Browser timezone
	@Column(length = 64)
	private TimeZone timezone;

	@Transient
	private boolean trattamentoDatiAccepted=false; // usato nel form

	/**
	 * Password confirm, only used on the frontend but needed by thymeleaf to make things simpler
	 */
	@Transient
	private String confirmPassword;

	@PrePersist
	void setDefaults() {
		timestamp=new Date();
		token=(long) (Math.random()*Long.MAX_VALUE);
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public long getToken() {
		return token;
	}

	public void setToken(long token) {
		this.token = token;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return new ToStringBuilder(this, ToStringStyle.SIMPLE_STYLE).
	       append("email", email).
	       append("timestamp", timestamp).
	       append("token", token).
	       toString();
	}

	public YadaUserCredentials getYadaUserCredentials() {
		return yadaUserCredentials;
	}

	public void setYadaUserCredentials(YadaUserCredentials yadaUserCredentials) {
		this.yadaUserCredentials = yadaUserCredentials;
	}

	public YadaRegistrationType getRegistrationType() {
		return registrationType;
	}

	public void setRegistrationType(YadaRegistrationType yadaRegistrationType) {
		this.registrationType = yadaRegistrationType;
	}

	public YadaClause getTrattamentoDati() {
		return trattamentoDati;
	}

	public void setTrattamentoDati(YadaClause trattamentoDati) {
		this.trattamentoDati = trattamentoDati;
	}

	public boolean isTrattamentoDatiAccepted() {
		return trattamentoDatiAccepted;
	}

	public void setTrattamentoDatiAccepted(boolean trattamentoDatiAccepted) {
		this.trattamentoDatiAccepted = trattamentoDatiAccepted;
	}

	public long getVersion() {
		return version;
	}

	public String getConfirmPassword() {
		return confirmPassword;
	}

	public void setConfirmPassword(String confirm) {
		this.confirmPassword = confirm;
	}

	public String getDestinationUrl() {
		return destinationUrl;
	}

	public void setDestinationUrl(String destinationUrl) {
		this.destinationUrl = destinationUrl;
	}

	public TimeZone getTimezone() {
		return timezone;
	}

	public void setTimezone(TimeZone timezone) {
		this.timezone = timezone;
	}

//	public String getGeneric1() {
//		return generic1;
//	}
//
//	public void setGeneric1(String generic1) {
//		this.generic1 = generic1;
//	}
//
//	public String getGeneric2() {
//		return generic2;
//	}
//
//	public void setGeneric2(String generic2) {
//		this.generic2 = generic2;
//	}
//
//	public String getGeneric3() {
//		return generic3;
//	}
//
//	public void setGeneric3(String generic3) {
//		this.generic3 = generic3;
//	}

}
