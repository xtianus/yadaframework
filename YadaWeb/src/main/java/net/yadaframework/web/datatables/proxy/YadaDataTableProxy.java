package net.yadaframework.web.datatables.proxy;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonIgnore;

import net.yadaframework.web.datatables.YadaDataTable;
import net.yadaframework.web.datatables.YadaDataTableConfigurer;
import net.yadaframework.web.datatables.config.YadaDataTableHTML;
import net.yadaframework.web.datatables.options.YadaDTOptions;

/**
 * This class implements the methods needed for <b>internal use</b> 
 * so that they don't pollute the fluent interface.
 */
public class YadaDataTableProxy extends YadaDataTable {

	public YadaDataTableProxy(String id, Locale locale) {
		super(id, locale);
	}
	
	/**
	 * Set the configurer used to create this instance, for internal use.
	 * @param configurer
	 */
	public void setConfigurer(YadaDataTableConfigurer configurer) {
		this.configurer = configurer;
	}

	/**
	 * Gets the configurer used to create this instance, for internal use.
	 */
	public Object getConfigurer() {
		return configurer;
	}

	public YadaDTOptions getOptions() {
		return options;
	}
	
	public String getId() {
		return id;
	}
	
	public YadaDataTableHTML getStructure() {
		// We use the trick of using the first getter to make sure that prepareConfiguration() is called before serialization
		prepareConfiguration();
		return yadaDataTableHTML;
	}
	
	public String getAjaxUrl() {
		return ajaxUrl;
	}
	
	public String getLanguageUrl() {
		String languageUrl = null;
		if (yadaDataTableLanguage != null) {
			languageUrl = yadaDataTableLanguage.getLanguageUrl(locale.getLanguage());
		}
		return languageUrl;
	}
}
