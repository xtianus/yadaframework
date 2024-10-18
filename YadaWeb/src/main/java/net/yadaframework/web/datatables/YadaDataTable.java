package net.yadaframework.web.datatables;

import java.util.Locale;

import org.springframework.context.MessageSource;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import net.yadaframework.components.YadaUtil;
import net.yadaframework.web.datatables.config.YadaDataTableCommands;
import net.yadaframework.web.datatables.config.YadaDataTableHTML;
import net.yadaframework.web.datatables.options.YadaDataTableOptions;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class YadaDataTable {
	private MessageSource messageSource;
	private Locale locale;

	private String id;
	private YadaDataTableHTML yadaDataTableHTML;	
	@JsonProperty private YadaDataTableOptions options = new YadaDataTableOptions(this);
	
	// Package visibility
	YadaDataTable(String id, MessageSource messageSource, Locale locale) {
		this.id = id;
		this.locale = locale;
		this.messageSource = messageSource;
	}
	
	/**
	 * Set the HTML table structure
	 */
	public YadaDataTableHTML dtHTML() {
		yadaDataTableHTML = YadaUtil.lazyUnsafeInit(yadaDataTableHTML, () -> new YadaDataTableHTML(this, options, messageSource, locale));
    	return yadaDataTableHTML;
	}
	
	public YadaDataTableOptions dtOptions() {
		return options;
	}

	public String getId() {
		return id;
	}
	
}
