package net.yadaframework.security.components;

import java.util.List;

import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.social.facebook.api.Facebook;
import org.springframework.social.facebook.api.User;
import org.springframework.social.facebook.api.impl.FacebookTemplate;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.security.YadaUserDetailsService;
import net.yadaframework.security.persistence.entity.YadaSocialCredentials;
import net.yadaframework.security.persistence.entity.YadaUserCredentials;
import net.yadaframework.security.persistence.repository.YadaSocialCredentialsRepository;
import net.yadaframework.security.persistence.repository.YadaUserCredentialsRepository;
import net.yadaframework.web.YadaNotify;
import net.yadaframework.web.YadaSocialRegistrationData;
import net.yadaframework.web.YadaViews;
import net.yadaframework.web.YadaWebUtil;

/**
 * Social network login
 *
 */
@Component
public class YadaSecuritySocial {
	private final transient Logger log = LoggerFactory.getLogger(getClass());
	
	public enum YadaSocialAuthenticationOutcome {
		/**
		 * The user has been socially authenticated and does not need a redirect
		 */
		AUTHENTICATED_NORMAL,
		/**
		 * The user has been socially authenticated and needs a redirect to the original url requested
		 */
		AUTHENTICATED_REDIRECT,
		/**
		 * The user has been socially authenticated but it is not yet registered on the server
		 */
		AUTHENTICATED_UNREGISTERED,
		/**
		 * The social profile can not be retrieved
		 */
		UNAUTHENTICATED_NOPROFILE,
		/**
		 * The social profile is not verified and the verification check was enabled
		 */
		UNAUTHENTICATED_NOTVERIFIED,
		/**
		 * Generic social authentication error
		 */
		UNAUTHENTICATED_OTHER;
		
		/**
		 * User social profile information
		 */
		public YadaSocialRegistrationData yadaSocialRegistrationData;
		
		public YadaSocialAuthenticationOutcome setYadaSocialRegistrationData(YadaSocialRegistrationData yadaSocialRegistrationData) {
			this.yadaSocialRegistrationData = yadaSocialRegistrationData;
			return this;
		}
		
	}

	@Autowired private YadaWebUtil yadaWebUtil;
	@Autowired private YadaNotify yadaNotify;
	@Autowired private YadaSecurityUtil yadaSecurityUtil;
	@Autowired private YadaSocialCredentialsRepository yadaSocialCredentialsRepository;
	@Autowired private YadaUserCredentialsRepository yadaUserCredentialsRepository;
	@Autowired private YadaConfiguration config;
	@Autowired private YadaUserDetailsService yadaUserDetailsService;

	private class Credentials {
		YadaSocialCredentials yadaSocialCredential = null;
		YadaUserCredentials yadaUserCredential = null;
	}
	
	/**
	 * Link social credentials and normal credentials
	 * @param credentials
	 * @param email
	 * @param socialId
	 */
	private void linkSocialCredentials(Credentials credentials, String email, String socialId) {
		if (credentials.yadaSocialCredential==null) {
			credentials.yadaSocialCredential = new YadaSocialCredentials();
			credentials.yadaSocialCredential.setYadaUserCredentials(credentials.yadaUserCredential);
		}
		credentials.yadaSocialCredential.setEmail(email);
		credentials.yadaSocialCredential.setSocialId(socialId);
		credentials.yadaSocialCredential.setType(config.getFacebookType());
		yadaSocialCredentialsRepository.save(credentials.yadaSocialCredential);
	}
	
