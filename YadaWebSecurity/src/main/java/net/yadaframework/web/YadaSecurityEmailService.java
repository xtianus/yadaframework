package net.yadaframework.web;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.thymeleaf.spring4.SpringTemplateEngine;

import net.yadaframework.components.YadaUtil;
import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.security.components.YadaTokenHandler;
import net.yadaframework.security.persistence.entity.YadaAutoLoginToken;
import net.yadaframework.security.persistence.entity.YadaRegistrationRequest;
import net.yadaframework.security.persistence.entity.YadaUserCredentials;
import net.yadaframework.security.persistence.repository.YadaAutoLoginTokenRepository;

@Service
// Deve stare in questo package perchè tirato dentro da YadaWebConfig, altrimenti SpringTemplateEngine non viene iniettato
public class YadaSecurityEmailService {
	private transient final Logger log = LoggerFactory.getLogger(getClass());	
	
	@Autowired private YadaConfiguration config;
	@Autowired private JavaMailSender mailSender;
	
	@Resource private SpringTemplateEngine emailTemplateEngine;
    
    @Autowired private MessageSource messageSource;
    
    @Autowired private ServletContext servletContext;
    @Autowired private ApplicationContext applicationContext;
    @Autowired private YadaTokenHandler yadaTokenHandler;
	@Autowired private YadaAutoLoginTokenRepository yadaAutoLoginTokenRepository;
    @Autowired private YadaWebUtil yadaWebUtil;
    @Autowired private YadaEmailService yadaEmailService;
    @Autowired private YadaUtil yadaUtil;
    
    /**
     * Convert a site-relative link to absolute, because in emails we can't use @{}.
     * Example: th:href="${beans.yadaEmailService.buildLink('/read/234')}"
     * @param relativeLink
     * @return absolute link
     */
    public String buildLink(String relativeLink) {
    	String myServerAddress = config.getServerAddress();
    	String relative = StringUtils.prependIfMissing(relativeLink, "/");
    	return myServerAddress + relative;
    }
    
    public boolean sendEmailChangeConfirmationToUser(YadaRegistrationRequest yadaRegistrationRequest, HttpServletRequest request, Locale locale) {
		final String emailName = "emailChangeConfirmation";
		final String[] toEmail = {yadaRegistrationRequest.getEmail()};
		final String[] subjectParams = {yadaRegistrationRequest.getEmail()};

		// Creo il link che l'utente deve cliccare
		String myServerAddress = yadaWebUtil.getWebappAddress(request);
		String fullLink = myServerAddress + "/changeEmailConfirm/" + yadaTokenHandler.makeLink(yadaRegistrationRequest, null);
		
		final Map<String, Object> templateParams = new HashMap<String, Object>();
		templateParams.put("fullLink", fullLink);
		
		Map<String, String> inlineResources = new HashMap<String, String>();
		inlineResources.put("logosmall", config.getEmailLogoImage());
		return yadaEmailService.sendHtmlEmail(toEmail, emailName, subjectParams, templateParams, inlineResources, locale, true);
	}

	public boolean sendEmailChangeConfirmation(YadaRegistrationRequest yadaRegistrationRequest, HttpServletRequest request, Locale locale) {
		final String emailName = "emailChangeConfirmation";
		final String[] toEmail = {yadaRegistrationRequest.getEmail()};
		//final String[] toEmail = config.getSupportRequestRecipients();
		final String[] subjectParams = {yadaRegistrationRequest.getEmail()};

		// Creo il link che l'utente deve cliccare
		String myServerAddress = yadaWebUtil.getWebappAddress(request);
		String fullLink = myServerAddress + "/changeEmailConfirm/" + yadaTokenHandler.makeLink(yadaRegistrationRequest, null);
		
		final Map<String, Object> templateParams = new HashMap<String, Object>();
		templateParams.put("fullLink", fullLink);
		
		Map<String, String> inlineResources = new HashMap<String, String>();
		inlineResources.put("logosmall", config.getEmailLogoImage());
		return yadaEmailService.sendHtmlEmail(toEmail, emailName, subjectParams, templateParams, inlineResources, locale, true);
	}

	/**
	 * 
	 * @param yadaRegistrationRequest
	 * @param linkParameters can be null
	 * @param request
	 * @param locale
	 * @return
	 */
	public boolean sendRegistrationConfirmation(YadaRegistrationRequest yadaRegistrationRequest, Map<String,String> linkParameters, HttpServletRequest request, Locale locale) {
		final String emailName = "registrationConfirmation";
		final String[] toEmail = {yadaRegistrationRequest.getEmail()};
		final String[] subjectParams = {yadaEmailService.timestamp(locale)};
		
		String myServerAddress = yadaWebUtil.getWebappAddress(request);
		String fullLink = myServerAddress + "/registrationConfirmation/" + yadaTokenHandler.makeLink(yadaRegistrationRequest, linkParameters);
		
		final Map<String, Object> templateParams = new HashMap<String, Object>();
		templateParams.put("fullLink", fullLink);
		
		Map<String, String> inlineResources = new HashMap<String, String>();
		inlineResources.put("logosmall", config.getEmailLogoImage());
		return yadaEmailService.sendHtmlEmail(toEmail, emailName, subjectParams, templateParams, inlineResources, locale, false);
	}

