package net.yadaframework.web.dialect;

import org.apache.commons.lang3.StringUtils;
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
 * Class that handles the convenience attribute yada:ajax="url".
 * The result will be a data-yadaHref="url" attribute, and yadaAjax added to class.
 *
 */
public class YadaAjaxAttrProcessor extends AbstractAttributeTagProcessor {
	private final Logger log = LoggerFactory.getLogger(getClass());

	public static final int ATTR_PRECEDENCE = 10000;
    public static final String ATTR_NAME = "ajax";

    protected String resultAttribute = "data-yadaHref";

	/**
	 * @param config
	 */
	public YadaAjaxAttrProcessor(final String dialectPrefix) {
        this(dialectPrefix, ATTR_PRECEDENCE, ATTR_NAME);
	}

	protected YadaAjaxAttrProcessor(final String dialectPrefix, int attrPrecedence, String attrName) {
		super(
            TemplateMode.HTML, // This processor will apply only to HTML mode
            dialectPrefix,     // Prefix to be applied to name for matching
            null,              // No tag name: match any tag name
            false,             // No prefix to be applied to tag name
            attrName,         // Name of the attribute that will be matched
            true,              // Apply dialect prefix to attribute name
            attrPrecedence,   // Precedence (inside dialect's own precedence)
            true);             // Remove the matched attribute afterwards
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

    	// Prevent use on button type="submit"
    	if ("button".equals(tag.getElementCompleteName()) && "submit".equals(tag.getAttributeValue("type"))) {
    		log.error("yada:ajax can not be used on submit buttons because they have a different usage");
    		return;
    	}

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
        final String targetUrl = (String) expression.execute(context);

        /*
         * Set the value into the 'data-yadaHref' attribute
         */
        if (targetUrl != null) {
        	structureHandler.setAttribute(resultAttribute, targetUrl);
        	String currentClasses = StringUtils.trimToEmpty(tag.getAttributeValue("class"));
        	structureHandler.setAttribute("class", currentClasses + (StringUtils.isEmpty(currentClasses)?"":" ") + "yadaAjax");
        }

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
