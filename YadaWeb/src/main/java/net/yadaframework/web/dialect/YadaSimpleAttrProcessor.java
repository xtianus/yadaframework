package net.yadaframework.web.dialect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractAttributeTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.standard.expression.IStandardExpression;
import org.thymeleaf.standard.expression.IStandardExpressionParser;
import org.thymeleaf.standard.expression.StandardExpressions;
import org.thymeleaf.templatemode.TemplateMode;

/**
 * Converts from a "yada:xxx" thymeleaf attribute to a plain html attribute.
 * For example from "yada:successHandler" to "data-yadaSuccessHandler".
 * It must be configured in YadaDialect.
 */
public class YadaSimpleAttrProcessor extends AbstractAttributeTagProcessor {
	private final Logger log = LoggerFactory.getLogger(getClass());
	// Tutorial: http://www.thymeleaf.org/doc/html/Extending-Thymeleaf.html
   // A value of 10000 is higher than any attribute in the
    // SpringStandard dialect. So this attribute will execute
    // after all other attributes from that dialect, if in the
    // same tag.
	public static final int ATTR_PRECEDENCE = 9000;

	private String replacementAttribute;

	/**
	 * Creates a new attribute processor that converts from the thymeleaf attribute to a html attribute.
	 * @param dialectPrefix dialect prefix ("yada")
	 * @param attributeFrom attribute to convert from (e.g. "confirm")
	 * @param attributeTo attribute to convert to (e.g. "data-yadaConfirm")
	 */
	public YadaSimpleAttrProcessor(final String dialectPrefix, String attributeFrom, String attributeTo) {
        super(
                TemplateMode.HTML, // This processor will apply only to HTML mode
                dialectPrefix,     // Prefix to be applied to name for matching
                null,              // No tag name: match any tag name
                false,             // No prefix to be applied to tag name
                attributeFrom,         // Name of the attribute that will be matched
                true,              // Apply dialect prefix to attribute name
                ATTR_PRECEDENCE,   // Precedence (inside dialect's own precedence)
                true);             // Remove the matched attribute afterwards
        replacementAttribute = attributeTo;
	}

//	@Override
//	public int getPrecedence() {
//        return ATTR_PRECEDENCE;
//	}

//    @Override
//    protected String getTargetAttributeName(
//            final Arguments arguments, final Element element, final String attributeName) {
//        return ATTR_NAME;
//    }

    @Override
    protected void doProcess(
            final ITemplateContext context, final IProcessableElementTag tag,
            final AttributeName attributeName, final String attributeValue,
            final IElementTagStructureHandler structureHandler) {

        final IEngineConfiguration configuration = context.getConfiguration();
        
        String value = "";
        if (attributeValue!=null) {
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
        	value = (String) expression.execute(context);
        }

        structureHandler.setAttribute(replacementAttribute, value);
    }

    /**
     *
     */
//    @Override
//    protected String getTargetAttributeValue(final Arguments arguments, final Element element, final String attributeName) {
//    	String url = super.getTargetAttributeValue(arguments, element, attributeName);
//    	String resultUrl = yadaDialectUtil.getResAttributeValue(arguments, url);
//        return RequestDataValueProcessorUtils.processUrl(arguments.getConfiguration(), arguments, resultUrl);
//    }

//	@Override
//	protected ModificationType getModificationType(Arguments arguments, Element element, String attributeName, String newAttributeName) {
//		return ModificationType.SUBSTITUTION;
//	}

//	@Override
//	protected Map<String, String> getModifiedAttributeValues(Arguments arguments, Element element, String attributeName) {
//		final Configuration configuration = arguments.getConfiguration();
//		final String attributeValue = element.getAttributeValue(attributeName);
//		final IStandardExpressionParser parser = StandardExpressions.getExpressionParser(configuration);
//		String trailingUrl = attributeValue; // Nel caso sia stato usato un character literal che manda in exception il parser
//		try {
//			final IStandardExpression expression = parser.parseExpression(configuration, arguments, attributeValue);
//			trailingUrl = (String) expression.execute(configuration, arguments);
//		} catch (Exception e) {
//			// Ignored
//		}
//		final Map<String,String> values = new HashMap<String, String>();
//		values.put("href", getResourcesUrl(trailingUrl));
//		return values;
//	}

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
