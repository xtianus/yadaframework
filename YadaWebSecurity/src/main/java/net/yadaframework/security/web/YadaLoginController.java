package net.yadaframework.security.web;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.yadaframework.components.YadaNotify;
import net.yadaframework.components.YadaWebUtil;
import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.security.components.YadaAuthenticationFailureHandler;
import net.yadaframework.security.components.YadaAuthenticationSuccessHandler;
import net.yadaframework.security.components.YadaTokenHandler;
import net.yadaframework.security.components.YadaUserDetailsService;
import net.yadaframework.security.persistence.entity.YadaAutoLoginToken;
import net.yadaframework.security.persistence.entity.YadaUserCredentials;
import net.yadaframework.security.persistence.repository.YadaAutoLoginTokenDao;
import net.yadaframework.web.YadaViews;

/**
 * This controller handles the opening of the login form/modal, autologin links, and the result of an ajax login.
 * You still need a @RequestMapping for the FailureUrlNormalRequest request which is called at initial or failed login
 * and normally goes to the homepage (see your application's SecurityConfig). The YadaTools init task should have taken
 * care of that already.
 */
@Controller
public class YadaLoginController {
	private final transient Logger log = LoggerFactory.getLogger(getClass());

	@Autowired private YadaNotify yadaNotify;
	@Autowired private YadaWebUtil yadaWebUtil;
	@Autowired private YadaSession yadaSession;
	@Autowired private YadaConfiguration yadaConfiguration;
	@Autowired private YadaTokenHandler yadaTokenHandler;
	@Autowired private YadaAutoLoginTokenDao yadaAutoLoginTokenDao;
	@Autowired private YadaUserDetailsService yadaUserDetailsService;
	@Autowired private YadaAuthenticationFailureHandler failureHandler;
	@Autowired protected YadaAuthenticationSuccessHandler successHandler;
	
	/**
	 * This can be the success handler target url in order to close the login modal after successful login
	 * @return
	 */
	@RequestMapping("/yada/closeModal")
	public String closeModal() {
		return YadaViews.AJAX_CLOSE_MODAL;
	}
	
	/**
	 * This method can be set as the default ajax login target in a Security Config, and will redirect to the targetUrl when specified.
	 * Example: successHandler.setDefaultTargetUrlAjaxRequest("/yadaLoginSuccess?targetUrl=/configurator/");
	 * @param targetUrl the final url after login, without any language in the path (will be added)
	 * @param model
	 * @return
	 */
	@RequestMapping("/yadaLoginSuccess")
	public String yadaLoginSuccess(String targetUrl, Model model) {
		if (targetUrl==null) {
			return YadaViews.AJAX_SUCCESS;
		}
		// Automatically add the language in the target url path when needed
		if (yadaConfiguration.isLocalePathVariableEnabled()) {
			Locale locale = LocaleContextHolder.getLocale();
			targetUrl = yadaWebUtil.enhanceUrl(targetUrl, locale);
//			if (locale!=null) {
//				String langPath = "/" + locale.getLanguage(); // e.g. "/en"
//				if(!targetUrl.startsWith(langPath)) {
//					targetUrl = langPath + targetUrl;
//				}
//			}
		}
		model.addAttribute("targetUrl", targetUrl);
		return YadaViews.AJAX_REDIRECT;
	}

	@RequestMapping("/ajaxLoginForm")
	@Deprecated // This should be application-specific
	public String ajaxLoginForm(Model model) {
		return "/modalLogin";
	}

	@RequestMapping("/ajaxLoginOk")
	@ResponseBody
	@Deprecated
	// The "loginSuccess" return body causes a page reload. If a page reload is needed, do it in the login form successHandler.
	public String ajaxLoginOk(Model model) {
		return "loginSuccess";
	}

