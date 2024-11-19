package net.yadaframework.web.datatables.options;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import net.yadaframework.core.YadaFluentBase;
import net.yadaframework.exceptions.YadaInvalidUsageException;
import net.yadaframework.web.YadaJsonRawStringSerializer;

/**
 * Defines a column in DataTables.
 * @see <a href="https://datatables.net/reference/option/columns">DataTables Reference: columns</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class YadaDTColumns extends YadaFluentBase<YadaDTOptions> {
	private static final Pattern SAFE_JS_FUNCTION_NAME = Pattern.compile("^[a-zA-Z0-9_\\-\\.]+$");
	
    protected String ariaTitle;
    protected String cellType;
    protected String className;
    protected String contentPadding;
    @JsonSerialize(using = YadaJsonRawStringSerializer.class)
    protected String createdCell;
    // data can be "null" in the json (annotation on the getter because it should not be null in a YadaDTColumnDef)
    protected Object data;
    @JsonSerialize(using = YadaJsonRawStringSerializer.class)
    protected String dataFunction;
    protected String defaultContent;
    protected String footer;
    protected String name;
    protected Boolean orderable;
    @JsonInclude(JsonInclude.Include.NON_EMPTY) 
    protected List<Integer> orderData = new ArrayList<Integer>();
    protected String orderDataType; // Don't think this is useful
    protected List<String> orderSequence;
    @JsonSerialize(using = YadaJsonRawStringSerializer.class)
    protected String render; // Must be a js function
    protected Boolean searchable;
    protected String title;
    // protected String type; // Client side searching/sorting only, so useless in Yada Framework
    protected Boolean visible;
    protected String width;
    protected Integer responsivePriority;

    public YadaDTColumns(YadaDTOptions parent) {
        super(parent);
    }

//    /**
//     * @return Whether the column is orderable.
//     * @see <a href="https://datatables.net/reference/option/columns.orderable">DataTables Reference: columns.orderable</a>
//     */
//    public boolean dtIsOrderable() {
//        return orderable;
//    }

//    /**
//     * @return Whether the column is searchable.
//     * @see <a href="https://datatables.net/reference/option/columns.searchable">DataTables Reference: columns.searchable</a>
//     */
//    public boolean dtIsSearchable() {
//        return searchable;
//    }

