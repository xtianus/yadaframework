package net.yadaframework.security.web;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.util.UriComponentsBuilder;
import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.model.AttributeValueQuotes;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IModelFactory;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractAttributeTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.standard.expression.IStandardExpression;
import org.thymeleaf.standard.expression.IStandardExpressionParser;
import org.thymeleaf.standard.expression.StandardExpressions;
import org.thymeleaf.templatemode.TemplateMode;
import org.unbescape.html.HtmlEscape;

// TODO this used to be included in the "yada" dialect before splitting the YadaWebSecurity project.
// Now it's not used anymore, waiting for a new "yadasec" dialect

/**
 * Implements yada:actionUpload, which sets the action of a multipart form and, when CSRF is enabled, adds the CSRF token
 * as a hidden field at the start of the form. The token is added only when the action is a url of the same origin (no
 * scheme and host), and any token found in the action query is removed: tokens are never sent in a url.
 * An oversized plain form can't be CSRF-validated because its body is not parsed: YadaUploadLimitAccessDeniedHandler
 * answers 413 without reaching the controller. Ajax uploads send the token in a header and reach the controller,
 * where YadaCommonsMultipartResolver.limitExceeded() is true.
 * Example: &lt;form yada:actionUpload="@{/submitImage}" method="post" enctype="multipart/form-data"&gt;
 */
public class YadaActionUploadAttrProcessor extends AbstractAttributeTagProcessor {
	// A value of 10000 is higher than any attribute in the
	// SpringStandard dialect. So this attribute will execute
	// after all other attributes from that dialect, if in the
	// same tag.
	public static final int ATTR_PRECEDENCE = 10000;
    public static final String ATTR_NAME = "actionUpload"; // Nome del tag nel dialect
   
	public YadaActionUploadAttrProcessor(final String dialectPrefix) {
        super(
                TemplateMode.HTML, // This processor will apply only to HTML mode
                dialectPrefix,     // Prefix to be applied to name for matching
                null,              // No tag name: match any tag name
                false,             // No prefix to be applied to tag name
                ATTR_NAME,         // Name of the attribute that will be matched
                true,              // Apply dialect prefix to attribute name
                ATTR_PRECEDENCE,   // Precedence (inside dialect's own precedence)
                true);             // Remove the matched attribute afterwards
	}

    @Override
    protected void doProcess(
            final ITemplateContext context, final IProcessableElementTag tag,
            final AttributeName attributeName, final String attributeValue,
            final IElementTagStructureHandler structureHandler) {

        final IEngineConfiguration configuration = context.getConfiguration();

        /*
         * Obtain the Thymeleaf Standard Expression parser
         */
        final IStandardExpressionParser parser = StandardExpressions.getExpressionParser(configuration);

        /*
         * Parse the attribute value as a Thymeleaf Standard Expression
         */
        final IStandardExpression expression = parser.parseExpression(context, attributeValue);

        /*
         * Execute the expression just parsed
         */
        final String origurl = (String) expression.execute(context);
        String action = origurl;

    	CsrfToken token = (CsrfToken) context.getVariable("_csrf");
    	// token è null quando il csrf è stato disabilitato
    	if (token!=null) {
    		// The token is never put in the url: it is sent in a hidden field, and only to the same origin
    		action = YadaMultipartFormTagProcessor.removeQueryParameter(origurl, token.getParameterName());
    		if (isSameOrigin(action)) {
    			IModelFactory modelFactory = context.getModelFactory();
    			Map<String, String> attributes = new LinkedHashMap<>();
    			attributes.put("type", "hidden");
    			attributes.put("name", HtmlEscape.escapeHtml5Xml(token.getParameterName()));
    			attributes.put("value", HtmlEscape.escapeHtml5Xml(token.getToken()));
    			IModel hiddenField = modelFactory.createModel();
    			hiddenField.add(modelFactory.createStandaloneElementTag("input", attributes, AttributeValueQuotes.DOUBLE, false, false));
    			structureHandler.insertImmediatelyAfter(hiddenField, false);
    		}
    	}
    	structureHandler.setAttribute("action", HtmlEscape.escapeHtml4Xml(action));

    }

    /**
     * @return true for a url without scheme and host, that is sent to the origin of the page
     */
    static boolean isSameOrigin(String url) {
    	if (url.startsWith("//") || url.startsWith("\\")) {
    		return false;
    	}
    	return UriComponentsBuilder.fromUriString(url).build().getScheme()==null;
    }

}
