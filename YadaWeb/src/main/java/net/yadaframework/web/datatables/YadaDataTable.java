package net.yadaframework.web.datatables;

import java.util.List;
import java.util.Locale;

import org.springframework.context.MessageSource;

import net.yadaframework.web.datatables.config.YadaDataTableCommands;
import net.yadaframework.web.datatables.options.YadaDataTableOptions;

public class YadaDataTable {
	private MessageSource messageSource;
	private Locale locale;

	private String id;
	private YadaDataTableCommands yadaDataTableCommands;
	
	private String classes;
	private List<String> columns;
	private boolean disableCheckbox;
	private YadaDataTableOptions jsoptions;
	
	// Package visibility
	YadaDataTable(String id, MessageSource messageSource, Locale locale) {
		this.id = id;
		this.locale = locale;
		this.messageSource = messageSource;
	}
	
	/**
	 * Adds the add/edit/delete commands to the table
	 */
	public YadaDataTableCommands commands() {
        if (this.yadaDataTableCommands == null) {
            this.yadaDataTableCommands = new YadaDataTableCommands(this, messageSource, locale);
        }
    	return this.yadaDataTableCommands;
	}

    public YadaDataTable classes(String classes) {
        this.classes = classes;
        return this;
    }

    public YadaDataTable columns(List<String> columns) {
        this.columns = columns;
        return this;
    }

    public YadaDataTable disableCheckbox(boolean disableCheckbox) {
        this.disableCheckbox = disableCheckbox;
        return this;
    }

    public YadaDataTableOptions jsoptions() {
        if (this.jsoptions == null) {
            this.jsoptions = new YadaDataTableOptions(this);
        }
    	return this.jsoptions;
    }	
}
