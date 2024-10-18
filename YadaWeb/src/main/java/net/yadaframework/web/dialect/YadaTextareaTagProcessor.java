package net.yadaframework.web.dialect;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.AttributeValueQuotes;
import org.thymeleaf.model.ICloseElementTag;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IModelFactory;
import org.thymeleaf.model.IOpenElementTag;
import org.thymeleaf.model.ITemplateEvent;
import org.thymeleaf.model.IText;
import org.thymeleaf.processor.element.AbstractElementModelProcessor;
import org.thymeleaf.processor.element.IElementModelStructureHandler;
import org.thymeleaf.templatemode.TemplateMode;

import net.yadaframework.core.YadaConfiguration;

/**
 * Defines the &lt;yada:textarea> tag
 *
 */
public class YadaTextareaTagProcessor extends AbstractElementModelProcessor {
	// Tag syntax:
	//	<yada:textarea yada:counterId=...>
	//	</yada:textarea>

	@SuppressWarnings("unused")
	private final transient Logger log = LoggerFactory.getLogger(getClass());

	private static final String TAG_NAME = "textarea"; // Tag of the dialect: "yada:textarea"
	public static final int PRECEDENCE = 12000; // Must be higher than any yada: attribute (why?)

    private final String dialectPrefix;

    private final YadaDialectUtil yadaDialectUtil;

	public YadaTextareaTagProcessor(String dialectPrefix, YadaConfiguration config) {
		super(
			TemplateMode.HTML, // This processor will apply only to HTML mode
			dialectPrefix,     // Prefix to be applied to name for matching
			TAG_NAME,          // Tag name: match specifically this tag
			true,              // Apply dialect prefix to tag name
			null,              // No attribute name: will match by tag name
			false,             // No prefix to be applied to attribute name
			PRECEDENCE);       // Precedence (inside dialect's own precedence)

		this.dialectPrefix = dialectPrefix;
		this.yadaDialectUtil = new YadaDialectUtil(config);
	}

	@Override
	protected void doProcess(ITemplateContext context, IModel model, IElementModelStructureHandler structureHandler) {
		IText text;
		String targetAttributesString = "";
        for (int i = 0; i < model.size(); i++) {
        	ITemplateEvent iTemplateEvent = model.get(i);
        	if (iTemplateEvent instanceof IOpenElementTag) {
        		final IOpenElementTag openTag = (IOpenElementTag) iTemplateEvent;
         		String tagName = yadaDialectUtil.removePrefix(openTag.getElementCompleteName(), dialectPrefix);
        		switch (tagName) {
        		case "textarea":
        			targetAttributesString = processInputTag(openTag, context, structureHandler);
        			structureHandler.setLocalVariable("yadaTagId",  yadaDialectUtil.makeYadaTagId(openTag));
        			break;
        		case "addonLeft": // TODO not tested!
        			// IText text = (IText) model.get(++i); // Text node
        			structureHandler.setLocalVariable("yadaAddonLeft", yadaDialectUtil.getInnerHtml(model, i));
        			break;
        		case "addonRight": // TODO not tested!
        			// text = (IText) model.get(++i); // Text node
        			structureHandler.setLocalVariable("yadaAddonRight", yadaDialectUtil.getInnerHtml(model, i));
        			break;
        		case "validationError": // TODO not tested!
        			// Does not work properly yet, because th: attributes are not executed so the error message can only be static
        			text = (IText) model.get(++i); // Text node
        			processValidationErrorTag(openTag, context, structureHandler, text);
        			// structureHandler.setLocalVariable("yadaValidationMessage", text.getText());
        			break;
        		case "help": // TODO not tested!
        			text = (IText) model.get(++i); // Text node
        			structureHandler.setLocalVariable("yadaHelpText", text.getText());
        			break;
        		}
        	}
		}

        structureHandler.setLocalVariable("yadaTargetAttributesString", targetAttributesString);

        // Add the replacement tag
        Map<String, String> divAttributes = new HashMap<>();
        String targetImplementation = "/yada/formfields/textarea::field";
        divAttributes.put("th:replace", targetImplementation);
        final IModelFactory modelFactory = context.getModelFactory();
        IOpenElementTag replacementTagOpen = modelFactory.createOpenElementTag("div", divAttributes, AttributeValueQuotes.DOUBLE, false);
        ICloseElementTag replacementTagClose = modelFactory.createCloseElementTag("div");
        model.reset(); // Remove the yada tag completely
        model.add(replacementTagOpen);
        model.add(replacementTagClose);
	}

	/**
	 * Add a key='value' pair to a string of comma-separated pairs
	 * @param existing
	 * @param key
	 * @param value
	 * @return
	 */
	private String appendAttribute(String existing, String key, String value) {
		if (!existing.isEmpty()) {
			existing += ",";
		}
		return existing + key + "='" + value + "'";
	}

