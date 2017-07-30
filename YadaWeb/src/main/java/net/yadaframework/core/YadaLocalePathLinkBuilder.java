package net.yadaframework.core;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.context.i18n.LocaleContextHolder;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.linkbuilder.StandardLinkBuilder;

/**
 * Needed to create localized urls when using thymeleaf @{} syntax with context-relative paths like @{/somePath}.
 * 
 */
public class YadaLocalePathLinkBuilder extends StandardLinkBuilder  {
	
	private Pattern nonLocalizedUrls;
	
	public YadaLocalePathLinkBuilder(String notLocalizedResourcePattern) {
		super();
		nonLocalizedUrls = Pattern.compile(notLocalizedResourcePattern);
	}

	@Override
	protected String computeContextPath(IExpressionContext context, String base, Map<String, Object> parameters) {
		StringBuilder result = new StringBuilder(super.computeContextPath(context, base, parameters));
		// Adding the current locale, but only if it is not a resource
		boolean localizable = !nonLocalizedUrls.matcher(base).find();
		if (localizable) {
			Locale locale = LocaleContextHolder.getLocale();
			String localeString = locale.getLanguage();
			result.append("/").append(localeString);
		}
		return result.toString();
	}

}
