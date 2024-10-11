package net.yadaframework.web.datatables.options;

import java.util.ArrayList;
import java.util.List;

import net.yadaframework.core.YadaFluentBase;

/**
 * SearchPanes configuration for DataTables.
 *
 * @see <a href="https://datatables.net/reference/option/searchPanes">SearchPanes</a>
 */
class YadaDTSearchPanes extends YadaFluentBase<YadaDataTableOptions> {
    private Boolean cascadePanes;
    private String clear;
    private Boolean collapse;
    private List<Integer> columns;
    private Boolean controls;
    private String dtOpts;
    private String emptyMessage;
    private Boolean filterChanged;
    private Boolean hideCount;
    private Boolean initCollapsed;
    private String layout;
    private Boolean order;
    private Boolean orderable;
    private List<YadaDTSearchPane> panes;
    private Integer preSelect;
    private Integer threshold;
    private Integer viewCount;
    private Integer viewTotal;

    public YadaDTSearchPanes(YadaDataTableOptions parent) {
        super(parent);
    }

    /**
     * Set whether cascading panes are enabled.
     * 
     * @param cascadePanes if true, cascade panes are enabled
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/searchPanes.cascadePanes">cascadePanes</a>
     */
    public YadaDTSearchPanes cascadePanes(Boolean cascadePanes) {
        this.cascadePanes = cascadePanes;
        return this;
    }

    /**
     * Get the cascade panes option.
     * 
     * @return true if cascade panes are enabled
     */
    public Boolean getCascadePanes() {
        return cascadePanes;
    }

    /**
     * Set the clear button text.
     * 
     * @param clear the clear button text
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/searchPanes.clear">clear</a>
     */
    public YadaDTSearchPanes clear(String clear) {
        this.clear = clear;
        return this;
    }

    /**
     * Get the clear button text.
     * 
     * @return the clear button text
     */
    public String getClear() {
        return clear;
    }

    /**
     * Set whether panes should collapse.
     * 
     * @param collapse if true, panes will collapse
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/searchPanes.collapse">collapse</a>
     */
    public YadaDTSearchPanes collapse(Boolean collapse) {
        this.collapse = collapse;
        return this;
    }

    /**
     * Get the collapse option.
     * 
     * @return true if panes will collapse
     */
    public Boolean getCollapse() {
        return collapse;
    }

    /**
     * Set the columns for which the search panes will be shown.
     * 
     * @param columns a list of column indexes
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/searchPanes.columns">columns</a>
     */
    public YadaDTSearchPanes columns(List<Integer> columns) {
        this.columns = columns;
        return this;
    }

    /**
     * Add a single column to the columns list.
     * 
     * @param column a column index
     * @return this instance for method chaining
     */
    public YadaDTSearchPanes columns(Integer column) {
        if (this.columns == null) {
            this.columns = new ArrayList<>();
        }
        this.columns.add(column);
        return this;
    }

    /**
     * Get the columns list.
     * 
     * @return the list of column indexes
     */
    public List<Integer> getColumns() {
        return columns;
    }

    /**
     * Set whether the search controls should be shown.
     * 
     * @param controls if true, search controls are enabled
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/searchPanes.controls">controls</a>
     */
    public YadaDTSearchPanes controls(Boolean controls) {
        this.controls = controls;
        return this;
    }

    /**
     * Get the search controls option.
     * 
     * @return true if search controls are enabled
     */
    public Boolean getControls() {
        return controls;
    }

    /**
     * Set additional DataTables options for search panes.
     * 
     * @param dtOpts additional DataTables options
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/searchPanes.dtOpts">dtOpts</a>
     */
    public YadaDTSearchPanes dtOpts(String dtOpts) {
        this.dtOpts = dtOpts;
        return this;
    }

    /**
     * Get the additional DataTables options for search panes.
     * 
     * @return the additional DataTables options
     */
    public String getDtOpts() {
        return dtOpts;
    }

    /**
     * Set the empty message for panes with no data.
     * 
     * @param emptyMessage the empty message text
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/searchPanes.emptyMessage">emptyMessage</a>
     */
    public YadaDTSearchPanes emptyMessage(String emptyMessage) {
        this.emptyMessage = emptyMessage;
        return this;
    }

    /**
     * Get the empty message text.
     * 
     * @return the empty message text
     */
    public String getEmptyMessage() {
        return emptyMessage;
    }

    /**
     * Set whether the filter has been changed.
     * 
     * @param filterChanged if true, indicates the filter was changed
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/searchPanes.filterChanged">filterChanged</a>
     */
    public YadaDTSearchPanes filterChanged(Boolean filterChanged) {
        this.filterChanged = filterChanged;
        return this;
    }

    /**
     * Get the filterChanged option.
     * 
     * @return true if the filter has been changed
     */
    public Boolean getFilterChanged() {
        return filterChanged;
    }

