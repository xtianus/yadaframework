package net.yadaframework.security.components;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.spring6.SpringTemplateEngine;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import net.yadaframework.components.YadaEmailService;
import net.yadaframework.components.YadaUtil;
import net.yadaframework.components.YadaWebUtil;
import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.security.persistence.entity.YadaAutoLoginToken;
import net.yadaframework.security.persistence.entity.YadaRegistrationRequest;
import net.yadaframework.security.persistence.entity.YadaUserCredentials;
import net.yadaframework.security.persistence.repository.YadaAutoLoginTokenDao;

@Service
// Deve stare in questo package perchè tirato dentro da YadaWebConfig, altrimenti SpringTemplateEngine non viene iniettato
public class YadaSecurityEmailService {
	private transient final Logger log = LoggerFactory.getLogger(getClass());

	@Autowired private YadaConfiguration config;
	@Resource
	private SpringTemplateEngine emailTemplateEngine;
    @Autowired private YadaTokenHandler yadaTokenHandler;
    @Autowired private YadaWebUtil yadaWebUtil;
    @Autowired private YadaEmailService yadaEmailService;
	@Autowired private YadaAutoLoginTokenDao yadaAutoLoginTokenDao;
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

	public boolean sendEmailChangeConfirmation(YadaRegistrationRequest yadaRegistrationRequest, HttpServletRequest request, Locale locale) {
		final String emailName = "emailChangeConfirmation";
		final String[] toEmail = {yadaRegistrationRequest.getEmail()};

		// In the dev environment we use the request to get the address, otherwise it must have been configured
		String myServerAddress = config.isDevelopmentEnvironment() ? config.getWebappAddress(request) : config.getWebappAddress();
		String fullLink = myServerAddress + "/changeEmailConfirm/" + yadaTokenHandler.makeLink(yadaRegistrationRequest, null);

		final Map<String, Object> templateParams = new HashMap<>();
		templateParams.put("fullLink", fullLink);

		Map<String, String> inlineResources = new HashMap<>();
		inlineResources.put("logosmall", config.getEmailLogoImage());
		return yadaEmailService.sendHtmlEmail(toEmail, emailName, null, templateParams, inlineResources, locale, true);
	}

	/**
	 * Send a confirmation email when some user wants to register with his email address
	 * @param yadaRegistrationRequest
	 * @param linkParameters name-value paris of url parameters to add at the end of the confirmation link - can be null or empty
	 * @param request
	 * @param locale
	 * @return
	 */
	public boolean sendRegistrationConfirmation(YadaRegistrationRequest yadaRegistrationRequest, Map<String,String> linkParameters, HttpServletRequest request, Locale locale) {
		final String emailName = "registrationConfirmation";
		final String[] toEmail = {yadaRegistrationRequest.getEmail()};
		final String[] subjectParams = {yadaEmailService.timestamp(locale)};
		String link = config.getRegistrationConfirmationLink(locale);
		// In the dev environment we use the request to get the address, otherwise it must have been configured
		String myServerAddress = config.isDevelopmentEnvironment() ? config.getWebappAddress(request) : config.getWebappAddress();
		String fullLink = myServerAddress + link + yadaTokenHandler.makeLink(yadaRegistrationRequest, linkParameters);

		final Map<String, Object> templateParams = new HashMap<>();
		templateParams.put("fullLink", fullLink);
		templateParams.put("email", yadaRegistrationRequest.getEmail());

		Map<String, String> inlineResources = new HashMap<>();
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

		String destinationUrl = "/passwordReset/"; // TODO add to configuration

		// Checking the destination url  // This was a security issue
		//		if (StringUtils.isNotBlank(yadaRegistrationRequest.getDestinationUrl()))  {
		//			destinationUrl =  yadaRegistrationRequest.getDestinationUrl();
		//		}

		String fullLink = yadaWebUtil.getFullUrl(destinationUrl + yadaTokenHandler.makeLink(yadaRegistrationRequest, null), locale);

		final Map<String, Object> templateParams = new HashMap<>();
		templateParams.put("fullLink", fullLink);
		templateParams.put("email", yadaRegistrationRequest.getEmail());

		Map<String, String> inlineResources = new HashMap<>();
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
    @Deprecated // Use the same method on yadaTokenHandler
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
    @Deprecated // Use the same method on yadaTokenHandler
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
    @Deprecated // Use the same method on yadaTokenHandler
    public String makeAutologinLink(String targetAction, YadaUserCredentials targetUser, Date expiration, String hashCommand, String myServerAddress) {
    	YadaAutoLoginToken yadaAutoLoginToken = new YadaAutoLoginToken();
    	yadaAutoLoginToken.setExpiration(expiration);
    	yadaAutoLoginToken.setYadaUserCredentials(targetUser);
    	yadaAutoLoginToken = yadaAutoLoginTokenDao.save(yadaAutoLoginToken);
    	yadaAutoLoginTokenDao.deleteExpired(); // Rimuovo quelle vecchie, per pulizia
    	StringBuilder result = new StringBuilder(myServerAddress);
    	result.append("/autologin/");
    	result.append(yadaTokenHandler.makeLink(yadaAutoLoginToken.getId(), yadaAutoLoginToken.getToken(), null));
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
    @Deprecated // Use the same method on yadaTokenHandler
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
