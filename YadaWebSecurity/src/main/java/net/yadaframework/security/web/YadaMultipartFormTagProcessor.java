package net.yadaframework.security.web;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractElementTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.templatemode.TemplateMode;
import org.unbescape.html.HtmlEscape;

/**
 * Removes the CSRF token from the action URL of multipart POST forms, keeping any other query parameter and the fragment.
 * A token in a URL ends up in access logs, browser history and Referer headers. Multipart forms send the token in the
 * hidden field added by th:action (or by yada:actionUpload), or in the header added by yada.ajax.js, because the
 * MultipartFilter parses the body before Spring Security validates it.
 * This processor only cleans up templates that still write the token in the action, as the old upload workaround did.
 */
public class YadaMultipartFormTagProcessor extends AbstractElementTagProcessor {
	private static final String TAG_NAME = "form";
	private static final int PRECEDENCE = 10000;

	public YadaMultipartFormTagProcessor(final String dialectPrefix) {
		super(TemplateMode.HTML, dialectPrefix, TAG_NAME, false, null, false, PRECEDENCE);
	}

	@Override
	protected void doProcess(ITemplateContext context, IProcessableElementTag tag, IElementTagStructureHandler structureHandler) {
		String enctype = tag.getAttributeValue("enctype");
		String method = tag.getAttributeValue("method");
		String action = tag.getAttributeValue("action");
		if (!"multipart/form-data".equalsIgnoreCase(enctype) || !"post".equalsIgnoreCase(method) || action == null || action.isBlank()) {
			return;
		}
		CsrfToken token = (CsrfToken) context.getVariable("_csrf");
		if (token == null) {
			return;
		}
		// Attribute values are kept escaped, e.g. "&amp;" between query parameters
		String unescapedAction = HtmlEscape.unescapeHtml(action);
		String cleanAction = removeQueryParameter(unescapedAction, token.getParameterName());
		if (!cleanAction.equals(unescapedAction)) {
			structureHandler.setAttribute("action", HtmlEscape.escapeHtml4Xml(cleanAction));
		}
	}

	/**
	 * Removes a parameter from the query of a url, leaving the rest of the url unchanged
	 * @param url the url, possibly with a query and a fragment
	 * @param parameterName the parameter to remove
	 * @return the url without the parameter, or the original url when the parameter is not present
	 */
	static String removeQueryParameter(String url, String parameterName) {
		UriComponents components = UriComponentsBuilder.fromUriString(url).build();
		if (!components.getQueryParams().containsKey(parameterName)) {
			return url;
		}
		return UriComponentsBuilder.fromUriString(url).replaceQueryParam(parameterName).build().toUriString();
	}
}
