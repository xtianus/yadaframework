package net.yadaframework.security.components;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.social.facebook.api.Facebook;
import org.springframework.social.facebook.api.User;
import org.springframework.social.facebook.api.impl.FacebookTemplate;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;

import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.security.persistence.entity.YadaSocialCredentials;
import net.yadaframework.security.persistence.entity.YadaUserCredentials;
import net.yadaframework.security.persistence.repository.YadaSocialCredentialsDao;
import net.yadaframework.security.persistence.repository.YadaUserCredentialsDao;
import net.yadaframework.security.web.YadaSocialRegistrationData;
import net.yadaframework.web.YadaViews;

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

//	@Autowired private YadaWebUtil yadaWebUtil;
//	@Autowired private YadaNotify yadaNotify;
	@Autowired private YadaSecurityUtil yadaSecurityUtil;
	@Autowired private YadaSocialCredentialsDao yadaSocialCredentialsDao;
	@Autowired private YadaUserCredentialsDao yadaUserCredentialsDao;
	@Autowired private YadaConfiguration config;
	@Autowired private YadaUserDetailsService yadaUserDetailsService;
	
	private GoogleIdTokenVerifier googleIdTokenVerifier;

	private class Credentials {
		YadaSocialCredentials yadaSocialCredential = null;
		YadaUserCredentials yadaUserCredential = null;
	}
	
	@EventListener
	public void init(ContextRefreshedEvent event) throws GeneralSecurityException, IOException {
		HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
		JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
		googleIdTokenVerifier = new GoogleIdTokenVerifier.Builder(httpTransport, jsonFactory)
		    .setAudience(Collections.singletonList(config.getGoogleClientId()))
		    .build();
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
		yadaSocialCredentialsDao.save(credentials.yadaSocialCredential);
	}
	
	/**
	 * Perform google authentication. The accessToken must be retrieved using the google SDK.
	 * @param accessToken retrieved using the google SDK
	 * @param verifiedOnly true to prevent login with non-verified accounts
	 * @param model
	 * @return the authentication outcome containing the user social profile
	 */
	public YadaSocialAuthenticationOutcome googleLogin(String accessToken, boolean verifiedOnly, Model model) {
		try {
			GoogleIdToken idToken = googleIdTokenVerifier.verify(accessToken);
			if (idToken != null) {
				Payload profile = idToken.getPayload();
				
				// Get profile information from payload
				String email = profile.getEmail();
				  
				if (StringUtils.isBlank(email)) {
					return YadaSocialAuthenticationOutcome.UNAUTHENTICATED_NOPROFILE;
				}
				  
				boolean emailVerified = Boolean.TRUE.equals(profile.getEmailVerified());
				if (verifiedOnly && !emailVerified) {
					return YadaSocialAuthenticationOutcome.UNAUTHENTICATED_NOTVERIFIED;
				}
				YadaSocialRegistrationData yadaSocialRegistrationData = new YadaSocialRegistrationData();
				yadaSocialRegistrationData.socialId = profile.getSubject();
				yadaSocialRegistrationData.email = StringUtils.trimToEmpty(email.toLowerCase());
				yadaSocialRegistrationData.name = (String) profile.get("name");
				yadaSocialRegistrationData.surname = (String) profile.get("family_name");
				yadaSocialRegistrationData.accessToken = accessToken;
				yadaSocialRegistrationData.socialType = config.getGoogleType();
				yadaSocialRegistrationData.pictureUrl = (String) profile.get("picture");
				String localeString = (String) profile.get("locale");
				try {
					yadaSocialRegistrationData.locale = LocaleUtils.toLocale(localeString);
				} catch (Exception e) {
					log.info("Can't parse locale '{}' (ignored)", localeString);
				}
				
				return finalizeLogin(yadaSocialRegistrationData, model);
				
			} else {
				return YadaSocialAuthenticationOutcome.UNAUTHENTICATED_NOPROFILE;
			}
		} catch (Exception e) {
			log.error("Google Exception", e);
			return YadaSocialAuthenticationOutcome.UNAUTHENTICATED_OTHER;
		}

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
			// // Using spring social facebook < 3.0 I get the following exception:
			// // UncategorizedApiException: (#12) bio field is deprecated for versions v2.8 and higher
			// User profile = facebook.userOperations().getUserProfile();
			// Workaround (https://stackoverflow.com/a/39902266/587641)
			// Available fields: { "id", "about", "age_range", "birthday", "context", "cover", "currency", "devices", "education", "email", "favorite_athletes", "favorite_teams", "first_name", "gender", "hometown", "inspirational_people", "installed", "install_type", "is_verified", "languages", "last_name", "link", "locale", "location", "meeting_for", "middle_name", "name", "name_format", "political", "quotes", "payment_pricepoints", "relationship_status", "religion", "security_settings", "significant_other", "sports", "test_group", "timezone", "third_party_id", "updated_time", "verified", "video_upload_limits", "viewer_can_send_gift", "website", "work"}
			String [] fields = { "id", "email", "first_name", "last_name", "verified" };
			// The field 'email' is only accessible on the User object after the user grants the 'email' permission.
			User profile = facebook.fetchObject("me", User.class, fields);
			if (profile==null || StringUtils.isBlank(profile.getEmail())) {
				return YadaSocialAuthenticationOutcome.UNAUTHENTICATED_NOPROFILE;
			}
			log.debug("Social login for {} - {}", profile.getName(), profile.getEmail());
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
			
			return finalizeLogin(yadaSocialRegistrationData, model);
			
		} catch (Throwable e) {
			log.error("Facebook Exception", e);
			return YadaSocialAuthenticationOutcome.UNAUTHENTICATED_OTHER;
		}
	}
	
	private YadaSocialAuthenticationOutcome finalizeLogin(YadaSocialRegistrationData yadaSocialRegistrationData, Model model) {
		String savedRequestUrl = yadaSecurityUtil.getSavedRequestUrl();
		
		// Check if we already got the social credentials, and log in
		List<YadaSocialCredentials> yadaSocialCredentialsList = yadaSocialCredentialsDao.findBySocialIdAndType(yadaSocialRegistrationData.socialId, config.getFacebookType());
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
		YadaUserCredentials yadaUserCredentials = yadaUserCredentialsDao.findFirstByUsername(yadaSocialRegistrationData.email);
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