	@RequestMapping("/autologin/{tokenLink}")
	public String autologin(@PathVariable String tokenLink, String action, RedirectAttributes  redirectAttributes, HttpSession session, HttpServletRequest request, HttpServletResponse response) {
		try {
			action = URLDecoder.decode(action, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			log.error("This will never happen (ignored)", e);
		}
		long[] parts = yadaTokenHandler.parseLink(tokenLink);
		if (parts!=null && parts.length==2) {
			long id = parts[0];
			long token = parts[1];
			List<YadaAutoLoginToken> yadaAutoLoginTokenList = yadaAutoLoginTokenDao.findByIdAndTokenOrderByTimestampDesc(id, token);
			if (yadaAutoLoginTokenList!=null && !yadaAutoLoginTokenList.isEmpty()) {
				YadaAutoLoginToken yadaAutoLoginToken = yadaAutoLoginTokenList.get(0);
				Date expiration = yadaAutoLoginToken.getExpiration();
				if (expiration==null || expiration.after(new Date())) {
					YadaUserCredentials yadaUserCredentials = yadaAutoLoginToken.getYadaUserCredentials();
					if (yadaSession.getCurrentUserProfile()!=null) {
						// A user is already logged in.
						// I wanted to logout the current user but it doesn't work: if I use SecurityContextLogoutHandler().logout() the Session is cleared
						// and I get a login page, while if I use request.logout() the session is not cleared but a new session copy of the current one before
						// the yadaSession.clearCaches() call is created, so that I have a mixed situation where the spring user is the new one and the session
						// contains the old UserProfile id.
						// This is probably because the HTTPSession contains the UserPrincipal and other stuff that is not reset.
						// So I just exit, showing an error if the current user is different from the new one.
						if (!yadaSession.getCurrentUserProfile().getUserCredentials().getId().equals(yadaUserCredentials.getId())) {
//					try {
//						// Attempt logout of current user
//						// https://stackoverflow.com/questions/5727380/how-to-manually-log-out-a-user-with-spring-security/5727444
//						request.logout();
//						// new SecurityContextLogoutHandler().logout(request, null, null);
//					} catch (ServletException e) {
//						log.debug("Can't logout current user (ignored)");
//					}
							// Current user is different, just exit with error
							// TODO localized message
							yadaNotify.title("Already logged in", redirectAttributes).error().message("You can't perform an autologin while logged in as a different user").add();
							return "redirect:"+action;
						}
						// If the current user is the same, just don't do anything
					} else {
						// No user is currently logged in
						log.info("Performing autologin with token {} to username {} ", tokenLink, yadaUserCredentials.getUsername());
						yadaSession.clearCaches();
						try {
							// Need to call the login success handler to perform eventual login tasks
							Authentication authentication = yadaUserDetailsService.authenticateAs(yadaUserCredentials, request, response);
							successHandler.onAuthenticationSuccessCustom(request, authentication);
						} catch (Exception e) {
							log.error("Auhtentication success handler failed on autologin (ignored)", e);
						}
					}
					// TODO delete autologin link after use
					// TODO Questo l'ho disabilitato fintanto che non aggiusto che l'autenticazione (social) non ti porta sulla pagina inizialmente richiesta
					// TODO yadaAutoLoginTokenRepository.delete(yadaAutoLoginToken); // Tokens are deleted at first use (for security reasons)
				} else {
					log.info("YadaAutoLoginToken expired for {}", tokenLink);
					// TODO localized message
					yadaNotify.title("Link expired", redirectAttributes).error().message("The provided address is no longer valid").add();
					return "redirect:"+failureHandler.getFailureUrlNormalRequest(); // Redirect to login page
				}
			} else {
				// Token expired or forged
				log.info("No yadaAutoLoginToken found for {}", tokenLink);
				// TODO localized message
				yadaNotify.title("Link expired", redirectAttributes).error().message("The provided address is no longer valid").add();
				return "redirect:"+failureHandler.getFailureUrlNormalRequest(); // Redirect to login page
			}
		} else {
			// TODO localized message
			yadaNotify.title("Invalid URL", redirectAttributes).error().message("The provided address is invalid").add();
			return "redirect:"+failureHandler.getFailureUrlNormalRequest(); // Redirect to login page
		}
		// If not authenticated, this will trigger authentication but will clear any yadaNotify because of the double redirect.
		return "redirect:"+action;
	}

	@RequestMapping("/loginModal")
	@Deprecated // This should be application-specific
	public String loginModal() {
		return "/fragments/modalLogin";
	}

}
