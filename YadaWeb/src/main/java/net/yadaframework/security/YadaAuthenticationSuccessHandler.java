package net.yadaframework.security;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import net.yadaframework.persistence.repository.YadaUserCredentialsRepository;

// Si può inserire il codice da eseguire dopo un login che ha avuto successo
@Component
public class YadaAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {
	private String defaultTargetUrlAjaxRequest = "/";
	private String defaultTargetUrlNormalRequest = "/";

	@Autowired YadaUserCredentialsRepository userCredentialsRepository;

	public YadaAuthenticationSuccessHandler() {
	}
	
	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws ServletException, IOException {
		String username = authentication.getName();
		userCredentialsRepository.updateLoginTimestamp(username.toLowerCase());
		// Quando il login ha successo, resetto il contatore dei tentativi falliti che altrimenti cresce sempre finchè non sbaglio due volte (!)
		userCredentialsRepository.resetFailedAttempts(username.toLowerCase());
		//		String redirectUrl = "/register"; // !!!!!!!!!!!!!!
		//		request.setAttribute("redirectUrl", redirectUrl);
		if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
			super.setDefaultTargetUrl(defaultTargetUrlAjaxRequest);
		} else {
			super.setDefaultTargetUrl(defaultTargetUrlNormalRequest);
		}
		super.onAuthenticationSuccess(request, response, authentication);
	}

	public String getDefaultTargetUrlAjaxRequest() {
		return defaultTargetUrlAjaxRequest;
	}

	/**
	 * Target url when the request is Ajax
	 */
	public void setDefaultTargetUrlAjaxRequest(String defaultTargetUrlAjaxRequest) {
		this.defaultTargetUrlAjaxRequest = defaultTargetUrlAjaxRequest;
	}

	public String getDefaultTargetUrlNormalRequest() {
		return defaultTargetUrlNormalRequest;
	}

	/**
	 * Target url when the request is not Ajax
	 */
	public void setDefaultTargetUrlNormalRequest(String defaultTargetUrlNormalRequest) {
		this.defaultTargetUrlNormalRequest = defaultTargetUrlNormalRequest;
	}

}
