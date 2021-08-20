package net.yadaframework.web.dialect;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IOpenElementTag;
import org.thymeleaf.model.ITemplateEvent;
import org.thymeleaf.standard.expression.IStandardExpression;
import org.thymeleaf.standard.expression.IStandardExpressionParser;
import org.thymeleaf.standard.expression.StandardExpressions;

import net.yadaframework.core.YadaConfiguration;

public class YadaDialectUtil {
	private final transient Logger log = LoggerFactory.getLogger(getClass());
	private final YadaConfiguration config;
	
	static final String YADA_PREFIX = "yada";
	static final String THYMELEAF_PREFIX = "th";
	
    public final static String YADA_PREFIX_WITHCOLUMN = YADA_PREFIX + ":";
    public final static String THYMELEAF_PREFIX_WITHCOLUMN = THYMELEAF_PREFIX + ":";


    private enum AppendType {
    	NONE,
    	APPEND,
    	PREPEND, 
    	APPEND_WITH_SPACE, 
    	PREPEND_WITH_SPACE;
    }

	public YadaDialectUtil(YadaConfiguration config) {
		this.config=config;
	}
	
	/**
	 * Returns a unique identifier on the page, for the given tag
	 * @param someTag
	 * @return
	 */
	public String makeYadaTagId(ITemplateEvent someTag) {
		return someTag.getLine() + "c" + someTag.getCol();
	}
	
	/**
	 * Retrieves a map of attributes from the custom tag, where all HTML attributes are kept as they are and thymeleaf
	 * th: attributes are converted to HTML attributes when possible. The map is then converted to a comma-separated string
	 * @param customTag
	 * @param context
	 * @return a comma-separated string of name=value attributes to be used in th:attr
	 */
	public String getConvertedCustomTagAttributeString(IOpenElementTag customTag, ITemplateContext context) {
		// First, get all HTML attributes
		Map<String, String> newAttributes = getHtmlAttributes(customTag);
		// Then convert all th: attributes to HTML attributes
		convertThAttributes(customTag, newAttributes, context);
        String newAttributesString = newAttributes.toString();
        newAttributesString = StringUtils.chop(newAttributesString); // Remove }
        newAttributesString = StringUtils.removeStart(newAttributesString, "{"); // Remove {
		return newAttributesString;
	}
	
	/**
	 * Get all HTML attributes from the custom sourceTag 
	 * @param sourceTag
	 * @return the HTML attributes found on the tag
	 */ 
	private Map<String, String> getHtmlAttributes(IOpenElementTag sourceTag) {
		Map<String, String> newAttributes = new HashMap<>();
		Map<String, String> sourceAttributes = sourceTag.getAttributeMap();
		for (Map.Entry<String,String> sourceAttribute : sourceAttributes.entrySet()) {
			String attributeName = sourceAttribute.getKey();
			String attributeValue = sourceAttribute.getValue();
			if (!attributeName.startsWith(YADA_PREFIX_WITHCOLUMN) && !attributeName.startsWith(THYMELEAF_PREFIX_WITHCOLUMN)) {
				newAttributes.put(attributeName, attributeValue==null?attributeName:attributeValue);
			}
		}
		return newAttributes;
	}
	
	private void convertThAttributes(IOpenElementTag sourceTag, Map<String, String> newAttributes, ITemplateContext context) {
		Map<String, String> sourceAttributes = sourceTag.getAttributeMap();
		final IEngineConfiguration configuration = context.getConfiguration();
		final IStandardExpressionParser parser = StandardExpressions.getExpressionParser(configuration);
		// Most of th: attributes have a direct equivalent, some must have a special treatment
		for (Map.Entry<String,String> sourceAttribute : sourceAttributes.entrySet()) {
			String fullThAttributeName = sourceAttribute.getKey();
			String attributeValue = sourceAttribute.getValue();
			if (fullThAttributeName.startsWith(THYMELEAF_PREFIX_WITHCOLUMN)) {
				String attributeName = removePrefix(fullThAttributeName, THYMELEAF_PREFIX); // from "th:value" to "value"
				String parsedValue = null;
				if (!"attr".equals(attributeName) && !"attrappend".equals(attributeName) && !"attrprepend".equals(attributeName)) {
					// attr and simila attributes don't use expressions as value
					try {
						final IStandardExpression expression = parser.parseExpression(context, attributeValue);
						parsedValue = (String) expression.execute(context);
					} catch (Exception e) {
						log.debug("Can't parse \"{}\" for attribute \"{}\" - skipping", attributeValue, fullThAttributeName);
						continue; // Next attribute
					}
				}
				// Hanlde some th attributes
				switch (attributeName) {
				case "field":
					// Add "name" and "value" attributes
					newAttributes.put("name", parsedValue);
					newAttributes.put("value", parsedValue);
					break;
				case "attr":
					handleAttr(attributeValue, newAttributes, AppendType.NONE, context, parser);
					break;
				case "alt-title":
					// Add "alt" and "title" attributes
					newAttributes.put("alt", parsedValue);
					newAttributes.put("title", parsedValue);
					break;
				case "lang-xmllang":
					// Add "lang" and "xmllang" attributes
					newAttributes.put("lang", parsedValue);
					newAttributes.put("xmllang", parsedValue);
					break;
				case "attrappend":
					handleAttr(attributeValue, newAttributes, AppendType.APPEND, context, parser);
					break;
				case "attrprepend":
					handleAttr(attributeValue, newAttributes, AppendType.PREPEND, context, parser);
					break;
				case "classappend":
					appendMapValue("class", attributeValue, AppendType.APPEND_WITH_SPACE, newAttributes);
					break;
				case "styleappend":
					appendMapValue("style", attributeValue, AppendType.APPEND, newAttributes);
					break;
				default:
					// Most th: attributes have plain HTML equivalent
					newAttributes.put(attributeName, parsedValue);
				}
			}
		}
	}
	
