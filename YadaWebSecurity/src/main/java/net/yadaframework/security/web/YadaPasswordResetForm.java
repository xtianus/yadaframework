package net.yadaframework.security.web;

/**
 * The password recovery request form: only the email address can be submitted.
 * The recovery request is always built on the server, see YadaRegistrationController.yadaPasswordResetPost().
 */
public class YadaPasswordResetForm {
	private String email;

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}
}
