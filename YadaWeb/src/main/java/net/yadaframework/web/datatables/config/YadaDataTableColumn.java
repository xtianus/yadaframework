package net.yadaframework.web.datatables.config;

import org.apache.commons.lang3.StringUtils;

import net.yadaframework.core.YadaFluentBase;
import net.yadaframework.exceptions.YadaInvalidUsageException;
import net.yadaframework.web.datatables.options.YadaDTColumns;
import net.yadaframework.web.datatables.proxy.YadaDTColumnsProxy;
import net.yadaframework.web.datatables.proxy.YadaDataTableHTMLProxy;

/**
 * Configuration for a column of a DataTables table.
 * @see YadaDTColumns for DataTables column options
 */
public class YadaDataTableColumn extends YadaFluentBase<YadaDataTableHTML> {
	private static final String DEFAULT_CONTENT = "---"; // Default column content when no data is received from BE, it prevents a popup error by DataTables
	protected String headerText;
	protected YadaDTColumnsProxy yadaDTColumns;
	
	protected int positionInTable; // Internal use
	protected Boolean orderAsc; // false for desc

	/**
	 * Make a new column configuration
	 * @param headerText the column title
	 * @param data the json path of the result in the ajax response. See {@link YadaDataTableHTML#dtColumnObj}
	 * @param parent
	 */
	protected YadaDataTableColumn(String headerText, String data, YadaDataTableHTML parent) {
		super(parent);
		this.headerText = StringUtils.trimToEmpty(headerText);
// I wanted to expose the DataTables Options but it's too much effort to implement (needs a wrapper that overrides the parent)		
//		// Create a new columns option with a normal YadaDTOptions parent
//		YadaDTColumnsProxy yadaDTColumnsProxy = (YadaDTColumnsProxy) parent.options.dtColumnsObj();
//		// Set it inside a wrapper with an overridden parent
//		// so that the parent returns YadaDataTableHTML instead of YadaDTOptions when used in YadaDataTableColumn
//		yadaDTColumns = new YadaDTColumnsWrapper(yadaDTColumnsProxy, (YadaDataTableHTMLProxy)parent);
		yadaDTColumns = (YadaDTColumnsProxy) parent.options.dtColumnsObj();
		yadaDTColumns.dtData(data);
		
	}
	
// I wanted to expose the DataTables Options but it's too much effort to implement: needs a wrapper that overrides the parent
// otherwise the parent is YadaDTOptions and not YadaDataTableHTML. Didn't find a way to fix this.
//	public YadaDTColumnsWrapper dt() {
//		return yadaDTColumns;
//	}
	
	/**
	 * css classes to set on the cell.
	 * @param cssClasses space-separated css classes
	 * @return this instance for method chaining
	 * @see <a href="https://datatables.net/reference/option/columns.className">DataTables Reference: columns.className</
	 */
	public YadaDataTableColumn dtCssClasses(String cssClasses) {
		yadaDTColumns.dtClassName(cssClasses);
		return this;
	}

	/**
	 * Makes the column not orderable
	 * @return this instance for method chaining
	 */
	public YadaDataTableColumn dtOrderableOff() {
		yadaDTColumns.dtOrderableOff();
		return this;
	}
	
	/**
	 * Makes the column not searchable
	 * @return this instance for method chaining
	 */
	public YadaDataTableColumn dtSearchableOff() {
		yadaDTColumns.dtSearchableOff();
		return this;
	}
	
	/**
	 * Set a name on this column for database operations and cross reference.
	 * Be careful that it matches the Entity attribute name when "data" is not a function. 
	 * Examples: "id", "useCredentials.username"
	 * When unset, the "data" option value is used.
	 * @param name the name of the column, can be null, must be unique otherwise
	 * @return this instance for method chaining
	 * @see YadaDataTableConfirmDialog#dtPlaceholderColumnName(String...)
	 */
	public YadaDataTableColumn dtName(String name) {
		yadaDTColumns.dtName(name);
		return this;
	}

	/**
	 * Choose this column for the initial sorting, ascending
	 * @return this instance for method chaining
	 * @see YadaDataTableColumn#dtOrderAsc(int) for multiple column sorting
	 */
	public YadaDataTableColumn dtOrderAsc() {
		// dtOrderData is set by YadaDataTableHTML
		orderAsc = true;
		addOrder(0); 
		return this;
	}
	
	/**
	 * Choose this column for the initial sorting, ascending
	 * @param precedence for multiple column sorting, a lower number means higher precedence
	 * @return this instance for method chaining
	 */
	public YadaDataTableColumn dtOrderAsc(int precedence) {
		orderAsc = true;
		addOrder(precedence); 
		return this;
	}
	
	/**
	 * Choose this column for the initial sorting, descending
	 * @return this instance for method chaining
	 * @see YadaDataTableColumn#dtOrderDesc(int) for multiple column sorting
	 */
	public YadaDataTableColumn dtOrderDesc() {
		orderAsc = false;
		addOrder(0); 
		return this;
	}
	
	/**
	 * Choose this column for the initial sorting, descending
	 * @param precedence for multiple column sorting, a lower number means higher precedence
	 * @return this instance for method chaining
	 */
	public YadaDataTableColumn dtOrderDesc(int precedence) {
		orderAsc = false;
		addOrder(precedence); 
		return this;
	}
	
	/**
     * In a responsive table control the order in which columns are hidden.
     * Responsive will automatically remove columns from the right-hand-side 
     * of the table when a table is too wide for a given display, unless this value is set.
     * @param responsivePriority The priority is an integer value where lower numbers
     * 			are given a higher priority (i.e. a column with priority 2 will be 
     * 			hidden before a column with priority 1). The default is 10000.  
     * @return This instance for method chaining.
     * @see <a href="https://datatables.net/reference/option/columns.responsivePriority">DataTables Reference: columns.responsivePriority</a>
	 */
	public YadaDataTableColumn dtResponsivePriority(int responsivePriority) {
		yadaDTColumns.dtResponsivePriority(responsivePriority);
		return this;
	}
	
	private void addOrder(int precedence) {
		if (parent.orderingMap.containsKey(precedence)) {
			throw new YadaInvalidUsageException("Duplicate order precedence '{}' in column '{}'", precedence, headerText);
		}
		parent.orderingMap.put(precedence, this); 
	}
	
	protected String getName() {
		return yadaDTColumns.getName();
	}
	
	private Object getData() {
		return yadaDTColumns.getData();
	}

	@Override
	public YadaDataTableHTML back() {
		// Check that name has not been repeated
		if (getName()!=null) {
			boolean duplicateName = parent.columns.stream().anyMatch(column -> column!=this && this.getName().equals(column.getName()));
			if (duplicateName) {
				throw new YadaInvalidUsageException("Duplicate column name: '{}'", getName());
			}
		}
		// When name is not set, use data
		if (getName()==null) {
			Object data = getData();
			if (data instanceof String dataString) {
				this.dtName(dataString);
			}
		}
		// Set the default content
		if (this.yadaDTColumns.getDefaultContent()==null) {
			this.yadaDTColumns.dtDefaultContent(DEFAULT_CONTENT);
		}
		return super.back();
	}
	
}
