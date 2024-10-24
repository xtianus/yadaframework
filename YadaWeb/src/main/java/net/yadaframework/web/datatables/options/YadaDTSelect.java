package net.yadaframework.web.datatables.options;

import net.yadaframework.core.YadaFluentBase;

/**
 * Configuration options for selecting rows in DataTables.
 * 
 * @see <a href="https://datatables.net/reference/option/select">Select</a>
 */
class YadaDTSelect extends YadaFluentBase<YadaDataTableOptions> {
    private Boolean blurable;
    private String className;
    private Boolean headerCheckbox;
    private String info;
    private String items;
    private Boolean selectable;
    private String selector;
    private String style;
    private Boolean toggleable;

    YadaDTSelect(YadaDataTableOptions parent) {
        super(parent);
    }

    /**
     * Set whether the selection is blurable.
     * 
     * @param blurable if true, blurable selection is enabled
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/select.blurable">blurable</a>
     */
    public YadaDTSelect dtBlurable(Boolean blurable) {
        this.blurable = blurable;
        return this;
    }

    /**
     * Get the blurable selection option.
     * 
     * @return true if blurable selection is enabled
     */
    public Boolean getBlurable() {
        return blurable;
    }

    /**
     * Set the class name for the selection.
     * 
     * @param className the CSS class name
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/select.className">className</a>
     */
    public YadaDTSelect dtClassName(String className) {
        this.className = className;
        return this;
    }

    /**
     * Get the class name for the selection.
     * 
     * @return the CSS class name
     */
    public String getClassName() {
        return className;
    }

    /**
     * Set whether a header checkbox is displayed.
     * 
     * @param headerCheckbox if true, a header checkbox is displayed
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/select.headerCheckbox">headerCheckbox</a>
     */
    public YadaDTSelect dtHeaderCheckbox(Boolean headerCheckbox) {
        this.headerCheckbox = headerCheckbox;
        return this;
    }

    /**
     * Get the header checkbox option.
     * 
     * @return true if the header checkbox is displayed
     */
    public Boolean getHeaderCheckbox() {
        return headerCheckbox;
    }

    /**
     * Set the information text for selection.
     * 
     * @param info the information text
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/select.info">info</a>
     */
    public YadaDTSelect dtInfo(String info) {
        this.info = info;
        return this;
    }

    /**
     * Get the information text for selection.
     * 
     * @return the information text
     */
    public String getInfo() {
        return info;
    }

    /**
     * Set the items to select.
     * 
     * @param items the items to be selected
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/select.items">items</a>
     */
    public YadaDTSelect dtItems(String items) {
        this.items = items;
        return this;
    }

    /**
     * Get the items to select.
     * 
     * @return the items to be selected
     */
    public String getItems() {
        return items;
    }

    /**
     * Set whether selection is enabled.
     * 
     * @param selectable if true, selection is enabled
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/select.selectable">selectable</a>
     */
    public YadaDTSelect dtSelectable(Boolean selectable) {
        this.selectable = selectable;
        return this;
    }

    /**
     * Get the selectable option.
     * 
     * @return true if selection is enabled
     */
    public Boolean getSelectable() {
        return selectable;
    }

    /**
     * Set the selector for selecting rows.
     * 
     * @param selector the selector string
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/select.selector">selector</a>
     */
    public YadaDTSelect dtSelector(String selector) {
        this.selector = selector;
        return this;
    }

    /**
     * Get the selector for selecting rows.
     * 
     * @return the selector string
     */
    public String getSelector() {
        return selector;
    }

    /**
     * Set the style of the selection.
     * 
     * @param style the style to apply
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/select.style">style</a>
     */
    public YadaDTSelect dtStyle(String style) {
        this.style = style;
        return this;
    }

    /**
     * Get the style of the selection.
     * 
     * @return the style applied to the selection
     */
    public String getStyle() {
        return style;
    }

    /**
     * Set whether selection is toggleable.
     * 
     * @param toggleable if true, selection is toggleable
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/select.toggleable">toggleable</a>
     */
    public YadaDTSelect dtToggleable(Boolean toggleable) {
        this.toggleable = toggleable;
        return this;
    }

    /**
     * Get the toggleable option.
     * 
     * @return true if selection is toggleable
     */
    public Boolean getToggleable() {
        return toggleable;
    }

    /**
     * Get the parent DataTable options.
     * 
     * @return the parent DataTable options
     */
    public YadaDataTableOptions parent() {
        return super.parent;
    }
}
