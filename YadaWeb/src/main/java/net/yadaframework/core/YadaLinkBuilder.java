package net.yadaframework.core;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.context.i18n.LocaleContextHolder;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.linkbuilder.StandardLinkBuilder;

import net.yadaframework.web.dialect.YadaDialectUtil;

/**
 * Called when @{/somePath} is found.
 * It has two purposes:
 * <ul>
 * <li>create localized urls when the language is in the path</li>
 * <li>append a version to the resources folder</li>
 * </ul>
 * 
 */
public class YadaLinkBuilder extends StandardLinkBuilder  {
	
	private Pattern nonLocalizedUrls;
	private YadaConfiguration config;
	private YadaDialectUtil yadaDialectUtil;
	
	public YadaLinkBuilder(YadaConfiguration config, String notLocalizedResourcePattern) {
		super();
		this.nonLocalizedUrls = Pattern.compile(notLocalizedResourcePattern);
		this.config = config;
		this.yadaDialectUtil = new YadaDialectUtil(config);
	}

	/**
	 * Adds the version number to resource folders
	 */
	@Override
	protected String processLink(IExpressionContext context, String link) {
		link = yadaDialectUtil.getVersionedAttributeValue(link); // "/res/" becomes "/res-0149/"
		return super.processLink(context, link);
	}

	/**
	 * Adds the language code to the context path when localePathVariable=true in the configuration
	 */
	@Override
	protected String computeContextPath(IExpressionContext context, String base, Map<String, Object> parameters) {
		StringBuilder result = new StringBuilder(super.computeContextPath(context, base, parameters));
		if (config.isLocalePathVariableEnabled()) {
			// Adding the current locale, but only if it is not a resource
			boolean localizable = !nonLocalizedUrls.matcher(base).find();
			if (localizable) {
				Locale locale = LocaleContextHolder.getLocale();
				String localeString = locale.getLanguage();
				result.append("/").append(localeString);
			}
		}
		return result.toString();
	}

}
