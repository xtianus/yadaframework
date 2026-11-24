package net.yadaframework.security.persistence.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import net.yadaframework.core.YadaRegistrationType;
import net.yadaframework.persistence.entity.YadaClause;

/**
 * A pending registration, password recovery or email change, confirmed by a link with a bearer token.
 * Subclasses must not define a id field.
 * <p>
 * Only the digest of the token secret is stored ({@link #getTokenHash()}). The secret itself is available through
 * {@link #getRawSecret()} only on the instance returned by YadaRegistrationRequestDao.issue(), so a request loaded
 * from the database can't produce its link again. Requests are created with issue() and never updated.
 */
@Entity
//Subclasses must not have an id but should still be tagged as @Entity
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

	// Removing validation messages because validation is better handled before getting to the database
	// @NotNull(message="{validation.registration.email.missing}")
	// @Email(message="{validation.registration.email.invalid}")
	// @Size(min=6, max=64, message="La lunghezza dell''email deve essere compresa tra {min} e {max} caratteri")
	@Column(nullable=false, length=64)
	private String email;

	// Removing validation messages because validation is better handled before getting to the database
	// @NotNull(message="Specificare una password")
	// TODO parametrizzare questa validazione con i valori della configurazione (se si può)
	// @Size(min=3, max=32, message="La lunghezza della password deve essere compresa tra {min} e {max} caratteri")
	//
	@Column(nullable=false, length=128)
	private String password;

	@Column(columnDefinition="TIMESTAMP NULL")
	private Date timestamp; // Creazione

	@Column(columnDefinition="TIMESTAMP NULL")
	private Date expiration; // The link can't be used from this time on; null means expired

	// SHA-256 digest of the token secret and purpose, see YadaSecurityTokenCodec
	@Column(length=64, nullable=false)
	private String tokenHash;

	// The token secret, known only on the instance that has just been issued; never stored
	@Transient
	private transient String rawSecret;

	// The destination url could be saved before sending the registration email, but it is usually better handled by the confirmation controller without this field.
	// This is also used by the password recovery process to create the confirmation link, but it's not safe because its value could be injected from the FE.
	// @Deprecated
	// @Column(length=255)
	// private String destinationUrl;

	private YadaRegistrationType registrationType;

	// ManyToOne: the same user can have several open requests (email change, password recovery)
	@ManyToOne
	private YadaUserCredentials yadaUserCredentials; // Used for email change

	// ManyToOne: every registration request refers to the same current version of the clause
	@ManyToOne
	@Deprecated // italian language; should have multiple values
	private YadaClause trattamentoDati; // Usato per visualizzare la clausola nel form, e poi per sapere quale clausola è stata accettata in fase di creazione utente

	// Browser timezone
	@Column(length = 64)
	@Deprecated // It is a mistake to save the timezone at registration time because it might be the administrator timezone
	private TimeZone timezone;

	@Transient
	@Deprecated // italian language
	private boolean trattamentoDatiAccepted=false; // usato nel form

	/**
	 * Password confirm, only used on the frontend but needed by thymeleaf to make things simpler
	 */
	@Transient
	private String confirmPassword;

	@PrePersist
	void setDefaults() {
		if (timestamp==null) {
			timestamp=new Date();
		}
	}

	/**
	 * Sets the token of a request that is being issued. Used by YadaRegistrationRequestDao.issue() only.
	 * @param rawSecret the secret that goes into the link
	 * @param tokenHash the digest of the secret
	 * @param timestamp the creation time
	 * @param expiration the time from which the link can't be used
	 */
	public void applyIssuedToken(String rawSecret, String tokenHash, Date timestamp, Date expiration) {
		this.rawSecret = rawSecret;
		this.tokenHash = tokenHash;
		this.timestamp = timestamp;
		this.expiration = expiration;
	}

	/**
	 * @return the token secret when this instance has just been issued, null otherwise
	 */
	public String getRawSecret() {
		return rawSecret;
	}

	/**
	 * @return the digest of the token secret
	 */
	public String getTokenHash() {
		return tokenHash;
	}

	/**
	 * @return the time from which the link can't be used, null for an invalid request
	 */
	public Date getExpiration() {
		return expiration;
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

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Shows neither the token nor the password
	 */
	@Override
	public String toString() {
		return new ToStringBuilder(this, ToStringStyle.SIMPLE_STYLE).
	       append("id", id).
	       append("registrationType", registrationType).
	       append("email", email).
	       append("timestamp", timestamp).
	       append("expiration", expiration).
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

	@Deprecated
	public YadaClause getTrattamentoDati() {
		return trattamentoDati;
	}

	@Deprecated
	public void setTrattamentoDati(YadaClause trattamentoDati) {
		this.trattamentoDati = trattamentoDati;
	}

	@Deprecated
	public boolean isTrattamentoDatiAccepted() {
		return trattamentoDatiAccepted;
	}

	@Deprecated
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

	@Deprecated // It is a mistake to save the timezone at registration time because it might be the administrator timezone
	public TimeZone getTimezone() {
		return timezone;
	}

	public void setTimezone(TimeZone timezone) {
		this.timezone = timezone;
	}


}

