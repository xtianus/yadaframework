package net.yadaframework.security.web;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractElementTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.templatemode.TemplateMode;

/**
 * Appends the CSRF token to multipart form actions so oversized uploads can be validated before body parsing.
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
		UriComponents actionComponents = UriComponentsBuilder.fromUriString(action).build();
		if (actionComponents.getQueryParams().containsKey(token.getParameterName())) {
			return;
		}
		String updatedAction = UriComponentsBuilder.fromUriString(action)
			.queryParam(token.getParameterName(), token.getToken())
			.build()
			.toUriString();
		structureHandler.setAttribute("action", updatedAction);
	}
}
