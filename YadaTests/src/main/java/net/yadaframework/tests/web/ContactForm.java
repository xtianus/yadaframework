package net.yadaframework.tests.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Form bean validated with Bean Validation
 */
public class ContactForm {
	@NotBlank
	@Size(max = 32)
	private String name;

	@NotBlank
	@Email
	private String email;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}
}
