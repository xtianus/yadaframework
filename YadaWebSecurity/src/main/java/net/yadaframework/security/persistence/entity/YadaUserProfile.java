package net.yadaframework.security.persistence.entity;

import java.io.Serializable;
import java.util.TimeZone;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;

@Entity
public class YadaUserProfile implements Serializable {
	private static final long serialVersionUID = 1L;
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	protected Long id;

	@Column(length = 32)
	protected String firstName;

	@Column(length = 32)
	protected String middleName;

	@Column(length = 64)
	protected String lastName;

	@OneToOne(optional = false, cascade = CascadeType.ALL, orphanRemoval = true)
	protected YadaUserCredentials yadaUserCredentials;
	
	@Column(length = 64)
	protected TimeZone timezone; 	// Timezone ID: "America/Los_Angeles",
									// "America/Argentina/ComodRivadavia"
									// https://en.wikipedia.org/wiki/List_of_tz_database_time_zones

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getMiddleName() {
		return middleName;
	}

	public void setMiddleName(String middleName) {
		this.middleName = middleName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public YadaUserCredentials getYadaUserCredentials() {
		return yadaUserCredentials;
	}

	public void setYadaUserCredentials(YadaUserCredentials yadaUserCredentials) {
		this.yadaUserCredentials = yadaUserCredentials;
	}

	public TimeZone getTimezone() {
		return timezone;
	}

	public void setTimezone(TimeZone timezone) {
		this.timezone = timezone;
	}
	
}