    /**
     * Set whether the count should be hidden.
     * 
     * @param hideCount if true, count is hidden
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/searchPanes.hideCount">hideCount</a>
     */
    public YadaDTSearchPanes hideCount(Boolean hideCount) {
        this.hideCount = hideCount;
        return this;
    }

    /**
     * Get the hideCount option.
     * 
     * @return true if count is hidden
     */
    public Boolean getHideCount() {
        return hideCount;
    }

    /**
     * Set whether panes should be initially collapsed.
     * 
     * @param initCollapsed if true, panes will be initially collapsed
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/searchPanes.initCollapsed">initCollapsed</a>
     */
    public YadaDTSearchPanes initCollapsed(Boolean initCollapsed) {
        this.initCollapsed = initCollapsed;
        return this;
    }

    /**
     * Get the initCollapsed option.
     * 
     * @return true if panes are initially collapsed
     */
    public Boolean getInitCollapsed() {
        return initCollapsed;
    }

    /**
     * Set the layout for the search panes.
     * 
     * @param layout the layout string
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/searchPanes.layout">layout</a>
     */
    public YadaDTSearchPanes layout(String layout) {
        this.layout = layout;
        return this;
    }

    /**
     * Get the layout for the search panes.
     * 
     * @return the layout string
     */
    public String getLayout() {
        return layout;
    }

    /**
     * Set the order for the search panes.
     * 
     * @param order if true, panes will be ordered
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/searchPanes.order">order</a>
     */
    public YadaDTSearchPanes order(Boolean order) {
        this.order = order;
        return this;
    }

    /**
     * Get the order option.
     * 
     * @return true if panes are ordered
     */
    public Boolean getOrder() {
        return order;
    }

    /**
     * Set whether panes are orderable.
     * 
     * @param orderable if true, panes are orderable
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/searchPanes.orderable">orderable</a>
     */
    public YadaDTSearchPanes orderable(Boolean orderable) {
        this.orderable = orderable;
        return this;
    }

    /**
     * Get the orderable option.
     * 
     * @return true if panes are orderable
     */
    public Boolean getOrderable() {
        return orderable;
    }

    /**
     * Set the list of panes for the search panes.
     * 
     * @param panes a list of YadaDTSearchPane instances
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/searchPanes.panes">panes</a>
     */
    public YadaDTSearchPanes panes(List<YadaDTSearchPane> panes) {
        this.panes = panes;
        return this;
    }

    /**
     * Add a new pane to the list of panes.
     * 
     * @return a new YadaDTSearchPane instance
     */
    public YadaDTSearchPane panes() {
        if (this.panes == null) {
            this.panes = new ArrayList<>();
        }
        YadaDTSearchPane newPane = new YadaDTSearchPane(this);
        this.panes.add(newPane);
        return newPane;
    }

    /**
     * Get the list of search panes.
     * 
     * @return the list of YadaDTSearchPane instances
     */
    public List<YadaDTSearchPane> getPanes() {
        return panes;
    }

    /**
     * Set the number of pre-selected panes.
     * 
     * @param preSelect the number of pre-selected panes
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/searchPanes.preSelect">preSelect</a>
     */
    public YadaDTSearchPanes preSelect(Integer preSelect) {
        this.preSelect = preSelect;
        return this;
    }

    /**
     * Get the number of pre-selected panes.
     * 
     * @return the number of pre-selected panes
     */
    public Integer getPreSelect() {
        return preSelect;
    }

    /**
     * Set the threshold for showing the search panes.
     * 
     * @param threshold the threshold value
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/searchPanes.threshold">threshold</a>
     */
    public YadaDTSearchPanes threshold(Integer threshold) {
        this.threshold = threshold;
        return this;
    }

    /**
     * Get the threshold value for showing the search panes.
     * 
     * @return the threshold value
     */
    public Integer getThreshold() {
        return threshold;
    }

    /**
     * Set the view count for panes.
     * 
     * @param viewCount the view count
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/searchPanes.viewCount">viewCount</a>
     */
    public YadaDTSearchPanes viewCount(Integer viewCount) {
        this.viewCount = viewCount;
        return this;
    }

    /**
     * Get the view count for panes.
     * 
     * @return the view count
     */
    public Integer getViewCount() {
        return viewCount;
    }

    /**
     * Set the view total for panes.
     * 
     * @param viewTotal the view total
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/searchPanes.viewTotal">viewTotal</a>
     */
    public YadaDTSearchPanes viewTotal(Integer viewTotal) {
        this.viewTotal = viewTotal;
        return this;
    }

    /**
     * Get the view total for panes.
     * 
     * @return the view total
     */
    public Integer getViewTotal() {
        return viewTotal;
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
