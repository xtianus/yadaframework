package net.yadaframework.security.components;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import net.yadaframework.components.YadaUtil;
import net.yadaframework.components.YadaWebUtil;
import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.exceptions.YadaInvalidUsageException;
import net.yadaframework.security.persistence.entity.YadaAutoLoginToken;
import net.yadaframework.security.persistence.entity.YadaRegistrationRequest;
import net.yadaframework.security.persistence.entity.YadaUserCredentials;
import net.yadaframework.security.persistence.repository.YadaAutoLoginTokenDao;

/**
 * Handles autologin links: creation and parsing.
 * The autologin link has the following format: /autologin/id-token?action=someAction#hashCommand
 * id and token are from the same YadaAutoLoginToken stored in the database.
 */

@Component
public class YadaTokenHandler {
	private final transient Logger log = LoggerFactory.getLogger(getClass());

    @Autowired private YadaUtil yadaUtil;
    @Autowired private YadaWebUtil yadaWebUtil;
	@Autowired private YadaConfiguration config;
	@Autowired private YadaAutoLoginTokenDao yadaAutoLoginTokenDao;

	/**
	 * Create a new YadaAutoLoginToken for the given user that expires after the configured amount of hours (config/security/autologinExpirationHours)
	 * @param targetUser
	 * @return
	 */
	public YadaAutoLoginToken makeAutoLoginToken(YadaUserCredentials targetUser) {
		Date expiration = yadaUtil.addHours(new Date(), config.getAutologinExpirationHours());
		return makeAutoLoginToken(targetUser, expiration);
	}

	/**
	 * Create a new YadaAutoLoginToken for the given user
	 * @param targetUser
	 * @param expiration can be null to not expire ever
	 * @return
	 */
    public YadaAutoLoginToken makeAutoLoginToken(YadaUserCredentials targetUser, Date expiration) {
    	yadaAutoLoginTokenDao.deleteExpired(); // Cleanup expired ones
    	YadaAutoLoginToken yadaAutoLoginToken = new YadaAutoLoginToken();
    	yadaAutoLoginToken.setExpiration(expiration);
    	yadaAutoLoginToken.setYadaUserCredentials(targetUser);
    	yadaAutoLoginToken = yadaAutoLoginTokenDao.save(yadaAutoLoginToken);
    	return yadaAutoLoginToken;
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
	 * @param yadaAutoLoginToken
	 * @param targetAction
	 * @param hashCommand
	 * @param myServerAddress
	 * @param request
	 * @return
	 */
	public String makeAutologinLink(YadaAutoLoginToken yadaAutoLoginToken, String targetAction, String hashCommand, String myServerAddress, HttpServletRequest request) {
		myServerAddress = ensureServerAddress(myServerAddress, request);
    	StringBuilder result = new StringBuilder(myServerAddress);
    	result.append("/autologin/");
    	result.append(makeLink(yadaAutoLoginToken.getId(), yadaAutoLoginToken.getToken(), null));
		result.append("?action=").append(yadaWebUtil.urlEncode(targetAction));
		if (hashCommand!=null) {
			result.append(hashCommand);
		}
    	return result.toString();
	}

	//Idem but without HttpServletRequest because we have the myServerAddress not null
	/**
	 * Return the autologin link generated from the given parameters
	 * @param yadaAutoLoginToken
	 * @param targetAction
	 * @param hashCommand
	 * @param myServerAddress
	 * @param request
	 * @return
	 */
	public String makeAutologinLink(YadaAutoLoginToken yadaAutoLoginToken, String targetAction, String hashCommand, String myServerAddress) {
		StringBuilder result = new StringBuilder(myServerAddress);
		result.append("/autologin/");
		result.append(makeLink(yadaAutoLoginToken.getId(), yadaAutoLoginToken.getToken(), null));
		result.append("?action=").append(yadaWebUtil.urlEncode(targetAction));
		if (hashCommand!=null) {
			result.append(hashCommand);
		}
		return result.toString();
	}

	/**
	 * Create a token-link
	 * @param yadaRegistrationRequest
	 * @param linkParameters name-value paris of url parameters to add at the end - can be null or empty
	 * @return una stringa <ID>-<token>
	 */
	// used by YadaSecurityEmailService
	public String makeLink(YadaRegistrationRequest yadaRegistrationRequest, Map<String, String> linkParameters) {
		return makeLink(yadaRegistrationRequest.getId(), yadaRegistrationRequest.getToken(), linkParameters);
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
	 * Create a token-link, used both for autologin links and registration links.
	 * @param id
	 * @param token
	 * @param linkParameters name-value paris of url parameters to add at the end - can be null or empty
	 * @return a string <ID>-<token>
	 */
	public String makeLink(long id, long token, Map<String, String> linkParameters) {
		StringBuilder result = new StringBuilder(id + "-" + token).append("?");
		if (linkParameters!=null) {
			try {
				for (String key : linkParameters.keySet()) {
					String name = URLEncoder.encode(key, "UTF-8");
					String value = URLEncoder.encode(linkParameters.get(key), "UTF-8");
					result.append(name).append("=").append(value).append("&");
				}
			} catch (UnsupportedEncodingException e) {
				log.error("Impossible error occurred", e);
			}
		}
		String link = result.toString();
		link = StringUtils.removeEnd(link, "&");
		link = StringUtils.removeEnd(link, "?");
		return link;
	}

	/**
	 * Splits a token-link string into the two components: id and token.
	 * @param linkId with the format <ID>-<token>
	 * @return a two dimensional array with {<ID>, <token>}
	 */
	public long[] parseLink(String linkId) {
		try {
			String[] parts = linkId.split("-");
			long[] result = new long[2];
			result[0] = Long.parseLong(parts[0]);
			result[1] = Long.parseLong(parts[1]);
			return result;
		} catch (Exception e) {
			log.debug("Invalid linkId '{}'", linkId);
			return null;
		}
	}

}
