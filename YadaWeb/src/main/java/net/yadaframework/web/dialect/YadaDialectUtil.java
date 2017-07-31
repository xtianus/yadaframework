package net.yadaframework.web.dialect;

import org.thymeleaf.context.ITemplateContext;

import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.exceptions.YadaInvalidUsageException;

public class YadaDialectUtil {
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
    	// The contextPath is applied by @{} so it's not needed here
    	// String contextPath = ((org.thymeleaf.context.IWebContext)context).getRequest().getContextPath();
    	int dividerPos = value.indexOf('/', 1); // Second slash
    	String valueType = value.substring(1, dividerPos); // e.g. "res"
    	String valuePrefix = value.substring(0, dividerPos); // e.g. "/res"
    	String valueSuffix = value.substring(dividerPos); // e.g. "/xxx"
    	boolean isResource = config.getResourceDir().equals(valueType);
    	if (isResource) {
    		return applyVersion(config.getVersionedResourceDir(), valueSuffix); // /site/res-0002/xxx
    	}
    	boolean isYada = config.getYadaResourceDir().equals(valueType);
    	if (isYada) {
    		return applyVersion(config.getVersionedYadaResourceDir(), valueSuffix); // /site/yadares-7/xxx
    	}
    	boolean isContent = config.getContentName().equals(valueType);
    	if (isContent) {
    		String contentUrlBase = config.getContentUrl(); // e.g. "/contents" or "http://somecdn.com/somecontext"
    		boolean localUrl = contentUrlBase.charAt(0) == '/' && contentUrlBase.charAt(1) != '/';
    		if (localUrl) {
    			return applyVersion(contentUrlBase + "-" + config.getApplicationBuild(), valueSuffix); // /site/contents-002/xxx
    		}
    		return contentUrlBase + "/" + valueSuffix; // e.g. http://somecdn.com/somecontext/xxx
    	}
    	return value;
    }

	private String applyVersion(String versionedDir, String valueSuffix) {
		StringBuilder result = new StringBuilder(versionedDir).append(valueSuffix); // e.g. "/site/res-0002/xxx
		return result.toString();
	}

}
