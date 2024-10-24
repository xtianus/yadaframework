package net.yadaframework.web.datatables.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.context.MessageSource;

import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.core.YadaFluentBase;
import net.yadaframework.exceptions.YadaInvalidUsageException;
import net.yadaframework.web.datatables.YadaDataTable;
import net.yadaframework.web.datatables.options.YadaDataTableOptions;

public class YadaDataTableHTML extends YadaFluentBase<YadaDataTable> {
	private YadaConfiguration config;
	private MessageSource messageSource; // TODO remove !!!!!!!!!!!!!!!
	private Locale locale; // TODO remove !!!!!!!!!!!!!!!
	// Package visible
	YadaDataTableOptions options;

	private String cssClasses;
	private String selectCheckboxTitle; // When null, no select checkbox in first row
	// private List<String> columnTitles = new ArrayList<String>();
	private String commandsTitle;
	private Boolean showFooter = false;
	
	// Package visible
	List<YadaDataTableColumn> columns = new ArrayList<>();
	private List<YadaDataTableButton> buttons = new ArrayList<>();
	
	public YadaDataTableHTML(YadaDataTable parent, YadaDataTableOptions options, YadaConfiguration config, MessageSource messageSource, Locale locale) {
		super(parent);
		this.messageSource = messageSource;
		this.locale = locale;
		this.options = options;
		this.config = config;
	}
	
	/**
	 * Add a new column to the table. Order is preserved.
	 * @param headerText the text to show in the header for this column, can be null 
	 * @return this instance for method chaining
	 */
	public YadaDataTableHTML dtColumn(String headerText) {
		return dtColumn(headerText, null);
	}
	
	/**
	 * Add a new column to the table. Order is preserved.
	 * @param headerText the text to show in the header for this column, can be null 
	 * @param name the name of the column for cross reference, can be null
	 * @return this instance for method chaining
	 */
	public YadaDataTableHTML dtColumn(String headerText, String name) {
		YadaDataTableColumn column = new YadaDataTableColumn(headerText, name, this);
		this.columns.add(column);
		return this;
	}
	
	/**
	 * Add a new button to the table. It can appear both in the commands column and in the toolbar.
	 * Order is preserved.
	 * @param text Tooltip text for the command icon and text for the toolbar button. Can be a message key.
	 */
	public YadaDataTableButton dtButton(String text) {
		YadaDataTableButton button = new YadaDataTableButton(text, this, config);
		this.buttons.add(button);
		return button;
	}
	
	/**
	 * Shows a footer with the same content as the header
	 * @return this instance for method chaining
	 */
	public YadaDataTableHTML dtFooter() {
		this.showFooter = true;
		return this;
	}
	
	/**
	 * Adds the leftmost column with select checkboxes
	 * @param the title text or key 
	 * @return this instance for method chaining
	 */
	public YadaDataTableHTML dtColumnCheckbox(String title) {
		this.selectCheckboxTitle = title;
		return this;
	}
	
	/**
	 * Adds the rightmost column with command icons
	 * @param title
	 * @return this instance for method chaining
	 */
	public YadaDataTableHTML dtColumnCommands(String title) {
		this.commandsTitle = title;
		return this;
	}
	
	/**
	 * Set the css classes to set on the table tag. Defaults to 'table-striped no-wrap'
	 * @param cssClasses
	 * @return this instance for method chaining
	 */
	public YadaDataTableHTML dtCssClasses(String cssClasses) {
		this.cssClasses = cssClasses;
		return this;
	}
	
	public YadaDataTableOptions getOptions() {
		return options;
	}

	public boolean isCommandsTitle() {
		return commandsTitle!=null;
	}
	
	public boolean isSelectCheckbox() {
		return selectCheckboxTitle!=null;
	}
	
	public String getSelectCheckboxTitle() {
		return selectCheckboxTitle;
	}

	public List<YadaDataTableColumn> getColumns() {
		return columns;
	}

	public String getCssClasses() {
		return cssClasses;
	}

	public String getCommandsTitle() {
		return commandsTitle;
	}
	
	public Boolean isShowFooter() {
		return showFooter;
	}

	@Override
	protected void validate() {
		boolean hasCommand = buttons.stream().anyMatch(button -> !button.isGlobal());
		// When the commands column is set, there must be at least one button which is not a global one
		if (commandsTitle != null && !hasCommand) {
			throw new YadaInvalidUsageException("Commands column is added but there are only global buttons");
		}
		// When the commands column is not set but it should, add it with an empty title
		if (commandsTitle == null && hasCommand) {
			dtColumnCommands("");
		}
		// When there is no selectCheckbox column, but there is a least one non-global button, add the column
		if (!isSelectCheckbox() && hasCommand) {
			dtColumnCheckbox("");
		}
	}

	public List<YadaDataTableButton> getButtons() {
		return buttons;
	}



}