	/**
	 * Handles "th:attr", "th:attrappend", "th:attrprepend" by splitting the comma-separated string into individual name-values pairs for newAttributes
	 * @param attributeValue
	 * @param newAttributes
	 * @param appendType
	 * @param context
	 * @param parser
	 */
	private void handleAttr(String attributeValue, Map<String, String> newAttributes, AppendType appendType, ITemplateContext context, IStandardExpressionParser parser) {
		// Add each comma-separated attribute
		String[] attrAttributes = attributeValue.split(" *, *");
		for (String attrAttribute : attrAttributes) {
			// "name=value"
			String[] nameThenValue = attrAttribute.split(" *= *");
			String name = nameThenValue[0];
			String value = nameThenValue[1];
			final IStandardExpression attrExpression = parser.parseExpression(context, value);
			String finalValue = (String) attrExpression.execute(context);
			appendMapValue(name, finalValue, appendType, newAttributes);
		}
	}
	
	/**
	 * The value is appended to existing values found in the map with the same name
	 * @param name
	 * @param value
	 * @param appendType
	 * @param newAttributes
	 */
	private void appendMapValue(String name, String value, AppendType appendType, Map<String, String> newAttributes) {
		String current  = newAttributes.get(name);
		if (StringUtils.isNotBlank(current)) {
			if (appendType==AppendType.APPEND) {
				// Append to current value
				value = current + value;
			} else if (appendType==AppendType.PREPEND) {
				// Prepend to current value
				value = value + current;
			} else if (appendType==AppendType.APPEND_WITH_SPACE) {
				// Prepend to current value
				value = current + " " + value;
			} else if (appendType==AppendType.PREPEND_WITH_SPACE) {
				// Prepend to current value
				value = value + " " + current;
			}
		}
		newAttributes.put(name, value);
	}
	
	/**
	 * Remove the dialect prefix from the start of the value
	 * @param value e.g. "yada:someAttributeName"
	 * @param dialectPrefixNoColumn e.g. "yada"
	 * @return the value stripped from the starting dialect prefix, e.g. "someAttributeName"
	 */
	public String removePrefix(String value, String dialectPrefixNoColumn) {
		return StringUtils.removeStart(value, dialectPrefixNoColumn + ":");
	}

    /**
     * Browser cache bypass trick for resources.
     * Converts "/res/xxx" into "/res-123/xxx", "/yadares/xxx" into "/yadares-7/xxx", where the number is the application build number.
     */
	public String getVersionedAttributeValue(String value) {
    	try {
    		if (StringUtils.isBlank(value)) {
    			return value;
    		}
    		if (value.startsWith("//")) {
    			return value;
    		}
    		if (!value.startsWith("/")) {
    			return value; // Don't handle relative paths, for speed
    		}
			// The contextPath is applied by @{} so it's not needed here
			// String contextPath = ((org.thymeleaf.context.IWebContext)context).getRequest().getContextPath();
			int dividerPos = value.indexOf('/', 1); // Second slash
			if (dividerPos<0) {
				return value; // No second slash
			}
			String valueType = value.substring(1, dividerPos); // e.g. "res"
			String valueSuffix = value.substring(dividerPos); // e.g. "/xxx"
			boolean isResource = config.getResourceDir().equals(valueType);
			if (isResource) {
				return applyVersion(config.getVersionedResourceDir(), valueSuffix); // /site/res-0002/xxx
			}
			boolean isYada = config.getYadaResourceDir().equals(valueType);
			if (isYada) {
				return applyVersion(config.getVersionedYadaResourceDir(), valueSuffix); // /site/yadares-7/xxx
			}

			// The "contents" folder is not versioned anymore.
			// Cache bypass is better implemented by versioning the file name of anything stored there.
			// YadaAttachedFile should do this automatically.

			// The problem with contents is that the version should be taken from the file timestamp so here it should accept any value but I don't know how to make it work with any version value
//    	boolean isContent = config.getContentName().equals(valueType);
//    	if (isContent) {
//    		String contentUrlBase = config.getContentUrl(); // e.g. "/contents" or "http://somecdn.com/somecontext"
//    		if (config.isContentUrlLocal()) {
//    			return applyVersion(contentUrlBase.substring(1) + "/" + config.getApplicationBuild(), valueSuffix); // /site/contents/002/xxx
//    		}
//    		return contentUrlBase + "/" + valueSuffix; // e.g. http://somecdn.com/somecontext/xxx
//    	}
		} catch (Exception e) {
			log.error("getVersionedAttributeValue failed for value='{}'", value, e);
		}
    	return value;
    }

    /**
     *
     * @param versionedDir without leading /
     * @param valueSuffix
     * @return
     */
	private String applyVersion(String versionedDir, String valueSuffix) {
		StringBuilder result = new StringBuilder("/").append(versionedDir).append(valueSuffix); // e.g. "/res-0002/xxx
		return result.toString();
	}

}
