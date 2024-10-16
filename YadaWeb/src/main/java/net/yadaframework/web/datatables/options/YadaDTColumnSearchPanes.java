
package net.yadaframework.web.datatables.options;

import net.yadaframework.core.YadaFluentBase;

/**
 * Represents column search panes configuration for DataTables.
 * 
 * @see <a href="https://datatables.net/reference/option/columns.searchPanes">ColumnSearchPanes</a>
 */
class YadaDTColumnSearchPanes extends YadaFluentBase<YadaDataTableOptions> {
    private Boolean show;
    private Boolean threshold;

    YadaDTColumnSearchPanes(YadaDataTableOptions parent) {
        super(parent);
    }

    /**
     * Set whether the column search pane is shown.
     * 
     * @param show if true, the column search pane is shown
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/columns.searchPanes.show">show</a>
     */
    public YadaDTColumnSearchPanes show(Boolean show) {
        this.show = show;
        return this;
    }

    /**
     * Get whether the column search pane is shown.
     * 
     * @return true if the column search pane is shown
     */
    public Boolean getShow() {
        return show;
    }

    /**
     * Set the threshold for the column search pane.
     * 
     * @param threshold the threshold value
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/columns.searchPanes.threshold">threshold</a>
     */
    public YadaDTColumnSearchPanes threshold(Boolean threshold) {
        this.threshold = threshold;
        return this;
    }

    /**
     * Get the threshold for the column search pane.
     * 
     * @return the threshold value
     */
    public Boolean getThreshold() {
        return threshold;
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
