package net.yadaframework.web.datatables;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import net.yadaframework.components.YadaWebUtil;
import net.yadaframework.web.datatables.config.YadaDataTableHTML;
import net.yadaframework.web.datatables.config.YadaDataTableLanguage;
import net.yadaframework.web.datatables.options.YadaDTOptions;
import net.yadaframework.web.datatables.options.proxy.YadaDataTableOptionsProxy;

/**
 * Class representing a DataTable.
 */

// The whole purpose of refactoring from the old version is to have autosuggestion in the IDE and syntax check/validation
// because there are too many configuration options and parameters to be remembered.
// The drawback is that I can't use thymeleaf features nicely and the user needs to type some HTML in the java code.

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class YadaDataTable {
	@JsonIgnore private YadaWebUtil yadaWebUtil = YadaWebUtil.INSTANCE;

	private String id;
	private String datasource;
	private YadaDataTableLanguage yadaDataTableLanguage;	
	private YadaDataTableOptionsProxy options = new YadaDataTableOptionsProxy(this);
	@JsonIgnore private YadaDataTableHTML yadaDataTableHTML;	
	
	/**
	 * Create a new instance of DataTable configuration. Use for simple use cases when performance is not a concern
	 * and the instance must not be reused in other requests. Otherwise use the factory.
	 * @param id
	 * @param datasource the URL where data is fetched from (without language in the path). Can contain thymeleaf expressions
	 * @see YadaDataTableFactory#getSingleton(String, Locale, YadaDataTableConfigurer)
	 */
	public YadaDataTable(String id, String datasource) {
		this.id = id;
		this.datasource = yadaWebUtil.ensureThymeleafUrl(datasource);
		this.yadaDataTableHTML = new YadaDataTableHTML(this, options);
	}
	
	/**
	 * Set i18n options. Default is American English.
	 * @param languageBaseUrl the URL of the root folder for the DataTable i18n translations. 
	 * 			Can be a CDN or a local folder e.g. "/static/datatables-2.1.8/i18n" depending on where the sources have been saved.
	 * @return an instance of YadaDataTableLanguage for method chaining
	 */
	public YadaDataTableLanguage dtLanguageObj(String languageBaseUrl) {
		this.yadaDataTableLanguage = new YadaDataTableLanguage(languageBaseUrl, this);
		return this.yadaDataTableLanguage;
	}
	
	/**
	 * Set the HTML table structure
	 */
	public YadaDataTableHTML dtHTMLObj() {
		// yadaDataTableHTML = YadaUtil.lazyUnsafeInit(yadaDataTableHTML, () -> new YadaDataTableHTML(this, options, config, messageSource, locale));
    	return yadaDataTableHTML;
	}
	
	public YadaDTOptions dtOptionsObj() {
		return options;
	}

	public String getId() {
		return id;
	}

	@JsonIgnore
	public YadaDataTableHTML getHTML() {
		return yadaDataTableHTML;
	}

	public String getDatasource() {
		return datasource;
	}

	public YadaDataTableLanguage getLanguage() {
		return yadaDataTableLanguage;
	}

	public YadaDTOptions getOptions() {
		return options;
	}
	
}
