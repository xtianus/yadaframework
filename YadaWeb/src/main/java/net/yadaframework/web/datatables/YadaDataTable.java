package net.yadaframework.web.datatables;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import net.yadaframework.components.YadaWebUtil;
import net.yadaframework.web.datatables.config.YadaDataTableHTML;
import net.yadaframework.web.datatables.config.YadaDataTableLanguage;
import net.yadaframework.web.datatables.options.YadaDTOptions;
import net.yadaframework.web.datatables.proxy.YadaDTOptionsProxy;
import net.yadaframework.web.datatables.proxy.YadaDataTableHTMLProxy;
import net.yadaframework.web.datatables.proxy.YadaDataTableLanguageProxy;

/**
 * Class representing a DataTable.
 */

// The whole purpose of refactoring from the old version is to have autosuggestion in the IDE and syntax check/validation
// because there are too many configuration options and parameters to be remembered.
// The drawback is that I can't use thymeleaf features nicely and the user needs to type some HTML in the java code.

// Uses @JsonPropertyOrder to ensure that prepareConfiguration() is called before serialization. 
// DO NOT move the "html" property from the first position!
@JsonPropertyOrder({ "html", "options" })
@JsonInclude(JsonInclude.Include.NON_NULL)
public class YadaDataTable {
	@JsonIgnore private YadaWebUtil yadaWebUtil = YadaWebUtil.INSTANCE;
	@JsonIgnore protected Locale locale;
	@JsonIgnore protected YadaDataTableConfigurer configurer; // Need a reference to check for changes in development
	@JsonIgnore protected YadaDataTableLanguageProxy yadaDataTableLanguage; 
	
	protected String id;
	protected String ajaxUrl;
	protected YadaDataTableHTMLProxy yadaDataTableHTML;
	protected YadaDTOptionsProxy options = new YadaDTOptionsProxy(this);
	
	/**
	 * Constructor used internally by {@link YadaDataTableFactory}
	 * @param id
	 * @param datasource
	 * @param locale
	 * @see YadaDataTableFactory#getSingleton(String, String, Locale)
	 * @see #YadaDataTable(String, String)
	 */
	// Must not be public because there's no need to store the locale when the instance lives with the current request (i.e. is not a singleton)
	protected YadaDataTable(String id, Locale locale) {
		this.id = id;
		this.yadaDataTableHTML = new YadaDataTableHTMLProxy(this, options);
		this.locale = locale;
	}

	protected void prepareConfiguration() {
		yadaDataTableHTML.prepareConfiguration();
	}
	
	/**
	 * Set the base url where language files are located. It can be the official DataTables URL or a local endpoint.
	 * @param languageBaseUrl the URL of the root folder for the DataTable i18n translations. 
	 * 			Can be a CDN or a local folder e.g. "/static/datatables-2.1.8/i18n" depending on where the sources have been saved.
	 * 			It can contain Thymeleaf expressions.
	 * 			When null or empty, the default value is used: https://cdn.datatables.net/plug-ins/2.1.8/i18n/
	 * @return an instance of YadaDataTableLanguage for method chaining
	 */
	public YadaDataTableLanguage dtLanguageObj(String languageBaseUrl) {
		this.yadaDataTableLanguage = new YadaDataTableLanguageProxy(languageBaseUrl, this);
		return this.yadaDataTableLanguage;
	}
	
	/**
	 * Set the URL where data is fetched from (without language in the path). Can contain Thymeleaf expressions.
	 * @param ajaxUrl
	 * @return this instance for method chaining
	 */
	public YadaDataTable dtAjaxUrl(String ajaxUrl) {
		this.ajaxUrl = yadaWebUtil.ensureThymeleafUrl(ajaxUrl);
		return this;
	}
	
	/**
	 * Set the HTML table structure
	 */
	public YadaDataTableHTML dtHTMLObj() {
    	return yadaDataTableHTML;
	}
	
	public YadaDTOptions dtOptionsObj() {
		return options;
	}
	
}
