package net.yadaframework.security;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.stereotype.Component;

import net.yadaframework.core.YadaConfiguration;

/**
 * Handler called during logout 
 *
 */
@Component
@Scope("prototype") // In case you have more than one YadaSecurityConfig bean
public class YadaLogoutSuccessHandler extends SimpleUrlLogoutSuccessHandler {
//	private final transient Logger log = LoggerFactory.getLogger(getClass());

	@Autowired private YadaConfiguration yadaConfiguration;

	/**
	 * When the "locale in path" is enabled, ensures that the target logout url has the language in place.
	 */
	@Override
	protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response) {
		String targetUrl = super.determineTargetUrl(request, response);
		if (yadaConfiguration.isLocalePathVariableEnabled()) {
			// Ensure the language path at the start
			Locale requestLocale = LocaleContextHolder.getLocale();
			String localePath = "/" + requestLocale.getLanguage();
			if (!targetUrl.startsWith("http") && !targetUrl.startsWith(localePath)) {
				return localePath + targetUrl;
			}
		}
		return targetUrl;
	}
	


}
