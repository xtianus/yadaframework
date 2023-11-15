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
import org.thymeleaf.processor.element.AbstractElementModelProcessor;
import org.thymeleaf.processor.element.IElementModelStructureHandler;
import org.thymeleaf.templatemode.TemplateMode;

import net.yadaframework.core.YadaConfiguration;

/**
 * Defines the &lt;yada:inputCounter> tag
 *
 */
public class YadaInputCounterTagProcessor extends AbstractElementModelProcessor {
	// Tag syntax:
	//	<yada:inputCounter></yada:inputCounter>
	
	@SuppressWarnings("unused")
	private final transient Logger log = LoggerFactory.getLogger(getClass());

	private static final String TAG_NAME = "inputCounter";
	public static final int PRECEDENCE = 12000; // Must be higher than any yada: attribute
    
    private final String dialectPrefix;
    
    private final YadaDialectUtil yadaDialectUtil;

	public YadaInputCounterTagProcessor(String dialectPrefix, YadaConfiguration config) {
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
        		case "inputCounter":
        			processTag(openTag, context, structureHandler);
        			structureHandler.setLocalVariable("yadaTagId",  yadaDialectUtil.makeYadaTagId(openTag)); // Not used yet
        			break;
        		}
        	}
		}
        
        // Add the replacement tag
        Map<String, String> divAttributes = new HashMap<>();
        divAttributes.put("th:replace", "/yada/formfields/inputCounter::field");
        final IModelFactory modelFactory = context.getModelFactory();
        IOpenElementTag replacementTagOpen = modelFactory.createOpenElementTag("div", divAttributes, AttributeValueQuotes.DOUBLE, false);
        ICloseElementTag replacementTagClose = modelFactory.createCloseElementTag("div");
        model.reset(); // Remove the yada tag completely
        model.add(replacementTagOpen);
        model.add(replacementTagClose);
	}
	
	private void processTag(IOpenElementTag sourceTag, ITemplateContext context, IElementModelStructureHandler structureHandler) {
        // Convert all attributes of the source tag
        Map<String, String> inputSourceAttributes = sourceTag.getAttributeMap();
        String targetAttributesString = yadaDialectUtil.getConvertedCustomTagAttributeString(sourceTag, context);
        structureHandler.setLocalVariable(TAG_NAME, inputSourceAttributes); // So I can do like ${input.id} to get the original id attribute and so on
        structureHandler.setLocalVariable("targetAttributesString", targetAttributesString);
	}

}
