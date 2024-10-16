package net.yadaframework.web.datatables.config;

import java.util.Locale;

import org.springframework.context.MessageSource;

import net.yadaframework.core.YadaFluentBase;
import net.yadaframework.web.datatables.YadaDataTable;

public class YadaDataTableCommands extends YadaFluentBase<YadaDataTable> {
	private MessageSource messageSource;
	private Locale locale;
	
	private String addButton;
	private String editButton;
	private String deleteButton;
	
	public YadaDataTableCommands(YadaDataTable parent, MessageSource messageSource, Locale locale) {
		super(parent);
		this.messageSource = messageSource;
		this.locale = locale;
	}
	
	/**
	 * Enables the "Add" button and sets its label.
	 * @param addLabel either the label text or the label key for localization
	 */
    public YadaDataTableCommands add(String addLabel) {
    	this.addButton = messageSource.getMessage(addLabel, null, addLabel, locale);
        return this;
    }

    /**
     * Enables the "Edit" button and sets its label.
     * @param editLabel either the label text or the label key for localization
     */
    public YadaDataTableCommands edit(String editLabel) {
        this.editButton = messageSource.getMessage(editLabel, null, editLabel, locale);
        return this;
    }

    /**
     * Enables the "Delete" button and sets its label.
     * @param deleteLabel either the label text or the label key for localization
     * @return
     */
    public YadaDataTableCommands delete(String deleteLabel) {
        this.deleteButton = messageSource.getMessage(deleteLabel, null, deleteLabel, locale);
        return this;
    }	
	

}
