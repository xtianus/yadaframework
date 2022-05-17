package net.yadaframework.web.dialect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractAttributeTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.templatemode.TemplateMode;

import net.yadaframework.core.YadaConfiguration;

/**
 * Converts from a "yada:xxx" thymeleaf attribute to a plain html attribute.
 * For example from "yada:successHandler" to "data-yadaSuccessHandler".
 * It must be configured in YadaDialect.
 */
public class YadaSimpleAttrProcessor extends AbstractAttributeTagProcessor {
	private final Logger log = LoggerFactory.getLogger(getClass());

	public static final int ATTR_PRECEDENCE = 10000;

	private String replacementAttribute;
	private final YadaDialectUtil yadaDialectUtil;

	/**
	 * Creates a new attribute processor that converts from the thymeleaf attribute to a html attribute.
	 * @param dialectPrefix dialect prefix ("yada")
	 * @param attributeFrom attribute to convert from (e.g. "confirm")
	 * @param attributeTo attribute to convert to (e.g. "data-yadaConfirm")
	 */
	public YadaSimpleAttrProcessor(final String dialectPrefix, String attributeFrom, String attributeTo, YadaConfiguration config) {
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
        this.yadaDialectUtil = new YadaDialectUtil(config);
	}

    @Override
    protected void doProcess(
            final ITemplateContext context, final IProcessableElementTag tag,
            final AttributeName attributeName, final String attributeValue,
            final IElementTagStructureHandler structureHandler) {

        String value = "";
        if (attributeValue!=null) {
        	value = yadaDialectUtil.parseExpression(attributeValue, context, String.class);
        }

        structureHandler.setAttribute(replacementAttribute, value);
    }

}