	/**
	 * Invio la mail per il recupero password
	 * @param logoImage e.g. "/res/img/logo-small.jpg"
	 * @param yadaRegistrationRequest
	 * @param request
	 * @param response
	 * @param locale
	 * @return
	 */
	public boolean sendPasswordRecovery(YadaRegistrationRequest yadaRegistrationRequest, HttpServletRequest request, Locale locale) {
		final String emailName = "passwordRecovery";
		final String[] toEmail = new String[] {yadaRegistrationRequest.getEmail()};
		final String[] subjectParams = {yadaRegistrationRequest.getEmail()};

		String myServerAddress = yadaWebUtil.getWebappAddress(request);
		String fullLink = myServerAddress + "/passwordReset/" + yadaTokenHandler.makeLink(yadaRegistrationRequest, null);

		final Map<String, Object> templateParams = new HashMap<String, Object>();
		templateParams.put("fullLink", fullLink);
		
		Map<String, String> inlineResources = new HashMap<String, String>();
		inlineResources.put("logosmall", config.getEmailLogoImage());
		return yadaEmailService.sendHtmlEmail(toEmail, emailName, subjectParams, templateParams, inlineResources, locale, true);
	}

    /**
     * Create a link that will not require the user to log in. The server address is taken from the configuration.
     * @param targetAction
     * @param targetUser
     * @param hashCommand optional "anchor" starting with #, e.g. "#storyId=49;command=showMessages"
     * @return
     */
    public String makeAutologinLink(String targetAction, YadaUserCredentials targetUser, String hashCommand) {
		String myServerAddress = config.getServerAddress();
		Date expiration = yadaUtil.addHours(new Date(), config.getAutologinExpirationHours());
    	return makeAutologinLink(targetAction, targetUser, expiration, hashCommand, myServerAddress);
    }
    
    /**
     * Create a link that will not require the user to log in. The server address is taken from the request.
     * @param targetAction
     * @param targetUser
     * @param expiration expiration date, null for never
     * @param hashCommand optional "anchor", e.g. "#storyId=49;command=showMessages"
     * @return
     */
    public String makeAutologinLink(String targetAction, YadaUserCredentials targetUser, Date expiration, String hashCommand, HttpServletRequest request) {
    	String myServerAddress = yadaWebUtil.getWebappAddress(request);
    	return makeAutologinLink(targetAction, targetUser, expiration, hashCommand, myServerAddress);
    }

    /**
     * Create a link that will not require the user to log in
     * @param targetAction
     * @param targetUser
     * @param expiration expiration date, null for never
     * @param hashCommand optional "anchor" starting with #, e.g. "#storyId=49;command=showMessages"
     * @param myServerAddress our server address
     * @return
     */
    public String makeAutologinLink(String targetAction, YadaUserCredentials targetUser, Date expiration, String hashCommand, String myServerAddress) {
    	YadaAutoLoginToken yadaAutoLoginToken = new YadaAutoLoginToken();
    	yadaAutoLoginToken.setExpiration(expiration);
    	yadaAutoLoginToken.setYadaUserCredentials(targetUser);
    	yadaAutoLoginToken = yadaAutoLoginTokenRepository.save(yadaAutoLoginToken);
    	yadaAutoLoginTokenRepository.deleteExpired(); // Rimuovo quelle vecchie, per pulizia
    	StringBuilder result = new StringBuilder(myServerAddress);
    	result.append("/autologin/");
    	result.append(yadaTokenHandler.makeLink(yadaAutoLoginToken, null));
		result.append("?action=").append(yadaWebUtil.urlEncode(targetAction));
		if (hashCommand!=null) {
			result.append(hashCommand);
		}
    	return result.toString();
    }
    
    /**
     * Add a string of parameters to the target action link
     * @param autologinLink a link like "xxx?action=aaa#command"
     * @param moreParameters not-encoded request parameters like "num=1&size=10" - no "?" nor initial "&" must be specified
     * @return the original string with the new parameters inserted at the end: "xxx?action=aaa%26num=1%26size=10#command"
     */
    public String extendAutologinLink(String autologinLink, String moreParameters) {
		String[] parts = autologinLink.split("\\?");
		String url = parts[0];
		String actionParam = parts[1];
		parts = actionParam.split("#");
		String actionUrl = parts[0];
		String actionHash = (parts.length>1 ? "#"+parts[1] : "");
		String encodedParameters = yadaWebUtil.urlEncode("?" + moreParameters);
		StringBuilder sb = new StringBuilder(url);
		sb.append("?").append(actionUrl).append(encodedParameters).append(actionHash);
		return sb.toString();
    }
    
}
