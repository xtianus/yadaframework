package net.yadaframework.core;

/**
 * Una YadaRegistrationRequest può essere usata anche per altri scopi oltre alla registrazione.
 * Questo enum indica lo scopo.
 *
 */
public enum YadaRegistrationType {
	REGISTRATION,		// Registrazione nuovo utente
	PASSWORD_RECOVERY,	// Recupero password di utente esistente che l'ha dimenticata
	EMAIL_CHANGE,		// Cambio email di utente esistente
	SOCIAL_REGISTRATION // Un login social per un utente inesistente
	;

	public static YadaRegistrationType fromInt(int num) {
		return YadaRegistrationType.values()[num];  // Starts at 0
	}
	
	public int toInt() {
		return ordinal(); // Starts at 0
	}
	

}
