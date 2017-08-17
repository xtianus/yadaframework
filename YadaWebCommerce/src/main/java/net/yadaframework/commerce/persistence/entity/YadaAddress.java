package net.yadaframework.commerce.persistence.entity;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;

import net.yadaframework.security.persistence.entity.YadaUserProfile;

@Entity
public class YadaAddress implements Serializable {
	private static final long serialVersionUID = 1L;
	
	// For synchronization with external databases
	@Column(columnDefinition="DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
	@Temporal(TemporalType.TIMESTAMP)
	protected Date modified;
	
	// For optimistic locking
	@Version
	protected long version;

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	protected Long id;
	
	@ManyToOne
	protected YadaUserProfile owner;

	@Column(length = 64)
	protected String street;

	@Column(length = 8)
	protected String number;
	
	@Column(length = 64)
	protected String city;
	
	@Column(length = 16)
	protected String zipCode;
	
	@Column(length = 64)
	protected String state;
	
	@Column(length = 64)
	protected String country; // SOUTH GEORGIA AND THE SOUTH SANDWICH ISLANDS

	/**
	 * Additional notes on the address
	 */
	@Column(length = 64)
	protected String notes;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getStreet() {
		return street;
	}

	public void setStreet(String street) {
		this.street = street;
	}

	public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
		this.number = number;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getZipCode() {
		return zipCode;
	}

	public void setZipCode(String zipCode) {
		this.zipCode = zipCode;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public YadaUserProfile getOwner() {
		return owner;
	}

	public void setOwner(YadaUserProfile owner) {
		this.owner = owner;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
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

}
