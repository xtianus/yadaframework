package net.yadaframework.tests.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import net.yadaframework.security.persistence.entity.YadaUserProfile;

/**
 * Minimal concrete user profile of the YadaTests application.
 */
@Entity
public class UserProfile extends YadaUserProfile {
	private static final long serialVersionUID = 1L;

	@Column(length = 32)
	private String nickname;

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}
}