	/**
	 * Perform facebook authentication. The accessToken must be retrieved using the Facebook SDK.
	 * @param accessToken retrieved using the Facebook SDK
	 * @param verifiedOnly true to prevent login with non-verified accounts
	 * @param model
	 * @return the authentication outcome containing the user social profile
	 */
	public YadaSocialAuthenticationOutcome facebookLogin(String accessToken, boolean verifiedOnly, Model model) {
		try {
			Facebook facebook = new FacebookTemplate(accessToken);
			User profile = facebook.userOperations().getUserProfile();
			if (profile==null || StringUtils.isBlank(profile.getEmail())) {
				return YadaSocialAuthenticationOutcome.UNAUTHENTICATED_NOPROFILE;
			}
			log.debug("Social login for {}", profile.getEmail());
			if (verifiedOnly && !profile.isVerified()) {
				return YadaSocialAuthenticationOutcome.UNAUTHENTICATED_NOTVERIFIED;
			}
			
			YadaSocialRegistrationData yadaSocialRegistrationData = new YadaSocialRegistrationData();
			yadaSocialRegistrationData.socialId = profile.getId();
			yadaSocialRegistrationData.email = StringUtils.trimToEmpty(profile.getEmail()).toLowerCase();
			yadaSocialRegistrationData.name = profile.getFirstName();
			yadaSocialRegistrationData.surname = profile.getLastName();
			yadaSocialRegistrationData.accessToken = accessToken;
			yadaSocialRegistrationData.socialType = config.getFacebookType();
			
			String savedRequestUrl = yadaSecurityUtil.getSavedRequestUrl();
			
			// Check if we already got the social credentials, and log in
			List<YadaSocialCredentials> yadaSocialCredentialsList = yadaSocialCredentialsRepository.findBySocialIdAndType(yadaSocialRegistrationData.socialId, config.getFacebookType());
			if (!yadaSocialCredentialsList.isEmpty()) {
				YadaSocialCredentials yadaSocialCredentials = yadaSocialCredentialsList.get(0);
				YadaUserCredentials userCredentials = yadaSocialCredentials.getYadaUserCredentials();
				yadaUserDetailsService.authenticateAs(userCredentials);
				log.debug("Social Login: user='{}'", userCredentials.getUsername());
				if (savedRequestUrl!=null) {
					model.addAttribute(YadaViews.AJAX_REDIRECT_URL, savedRequestUrl);
					return YadaSocialAuthenticationOutcome.AUTHENTICATED_REDIRECT.setYadaSocialRegistrationData(yadaSocialRegistrationData);
				}
				return YadaSocialAuthenticationOutcome.AUTHENTICATED_NORMAL.setYadaSocialRegistrationData(yadaSocialRegistrationData);
			}
			
			// Check if we already got the email, and log in after creating the social credentials
			YadaUserCredentials yadaUserCredentials = yadaUserCredentialsRepository.findFirstByUsername(yadaSocialRegistrationData.email);
			if (yadaUserCredentials!=null) {
				Credentials credentials = new Credentials();
				credentials.yadaUserCredential = yadaUserCredentials;
				linkSocialCredentials(credentials, yadaSocialRegistrationData.email, yadaSocialRegistrationData.socialId);
				yadaUserDetailsService.authenticateAs(credentials.yadaUserCredential);
				log.debug("Social Login: user='{}'", credentials.yadaUserCredential.getUsername());
				
				if (savedRequestUrl!=null) {
					model.addAttribute(YadaViews.AJAX_REDIRECT_URL, savedRequestUrl);
					return YadaSocialAuthenticationOutcome.AUTHENTICATED_REDIRECT.setYadaSocialRegistrationData(yadaSocialRegistrationData);
				}
				return YadaSocialAuthenticationOutcome.AUTHENTICATED_NORMAL.setYadaSocialRegistrationData(yadaSocialRegistrationData);
			}
			
			// User does not exist, proceed with registration
			return YadaSocialAuthenticationOutcome.AUTHENTICATED_UNREGISTERED.setYadaSocialRegistrationData(yadaSocialRegistrationData);
		} catch (Throwable e) {
			log.error("Facebook Exception", e);
			return YadaSocialAuthenticationOutcome.UNAUTHENTICATED_OTHER;
		}
	}

//	private YadaRegistrationRequest makeRegistrationRequest(YadaRegistrationRequest yadaRegistrationRequest) {
//		if (yadaRegistrationRequest==null) {
//			yadaRegistrationRequest = new YadaRegistrationRequest();
//		}
////		YadaClause trattamentoDati = yadaClauseRepository.getTrattamentoDati();
////		yadaRegistrationRequest.setTrattamentoDati(trattamentoDati);
//		return yadaRegistrationRequest;
//	}
	
}
