package net.yadaframework.web.datatables.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import net.yadaframework.core.YadaFluentBase;
import net.yadaframework.exceptions.YadaInvalidUsageException;
import net.yadaframework.persistence.YadaDataTableDao;
import net.yadaframework.web.datatables.YadaDataTable;
import net.yadaframework.web.datatables.options.YadaDTColumns;
import net.yadaframework.web.datatables.proxy.YadaDTOptionsProxy;
import net.yadaframework.web.datatables.proxy.YadaDataTableButtonProxy;
import net.yadaframework.web.datatables.proxy.YadaDataTableColumnProxy;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class YadaDataTableHTML extends YadaFluentBase<YadaDataTable> {
	private Logger log = LoggerFactory.getLogger(this.getClass());

	// Only the buttons array is currently used on the HTML side
	protected List<YadaDataTableButtonProxy> buttons = new ArrayList<>();

	@JsonIgnore protected String cssClasses;
	@JsonIgnore protected String selectCheckboxTitle; // When null, no select checkbox in first row
	// private List<String> columnTitles = new ArrayList<String>();
	@JsonIgnore protected String commandsTitle;
	@JsonIgnore protected Boolean showFooter = false;

	@JsonIgnore protected List<YadaDataTableColumnProxy> columns = new ArrayList<>();
	@JsonIgnore protected Map<Integer, YadaDataTableColumn> orderingMap = new TreeMap<>(); // order precedence --> column
	@JsonIgnore protected YadaDTOptionsProxy options;
	
	@JsonIgnore protected boolean backCalled = false;
	
	protected YadaDataTableHTML(YadaDataTable parent, YadaDTOptionsProxy options) {
		super(parent);
		this.options = options;
	}
	
	/**
	 * Add a new column to the table. Order is preserved.
	 * @param headerText the text to show in the header for this column, can be null 
	 * @param data the json path of the result in the ajax response, for example "id" or "useCredentials.username".<br>
	 * 		It can eventually be localized to access localized strings using e.g. <pre>"title."+locale.getLanguage()</pre>
	 * 		for a title in the current user locale.<br>
	 * 		Can also be a javascript function provided on page if the value is like "someFunction()".
	 * 		In that case the function signature should be <pre>function(data, type, row, meta)</pre>
	 * 		Remember in such case to set a name on the column with {@link YadaDataTableColumn#dtName(String)} for database operations.
	 * 		@see <a href="https://datatables.net/reference/option/columns.data">DataTables Reference: columns.data</a> 
	 * @return column instance for method chaining
	 */
	public YadaDataTableColumn dtColumnObj(String headerText, String data) {
		YadaDataTableColumnProxy column = new YadaDataTableColumnProxy(headerText, data, this);
		this.columns.add(column);
		return column;
	}
	
	/**
	 * Add a new button to the table. It can appear both in the commands column and in the toolbar.
	 * Order is preserved.
	 * @param text Tooltip text for the command icon and text for the toolbar button. Can be a message key.
	 */
	public YadaDataTableButton dtButtonObj(String text) {
		YadaDataTableButtonProxy button = new YadaDataTableButtonProxy(text, this);
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

	// This is private not to pollute the fluent interface
	private boolean isSelectCheckbox() {
		return selectCheckboxTitle!=null;
	}

	@Override
	public YadaDataTable back() {
		//
		// Validation
		//
		boolean hasCommand = buttons.stream().anyMatch(button -> !button.isGlobal());
		// When the commands column is set, there must be at least one button which is not a global one
		if (commandsTitle != null && !hasCommand) {
			throw new YadaInvalidUsageException("Commands column is added but there are only global buttons");
		}
// Not sure it's correct to force the command column when there is a non-global button		
//		// When the commands column is not set but it should, add it with an empty title
//		if (commandsTitle == null && hasCommand) {
//			log.debug("Commands column forced because of non-global button");
//			dtColumnCommands("");
//		}
		// When there is no selectCheckbox column, but there is a least one non-global button, add the column
		if (!isSelectCheckbox() && hasCommand) {
			log.warn("Checkbox column forced because of non-global button. Use dtColumnCheckbox() to add it explicitly");
			dtColumnCheckbox("");
		}
		//
		// Configuration
		//
		setColumnPositions();
		// Set the ordering options
		makeOrderingOptions();
		// Column for checkboxes. Plain columns have already been defined in options
		if (isSelectCheckbox()) {
			YadaDTColumns newColumn = options.addNewColumn(0);
			newColumn.dtData(null).dtOrderable(false).dtSearchable(false).dtDefaultContent("");
			// NOTE: can't use a "myFunction()" in the dtRender because that function must be defined on the table row (i.e. on the data source)
			//       and we can't do that.
			// newColumn.dtRender("yada.dtCheckboxRender()").dtWidth("50px").dtClassName("yadaCheckInCell");
			newColumn.dtRender("yada.dtCheckboxRender").dtWidth("50px").dtClassName("yadaCheckInCell");
		}
		// Last column for commands
		if (commandsTitle!=null) {
			options.dtColumnsObj()
			.dtClassName("yadaCommandButtonCell")
			.dtName(YadaDataTableDao.COLUMN_COMMAND)
			.dtOrderable(false).dtSearchable(false)
			.dtWidth("50px")
			.dtRender("yada.dtCommandRender");
		}
		
		backCalled = true;
		return super.back();
	}
	
	private void setColumnPositions() {
		int position = isSelectCheckbox() ? 1 : 0;
		for (YadaDataTableColumnProxy column : columns) {
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

	private void makeOrderingOptions() {
		// Example:
		//	[
		//	    { idx: 1, 'asc' },
		//	    { idx: 2, 'desc' }
		//	]
		if (orderingMap.isEmpty()) {
			return;
		}
		int totColumns = 0;
		for (YadaDataTableColumn column : orderingMap.values()) {
			totColumns++;
			String direction = Boolean.FALSE.equals(column.orderAsc) ? "desc" : "asc";
			options.dtOrder(column.positionInTable, direction);
		}
		options.dtOrderMulti(totColumns>1);
	}

	
}
