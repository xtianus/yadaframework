package net.yadaframework.security.persistence.entity;
import java.io.Serializable;
import java.util.Date;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

/**
 * Used to create an url to access the site with an automatic login.
 * <p>
 * Only the digest of the token secret is stored ({@link #getTokenHash()}). The secret is available through
 * {@link #getRawSecret()} only on the instance returned by YadaTokenHandler.makeAutoLoginToken(), so a token loaded
 * from the database can't produce its link again. A token works once and only before its mandatory expiration.
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class YadaAutoLoginToken implements Serializable {
	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unused")
	private final transient Logger log = LoggerFactory.getLogger(getClass());

	// For optimistic locking
	@Version
	protected long version;

	@Id
	@GeneratedValue(strategy= GenerationType.IDENTITY)
	private Long id;

	// SHA-256 digest of the token secret and purpose, see YadaSecurityTokenCodec
	@Column(length=64, nullable=false)
	private String tokenHash;

	// The token secret, known only on the instance that has just been issued; never stored
	@Transient
	private transient String rawSecret;

	@Column(columnDefinition="TIMESTAMP NULL")
	private Date timestamp; // Token creation date

	@Column(columnDefinition="TIMESTAMP NULL")
	private Date expiration; // Token expiration. Null = invalid token

	// ManyToOne: the same user can hold several auto-login tokens at the same time, so a OneToOne
	// would make Hibernate generate a unique constraint that the data cannot satisfy.
	@ManyToOne
	private YadaUserCredentials yadaUserCredentials;

	@PrePersist
	void setDefaults() {
		timestamp = new Date();
	}

	/**
	 * Sets the token of a new instance that is being issued. Used by YadaAutoLoginTokenDao.issue() only.
	 * @param rawSecret the secret that goes into the link
	 * @param tokenHash the digest of the secret
	 */
	public void applyIssuedToken(String rawSecret, String tokenHash) {
		this.rawSecret = rawSecret;
		this.tokenHash = tokenHash;
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
	 * Shows no token data
	 */
	@Override
	public String toString() {
		return "YadaAutoLoginToken[id=" + id + ", expiration=" + expiration + "]";
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Date getExpiration() {
		return expiration;
	}

	public void setExpiration(Date expiration) {
		this.expiration = expiration;
	}

	public YadaUserCredentials getYadaUserCredentials() {
		return yadaUserCredentials;
	}

	public void setYadaUserCredentials(YadaUserCredentials yadaUserCredentials) {
		this.yadaUserCredentials = yadaUserCredentials;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}



}
