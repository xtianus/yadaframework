package net.yadaframework.web.dialect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.context.ITemplateContext;

import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.exceptions.YadaInvalidValueException;

public class YadaDialectUtil {
	private final transient Logger log = LoggerFactory.getLogger(getClass());
	private final YadaConfiguration config;

	public YadaDialectUtil(YadaConfiguration config) {
		this.config=config;
	}
	
    /**
     * Converts "/res/xxx" into "/res-123/xxx", 
     * "/yadares/xxx" into "/yadares-7/xxx",
     * "/content/xxx" into either "/content-123/xxx" or "http://somecdn.com/somecontext/xxx" depending on conf/paths/contentDir/@url
     * and leaves "/static" alone
     */
    protected String getVersionedAttributeValue(final ITemplateContext context, String value) {
    	try {
			// The contextPath is applied by @{} so it's not needed here
			// String contextPath = ((org.thymeleaf.context.IWebContext)context).getRequest().getContextPath();
			int dividerPos = value.indexOf('/', 1); // Second slash
			if (dividerPos<0) {
				throw new YadaInvalidValueException("Invalid url: {}", value);
			}
			String valueType = value.substring(1, dividerPos); // e.g. "res"
//    	String valuePrefix = value.substring(0, dividerPos); // e.g. "/res"
			String valueSuffix = value.substring(dividerPos); // e.g. "/xxx"
			boolean isResource = config.getResourceDir().equals(valueType);
			if (isResource) {
				return applyVersion(config.getVersionedResourceDir(), valueSuffix); // /site/res-0002/xxx
			}
			boolean isYada = config.getYadaResourceDir().equals(valueType);
			if (isYada) {
				return applyVersion(config.getVersionedYadaResourceDir(), valueSuffix); // /site/yadares-7/xxx
			}
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
