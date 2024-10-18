package net.yadaframework.web.datatables.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.context.MessageSource;

import net.yadaframework.core.YadaFluentBase;
import net.yadaframework.web.datatables.YadaDataTable;
import net.yadaframework.web.datatables.options.YadaDataTableOptions;

public class YadaDataTableHTML extends YadaFluentBase<YadaDataTable> {
	private MessageSource messageSource;
	private Locale locale;
	private YadaDataTableOptions options;

	private boolean selectCheckbox = true;
	private List<String> columnTitles = new ArrayList<String>();
	private String cssClasses;
	
	private boolean buttonAdd = false;
	private boolean buttonEdit = false;
	private boolean buttonDelete = false;
	private String buttonAddLabel; // Label text or key
	private String buttonEditLabel;
	private String buttonDeleteLabel;
	
	public YadaDataTableHTML(YadaDataTable parent, YadaDataTableOptions options, MessageSource messageSource, Locale locale) {
		super(parent);
		this.messageSource = messageSource;
		this.locale = locale;
		this.options = options;
	}
	
	/**
	 * Removes the leftmost column with select checkboxes 
	 */
	public YadaDataTableHTML dtNoSelectCheckbox() {
		this.selectCheckbox = false;
		return this;
	}
	
	public YadaDataTableHTML dtColumn(String columnTitle) {
		columnTitles.add(columnTitle);
		return this;
	}
	
	public YadaDataTableHTML dtCssClasses(String cssClasses) {
		this.cssClasses = cssClasses;
		return this;
	}
	
	public YadaDataTableHTML dtButtonAdd() {
		this.buttonAdd = true;
		return this;
	}
	
	public YadaDataTableHTML dtButtonAdd(String label) {
		dtButtonAdd();
		this.buttonAddLabel = label;
		return this;
	}
	
	public YadaDataTableHTML dtButtonEdit() {
		this.buttonEdit = true;
		return this;
	}
	
	public YadaDataTableHTML dtButtonEdit(String label) {
		dtButtonEdit();
		this.buttonEditLabel = label;
		return this;
	}
	
	public YadaDataTableHTML dtButtonDelete() {
		this.buttonDelete = true;
		return this;
	}
	
	public YadaDataTableHTML dtButtonDelete(String label) {
		dtButtonDelete();
		this.buttonDeleteLabel = label;
		return this;
	}


}