//    /**
//     * @return Whether the column is visible.
//     * @see <a href="https://datatables.net/reference/option/columns.visible">DataTables Reference: columns.visible</a>
//     */
//    public boolean dtIsVisible() {
//        return visible;
//    }

    // Fluent Setters
    /**
     * @param ariaTitle The ARIA label for the column for accessibility purposes.
     * @return This instance for method chaining.
     * @see <a href="https://datatables.net/reference/option/columns.ariaTitle">DataTables Reference: columns.ariaTitle</a>
     */
    public YadaDTColumns dtAriaTitle(String ariaTitle) {
        this.ariaTitle = ariaTitle;
        return this;
    }

    /**
     * @param cellType The cell type for the column (e.g., "td" or "th").
     * @return This instance for method chaining.
     * @see <a href="https://datatables.net/reference/option/columns.cellType">DataTables Reference: columns.cellType</a>
     */
    public YadaDTColumns dtCellType(String cellType) {
        this.cellType = cellType;
        return this;
    }

    /**
     * @param className The class name to be added to all cells in the column.
     * @return This instance for method chaining.
     * @see <a href="https://datatables.net/reference/option/columns.className">DataTables Reference: columns.className</a>
     */
    public YadaDTColumns dtClassName(String className) {
        this.className = className;
        return this;
    }

    /**
     * @param contentPadding The padding to be applied to the content of the column.
     * @return This instance for method chaining.
     * @see <a href="https://datatables.net/reference/option/columns.contentPadding">DataTables Reference: columns.contentPadding</a>
     */
    public YadaDTColumns dtContentPadding(String contentPadding) {
        this.contentPadding = contentPadding;
        return this;
    }

    /**
     * @param createdCell The function to be executed when a cell is created.
     * @return This instance for method chaining.
     * @see <a href="https://datatables.net/reference/option/columns.createdCell">DataTables Reference: columns.createdCell</a>
     */
    public YadaDTColumns dtCreatedCell(String createdCell) {
        this.createdCell = createdCell;
        return this;
    }
    
    /**
     * Use the original data source for the row rather than plucking data directly from it. 
     * @return This instance for method chaining.
     * @see #dtData(String)
     * @see <a href="https://datatables.net/reference/option/columns.data">DataTables Reference: columns.data</a>
     */
    public YadaDTColumns dtDataNull() {
    	if (this.dataFunction!=null) {
    		throw new YadaInvalidUsageException("Cannot set both data and dataFunction");
    	}
        this.data = null;
        return this;
    }
    

    /**
     * This property can be used to read and write data to and from any data source property.
     * @param index Treated as an array index for the data source. 
     * 			This is the default that DataTables uses (incrementally increased for each column).
     * @return This instance for method chaining.
     * @see #dtData(String)
     * @see <a href="https://datatables.net/reference/option/columns.data">DataTables Reference: columns.data</a>
     */
    public YadaDTColumns dtData(Integer index) {
    	if (this.dataFunction!=null) {
    		throw new YadaInvalidUsageException("Cannot set both data and dataFunction");
    	}
        this.data = index;
        return this;
    }
    
    /**
     * This property can be used to read and write data to and from any data source property.
     * Set to null to use the original data source for the row
     * @param path The data source for the column, eventually as an array notation
     * @return This instance for method chaining.
     * @see #dtDataFunction(String)
     * @see <a href="https://datatables.net/reference/option/columns.data">DataTables Reference: columns.data</a>
     */
    public YadaDTColumns dtData(String path) {
    	if (this.dataFunction!=null) {
    		throw new YadaInvalidUsageException("Cannot set both data and dataFunction");
    	}
    	this.data = path;
    	return this;
    }
    
    /**
     * This property can be used to read and write data to and from any data source property.
     * @param function The function given will be executed whenever DataTables needs to set 
     * 	or get the data for a cell in the column.
     * @return This instance for method chaining.
     * @see #dtData(String)
     * @see <a href="https://datatables.net/reference/option/columns.data">DataTables Reference: columns.data</a>
     */
    public YadaDTColumns dtDataFunction(String function) {
    	if (this.data!=null) {
    		// Can't tell if data has been specifically set to null though, which should not be allowed
    		throw new YadaInvalidUsageException("Cannot set both data and dataFunction");
    	}
    	this.dataFunction = function;
    	return this;
    }

    /**
     * Static content in a column or default content when the data source is null.
     * @param htmlContent The default content for the column if the data source is null.
     * @return This instance for method chaining.
     * @see <a href="https://datatables.net/reference/option/columns.defaultContent">DataTables Reference: columns.defaultContent</a>
     */
    public YadaDTColumns dtDefaultContent(String htmlContent) {
        this.defaultContent = htmlContent;
        return this;
    }

    /**
     * @param footer The content for the footer of the column.
     * @return This instance for method chaining.
     * @see <a href="https://datatables.net/reference/option/columns.footer">DataTables Reference: columns.footer</a>
     */
    public YadaDTColumns dtFooter(String footer) {
        this.footer = footer;
        return this;
    }

    /**
     * Set a descriptive name for a column.
     * @param name The name for the column.
     * @return This instance for method chaining.
     * @see <a href="https://datatables.net/reference/option/columns.name">DataTables Reference: columns.name</a>
     */
    public YadaDTColumns dtName(String name) {
        this.name = name;
        return this;
    }

    /**
     * @param orderData The columns to order by when this column is sorted.
     * @return This instance for method chaining.
     * @see <a href="https://datatables.net/reference/option/columns.orderData">DataTables Reference: columns.orderData</a>
     */
    public YadaDTColumns dtOrderData(Integer orderData) {
        this.orderData.add(orderData);
        return this;
    }

    /**
     * @param orderDataType The ordering data type for the column.
     * @return This instance for method chaining.
     * @see <a href="https://datatables.net/reference/option/columns.orderDataType">DataTables Reference: columns.orderDataType</a>
     */
    public YadaDTColumns dtOrderDataType(String orderDataType) {
        this.orderDataType = orderDataType;
        return this;
    }

    /**
     * @param orderSequence The ordering sequence for the column.
     * @return This instance for method chaining.
     * @see <a href="https://datatables.net/reference/option/columns.orderSequence">DataTables Reference: columns.orderSequence</a>
     */
    public YadaDTColumns dtOrderSequence(String orderSequence) {
        if (this.orderSequence == null) {
            this.orderSequence = new ArrayList<>();
        }
        this.orderSequence.add(orderSequence);
        return this;
    }

    /**
     * Disable ordering for the column.
     * @return This instance for method chaining.
     * @see <a href="https://datatables.net/reference/option/columns.orderable">DataTables Reference: columns.orderable</a>
     */
    public YadaDTColumns dtOrderableOff() {
        this.orderable = false;
        return this;
    }

    /**
     * @param renderFunction The rendering function for the column's data, with no parentheses.
     * @return This instance for method chaining.
     * @see <a href="https://datatables.net/reference/option/columns.render">DataTables Reference: columns.render</a>
     */
    public YadaDTColumns dtRender(String renderFunction) {
    	if (!SAFE_JS_FUNCTION_NAME.matcher(renderFunction).matches()) {
    		throw new YadaInvalidUsageException("Invalid javascript function name '{}'", renderFunction);
    	}
        this.render = renderFunction;
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
    public YadaDTColumns dtResponsivePriority(int responsivePriority) {
        this.responsivePriority = responsivePriority;
        return this;
    }

    /**
     * Disables searching for this column.
     * @return This instance for method chaining.
     * @see <a href="https://datatables.net/reference/option/columns.searchable">DataTables Reference: columns.searchable</a>
     */
    public YadaDTColumns dtSearchableOff() {
        this.searchable = false;
        return this;
    }

    /**
     * @param title The title for the column.
     * @return This instance for method chaining.
     * @see <a href="https://datatables.net/reference/option/columns.title">DataTables Reference: columns.title</a>
     */
    public YadaDTColumns dtTitle(String title) {
        this.title = title;
        return this;
    }

    /**
     * Sets a column as not visible.
     * @return This instance for method chaining.
     * @see <a href="https://datatables.net/reference/option/columns.visible">DataTables Reference: columns.visible</a>
     */
    public YadaDTColumns dtVisibleOff() {
        this.visible = false;
        return this;
    }

    /**
     * @param width The width of the column.
     * @return This instance for method chaining.
     * @see <a href="https://datatables.net/reference/option/columns.width">DataTables Reference: columns.width</a>
     */
    public YadaDTColumns dtWidth(String width) {
        this.width = width;
        return this;
    }
}
