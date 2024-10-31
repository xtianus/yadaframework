package net.yadaframework.web.datatables.options;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import net.yadaframework.core.YadaFluentBase;
import net.yadaframework.exceptions.YadaInvalidUsageException;
import net.yadaframework.raw.YadaRawStringSerializer;

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
    protected String createdCell;
    // data can be "null" in the json (annotation on the getter because it should not be null in a YadaDTColumnDef)
    protected String data;
    protected String defaultContent;
    protected String editField;
    protected String footer;
    protected String name;
    protected Boolean orderable;
    @JsonInclude(JsonInclude.Include.NON_EMPTY) 
    protected List<Integer> orderData = new ArrayList<Integer>();
    protected String orderDataType;
    protected List<String> orderSequence;
    @JsonSerialize(using = YadaRawStringSerializer.class)
    protected String render; // Must be a js function
    protected Boolean searchable;
    protected String title;
    protected String type;
    protected Boolean visible;
    protected String width;
    protected Integer responsivePriority;
    protected YadaDTOptionSearchBuilder searchBuilder;
    protected String searchBuilderTitle;
    protected String searchBuilderType;
    protected String searchPanes;

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
     * @param data The data source for the column.
     * @return This instance for method chaining.
     * @see <a href="https://datatables.net/reference/option/columns.data">DataTables Reference: columns.data</a>
     */
    public YadaDTColumns dtData(String data) {
        this.data = data;
        return this;
    }

    /**
     * @param defaultContent The default content for the column if the data source is null.
     * @return This instance for method chaining.
     * @see <a href="https://datatables.net/reference/option/columns.defaultContent">DataTables Reference: columns.defaultContent</a>
     */
    public YadaDTColumns dtDefaultContent(String defaultContent) {
        this.defaultContent = defaultContent;
        return this;
    }

    /**
     * @param editField The field to be edited in Editor for this column.
     * @return This instance for method chaining.
     * @see <a href="https://datatables.net/reference/option/columns.editField">DataTables Reference: columns.editField</a>
     */
    public YadaDTColumns dtEditField(String editField) {
        this.editField = editField;
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
     * @param orderable Whether the column is orderable.
     * @return This instance for method chaining.
     * @see <a href="https://datatables.net/reference/option/columns.orderable">DataTables Reference: columns.orderable</a>
     */
    public YadaDTColumns dtOrderable(boolean orderable) {
        this.orderable = orderable;
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
     * @param responsivePriority The responsive priority for the column.
     * @return This instance for method chaining.
     * @see <a href="https://datatables.net/reference/option/columns.responsivePriority">DataTables Reference: columns.responsivePriority</a>
     */
    public YadaDTColumns dtResponsivePriority(int responsivePriority) {
        this.responsivePriority = responsivePriority;
        return this;
    }

    /**
     * @return The search builder instance for configuring search options for the column.
     * @see <a href="https://datatables.net/reference/option/columns.searchBuilder">DataTables Reference: columns.searchBuilder</a>
     */
    public YadaDTOptionSearchBuilder dtSearchBuilder() {
        if (this.searchBuilder == null) {
            this.searchBuilder = new YadaDTOptionSearchBuilder(this);
        }
        return this.searchBuilder;
    }

    /**
     * @param searchPanes The search panes configuration for the column.
     * @return This instance for method chaining.
     * @see <a href="https://datatables.net/reference/option/columns.searchPanes">DataTables Reference: columns.searchPanes</a>
     */
    public YadaDTColumns dtSearchPanes(String searchPanes) {
        this.searchPanes = searchPanes;
        return this;
    }

    /**
     * @param searchable Whether the column is searchable.
     * @return This instance for method chaining.
     * @see <a href="https://datatables.net/reference/option/columns.searchable">DataTables Reference: columns.searchable</a>
     */
    public YadaDTColumns dtSearchable(boolean searchable) {
        this.searchable = searchable;
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
     * @param type The type of the column's data.
     * @return This instance for method chaining.
     * @see <a href="https://datatables.net/reference/option/columns.type">DataTables Reference: columns.type</a>
     */
    public YadaDTColumns dtType(String type) {
        this.type = type;
        return this;
    }

    /**
     * @param visible Whether the column is visible.
     * @return This instance for method chaining.
     * @see <a href="https://datatables.net/reference/option/columns.visible">DataTables Reference: columns.visible</a>
     */
    public YadaDTColumns dtVisible(boolean visible) {
        this.visible = visible;
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
    
    /**
     * @param searchBuilderTitle The search builder title for the column.
     * @return This instance for method chaining.
     * @see <a href="https://datatables.net/reference/option/columns.searchBuilderTitle">DataTables Reference: columns.searchBuilderTitle</a>
     */
    public YadaDTColumns dtSearchBuilderTitle(String searchBuilderTitle) {
    	this.searchBuilderTitle = searchBuilderTitle;
    	return this;
    }
    
    /**
     * @param searchBuilderType The search builder type for the column.
     * @return This instance for method chaining.
     * @see <a href="https://datatables.net/reference/option/columns.searchBuilderType">DataTables Reference: columns.searchBuilderType</a>
     */
    public YadaDTColumns dtSearchBuilderType(String searchBuilderType) {
    	this.searchBuilderType = searchBuilderType;
    	return this;
    }

}
