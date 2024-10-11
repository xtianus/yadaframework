
package net.yadaframework.web.datatables.options;

import java.util.ArrayList;
import java.util.List;

import net.yadaframework.core.YadaFluentBase;

/**
 * Represents a search builder configuration for DataTables.
 * 
 * @see <a href="https://datatables.net/reference/option/searchBuilder">SearchBuilder</a>
 */
class YadaDTSearchBuilder extends YadaFluentBase<YadaDataTableOptions> {
    private List<Integer> columns;
    private String conditions;
    private Integer depthLimit;
    private Boolean enterSearch;
    private Boolean filterChanged;
    private Boolean greyscale;
    private Boolean liveSearch;
    private String logic;
    private Boolean preDefined;

    public YadaDTSearchBuilder(YadaDataTableOptions parent) {
        super(parent);
    }

    /**
     * Set the list of columns to be used by the search builder.
     * 
     * @param columns list of column indexes
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/searchBuilder.columns">columns</a>
     */
    public YadaDTSearchBuilder columns(List<Integer> columns) {
        this.columns = columns;
        return this;
    }

    /**
     * Add a single column to the search builder columns list.
     * 
     * @param column column index
     * @return this instance for method chaining
     */
    public YadaDTSearchBuilder columns(Integer column) {
        if (this.columns == null) {
            this.columns = new ArrayList<>();
        }
        this.columns.add(column);
        return this;
    }

    /**
     * Get the list of columns.
     * 
     * @return the list of column indexes
     */
    public List<Integer> getColumns() {
        return columns;
    }

    /**
     * Set the conditions for the search.
     * 
     * @param conditions the conditions as a string
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/searchBuilder.conditions">conditions</a>
     */
    public YadaDTSearchBuilder conditions(String conditions) {
        this.conditions = conditions;
        return this;
    }

    /**
     * Get the conditions for the search.
     * 
     * @return the conditions string
     */
    public String getConditions() {
        return conditions;
    }

    /**
     * Set the depth limit for the search builder.
     * 
     * @param depthLimit the depth limit
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/searchBuilder.depthLimit">depthLimit</a>
     */
    public YadaDTSearchBuilder depthLimit(Integer depthLimit) {
        this.depthLimit = depthLimit;
        return this;
    }

    /**
     * Get the depth limit for the search builder.
     * 
     * @return the depth limit value
     */
    public Integer getDepthLimit() {
        return depthLimit;
    }

    /**
     * Set whether the search is triggered on enter key.
     * 
     * @param enterSearch if true, search is triggered on enter
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/searchBuilder.enterSearch">enterSearch</a>
     */
    public YadaDTSearchBuilder enterSearch(Boolean enterSearch) {
        this.enterSearch = enterSearch;
        return this;
    }

    /**
     * Get whether the search is triggered on enter key.
     * 
     * @return true if search is triggered on enter key
     */
    public Boolean getEnterSearch() {
        return enterSearch;
    }

    /**
     * Set whether the filter has changed.
     * 
     * @param filterChanged if true, indicates the filter has changed
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/searchBuilder.filterChanged">filterChanged</a>
     */
    public YadaDTSearchBuilder filterChanged(Boolean filterChanged) {
        this.filterChanged = filterChanged;
        return this;
    }

    /**
     * Get whether the filter has changed.
     * 
     * @return true if the filter has changed
     */
    public Boolean getFilterChanged() {
        return filterChanged;
    }

    /**
     * Set whether the search builder is displayed in greyscale.
     * 
     * @param greyscale if true, the search builder is displayed in greyscale
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/searchBuilder.greyscale">greyscale</a>
     */
    public YadaDTSearchBuilder greyscale(Boolean greyscale) {
        this.greyscale = greyscale;
        return this;
    }

    /**
     * Get whether the search builder is displayed in greyscale.
     * 
     * @return true if greyscale is enabled
     */
    public Boolean getGreyscale() {
        return greyscale;
    }

    /**
     * Set whether live search is enabled.
     * 
     * @param liveSearch if true, live search is enabled
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/searchBuilder.liveSearch">liveSearch</a>
     */
    public YadaDTSearchBuilder liveSearch(Boolean liveSearch) {
        this.liveSearch = liveSearch;
        return this;
    }

    /**
     * Get whether live search is enabled.
     * 
     * @return true if live search is enabled
     */
    public Boolean getLiveSearch() {
        return liveSearch;
    }

    /**
     * Set the logic for combining conditions (AND/OR).
     * 
     * @param logic the logic string (AND/OR)
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/searchBuilder.logic">logic</a>
     */
    public YadaDTSearchBuilder logic(String logic) {
        this.logic = logic;
        return this;
    }

    /**
     * Get the logic for combining conditions.
     * 
     * @return the logic string (AND/OR)
     */
    public String getLogic() {
        return logic;
    }

    /**
     * Set whether pre-defined criteria are used.
     * 
     * @param preDefined if true, pre-defined criteria are used
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/searchBuilder.preDefined">preDefined</a>
     */
    public YadaDTSearchBuilder preDefined(Boolean preDefined) {
        this.preDefined = preDefined;
        return this;
    }

    /**
     * Get whether pre-defined criteria are used.
     * 
     * @return true if pre-defined criteria are used
     */
    public Boolean getPreDefined() {
        return preDefined;
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
