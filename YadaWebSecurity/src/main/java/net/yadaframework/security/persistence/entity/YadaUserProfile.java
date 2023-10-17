package net.yadaframework.security.persistence.entity;

import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;

import org.apache.commons.lang3.LocaleUtils;

import net.yadaframework.persistence.entity.YadaAttachedFile;

@Entity
// We keep it simple and use a discriminator for inheritance. It's very unlikely that someone might need a joined table with no other options
// @Inheritance(strategy = InheritanceType.JOINED)
public class YadaUserProfile implements Serializable {
	private static final long serialVersionUID = 1L;

	// For optimistic locking
	@Version
	protected long version;

	@Id
	@GeneratedValue(strategy= GenerationType.IDENTITY)
	protected Long id;

	@Column(length = 32)
	protected String firstName;

	@Column(length = 32)
	protected String middleName;

	@Column(length = 64)
	protected String lastName;

	//@JsonView(YadaJsonView.WithEagerAttributes.class)
	@NotNull
	@OneToOne(optional = false, cascade = CascadeType.ALL, orphanRemoval = true)
	protected YadaUserCredentials userCredentials;

	@Column(length = 32)
	protected Locale locale;

	@Column(length = 64)
	protected TimeZone timezone; 	// Timezone ID: "America/Los_Angeles",
									// "America/Argentina/ComodRivadavia"
									// https://en.wikipedia.org/wiki/List_of_tz_database_time_zones
	protected boolean timezoneSetByUser = false; // True when the user has manually set the timezone and there is no more need to set it automatically

	@OneToOne(cascade=CascadeType.PERSIST)
	protected YadaAttachedFile avatar;

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

	@Transient
	public void setTimezone(String timezoneString) {
		this.timezone = TimeZone.getTimeZone(timezoneString);
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

	/**
	 * Set the locale from a locale string in the form ll_CC like en_US. Case sensitive.
	 */
	@Transient
	public void setLocale(String localeString) {
		this.locale = LocaleUtils.toLocale(localeString);
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

	public YadaAttachedFile getAvatar() {
		return avatar;
	}

	public void setAvatar(YadaAttachedFile avatar) {
		this.avatar = avatar;
	}

	public boolean isTimezoneSetByUser() {
		return timezoneSetByUser;
	}

	public void setTimezoneSetByUser(boolean timezoneSetByUser) {
		this.timezoneSetByUser = timezoneSetByUser;
	}

}
