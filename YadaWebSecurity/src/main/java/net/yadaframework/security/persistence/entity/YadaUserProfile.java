package net.yadaframework.security.persistence.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;

import net.yadaframework.web.YadaJsonView;

@Entity
// We keep it simple and use a discriminator for inheritance. It's very unlikely that someone might need a joined table with no other options
// @Inheritance(strategy = InheritanceType.JOINED)
public class YadaUserProfile implements Serializable {
	private static final long serialVersionUID = 1L;
	
	// For optimistic locking
	@Version
	protected long version;
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	protected Long id;

	@Column(length = 32)
	protected String firstName;

	@Column(length = 32)
	protected String middleName;

	@Column(length = 64)
	protected String lastName;

	@JsonView(YadaJsonView.WithEagerAttributes.class)
	@NotNull
	@OneToOne(optional = false, cascade = CascadeType.ALL, orphanRemoval = true)
	protected YadaUserCredentials userCredentials;
	
	@Column(length = 32)
	protected Locale locale;
	
	@Column(length = 64)
	protected TimeZone timezone; 	// Timezone ID: "America/Los_Angeles",
									// "America/Argentina/ComodRivadavia"
									// https://en.wikipedia.org/wiki/List_of_tz_database_time_zones

// Removed because I don't want to force the use of a YadaTicket table
//	@JsonIgnore
//	@OneToMany(mappedBy="owner", cascade=CascadeType.ALL)
//	protected List<YadaTicket> tickets;
	
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

	public YadaUserCredentials getUserCredentials() {
		return userCredentials;
	}

	public void setUserCredentials(YadaUserCredentials yadaUserCredentials) {
		this.userCredentials = yadaUserCredentials;
	}

	public TimeZone getTimezone() {
		return timezone;
	}

	public void setTimezone(TimeZone timezone) {
		this.timezone = timezone;
	}

	public long getVersion() {
		return version;
	}

	public Locale getLocale() {
		return locale;
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	@Override
	public int hashCode() {
		if (id!=null) {
			return id.hashCode();
		}
		return Objects.hash(firstName, lastName, middleName, userCredentials);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj==null) {
			return false;
		}
		if (obj instanceof YadaUserProfile && this.id!=null) {
			return this.id.equals(((YadaUserProfile)obj).getId());
		}
		return super.equals(obj);
	}
	
}
