package net.yadaframework.web.dialect;

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

import net.yadaframework.core.YadaConfiguration;

/**
 * Questa classe aggiunge il tag yada:href per il calcolo di un url tipo /res-0001/js/a.js dove 0001 è calcolato dinamicamente e coincide con
 * il numero di build.
 * Questo permette di evitare una sintassi tipo: 
 * th:href="@{|${res}/css/bootstrap.min.css|}" 
 * dove "res" è un valore iniettato da un HandlerInterceptorAdapter e usare invece 
 * yada:href="@{/res/css/bootstrap.min.css}"
 * 
 * Tutorial: http://www.thymeleaf.org/doc/html/Extending-Thymeleaf.html
 *
 */
public class YadaHrefAttrProcessor extends AbstractAttributeTagProcessor {
    // A value of 10000 is higher than any attribute in the
    // SpringStandard dialect. So this attribute will execute
    // after all other attributes from that dialect, if in the
    // same tag.
	public static final int ATTR_PRECEDENCE = 9000;
    public static final String ATTR_NAME = "href";
   
    private final YadaDialectUtil yadaDialectUtil;

	/**
	 * 
	 * @param resFolder Folder contenente le risorse da "versionare", per esempio "/res/"
	 * @param config
	 */
	public YadaHrefAttrProcessor(final String dialectPrefix, YadaConfiguration config) {
        super(
                TemplateMode.HTML, // This processor will apply only to HTML mode
                dialectPrefix,     // Prefix to be applied to name for matching
                null,              // No tag name: match any tag name
                false,             // No prefix to be applied to tag name
                ATTR_NAME,         // Name of the attribute that will be matched
                true,              // Apply dialect prefix to attribute name
                ATTR_PRECEDENCE,   // Precedence (inside dialect's own precedence)
                true);             // Remove the matched attribute afterwards
		yadaDialectUtil = new YadaDialectUtil(config);
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
        final String semiurl = (String) expression.execute(context);

        String resultUrl = yadaDialectUtil.getResAttributeValue(context, semiurl);

        /*
         * Set the new value into the 'href' attribute
         */
        if (resultUrl != null) {
        	structureHandler.setAttribute(ATTR_NAME, resultUrl);
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
