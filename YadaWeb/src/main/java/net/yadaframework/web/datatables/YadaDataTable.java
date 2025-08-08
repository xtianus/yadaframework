package net.yadaframework.web.datatables;

import java.util.Locale;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import jakarta.servlet.http.HttpServletRequest;
import net.yadaframework.components.YadaDataTableFactory;
import net.yadaframework.components.YadaWebUtil;
import net.yadaframework.exceptions.YadaInvalidUsageException;
import net.yadaframework.persistence.YadaDataTableDao;
import net.yadaframework.web.YadaDatatablesRequest;
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
// DO NOT move the "structure" property from the first position!
@JsonPropertyOrder({ "structure", "options" })
@JsonInclude(JsonInclude.Include.NON_NULL)
public class YadaDataTable {
	@JsonIgnore private YadaWebUtil yadaWebUtil = YadaWebUtil.INSTANCE;
	@JsonIgnore protected Locale locale;
	@JsonIgnore protected YadaDataTableConfigurer configurer; // Need a reference to check for changes in development
	@JsonIgnore protected YadaDataTableLanguageProxy yadaDataTableLanguage; 
	@JsonIgnore protected Class entityClass; // Used to fetch data from DB 
	@JsonIgnore protected String securityAsPath; // Path to check for user authorisation 
	
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
		if (ajaxUrl == null && entityClass==null) {
			throw new YadaInvalidUsageException("Either ajaxUrl or entityClass must be set for table '{}'", id);
		}
		// When the ajaxUrl is not set, we use the default url with the table id
		if (ajaxUrl == null) {
			// This must match the YadaController.yadaDataTableData() mapping
			ajaxUrl = yadaWebUtil.ensureThymeleafUrl("/yadaDataTableData");
		}
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
	 * Only to be used when the default handling is not enough.
	 * @param ajaxUrl the URL where data is fetched from
	 * @return this instance for method chaining
	 */
	public YadaDataTable dtAjaxUrl(String ajaxUrl) {
		this.ajaxUrl = yadaWebUtil.ensureThymeleafUrl(ajaxUrl);
		return this;
	}


	/**
	 * Type-safe overload that accepts a method reference to a controller method
	 * and extracts its @RequestMapping path (combined with class-level mapping if present).
	 *
	 * Usage example inside a controller: <pre>.dtAjaxUrl(this::userProfileTablePage)</pre>
	 *
	 * The referenced method is expected to have the signature:
	 * Map<String,Object> method(YadaDatatablesRequest request, Locale locale)
	 *
	 * @param handlerRef method reference to a controller handler
	 * @return this instance for method chaining
	 */
	public YadaDataTable dtAjaxUrl(YadaDtAjaxHandler handlerRef) {
		String url = YadaDataTableHelper.resolveAjaxUrlFromHandlerRef(handlerRef);
		this.ajaxUrl = yadaWebUtil.ensureThymeleafUrl(url);
		return this;
	}
	
	/**
	 * Set the entity class that will be used to fetch data from the database.
	 * Needs to be set when using yadaDataTableDao.getConvertedJsonPage() or an exception will be thrown
	 * by the ajax call.
	 * @param entityClass the entity
	 * @return this instance for method chaining
	 * @see YadaDataTableDao#getConvertedJsonPage(YadaDatatablesRequest, Class)
	 */
	public YadaDataTable dtEntityClass(Class entityClass) {
		this.entityClass = entityClass;
		// The entityClass is set only when the default YadaController is called, in which case
		// we need to enforce the same authorization as the current page for the ajax call.
		try {
			HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
			securityAsPath = request.getServletPath();
		} catch (Exception e) {
			throw new YadaInvalidUsageException("dtEntityClass() must be called in a web request", e);
		}
		return this;
	}

//	/**
//	 * Set the path to check for user authorisation when downloading the table data.
//	 * This should be a path that has been configured in SecurityConfig.
//	 * 
//	 * @param path the path to check for user authorisation
//	 * @return this instance for method chaining
//	 */
//	public YadaDataTable dtSecurityAs(String path) {
//		this.securityAsPath = path;
//		return this;
//	}
	
	/**
	 * Set the HTML table structure
	 */
	public YadaDataTableHTML dtStructureObj() {
    	return yadaDataTableHTML;
	}
	
	public YadaDTOptions dtOptionsObj() {
		return options;
	}
	
}
