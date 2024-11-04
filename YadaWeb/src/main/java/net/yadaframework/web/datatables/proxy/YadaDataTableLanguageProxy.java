package net.yadaframework.web.datatables.proxy;

import com.fasterxml.jackson.annotation.JsonIgnore;

import net.yadaframework.components.YadaWebUtil;
import net.yadaframework.web.datatables.YadaDataTable;
import net.yadaframework.web.datatables.config.YadaDataTableLanguage;

/**
 * This class implements the methods needed for <b>internal use</b> 
 * so that they don't pollute the fluent interface.
 */
public class YadaDataTableLanguageProxy extends YadaDataTableLanguage {
	@JsonIgnore private YadaWebUtil yadaWebUtil = YadaWebUtil.INSTANCE;

	public YadaDataTableLanguageProxy(String languageBaseUrl, YadaDataTable parent) {
		super(languageBaseUrl, parent);
	}
	
	public String getLanguageUrl(String language) {
		String languageFile = languageMap.get(language);
		if (languageFile!=null) {
			return yadaWebUtil.ensureThymeleafUrl(languageBaseUrl + languageFile);
		}
		return null;
	}
	

}