	// TODO this has been copied from YadaInputTagProcessor and not modified (does it work?) but eventually it should be
	// 		refactored into YadaDialectUtil for both classes
	private void processValidationErrorTag(IOpenElementTag sourceTag, ITemplateContext context, IElementModelStructureHandler structureHandler, IText textNode) {
        String errorText = StringUtils.trimToNull(textNode.getText());
        String yadaMessageKey = null;
        Boolean yadaInvalidFlag = null;
		Map<String, String> sourceAttributes = sourceTag.getAttributeMap();
        for (Map.Entry<String,String> sourceAttribute : sourceAttributes.entrySet()) {
			String attributeName = sourceAttribute.getKey();
			String attributeValue = sourceAttribute.getValue();
			if (attributeName.startsWith(YadaDialectUtil.YADA_PREFIX_WITHCOLUMN)) {
				// Handle "yada:" attributes
				String yadaAttributeName = yadaDialectUtil.removePrefix(attributeName, dialectPrefix);
				switch (yadaAttributeName) {
				case "invalidFlag":
					Object invalidFlagValue = yadaDialectUtil.parseExpression(attributeValue, context, Object.class);
					if (invalidFlagValue instanceof Boolean) {
						yadaInvalidFlag = (Boolean) invalidFlagValue;
					} else if (invalidFlagValue!=null) {
						yadaInvalidFlag = true;
					}
					break;
				case "messageKey":
					// messageKey has precedence over any other text
					yadaMessageKey = yadaDialectUtil.parseExpression(attributeValue, context, String.class);
					if (yadaInvalidFlag==null) {
						// If yadaInvalidFlag has not been specified, set it to true when the key exists
						yadaInvalidFlag = (yadaMessageKey!=null);
					}
					break;
				default:
					// yada:xyyy becomes a yadaXyyy variable
					structureHandler.setLocalVariable("yada" + StringUtils.capitalize(yadaAttributeName), attributeValue);
				}
			} else if (attributeName.startsWith("th:")) {
				// Handle some th: attributes because currently they are not processed for nested custom tags
				// TODO this won't be needed when we fix the parsing of th: attributes
				if ("th:text".equals(attributeName) || "th:utext".equals(attributeName)) {
					errorText = yadaDialectUtil.parseExpression(attributeValue, context, String.class);
				}
			}
		}
        structureHandler.setLocalVariable("yadaInvalidFlag", yadaInvalidFlag);
        if (yadaMessageKey==null) {
        	structureHandler.setLocalVariable("yadaValidationMessage", errorText);
        } else {
        	structureHandler.setLocalVariable("yadaMessageKey", yadaMessageKey);
        }
	}

	private String processInputTag(IOpenElementTag sourceTag, ITemplateContext context, IElementModelStructureHandler structureHandler) {
        // Convert all attributes of the source tag
        Map<String, String> inputSourceAttributes = sourceTag.getAttributeMap();
        String targetAttributesString = yadaDialectUtil.getConvertedHTMLCustomTagAttributeString(sourceTag, context);
        // Handle "yada:" attributes
        for (Map.Entry<String,String> sourceAttribute : inputSourceAttributes.entrySet()) {
			String attributeName = sourceAttribute.getKey();
			String attributeValue = sourceAttribute.getValue();
			if (attributeName.startsWith(YadaDialectUtil.YADA_PREFIX_WITHCOLUMN)) {
				String yadaAttributeName = yadaDialectUtil.removePrefix(attributeName, dialectPrefix);
				switch (yadaAttributeName) {
				case "text": // yada:text is a replacement for th:text that doesn't work
					String textareaValue = yadaDialectUtil.parseExpression(attributeValue, context, String.class);
					structureHandler.setLocalVariable("yadaTextareaValue", textareaValue);
					break;
				case "validationError":
					String yadaValidationMessage = yadaDialectUtil.parseExpression(attributeValue, context, String.class);
					if (StringUtils.isNotBlank(yadaValidationMessage)) {
						structureHandler.setLocalVariable("yadaValidationMessage", yadaValidationMessage);
						structureHandler.setLocalVariable("yadaInvalidFlag", true);
					}
					break;
				case "editor":
					// TODO use ckedit
					break;
				case "ajaxTriggerKeys":
					targetAttributesString = appendAttribute(targetAttributesString, "data-yadaAjaxTriggerKeys", attributeValue);
					break;
				case "ajaxResultFocus":
					targetAttributesString = appendAttribute(targetAttributesString, "data-yadaAjaxResultFocus", attributeValue);
					break;
				case "":
				default:
					// Every yada attribute becomes a local variable. Watch the case!
					// yadainputcounterid
					// yadaenumclassname
					// yadalabelkeyprefix
					// etc.
					String name = "yada" + yadaAttributeName.toLowerCase();
					String parsedValue = yadaDialectUtil.parseExpression(attributeValue, context, String.class);
					structureHandler.setLocalVariable(name, attributeValue==null?true:parsedValue);
				}
			}
		}
        inputSourceAttributes = new HashMap<String, String>(inputSourceAttributes); // Need to clone the source map which is readonly
        if (inputSourceAttributes.get("class")==null) {
        	// When class is missing, set it to empty string
        	inputSourceAttributes.put("class", "");
        }
        String name = "yada" + StringUtils.capitalize(TAG_NAME);
        structureHandler.setLocalVariable(name, inputSourceAttributes); // So I can do ${yadaTextarea.id} to get the original id attribute and so on
        return targetAttributesString;
	}

}
