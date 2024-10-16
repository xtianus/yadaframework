
package net.yadaframework.web.datatables.options;

import net.yadaframework.core.YadaFluentBase;

/**
 * Represents column definition configuration for DataTables.
 * 
 * @see <a href="https://datatables.net/reference/option/columnDefs">ColumnDefs</a>
 */
class YadaDTColumnDef extends YadaFluentBase<YadaDataTableOptions> {
    private Integer targets;
    private Boolean visible;

    YadaDTColumnDef(YadaDataTableOptions parent) {
        super(parent);
    }

    /**
     * Set the target column index for the column definition.
     * 
     * @param targets the target column index
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/columnDefs.targets">targets</a>
     */
    public YadaDTColumnDef targets(Integer targets) {
        this.targets = targets;
        return this;
    }

    /**
     * Get the target column index for the column definition.
     * 
     * @return the target column index
     */
    public Integer getTargets() {
        return targets;
    }

    /**
     * Set whether the column is visible.
     * 
     * @param visible if true, the column is visible
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/columnDefs.visible">visible</a>
     */
    public YadaDTColumnDef visible(Boolean visible) {
        this.visible = visible;
        return this;
    }

    /**
     * Get whether the column is visible.
     * 
     * @return true if the column is visible
     */
    public Boolean getVisible() {
        return visible;
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
