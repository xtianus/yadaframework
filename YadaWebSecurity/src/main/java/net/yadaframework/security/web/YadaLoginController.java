package net.yadaframework.security.web;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
import net.yadaframework.security.components.YadaSecurityUtil;
import net.yadaframework.security.components.YadaSecurityTokenCodec.ParsedSecurityToken;
import net.yadaframework.security.components.YadaTokenHandler;
import net.yadaframework.security.components.YadaUserDetailsService;
import net.yadaframework.security.persistence.entity.YadaAutoLoginToken;
import net.yadaframework.security.persistence.entity.YadaUserCredentials;
import net.yadaframework.security.persistence.repository.YadaAutoLoginTokenDao;
import net.yadaframework.web.YadaViews;

/**
 * This controller handles the opening of the login form/modal, autologin links, and the result of an ajax login.
 * <p>
 * An autologin link (see YadaTokenHandler) opens a confirmation page with GET, which neither logs in nor uses the token,
 * so that link scanners can't consume it and a forged link can't silently log a visitor into another account. The
 * login happens with an explicit POST of that page, protected by the CSRF token of the visitor session, and uses the
 * token once. A visitor who is logged in as another user is never logged out by a link. Every redirect goes to a
 * local path (see {@link YadaWebUtil#safeLocalRedirect(String)}).
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
	@Autowired private YadaSecurityUtil yadaSecurityUtil;
	/** The CSRF token repository, when the application declares it as a bean; the Spring Security default otherwise */
	@Autowired private ObjectProvider<CsrfTokenRepository> csrfTokenRepositoryProvider;
	
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
	 * @param targetUrl the final url after login, without any language in the path (will be added). It must be a local path
	 * accepted by {@link YadaWebUtil#safeLocalRedirect(String)}, otherwise the redirect goes to "/"
	 * @param model
	 * @return
	 */
	@RequestMapping("/yadaLoginSuccess")
	public String yadaLoginSuccess(String targetUrl, Model model) {
		if (targetUrl==null) {
			return YadaViews.AJAX_SUCCESS;
		}
		// The parameter comes from the client: a target that would leave the site becomes "/"
		targetUrl = yadaWebUtil.safeLocalRedirect(targetUrl);
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

	/** The confirmation page of autologin links */
	public static final String AUTOLOGIN_CONFIRM_VIEW = "/yada/security/autologinConfirm";

	/**
	 * Shows the autologin confirmation page of a valid link. The token is not used and nobody is logged in.
	 * The page shows the username of the link and posts to {@link #autologin(String, String, RedirectAttributes, HttpServletRequest, HttpServletResponse)}.
	 * @param tokenLink the "id-secret" part of the link
	 * @param action the local path to open after login; any other value becomes "/"
	 */
	@GetMapping("/autologin/{tokenLink}")
	public String autologinConfirmation(@PathVariable String tokenLink, String action, Model model, RedirectAttributes redirectAttributes) {
		String safeAction = yadaWebUtil.safeLocalRedirect(action==null ? "/" : action);
		ParsedSecurityToken parsedToken = yadaTokenHandler.parseLink(tokenLink);
		YadaAutoLoginToken yadaAutoLoginToken = yadaAutoLoginTokenDao.findValid(parsedToken);
		if (yadaAutoLoginToken==null) {
			return invalidAutologin(parsedToken, redirectAttributes);
		}
		YadaUserCredentials yadaUserCredentials = yadaAutoLoginToken.getYadaUserCredentials();
		// The authenticated principal, not the user profile, tells whether someone is logged in
		String currentUsername = yadaSecurityUtil.getUsername();
		if (currentUsername!=null) {
			if (!currentUsername.equalsIgnoreCase(yadaUserCredentials.getUsername())) {
				alreadyLoggedIn(redirectAttributes);
			}
			return "redirect:" + safeAction;
		}
		model.addAttribute("yadaAutologinAccount", yadaUserCredentials.getUsername());
		model.addAttribute("yadaAutologinTokenLink", tokenLink);
		model.addAttribute("yadaAutologinAction", safeAction);
		return AUTOLOGIN_CONFIRM_VIEW;
	}

	/**
	 * Logs in with an autologin link, from the POST of the confirmation page. The CSRF token of the session is checked by
	 * Spring Security. The token is used once, and only when nobody is logged in: a different logged-in user stays
	 * logged in and the token stays unused.
	 * @param tokenLink the "id-secret" part of the link
	 * @param action the local path to open after login; any other value becomes "/"
	 */
	@PostMapping("/autologin/{tokenLink}")
	public String autologin(@PathVariable String tokenLink, String action, RedirectAttributes redirectAttributes, HttpServletRequest request, HttpServletResponse response) {
		String safeAction = yadaWebUtil.safeLocalRedirect(action==null ? "/" : action);
		ParsedSecurityToken parsedToken = yadaTokenHandler.parseLink(tokenLink);
		if (parsedToken==null) {
			return invalidAutologin(null, redirectAttributes);
		}
		// The authenticated principal, not the user profile, tells whether someone is logged in
		String currentUsername = yadaSecurityUtil.getUsername();
		if (currentUsername!=null) {
			// A user is already logged in.
			// I wanted to logout the current user but it doesn't work: if I use SecurityContextLogoutHandler().logout() the Session is cleared
			// and I get a login page, while if I use request.logout() the session is not cleared but a new session copy of the current one before
			// the yadaSession.clearCaches() call is created, so that I have a mixed situation where the spring user is the new one and the session
			// contains the old UserProfile id.
			// So I just exit, showing an error if the current user is different from the new one, and the token is not used.
			YadaAutoLoginToken yadaAutoLoginToken = yadaAutoLoginTokenDao.findValid(parsedToken);
			if (yadaAutoLoginToken==null || !currentUsername.equalsIgnoreCase(yadaAutoLoginToken.getYadaUserCredentials().getUsername())) {
				alreadyLoggedIn(redirectAttributes);
			}
			return "redirect:" + safeAction;
		}
		YadaUserCredentials yadaUserCredentials = yadaAutoLoginTokenDao.consume(parsedToken);
		if (yadaUserCredentials==null) {
			return invalidAutologin(parsedToken, redirectAttributes);
		}
		log.info("Performing autologin with token {} to username {}", parsedToken.id(), yadaUserCredentials.getUsername());
		yadaSession.clearCaches();
		try {
			// A new session id after login, against session fixation
			request.changeSessionId();
		} catch (IllegalStateException e) {
			log.debug("No session to change at autologin (ignored)");
		}
		try {
			// Need to call the login success handler to perform eventual login tasks
			Authentication authentication = yadaUserDetailsService.authenticateAs(yadaUserCredentials, request, response);
			// A new CSRF token after login, as Spring Security does for the login form
			CsrfTokenRepository csrfTokenRepository = csrfTokenRepositoryProvider.getIfAvailable(HttpSessionCsrfTokenRepository::new);
			new CsrfAuthenticationStrategy(csrfTokenRepository).onAuthentication(authentication, request, response);
			successHandler.onAuthenticationSuccessCustom(request, authentication);
		} catch (Exception e) {
			log.error("Auhtentication success handler failed on autologin (ignored)", e);
		}
		// If not authenticated, this will trigger authentication but will clear any yadaNotify because of the double redirect.
		return "redirect:" + safeAction;
	}

	private void alreadyLoggedIn(RedirectAttributes redirectAttributes) {
		// TODO localized message
		yadaNotify.title("Already logged in", redirectAttributes).error().message("You can't perform an autologin while logged in as a different user").add();
	}

	/**
	 * The outcome of a malformed, unknown, expired or used link, or of a disabled user. The link is never logged.
	 */
	private String invalidAutologin(ParsedSecurityToken parsedToken, RedirectAttributes redirectAttributes) {
		if (parsedToken==null) {
			log.info("Invalid autologin link");
		} else {
			log.info("Autologin token {} not found, expired, used or of a disabled user", parsedToken.id());
		}
		// TODO localized message
		yadaNotify.title("Link expired", redirectAttributes).error().message("The provided address is no longer valid").add();
		// The configured failure url, or "/" when it is not set
		return "redirect:" + yadaWebUtil.safeLocalRedirect(failureHandler.getFailureUrlNormalRequest()); // Redirect to login page
	}

	@RequestMapping("/loginModal")
	@Deprecated // This should be application-specific
	public String loginModal() {
		return "/fragments/modalLogin";
	}

}
