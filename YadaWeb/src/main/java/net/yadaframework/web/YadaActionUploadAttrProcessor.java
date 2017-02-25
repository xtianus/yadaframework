package net.yadaframework.web;

import java.util.Collections;
import java.util.List;

import org.springframework.context.ApplicationContext;
import org.springframework.security.web.csrf.CsrfToken;
import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IModelFactory;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractAttributeTagProcessor;
import org.thymeleaf.processor.element.AbstractElementTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.spring4.context.SpringContextUtils;
import org.thymeleaf.standard.expression.IStandardExpression;
import org.thymeleaf.standard.expression.IStandardExpressionParser;
import org.thymeleaf.standard.expression.StandardExpressions;
import org.thymeleaf.templatemode.TemplateMode;
import org.unbescape.html.HtmlEscape;

/**
 * Serve solo se il csrf è attivo.
 * Questa classe aggiunge il tag yada:actionUpload da usare quando si fa multipart file upload con Spring Security e la dimensione massima del file settata.
 * Vedi http://stackoverflow.com/questions/23856254/how-to-nicely-handle-file-upload-maxuploadsizeexceededexception-with-spring-secu
 * Senza questo tag, Spring Security lancia un 403 Forbidden quando il max filesize specificato nel filtro viene superato, e non si arriva al Controller.
 * Con questo tag (e il workaround del YadaCommonsMultipartResolver) invece tutto funziona e si arriva al Controller con un MultipartFile pari a null.
 * Il trucco consiste nel mettere il _csrf come parametro nell'url della form action.
 * Esempio: <form yada:actionUpload="@{/submitImage}"
 *  
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

//	@Override
//	public int getPrecedence() {
//        return ATTR_PRECEDENCE;
//	}
//
//    @Override
//    /**
//     * Ritorna il nome finale dell'attributo, dopo la trasformazione
//     */
//    protected String getTargetAttributeName(final Arguments arguments, final Element element, final String attributeName) {
//        return "action"; // non ATTR_NAME;
//    }

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
        StringBuilder urlBuilder = new StringBuilder(origurl);
   	
    	CsrfToken token = (CsrfToken) context.getVariable("_csrf");
    	// token è null quando il csrf è stato disabilitato
    	if (token!=null) { 
    		final String tokenName = token.getParameterName();
    		final String tokenValue = token.getToken();
			if (urlBuilder.lastIndexOf("?")>-1) {
				urlBuilder.append("&");
			} else {
				urlBuilder.append("?");
			}
			urlBuilder.append(tokenName).append("=").append(tokenValue);
    	}
    	structureHandler.setAttribute("action", urlBuilder.toString());
        
    }
    /**
     * 
     */
//    @Override
//    protected String getTargetAttributeValue(final Arguments arguments, final Element element, final String attributeName) {
//    	CsrfToken token = (CsrfToken) arguments.getContext().getVariables().get("_csrf");
//    	if (token!=null) { 
//    		StringBuilder urlBuilder = new StringBuilder(super.getTargetAttributeValue(arguments, element, attributeName));
//    		final String tokenName = token.getParameterName();
//    		final String tokenValue = token.getToken();
//    		if (urlBuilder.lastIndexOf("?")>-1) {
//    			urlBuilder.append("&");
//    		} else {
//    			urlBuilder.append("?");
//    		}
//    		urlBuilder.append(tokenName).append("=").append(tokenValue);
//    		return RequestDataValueProcessorUtils.processUrl(arguments.getConfiguration(), arguments, urlBuilder.toString());
//    	} else {
//    		// token è null quando il csrf è stato disabilitato
//    		return super.getTargetAttributeValue(arguments, element, attributeName);
//    	}
//    }

//	@Override	
//	protected ModificationType getModificationType(Arguments arguments, Element element, String attributeName, String newAttributeName) {
//		return ModificationType.SUBSTITUTION;
//	}
//
////	@Override
////	protected Map<String, String> getModifiedAttributeValues(Arguments arguments, Element element, String attributeName) {
////		final Configuration configuration = arguments.getConfiguration();
////		final String attributeValue = element.getAttributeValue(attributeName);
////		final IStandardExpressionParser parser = StandardExpressions.getExpressionParser(configuration);
////		String trailingUrl = attributeValue; // Nel caso sia stato usato un character literal che manda in exception il parser
////		try {
////			final IStandardExpression expression = parser.parseExpression(configuration, arguments, attributeValue);
////			trailingUrl = (String) expression.execute(configuration, arguments);
////		} catch (Exception e) {
////			// Ignored
////		}
////		final Map<String,String> values = new HashMap<String, String>();
////		values.put("href", getResourcesUrl(trailingUrl));
////		return values;	
////	}
//
//	@Override
//	protected boolean recomputeProcessorsAfterExecution(Arguments arguments, Element element, String attributeName) {
//		return false;
//	}
//
//	@Override
//	protected boolean removeAttributeIfEmpty(Arguments arguments, Element element, String attributeName, String newAttributeName) {
//		return false;
//	}

}
