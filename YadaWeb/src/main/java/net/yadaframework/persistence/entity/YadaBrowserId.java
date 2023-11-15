package net.yadaframework.persistence.entity;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;


/**
 * This class uniquely identifies the user browser.
 * It can be used to prevent multiple submissions when the user is not authenticated (no login performed).
 * Identification can either come from a cookie, from local storage, or from a number of "browser leaks"
 * that, used together, can produce a "best guess" about the browser identity. 
 * See https://browserleaks.com/
 */
@Table(
		uniqueConstraints = @UniqueConstraint(columnNames={"mostSigBits", "leastSigBits"})
)
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class YadaBrowserId implements Serializable {
	private static final long serialVersionUID = -5673120637677663672L;
	
	// For optimistic locking
	@Version
	private long version;

	@Id
	@GeneratedValue(strategy= GenerationType.IDENTITY)
	private Long id;
	
	// These could be null if another mean is used (TODO in the future)
	private Long mostSigBits; 	// UUID from a cookie
	private Long leastSigBits; 	// UUID from a cookie
	
	// TODO store ipv4/6, see http://stackoverflow.com/a/34881294/587641
	// TODO add elements from https://browserleaks.com/
	
	/**
	 * Sets the UUID, e.g. from a cookie value
	 * @param uuid
	 */
	@Transient
	public void setUUID(UUID uuid) {
		mostSigBits = uuid.getMostSignificantBits();
		leastSigBits = uuid.getLeastSignificantBits();
	}

	/**
	 * Returns the UUID, or null if not set
	 * @return
	 */
	@Transient
	public UUID getUUID() {
		if (mostSigBits>0 && leastSigBits>0) {
			return new UUID(mostSigBits, leastSigBits);
		}
		return null;
	}
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	
	/**
	 * 
	 * @return could be null
	 */
	public Long getMostSigBits() {
		return mostSigBits;
	}
	public void setMostSigBits(long mostSigBits) {
		this.mostSigBits = mostSigBits;
	}
	
	/**
	 * 
	 * @return could be null
	 */
	public Long getLeastSigBits() {
		return leastSigBits;
	}
	public void setLeastSigBits(long leastSigBits) {
		this.leastSigBits = leastSigBits;
	}

	public long getVersion() {
		return version;
	}

	public void setMostSigBits(Long mostSigBits) {
		this.mostSigBits = mostSigBits;
	}

	public void setLeastSigBits(Long leastSigBits) {
		this.leastSigBits = leastSigBits;
	}
	
	
}
