package net.yadaframework.security.persistence.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import net.yadaframework.security.components.YadaSecurityUtil;
import net.yadaframework.web.YadaJsonDateTimeShortSerializer;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class YadaUserCredentials implements Serializable {
	private static final long serialVersionUID = 1L;

	// For optimistic locking
	@Version
	private long version;

	@Id
	@GeneratedValue(strategy= GenerationType.IDENTITY)
	private Long id;

	// Non può essere NaturalId perchè sarebbe immutable
	// @NaturalId
	// @JsonView(YadaJsonView.WithEagerAttributes.class)
	@Column(nullable=false, unique=true, length=128)
	private String username; // uso sempre l'email

	@JsonIgnore
	@Column(nullable=false, length=128)
	private String password;

	@Transient
	private String newPassword; // For use in forms

	@Column(columnDefinition="TIMESTAMP NULL")
	private Date passwordDate;

	private boolean changePassword=false; // true quando un utente deve cambiare password al prossimo login

	@Column(columnDefinition="TIMESTAMP NULL")
	private Date creationDate;

	private boolean enabled=false;

	@ElementCollection(fetch= FetchType.EAGER)
	@Fetch(FetchMode.SUBSELECT) // Questo permette di fare una query sola invece di una per role
	private List<Integer> roles; // TODO this should be a Set to avoid duplications

	private int failedAttempts; // Fallimenti di login consecutivi. Viene modificato direttamente nel db da UserCredentialsRepository.resetFailedAttempts()

	@Column(columnDefinition="TIMESTAMP NULL")
	private Date lastFailedAttempt; // timestamp dell'ultimo login fallito
	@JsonSerialize(using=YadaJsonDateTimeShortSerializer.class)

	@Column(columnDefinition="TIMESTAMP NULL")
	private Date lastSuccessfulLogin; // timestamp dell'ultimo login completato - settare con userCredentialsRepository.updateLoginTimestamp()

	@JsonIgnore // Ignored because of lazy association
	@OneToMany(fetch=FetchType.LAZY, cascade= CascadeType.ALL, mappedBy="yadaUserCredentials") // Non posso mettere orphanRemoval=true perchè prendo una eccezione: A collection with cascade="all-delete-orphan" was no longer referenced by the owning entity instance
	private List<YadaSocialCredentials> yadaSocialCredentialsList;
	
	@PrePersist
	void setDefaults() {
		creationDate = new Date();
	}

//	@Transient
//	@JsonProperty("lastSuccessfulLogin")
//	// TODO questo è da rifare con @JsonSerialize(using=JsonDateSimpleSerializer.class)
//	public String getJsonLastSuccessfulLogin() {
//		Date date = getLastSuccessfulLogin();
//		if (date!=null) {
//			Locale locale = LocaleContextHolder.getLocale();
//			TimeZone timeZone = LocaleContextHolder.getTimeZone();
//			FastDateFormat fastDateFormat = FastDateFormat.getDateTimeInstance(FastDateFormat.SHORT, FastDateFormat.SHORT, timeZone, locale);
//			return fastDateFormat.format(date);
//		}
//		return null;
//	}
	
	/**
	 * Set one role removing any other roles
	 * @param role
	 */
	@Transient
	public void setOnlyRole(Integer role) {
		roles = new ArrayList<Integer>();
		roles.add(role);
	}
	
	/**
	 * Set one role removing any other roles
	 * @param role
	 * @deprecated @see #setOnlyRole(Integer)
	 */
	@Transient
	@Deprecated // The name is misleading
	public void setRole(Integer role) {
		roles = new ArrayList<Integer>();
		roles.add(role);
	}

	@Transient
	public boolean hasRole(Integer role) {
		if (roles==null) {
			return false;
		}
		return roles.contains(role);
	}

	/**
	 * Add all roles if not already present. Same as {@link #addRoles(Integer[])}
	 * @param roles
	 */
	@Transient
	public void ensureRoles(Integer[] roles) {
		for (Integer role : roles) {
			addRole(role);
		}
	}
	
	/**
	 * Add all roles if not already present. Same as {@link #ensureRoles(Integer[])}
	 * @param roles
	 */
	@Transient
	public void addRoles(Integer[] roles) {
		ensureRoles(roles);
	}

	/**
	 * Add a role if not already present. Same as {@link #addRole(Integer)}
	 * @param role
	 */
	@Transient
	public void ensureRole(Integer role) {
		if (roles==null) {
			roles = new ArrayList<Integer>();
		}
		if (!hasRole(role)) {
			roles.add(role);
		}
	}
	
	/**
	 * Add a role if not already present. Same as {@link #ensureRole(Integer)}
	 * @param role
	 */
	@Transient
	public void addRole(Integer role) {
		ensureRole(role);
	}

	/**
	 * Remove a role if present
	 * @param role
	 */
	@Transient
	public void removeRole(Integer role) {
		if (roles!=null) {
			if (hasRole(role)) {
				roles.remove(role);
			}
		}
	}

	public String getUsername() {
		return username;
	}

	/**
	 * Sets the username after trimming and lowercase conversion in the default locale
	 * @param username
	 */
	public void setUsername(String username) {
		this.username = username==null?null:username.trim().toLowerCase(Locale.ROOT);
	}

	public String getPassword() {
		return password;
	}

	/**
	 * Sets the password and its timestamp then clears the failed attempts and the "changePassword" flag
	 * @param password
	 * @param encoder the password encoder, or null for cleartext passwords
	 * @deprecated because it arbitrarily clears the changePassword flag
	 * @see YadaSecurityUtil#changePassword(YadaUserProfile, String)
	 */
	@Deprecated
	public void changePassword(String password, PasswordEncoder encoder) {
		if (encoder!=null) {
			password=encoder.encode(password);
		}
		this.password = password;
		this.passwordDate = new Date();
		this.changePassword = false;
		this.failedAttempts = 0;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	@Override
	public int hashCode() {
		if (id!=null) {
			return id.hashCode();
		}
		return Objects.hash(username, password);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj==null) {
			return false;
		}
		if (obj instanceof YadaUserCredentials) {
			if (this.id!=null) {
				return this.id.equals(((YadaUserCredentials)obj).getId());
			} else if (this.username!=null) {
				return this.username.equals(((YadaUserCredentials)obj).username);
			}
		}
		return super.equals(obj);
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public List<Integer> getRoles() {
		return roles;
	}

	public void setRoles(List<Integer> roles) {
		this.roles = roles;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	/**
	 * @return the changePassword
	 */
	public boolean isChangePassword() {
		return changePassword;
	}

	/**
	 * @param changePassword the changePassword to set
	 */
	public void setChangePassword(boolean changePassword) {
		this.changePassword = changePassword;
	}

	public Date getPasswordDate() {
		return passwordDate;
	}

	public void setPasswordDate(Date passwordDate) {
		this.passwordDate = passwordDate;
	}

	public int getFailedAttempts() {
		return failedAttempts;
	}

	public void setFailedAttempts(int failedAttempts) {
		this.failedAttempts = failedAttempts;
	}

	public Date getLastFailedAttempt() {
		return lastFailedAttempt;
	}

	public void setLastFailedAttempt(Date lastFailedAttempt) {
		this.lastFailedAttempt = lastFailedAttempt;
	}

	public Date getLastSuccessfulLogin() {
		return lastSuccessfulLogin;
	}

	public void setLastSuccessfulLogin(Date lastSuccessfulLogin) {
		this.lastSuccessfulLogin = lastSuccessfulLogin;
	}

	public List<YadaSocialCredentials> getYadaSocialCredentialsList() {
		return yadaSocialCredentialsList;
	}

	public void setYadaSocialCredentialsList(
			List<YadaSocialCredentials> yadaSocialCredentialsList) {
		this.yadaSocialCredentialsList = yadaSocialCredentialsList;
	}

	@Transient
	public void addYadaSocialCredentials(YadaSocialCredentials yadaSocialCredentials) {
		if (this.yadaSocialCredentialsList==null) {
			this.yadaSocialCredentialsList = new ArrayList<YadaSocialCredentials>();
		}
		this.yadaSocialCredentialsList.add(yadaSocialCredentials);
	}

	public String getNewPassword() {
		return newPassword;
	}

	public void setNewPassword(String newPassword) {
		this.newPassword = newPassword;
	}

	public long getVersion() {
		return version;
	}


}
