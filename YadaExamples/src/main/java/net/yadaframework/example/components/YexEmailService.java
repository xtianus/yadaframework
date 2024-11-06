package net.yadaframework.example.components;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import net.yadaframework.components.YadaEmailService;
import net.yadaframework.example.core.YexConfiguration;
import net.yadaframework.example.persistence.entity.UserProfile;
import net.yadaframework.security.components.YadaSecurityEmailService;

@Service
public class YexEmailService {

	private final Logger log = LoggerFactory.getLogger(getClass());

    private @Autowired YadaEmailService yadaEmailService;
    private @Autowired YadaSecurityEmailService yadaSecurityEmailService;
    private @Autowired YexConfiguration config;
    
	public boolean sendInvitation(UserProfile userProfile, String clearPassword, HttpServletRequest request, Locale locale) {
		final String emailName = "invitation";
		final String[] toEmail = {userProfile.getEmail()};

		// In the dev environment we use the request to get the address, otherwise it must have been configured
		String myServerAddress = config.isDevelopmentEnvironment() ? config.getWebappAddress(request) : config.getWebappAddress();
		String fullLink = myServerAddress;

		final Map<String, Object> templateParams = new HashMap<>();
		templateParams.put("fullLink", fullLink);
		templateParams.put("username", userProfile.getEmail());
		templateParams.put("clearPassword", clearPassword);

		Map<String, String> inlineResources = new HashMap<>();
		inlineResources.put("logosmall", config.getEmailLogoImage());
		return yadaEmailService.sendHtmlEmail(toEmail, emailName, null, templateParams, inlineResources, locale, false);
	}

}
