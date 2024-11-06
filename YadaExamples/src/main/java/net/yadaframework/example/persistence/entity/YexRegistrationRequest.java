package net.yadaframework.example.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Transient;

import net.yadaframework.security.persistence.entity.YadaRegistrationRequest;

@Entity
public class YexRegistrationRequest extends YadaRegistrationRequest {
	private static final long serialVersionUID = 1L;

	private String name;
	private String surname;
	
	@Transient
	private String username; // Used for antispam - the real username is the email field

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSurname() {
		return surname;
	}

	public void setSurname(String surname) {
		this.surname = surname;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

}
