package net.yadaframework.security.components;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import net.yadaframework.components.YadaUtil;
import net.yadaframework.components.YadaWebUtil;
import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.exceptions.YadaInvalidUsageException;
import net.yadaframework.security.components.YadaSecurityTokenCodec.ParsedSecurityToken;
import net.yadaframework.security.persistence.entity.YadaAutoLoginToken;
import net.yadaframework.security.persistence.entity.YadaRegistrationRequest;
import net.yadaframework.security.persistence.entity.YadaUserCredentials;
import net.yadaframework.security.persistence.repository.YadaAutoLoginTokenDao;

/**
 * Creates and parses the opaque bearer-token links of autologin and registration requests.
 * The autologin link has the following format: /autologin/id-secret?action=someAction#hashCommand
 * where id and secret belong to a YadaAutoLoginToken (see {@link YadaSecurityTokenCodec}). The link parameters are not
 * signed: the action is validated as a local path when the link is created and again when it is used.
 * <p>
 * Links can be built only from the instance returned when the token was issued, because the database keeps only the
 * digest of the secret: to send a link later, issue a new token.
 */

@Component
public class YadaTokenHandler {

    @Autowired private YadaUtil yadaUtil;
    @Autowired private YadaWebUtil yadaWebUtil;
	@Autowired private YadaConfiguration config;
	@Autowired private YadaAutoLoginTokenDao yadaAutoLoginTokenDao;
	@Autowired private YadaSecurityTokenCodec yadaSecurityTokenCodec;

	/**
	 * Create a new YadaAutoLoginToken for the given user that expires after the configured amount of hours (config/security/autologinExpirationHours)
	 * @param targetUser
	 * @return the new token, whose link can be built with makeAutologinLink() 
	 */
	public YadaAutoLoginToken makeAutoLoginToken(YadaUserCredentials targetUser) {
		Date expiration = yadaUtil.addHours(new Date(), config.getAutologinExpirationHours());
		return makeAutoLoginToken(targetUser, expiration);
	}

	/**
	 * Create a new YadaAutoLoginToken for the given user
	 * @param targetUser
	 * @param expiration the time from which the token can't be used, required
	 * @return the new token, whose link can be built with makeAutologinLink() 
	 * @throws YadaInvalidUsageException when the expiration is null
	 */
    public YadaAutoLoginToken makeAutoLoginToken(YadaUserCredentials targetUser, Date expiration) {
    	yadaAutoLoginTokenDao.deleteExpired(); // Cleanup expired ones
    	return yadaAutoLoginTokenDao.issue(targetUser, expiration);
    }

	/**
	 * If the myServerAddress param is null, fetch it either from request (in development) or from the configuration.
	 * @param myServerAddress can be null
	 * @param request can be null
	 * @return
	 */
	private String ensureServerAddress(String myServerAddress, HttpServletRequest request) {
		if (myServerAddress==null) {
			// In the dev environment we use the request to get the address, otherwise it must have been configured
			myServerAddress = config.isDevelopmentEnvironment() ? config.getWebappAddress(request) : config.getWebappAddress();
		}
		if (myServerAddress==null) {
			throw new YadaInvalidUsageException("The server address should be specified in the configuration");
		}
		return myServerAddress;
	}

	/**
	 * Return the autologin link generated from the given parameters
	 * @param yadaAutoLoginToken a token just returned by makeAutoLoginToken()
	 * @param targetAction the local path to open after login, e.g. "/dashboard?tab=users"; null for "/"
	 * @param hashCommand
	 * @param myServerAddress
	 * @param request
	 * @return
	 * @throws YadaInvalidUsageException when the token has not just been issued or the action is not a local path
	 */
	public String makeAutologinLink(YadaAutoLoginToken yadaAutoLoginToken, String targetAction, String hashCommand, String myServerAddress, HttpServletRequest request) {
		myServerAddress = ensureServerAddress(myServerAddress, request);
		return makeAutologinLink(yadaAutoLoginToken, targetAction, hashCommand, myServerAddress);
	}

	/**
	 * Return the autologin link generated from the given parameters
	 * @param yadaAutoLoginToken a token just returned by makeAutoLoginToken()
	 * @param targetAction the local path to open after login, e.g. "/dashboard?tab=users"; null for "/".
	 * 	It must be accepted by {@link YadaWebUtil#safeLocalRedirect(String)}.
	 * @param hashCommand optional "anchor" starting with #
	 * @param myServerAddress our server address
	 * @return
	 * @throws YadaInvalidUsageException when the token has not just been issued or the action is not a local path
	 */
	public String makeAutologinLink(YadaAutoLoginToken yadaAutoLoginToken, String targetAction, String hashCommand, String myServerAddress) {
		String secret = yadaAutoLoginToken.getRawSecret();
		if (secret==null) {
			throw new YadaInvalidUsageException("The link of an autologin token can only be built when the token is issued: create a new token");
		}
		String action = targetAction==null ? "/" : targetAction;
		if (!action.equals(yadaWebUtil.safeLocalRedirect(action))) {
			throw new YadaInvalidUsageException("The autologin action must be a local path");
		}
		StringBuilder result = new StringBuilder(myServerAddress);
		result.append("/autologin/");
		result.append(yadaSecurityTokenCodec.format(yadaAutoLoginToken.getId(), secret));
		result.append("?action=").append(yadaWebUtil.urlEncode(action));
		if (hashCommand!=null) {
			result.append(hashCommand);
		}
		return result.toString();
	}

	/**
	 * Create a token-link
	 * @param yadaRegistrationRequest a request just returned by YadaRegistrationRequestDao.issue()
	 * @param linkParameters name-value paris of url parameters to add at the end - can be null or empty
	 * @return a string &lt;ID>-&lt;secret>, followed by the parameters if any
	 * @throws YadaInvalidUsageException when the request has not just been issued
	 */
	// used by YadaSecurityEmailService
	public String makeLink(YadaRegistrationRequest yadaRegistrationRequest, Map<String, String> linkParameters) {
		String secret = yadaRegistrationRequest.getRawSecret();
		if (secret==null) {
			throw new YadaInvalidUsageException("The link of a registration request can only be built when the request is issued: create a new request");
		}
		StringBuilder result = new StringBuilder(yadaSecurityTokenCodec.format(yadaRegistrationRequest.getId(), secret)).append("?");
		if (linkParameters!=null) {
			for (String key : linkParameters.keySet()) {
				String name = URLEncoder.encode(key, StandardCharsets.UTF_8);
				String value = URLEncoder.encode(linkParameters.get(key), StandardCharsets.UTF_8);
				result.append(name).append("=").append(value).append("&");
			}
		}
		String link = result.toString();
		link = StringUtils.removeEnd(link, "&");
		link = StringUtils.removeEnd(link, "?");
		return link;
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

	/**
	 * Splits a token-link string into id and secret. Nothing is logged, because the value may carry a secret.
	 * @param linkId with the format &lt;ID>-&lt;secret>
	 * @return the id and secret, or null when the format is not valid
	 * @see YadaSecurityTokenCodec#parse(String)
	 */
	public ParsedSecurityToken parseLink(String linkId) {
		return yadaSecurityTokenCodec.parse(linkId);
	}

}
