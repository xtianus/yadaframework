
package net.yadaframework.web.datatables.options;

import net.yadaframework.core.YadaFluentBase;

/**
 * Represents column reorder configuration for DataTables.
 * 
 * @see <a href="https://datatables.net/reference/option/colReorder">ColReorder</a>
 */
public class YadaDTColReorder extends YadaFluentBase<YadaDTOptions> {
    private Boolean enable;
    private Integer fixedColumnsLeft;

    YadaDTColReorder(YadaDTOptions parent) {
        super(parent);
    }

    /**
     * Set whether column reordering is enabled.
     * 
     * @param enable if true, column reordering is enabled
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/colReorder.enable">enable</a>
     */
    public YadaDTColReorder dtEnable(Boolean enable) {
        this.enable = enable;
        return this;
    }

    /**
     * Get whether column reordering is enabled.
     * 
     * @return true if column reordering is enabled
     */
    public Boolean getEnable() {
        return enable;
    }

    /**
     * Set the number of columns on the left that are fixed during reorder.
     * 
     * @param fixedColumnsLeft the number of fixed columns on the left
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/colReorder.fixedColumnsLeft">fixedColumnsLeft</a>
     */
    public YadaDTColReorder dtFixedColumnsLeft(Integer fixedColumnsLeft) {
        this.fixedColumnsLeft = fixedColumnsLeft;
        return this;
    }

    /**
     * Get the number of columns on the left that are fixed during reorder.
     * 
     * @return the number of fixed columns on the left
     */
    public Integer getFixedColumnsLeft() {
        return fixedColumnsLeft;
    }

    /**
     * Get the parent DataTable options.
     * 
     * @return the parent DataTable options
     */
    public YadaDTOptions parent() {
        return super.parent;
    }
}
