
package net.yadaframework.web.datatables.options;

import net.yadaframework.core.YadaFluentBase;

/**
 * Represents row grouping configuration for DataTables.
 * 
 * @see <a href="https://datatables.net/reference/option/rowGroup">RowGroup</a>
 */
class YadaDTRowGroup extends YadaFluentBase<YadaDataTableOptions> {
    private Boolean dataSrc;
    private Boolean startRender;

    YadaDTRowGroup(YadaDataTableOptions parent) {
        super(parent);
    }

    /**
     * Set the data source for row grouping.
     * 
     * @param dataSrc the data source for row grouping
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/rowGroup.dataSrc">dataSrc</a>
     */
    public YadaDTRowGroup dtDataSrc(Boolean dataSrc) {
        this.dataSrc = dataSrc;
        return this;
    }

    /**
     * Get the data source for row grouping.
     * 
     * @return the data source for row grouping
     */
    public Boolean getDataSrc() {
        return dataSrc;
    }

    /**
     * Set the rendering function for row grouping.
     * 
     * @param startRender the rendering function
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/rowGroup.startRender">startRender</a>
     */
    public YadaDTRowGroup dtStartRender(Boolean startRender) {
        this.startRender = startRender;
        return this;
    }

    /**
     * Get the rendering function for row grouping.
     * 
     * @return the rendering function
     */
    public Boolean getStartRender() {
        return startRender;
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
