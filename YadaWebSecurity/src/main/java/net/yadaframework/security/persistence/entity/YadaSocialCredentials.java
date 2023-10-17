package net.yadaframework.security.persistence.entity;
import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Rappresenta le credenziali generate alla registrazione con un social login
 *
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class YadaSocialCredentials implements Serializable {
	@SuppressWarnings("unused")
	private final transient Logger log = LoggerFactory.getLogger(getClass());
	
	// For optimistic locking
	@Version
	private long version;

	private static final long serialVersionUID = 1L;
	@Id
	@GeneratedValue(strategy= GenerationType.IDENTITY)
	private Long id;
	
	@Column(nullable=false, unique=true, length=128)
	private String socialId;
	
	@Column(nullable=false, unique=false, length=128)
	private String email; // La memorizzo ma non serve, non viene mai usata
	
	private int type; // Facebook, Google, LinkedIn, etc.
	
	@ManyToOne(fetch= FetchType.EAGER, optional=false)
	private YadaUserCredentials yadaUserCredentials;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getSocialId() {
		return socialId;
	}

	public void setSocialId(String socialId) {
		this.socialId = socialId;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String socialEmail) {
		this.email = socialEmail;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public YadaUserCredentials getYadaUserCredentials() {
		return yadaUserCredentials;
	}

	public void setYadaUserCredentials(YadaUserCredentials yadaUserCredentials) {
		this.yadaUserCredentials = yadaUserCredentials;
	}

	public long getVersion() {
		return version;
	}
	
}
