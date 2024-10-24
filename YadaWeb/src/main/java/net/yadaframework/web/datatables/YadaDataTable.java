package net.yadaframework.web.datatables;

import java.util.Locale;

import org.springframework.context.MessageSource;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.web.datatables.config.YadaDataTableHTML;
import net.yadaframework.web.datatables.options.YadaDataTableOptions;

/**
 * Class representing a DataTable.
 */

// The whole purpose of refactoring from the old version is to have autosuggestion in the IDE and syntax check/validation
// because there are too many configuration options and parameters to be remembered.
// The drawback is that I can't use thymeleaf features nicely and the user needs to type some HTML in the java code.

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class YadaDataTable {
	private YadaConfiguration config;
	private MessageSource messageSource; // TODO remove !!!!!!!!!!!!!!!
	private Locale locale; // TODO remove !!!!!!!!!!!!!!!

	private String id;
	private YadaDataTableHTML yadaDataTableHTML;	
	@JsonProperty private YadaDataTableOptions options = new YadaDataTableOptions(this);
	
	/**
	 * Create a new instance of DataTable configuration. Use for simple use cases when performance is not a concern
	 * and the instance must not be reused in other requests. Otherwise use the factory.
	 * @param id
	 * @param config
	 * @param messageSource
	 * @param locale
	 * @see YadaDataTableFactory#getSingleton(String, Locale, YadaDataTableConfigurer)
	 */
	public YadaDataTable(String id, YadaConfiguration config, MessageSource messageSource, Locale locale) {
		this.id = id;
		this.locale = locale;
		this.messageSource = messageSource;
		this.config = config;
		this.yadaDataTableHTML = new YadaDataTableHTML(this, options, config, messageSource, locale);
	}
	
	/**
	 * Set the HTML table structure
	 */
	public YadaDataTableHTML dtHTML() {
		// yadaDataTableHTML = YadaUtil.lazyUnsafeInit(yadaDataTableHTML, () -> new YadaDataTableHTML(this, options, config, messageSource, locale));
    	return yadaDataTableHTML;
	}
	
	public YadaDataTableOptions dtOptions() {
		return options;
	}

	public String getId() {
		return id;
	}

	public YadaDataTableHTML getHTML() {
		return yadaDataTableHTML;
	}
	
}
