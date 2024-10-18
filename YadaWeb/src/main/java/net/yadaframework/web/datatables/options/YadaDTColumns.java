package net.yadaframework.web.datatables.options;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import net.yadaframework.core.YadaFluentBase;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class YadaDTColumns extends YadaFluentBase<YadaDataTableOptions> {
    private String ariaTitle;
    private String cellType;
    private String className;
    private String contentPadding;
    private String createdCell;
    private String data;
    private String defaultContent;
    private String editField;
    private String footer;
    private String name;
    private boolean orderable;
    private List<Integer> orderData = new ArrayList<Integer>();
    private String orderDataType;
    private List<String> orderSequence;
    private String render;
    private boolean searchable;
    private String title;
    private String type;
    private boolean visible;
    private String width;
    private Integer responsivePriority;
    private YadaDTOptionSearchBuilder searchBuilder;
    private String searchBuilderTitle;
    private String searchBuilderType;
    private String searchPanes;

    public YadaDTColumns(YadaDataTableOptions parent) {
        super(parent);
    }

    // Getters
    /**
     * @return The ARIA label for the column for accessibility purposes.
     * @see <a href="https://datatables.net/reference/option/columns.ariaTitle">DataTables Reference: columns.ariaTitle</a>
     */
    public String getAriaTitle() {
        return ariaTitle;
    }

    /**
     * @return The cell type for the column (e.g., "td" or "th").
     * @see <a href="https://datatables.net/reference/option/columns.cellType">DataTables Reference: columns.cellType</a>
     */
    public String getCellType() {
        return cellType;
    }

    /**
     * @return The class name to be added to all cells in the column.
     * @see <a href="https://datatables.net/reference/option/columns.className">DataTables Reference: columns.className</a>
     */
    public String getClassName() {
        return className;
    }

    /**
     * @return The padding to be applied to the content of the column.
     * @see <a href="https://datatables.net/reference/option/columns.contentPadding">DataTables Reference: columns.contentPadding</a>
     */
    public String getContentPadding() {
        return contentPadding;
    }

    /**
     * @return The function to be executed when a cell is created.
     * @see <a href="https://datatables.net/reference/option/columns.createdCell">DataTables Reference: columns.createdCell</a>
     */
    public String getCreatedCell() {
        return createdCell;
    }

    /**
     * @return The data source for the column.
     * @see <a href="https://datatables.net/reference/option/columns.data">DataTables Reference: columns.data</a>
     */
    public String getData() {
        return data;
    }

    /**
     * @return The default content for the column if the data source is null.
     * @see <a href="https://datatables.net/reference/option/columns.defaultContent">DataTables Reference: columns.defaultContent</a>
     */
    public String getDefaultContent() {
        return defaultContent;
    }

    /**
     * @return The field to be edited in Editor for this column.
     * @see <a href="https://datatables.net/reference/option/columns.editField">DataTables Reference: columns.editField</a>
     */
    public String getEditField() {
        return editField;
    }

    /**
     * @return The content for the footer of the column.
     * @see <a href="https://datatables.net/reference/option/columns.footer">DataTables Reference: columns.footer</a>
     */
    public String getFooter() {
        return footer;
    }

    /**
     * @return The name for the column.
     * @see <a href="https://datatables.net/reference/option/columns.name">DataTables Reference: columns.name</a>
     */
    public String getName() {
        return name;
    }

    /**
     * @return The columns to order by when this column is sorted.
     * @see <a href="https://datatables.net/reference/option/columns.orderData">DataTables Reference: columns.orderData</a>
     */
    public List<Integer> getOrderData() {
        return orderData;
    }

    /**
     * @return The ordering data type for the column.
     * @see <a href="https://datatables.net/reference/option/columns.orderDataType">DataTables Reference: columns.orderDataType</a>
     */
    public String getOrderDataType() {
        return orderDataType;
    }

    /**
     * @return The ordering sequence for the column.
     * @see <a href="https://datatables.net/reference/option/columns.orderSequence">DataTables Reference: columns.orderSequence</a>
     */
    public List<String> getOrderSequence() {
        return orderSequence;
    }

    /**
     * @return Whether the column is orderable.
     * @see <a href="https://datatables.net/reference/option/columns.orderable">DataTables Reference: columns.orderable</a>
     */
    public boolean isOrderable() {
        return orderable;
    }

    /**
     * @return The rendering function for the column's data.
     * @see <a href="https://datatables.net/reference/option/columns.render">DataTables Reference: columns.render</a>
     */
    public String getRender() {
        return render;
    }

    /**
     * @return The responsive priority for the column.
     * @see <a href="https://datatables.net/reference/option/columns.responsivePriority">DataTables Reference: columns.responsivePriority</a>
     */
    public Integer getResponsivePriority() {
        return responsivePriority;
    }

    /**
     * @return The search builder options for the column.
     * @see <a href="https://datatables.net/reference/option/columns.searchBuilder">DataTables Reference: columns.searchBuilder</a>
     */
    public YadaDTOptionSearchBuilder getSearchBuilder() {
        return searchBuilder;
    }

    /**
     * @return The search panes configuration for the column.
     * @see <a href="https://datatables.net/reference/option/columns.searchPanes">DataTables Reference: columns.searchPanes</a>
     */
    public String getSearchPanes() {
        return searchPanes;
    }

    /**
     * @return Whether the column is searchable.
     * @see <a href="https://datatables.net/reference/option/columns.searchable">DataTables Reference: columns.searchable</a>
     */
    public boolean isSearchable() {
        return searchable;
    }

    /**
     * @return The title for the column.
     * @see <a href="https://datatables.net/reference/option/columns.title">DataTables Reference: columns.title</a>
     */
    public String getTitle() {
        return title;
    }

    /**
     * @return The type of the column's data.
     * @see <a href="https://datatables.net/reference/option/columns.type">DataTables Reference: columns.type</a>
     */
    public String getType() {
        return type;
    }

    /**
     * @return Whether the column is visible.
     * @see <a href="https://datatables.net/reference/option/columns.visible">DataTables Reference: columns.visible</a>
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * @return The width of the column.
     * @see <a href="https://datatables.net/reference/option/columns.width">DataTables Reference: columns.width</a>
     */
    public String getWidth() {
        return width;
    }

    // Fluent Setters
    /**
     * @param ariaTitle The ARIA label for the column for accessibility purposes.
     * @return This instance for method chaining.
     * @see <a href="https://datatables.net/reference/option/columns.ariaTitle">DataTables Reference: columns.ariaTitle</a>
     */
    public YadaDTColumns ariaTitle(String ariaTitle) {
        this.ariaTitle = ariaTitle;
        return this;
    }

    /**
     * @param cellType The cell type for the column (e.g., "td" or "th").
     * @return This instance for method chaining.
     * @see <a href="https://datatables.net/reference/option/columns.cellType">DataTables Reference: columns.cellType</a>
     */
    public YadaDTColumns cellType(String cellType) {
        this.cellType = cellType;
        return this;
    }

    /**
     * @param className The class name to be added to all cells in the column.
     * @return This instance for method chaining.
     * @see <a href="https://datatables.net/reference/option/columns.className">DataTables Reference: columns.className</a>
     */
    public YadaDTColumns className(String className) {
        this.className = className;
        return this;
    }

    /**
     * @param contentPadding The padding to be applied to the content of the column.
     * @return This instance for method chaining.
     * @see <a href="https://datatables.net/reference/option/columns.contentPadding">DataTables Reference: columns.contentPadding</a>
     */
    public YadaDTColumns contentPadding(String contentPadding) {
        this.contentPadding = contentPadding;
        return this;
    }

    /**
     * @param createdCell The function to be executed when a cell is created.
     * @return This instance for method chaining.
     * @see <a href="https://datatables.net/reference/option/columns.createdCell">DataTables Reference: columns.createdCell</a>
     */
    public YadaDTColumns createdCell(String createdCell) {
        this.createdCell = createdCell;
        return this;
    }

    /**
     * @param data The data source for the column.
     * @return This instance for method chaining.
     * @see <a href="https://datatables.net/reference/option/columns.data">DataTables Reference: columns.data</a>
     */
    public YadaDTColumns data(String data) {
        this.data = data;
        return this;
    }

    /**
     * @param defaultContent The default content for the column if the data source is null.
     * @return This instance for method chaining.
     * @see <a href="https://datatables.net/reference/option/columns.defaultContent">DataTables Reference: columns.defaultContent</a>
     */
    public YadaDTColumns defaultContent(String defaultContent) {
        this.defaultContent = defaultContent;
        return this;
    }

    /**
     * @param editField The field to be edited in Editor for this column.
     * @return This instance for method chaining.
     * @see <a href="https://datatables.net/reference/option/columns.editField">DataTables Reference: columns.editField</a>
     */
    public YadaDTColumns editField(String editField) {
        this.editField = editField;
        return this;
    }

    /**
     * @param footer The content for the footer of the column.
     * @return This instance for method chaining.
     * @see <a href="https://datatables.net/reference/option/columns.footer">DataTables Reference: columns.footer</a>
     */
    public YadaDTColumns footer(String footer) {
        this.footer = footer;
        return this;
    }

    /**
     * @param name The name for the column.
     * @return This instance for method chaining.
     * @see <a href="https://datatables.net/reference/option/columns.name">DataTables Reference: columns.name</a>
     */
    public YadaDTColumns name(String name) {
        this.name = name;
        return this;
    }

    /**
     * @param orderData The columns to order by when this column is sorted.
     * @return This instance for method chaining.
     * @see <a href="https://datatables.net/reference/option/columns.orderData">DataTables Reference: columns.orderData</a>
     */
    public YadaDTColumns orderData(Integer orderData) {
        this.orderData.add(orderData);
        return this;
    }

    /**
     * @param orderDataType The ordering data type for the column.
     * @return This instance for method chaining.
     * @see <a href="https://datatables.net/reference/option/columns.orderDataType">DataTables Reference: columns.orderDataType</a>
     */
    public YadaDTColumns orderDataType(String orderDataType) {
        this.orderDataType = orderDataType;
        return this;
    }

    /**
     * @param orderSequence The ordering sequence for the column.
     * @return This instance for method chaining.
     * @see <a href="https://datatables.net/reference/option/columns.orderSequence">DataTables Reference: columns.orderSequence</a>
     */
    public YadaDTColumns orderSequence(String orderSequence) {
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
    public YadaDTColumns orderable(boolean orderable) {
        this.orderable = orderable;
        return this;
    }

    /**
     * @param render The rendering function for the column's data.
     * @return This instance for method chaining.
     * @see <a href="https://datatables.net/reference/option/columns.render">DataTables Reference: columns.render</a>
     */
    public YadaDTColumns render(String render) {
        this.render = render;
        return this;
    }

    /**
     * @param responsivePriority The responsive priority for the column.
     * @return This instance for method chaining.
     * @see <a href="https://datatables.net/reference/option/columns.responsivePriority">DataTables Reference: columns.responsivePriority</a>
     */
    public YadaDTColumns responsivePriority(int responsivePriority) {
        this.responsivePriority = responsivePriority;
        return this;
    }

    /**
     * @return The search builder instance for configuring search options for the column.
     * @see <a href="https://datatables.net/reference/option/columns.searchBuilder">DataTables Reference: columns.searchBuilder</a>
     */
    public YadaDTOptionSearchBuilder searchBuilder() {
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
    public YadaDTColumns searchPanes(String searchPanes) {
        this.searchPanes = searchPanes;
        return this;
    }

    /**
     * @param searchable Whether the column is searchable.
     * @return This instance for method chaining.
     * @see <a href="https://datatables.net/reference/option/columns.searchable">DataTables Reference: columns.searchable</a>
     */
    public YadaDTColumns searchable(boolean searchable) {
        this.searchable = searchable;
        return this;
    }

    /**
     * @param title The title for the column.
     * @return This instance for method chaining.
     * @see <a href="https://datatables.net/reference/option/columns.title">DataTables Reference: columns.title</a>
     */
    public YadaDTColumns title(String title) {
        this.title = title;
        return this;
    }

    /**
     * @param type The type of the column's data.
     * @return This instance for method chaining.
     * @see <a href="https://datatables.net/reference/option/columns.type">DataTables Reference: columns.type</a>
     */
    public YadaDTColumns type(String type) {
        this.type = type;
        return this;
    }

    /**
     * @param visible Whether the column is visible.
     * @return This instance for method chaining.
     * @see <a href="https://datatables.net/reference/option/columns.visible">DataTables Reference: columns.visible</a>
     */
    public YadaDTColumns visible(boolean visible) {
        this.visible = visible;
        return this;
    }

    /**
     * @param width The width of the column.
     * @return This instance for method chaining.
     * @see <a href="https://datatables.net/reference/option/columns.width">DataTables Reference: columns.width</a>
     */
    public YadaDTColumns width(String width) {
        this.width = width;
        return this;
    }
    
    /**
     * @param searchBuilderTitle The search builder title for the column.
     * @return This instance for method chaining.
     * @see <a href="https://datatables.net/reference/option/columns.searchBuilderTitle">DataTables Reference: columns.searchBuilderTitle</a>
     */
    public YadaDTColumns searchBuilderTitle(String searchBuilderTitle) {
    	this.searchBuilderTitle = searchBuilderTitle;
    	return this;
    }
    
    /**
     * @param searchBuilderType The search builder type for the column.
     * @return This instance for method chaining.
     * @see <a href="https://datatables.net/reference/option/columns.searchBuilderType">DataTables Reference: columns.searchBuilderType</a>
     */
    public YadaDTColumns searchBuilderType(String searchBuilderType) {
    	this.searchBuilderType = searchBuilderType;
    	return this;
    }

    /**
     * @return The search builder title for the column.
     * @see <a href="https://datatables.net/reference/option/columns.searchBuilderTitle">DataTables Reference: columns.searchBuilderTitle</a>
     */
    public String getSearchBuilderTitle() {
        return searchBuilderTitle;
    }

    /**
     * @return The search builder type for the column.
     * @see <a href="https://datatables.net/reference/option/columns.searchBuilderType">DataTables Reference: columns.searchBuilderType</a>
     */
    public String getSearchBuilderType() {
        return searchBuilderType;
    }
}
