package net.yadaframework.example.persistence.entity;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Transient;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import net.yadaframework.security.persistence.entity.YadaUserProfile;
import net.yadaframework.web.YadaJsonDateTimeShortSerializer;

@Entity
public class UserProfile extends YadaUserProfile {
	private static final long serialVersionUID = 1L;

	@Column(length = 32)
	String nickname; // This is an example of UserProfile customization

	/**
	 * Used in Datatables to define the row class.
	 * @See {@link https://datatables.net/manual/server-side}
	 */
	@Transient
	@JsonProperty("DT_RowClass")
	public String getDT_RowClass() {
		// Set a specific class on admins
		if (hasAnyRole("ADMIN")) {
			return "userProfileAdmin";
		}
		return null;
	}

	// Used in the edit form
	@Transient
	private boolean inviteEmail = false;

	@Transient
	public String getUserId() {
		return String.format("U%04d", id);
	}

	public Long getId() {
		return super.getId();
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append(nickname).append("(");
		result.append(id).append(")");
		return result.toString();
	}

	@JsonProperty("email")
	@Transient
	public String getEmail() {
		return userCredentials.getUsername();
	}

	@JsonProperty("enabled")
	@Transient
	public Boolean getEnabled() {
		return userCredentials.isEnabled();
	}

	@JsonProperty("registration")
	@JsonSerialize(using = YadaJsonDateTimeShortSerializer.class)
	@Transient
	public Date getRegistrationDate() {
		return userCredentials.getCreationDate();
	}

	@JsonProperty("loginDate")
	@JsonSerialize(using = YadaJsonDateTimeShortSerializer.class)
	@Transient
	public Date getLoginDate() {
		return userCredentials.getLastSuccessfulLogin();
	}
	
	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	@Transient
	public boolean isInviteEmail() {
		return inviteEmail;
	}

	public void setInviteEmail(boolean inviteEmail) {
		this.inviteEmail = inviteEmail;
	}

}
