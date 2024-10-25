package net.yadaframework.web.datatables.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.yadaframework.core.YadaFluentBase;
import net.yadaframework.exceptions.YadaInvalidUsageException;
import net.yadaframework.web.datatables.YadaDataTable;
import net.yadaframework.web.datatables.options.YadaDTColumns;
import net.yadaframework.web.datatables.options.proxy.YadaDataTableOptionsProxy;

// This class is not serialized as json
public class YadaDataTableHTML extends YadaFluentBase<YadaDataTable> {

	private String cssClasses;
	private String selectCheckboxTitle; // When null, no select checkbox in first row
	// private List<String> columnTitles = new ArrayList<String>();
	private String commandsTitle;
	private Boolean showFooter = false;
	private List<YadaDataTableButton> buttons = new ArrayList<>();

	// Package visible
	List<YadaDataTableColumn> columns = new ArrayList<>();
	Map<Integer, YadaDataTableColumn> orderingMap = new TreeMap<>(); // order precedence --> column
	YadaDataTableOptionsProxy options;
	
	public YadaDataTableHTML(YadaDataTable parent, YadaDataTableOptionsProxy options) {
		super(parent);
		this.options = options;
	}
	
	/**
	 * Add a new column to the table. Order is preserved.
	 * @param headerText the text to show in the header for this column, can be null 
	 * @return column instance for method chaining
	 */
	public YadaDataTableColumn dtColumnObj(String headerText) {
		YadaDataTableColumn column = new YadaDataTableColumn(headerText, this);
		this.columns.add(column);
		return column;
	}
	
	/**
	 * Add a new button to the table. It can appear both in the commands column and in the toolbar.
	 * Order is preserved.
	 * @param text Tooltip text for the command icon and text for the toolbar button. Can be a message key.
	 */
	public YadaDataTableButton dtButtonObj(String text) {
		YadaDataTableButton button = new YadaDataTableButton(text, this);
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

	public List<YadaDataTableButton> getButtons() {
		return buttons;
	}

	@Override
	protected void validate() {
		//
		// Validation
		//
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
		//
		// Configuration
		//
		setColumnPositions();
		// Set the ordering options
		if (!orderingMap.isEmpty()) {
			boolean multipleOrdering = orderingMap.size() > 1;
			if (multipleOrdering) {
				options.dtOrderMulti(true);
			}
			options.dtOrdering(makeOrderingOptions());
		}
		// Column for checkboxes
		if (isSelectCheckbox()) {
			YadaDTColumns newColumn = options.addNewColumn(0);
			newColumn.dtOrderable(false).dtSearchable(false);
			// ||||||||||||||||||| aggiungere definitzione della colonna e inserirla in options.columns all'inizio della List
			//	columnDef.push(
			//			{	// Colonna dei checkbox
			//				data: null,
			//				name: '_yadaSelectionColumn', // Non usato
			//				orderable: false,
			//				searchable: false,
			//				render: function ( data, type, row ) {
			//					if ( type === 'display' ) {
			//						return '<input type="checkbox" class="yadaCheckInCell s_rowSelector"/>';
			//					}
			//					return data;
			//				},
			//				width: "50px",
			//				className: "yadaCheckInCell"
			//			}	                   
		
		
		}
	}

	private void setColumnPositions() {
		int position = isSelectCheckbox() ? 1 : 0;
		for (YadaDataTableColumn column : columns) {
			column.setPositionInTable(position++);
		}
	}

//	/**
//	 * Get a column by its position. If there is a checkbox column, the position of other columns starts at 1 instead of 0
//	 * @param position the column position in the table, considering any initial checkbox column
//	 * @return the YadaDataTableColumn at that position in the table
//	 */
//	// Not used
//	private YadaDataTableColumn getColumnByPosition(int position) {
//		if (selectCheckboxTitle != null) {
//			position--;
//		}
//		if (position<0 || position>=columns.size()) {
//			throw new YadaInvalidUsageException("Invalid column position: "+position);
//		}
//		return columns.get(position);
//	}

	private String makeOrderingOptions() {
		// Example:
		//	[
		//	    { idx: 1, 'asc' },
		//	    { idx: 2, 'desc' }
		//	]
		StringBuilder result = new StringBuilder();
		result.append("[");
		for (YadaDataTableColumn column : orderingMap.values()) {
			String direction = Boolean.FALSE.equals(column.orderAsc) ? "desc" : "asc";
			result.append(String.format("{ idx: %d, '%s' }", column.positionInTable, direction));
		}
		result.append("]");
		return result.toString();
	}

}
