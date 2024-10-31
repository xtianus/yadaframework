package net.yadaframework.web.dialect;
import java.util.HashMap;
import java.util.Map;

import org.checkerframework.checker.units.qual.K;
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
import net.yadaframework.exceptions.YadaInvalidUsageException;
import net.yadaframework.web.datatables.YadaDataTable;

/**
 * Defines the &lt;yada:dataTable> tag
 *
 */
public class YadaDataTableTagProcessor extends AbstractElementModelProcessor {
	// Tag syntax:
	//	<yada:dataTable yada:tableObject="${yadaDataTable}">
	//	</yada:dataTable>

	@SuppressWarnings("unused")
	private final transient Logger log = LoggerFactory.getLogger(getClass());

	private static final String TAG_NAME = "dataTable"; // Tag of the dialect: "yada:dataTable"
	public static final int PRECEDENCE = 12000; // ???

    private final String dialectPrefix;

    private final YadaDialectUtil yadaDialectUtil;

	public YadaDataTableTagProcessor(String dialectPrefix, YadaConfiguration config) {
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
        boolean valid = false;
		for (int i = 0; i < model.size(); i++) {
        	ITemplateEvent iTemplateEvent = model.get(i);
        	if (iTemplateEvent instanceof IOpenElementTag) {
        		final IOpenElementTag openTag = (IOpenElementTag) iTemplateEvent;
        		String tagName = yadaDialectUtil.removePrefix(openTag.getElementCompleteName(), dialectPrefix).toLowerCase();
        		switch (tagName) {
        		case "datatable":
        			valid = processTag(openTag, context, structureHandler);
        			break;
        		}
        	}
		}		
		
		if (valid) {
			// Replace the yada:dataTable tag with a th:replace tag.
	        Map<String, String> divAttributes = new HashMap<>();
	        String targetImplementation = "~{/yada/dataTable::body}";
	        divAttributes.put("th:replace", targetImplementation);
	        final IModelFactory modelFactory = context.getModelFactory();
	        IOpenElementTag replacementTagOpen = modelFactory.createOpenElementTag("div", divAttributes, AttributeValueQuotes.DOUBLE, false);
	        ICloseElementTag replacementTagClose = modelFactory.createCloseElementTag("div");
	        model.reset(); // Remove the yada tag completely
	        model.add(replacementTagOpen);
	        model.add(replacementTagClose);
		} else {
			throw new YadaInvalidUsageException("Invalid {} tag", TAG_NAME);
		}
	}

	private boolean processTag(IOpenElementTag sourceTag, ITemplateContext context, IElementModelStructureHandler structureHandler) {
        // Convert all attributes of the source tag.
		// Currently only "yada:definition" is supported
		String handler;
        Map<String, String> inputSourceAttributes = sourceTag.getAttributeMap();
        for (Map.Entry<String,String> sourceAttribute : inputSourceAttributes.entrySet()) {
			String attributeName = sourceAttribute.getKey();
			String attributeValue = sourceAttribute.getValue();
			if (attributeName.startsWith(YadaDialectUtil.YADA_PREFIX_WITHCOLUMN)) {
				String yadaAttributeName = yadaDialectUtil.removePrefix(attributeName, dialectPrefix);
				switch (yadaAttributeName) {
				case "configuration":
					YadaDataTable yadaDataTable = yadaDialectUtil.parseExpression(attributeValue, context, YadaDataTable.class);
					structureHandler.setLocalVariable("yadaDataTable", yadaDataTable);
					if (yadaDataTable==null) {
						log.error("Missing yada:configuration object in Model for yada:" + TAG_NAME);
						return false;
					}
					break;
				case "preprocessor":
					handler = yadaDialectUtil.parseExpression(attributeValue, context, String.class);
					structureHandler.setLocalVariable("preprocessor", handler);
					break;
				case "postprocessor":
					handler = yadaDialectUtil.parseExpression(attributeValue, context, String.class);
					structureHandler.setLocalVariable("postprocessor", handler);
					break;
				}
			}
        }

        // This is for using HTML attributes but we don't need them yet
        //String targetAttributesString = yadaDialectUtil.getConvertedHTMLCustomTagAttributeString(sourceTag, context);
        // structureHandler.setLocalVariable("targetAttributesString", targetAttributesString);
        structureHandler.setLocalVariable("_"+TAG_NAME+"_", inputSourceAttributes); // So I can do like ${_dataTable_.id} to get the original id attribute and so on
        return true;
	}	

}
