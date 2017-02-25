package net.yadaframework.web.dialect;

import org.thymeleaf.context.ITemplateContext;

import net.yadaframework.core.YadaConfiguration;

public class YadaDialectUtil {
	private final YadaConfiguration config;

	// Cache per valori usati spesso
	private String resReplaceBase = null;
	private String yadaresReplaceBase = null;
	private String resUrlPrefix = null; // /site/res/
	private String yadaresUrlPrefix = null; // /site/yadares/
	int urlPrefixLen = 0;
	int urlYadaprefixLen = 0;


	public YadaDialectUtil(YadaConfiguration config) {
		this.config=config;
	}
	
    /**
     * 
     */
    protected String getResAttributeValue(final ITemplateContext context, String url) {
    	// Trasforma /site/res/img/favicon.ico in /site/res-0002/img/favicon.ico
    	if (resReplaceBase==null) {
    		String contextPath = ((org.thymeleaf.context.IWebContext)context).getRequest().getContextPath();
    		resUrlPrefix = contextPath + "/" + config.getResourceDir() + "/"; // /site/res/
    		urlPrefixLen = resUrlPrefix.length();
    		resReplaceBase = contextPath + "/" + config.getVersionedResourceDir() + "/"; // /site/res-0002/
    	}
    	if (url.startsWith(resUrlPrefix)) {
    		String trailingUrl = url.substring(urlPrefixLen);
    		url = resReplaceBase + trailingUrl;
    		return url;		
    	}
    	// Trasforma /site/yadares/img/favicon.ico in /site/yadares-05/img/favicon.ico
    	if (yadaresReplaceBase==null) {
    		String contextPath = ((org.thymeleaf.context.IWebContext)context).getRequest().getContextPath();
    		yadaresUrlPrefix = contextPath + "/" + config.getYadaResourceDir() + "/"; // /site/yadares/
    		urlYadaprefixLen = yadaresUrlPrefix.length();
    		yadaresReplaceBase = contextPath + "/" + config.getVersionedYadaResourceDir() + "/"; // /site/yadares-05/
    	}
    	if (url.startsWith(yadaresUrlPrefix)) {
    		String trailingUrl = url.substring(urlYadaprefixLen);
    		url = yadaresReplaceBase + trailingUrl;
    	}
    	//
    	return url;
    }
	

}
