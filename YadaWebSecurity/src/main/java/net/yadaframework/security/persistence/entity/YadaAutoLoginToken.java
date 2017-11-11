package net.yadaframework.security.persistence.entity;
import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonView;

import net.yadaframework.web.YadaJsonView;

/**
 * Consente di creare un'url da cui accedere direttamente a una pagina con login automatico
 *
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class YadaAutoLoginToken implements Serializable {
	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unused")
	private final transient Logger log = LoggerFactory.getLogger(getClass());
	
	// For synchronization with external databases
	@JsonView(YadaJsonView.WithEagerAttributes.class)
	@Column(columnDefinition="DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
	@Temporal(TemporalType.TIMESTAMP)
	protected Date modified;
	
	// For optimistic locking
	@Version
	protected long version;
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long id;

	private long token;
	
	private Date timestamp; // data di creazione
	private Date expiration; // Scadenza. Null = never expire

	@OneToOne
	private YadaUserCredentials yadaUserCredentials;
	
	@PrePersist
	void setDefaults() {
		timestamp = new Date();
		token = (long) (Math.random()*Long.MAX_VALUE);
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public long getToken() {
		return token;
	}

	public void setToken(long token) {
		this.token = token;
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

	public Date getModified() {
		return modified;
	}

	public void setModified(Date modified) {
		this.modified = modified;
	}

	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}

	

}
