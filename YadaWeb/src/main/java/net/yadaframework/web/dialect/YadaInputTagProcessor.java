package net.yadaframework.web.dialect;
import java.util.HashMap;
import java.util.Map;

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
 * Defines the &lt;yada:input> tag
 *
 */
public class YadaInputTagProcessor extends AbstractElementModelProcessor {
	// Tag syntax:
	//	<yada:input yada:counterId=... yada:noShowPassword ...>
	//		<yada:addonLeft>...</yada:addonLeft>
	//		<yada:addonRight>...</yada:addonRight>
	//		<yada:help th:text="#{help.key}">help</yada:help>
	//	</yada:input>
	
	@SuppressWarnings("unused")
	private final transient Logger log = LoggerFactory.getLogger(getClass());

	private static final String TAG_NAME = "input"; // Tag of the dialect: "yada:input"
    private static final int PRECEDENCE = 100;
    
    private final String dialectPrefix;
    
    private final YadaDialectUtil yadaDialectUtil;

	public YadaInputTagProcessor(String dialectPrefix, YadaConfiguration config) {
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
        
        for (int i = 0; i < model.size(); i++) {
        	ITemplateEvent iTemplateEvent = model.get(i);
        	if (iTemplateEvent instanceof IOpenElementTag) {
        		final IOpenElementTag openTag = (IOpenElementTag) iTemplateEvent;
        		String tagName = yadaDialectUtil.removePrefix(openTag.getElementCompleteName(), dialectPrefix);
        		switch (tagName) {
        		case "input":
        			processInputTag(openTag, context, structureHandler);
        			structureHandler.setLocalVariable("yadaTagId",  yadaDialectUtil.makeYadaTagId(openTag));
        			break;
        		case "addonLeft":
        			IText text = (IText) model.get(++i); // Text node
        			structureHandler.setLocalVariable("addonLeft", text.getText());
        			break;
        		case "addonRight":
        			text = (IText) model.get(++i); // Text node
        			structureHandler.setLocalVariable("addonRight", text.getText());
        			break;
        		case "help":
        			text = (IText) model.get(++i); // Text node
        			structureHandler.setLocalVariable("helpText", text.getText());
        			break;
        		}
        	}
		}
        
        // Add the replacement tag
        Map<String, String> divAttributes = new HashMap<>();
        divAttributes.put("th:replace", "/yada/formfields/input::field");
        final IModelFactory modelFactory = context.getModelFactory();
        IOpenElementTag replacementTagOpen = modelFactory.createOpenElementTag("div", divAttributes, AttributeValueQuotes.DOUBLE, false);
        ICloseElementTag replacementTagClose = modelFactory.createCloseElementTag("div");
        model.reset(); // Remove the yada tag completely
        model.add(replacementTagOpen);
        model.add(replacementTagClose);
	}
	
	private void processInputTag(IOpenElementTag sourceTag, ITemplateContext context, IElementModelStructureHandler structureHandler) {
        // Convert all attributes of the source tag
        Map<String, String> inputSourceAttributes = sourceTag.getAttributeMap();
        String targetAttributesString = yadaDialectUtil.getConvertedCustomTagAttributeString(sourceTag, context);
        // Handle "yada:" attributes
        for (Map.Entry<String,String> sourceAttribute : inputSourceAttributes.entrySet()) {
			String attributeName = sourceAttribute.getKey();
			String attributeValue = sourceAttribute.getValue();
			if (attributeName.startsWith(YadaDialectUtil.YADA_PREFIX_WITHCOLUMN)) {
				String yadaAttributeName = yadaDialectUtil.removePrefix(attributeName, dialectPrefix);
				switch (yadaAttributeName) {
				case "editor":
					// TODO use ckedit
					break;
				default:
					// Every yada attribute becomes a local variable
					// noShowPassword
					// inputCounterId
					// etc.
					structureHandler.setLocalVariable(yadaAttributeName, attributeValue==null?true:attributeValue);
				}
			} 
		}
        structureHandler.setLocalVariable(TAG_NAME, inputSourceAttributes); // So I can do ${input.id} to get the original id attribute and so on
        structureHandler.setLocalVariable("targetAttributesString", targetAttributesString);
	}

}
