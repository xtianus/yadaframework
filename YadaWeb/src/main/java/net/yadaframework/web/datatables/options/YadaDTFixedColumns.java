
package net.yadaframework.web.datatables.options;

import net.yadaframework.core.YadaFluentBase;

/**
 * Represents fixed columns configuration for DataTables.
 * 
 * @see <a href="https://datatables.net/reference/option/fixedColumns">FixedColumns</a>
 */
public class YadaDTFixedColumns extends YadaFluentBase<YadaDTOptions> {
    private Boolean leftColumns;
    private Boolean rightColumns;

    YadaDTFixedColumns(YadaDTOptions parent) {
        super(parent);
    }

    /**
     * Set whether the left columns are fixed.
     * 
     * @param leftColumns if true, the left columns are fixed
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/fixedColumns.leftColumns">leftColumns</a>
     */
    public YadaDTFixedColumns dtLeftColumns(Boolean leftColumns) {
        this.leftColumns = leftColumns;
        return this;
    }

    /**
     * Get whether the left columns are fixed.
     * 
     * @return true if the left columns are fixed
     */
    public Boolean getLeftColumns() {
        return leftColumns;
    }

    /**
     * Set whether the right columns are fixed.
     * 
     * @param rightColumns if true, the right columns are fixed
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/fixedColumns.rightColumns">rightColumns</a>
     */
    public YadaDTFixedColumns dtRightColumns(Boolean rightColumns) {
        this.rightColumns = rightColumns;
        return this;
    }

    /**
     * Get whether the right columns are fixed.
     * 
     * @return true if the right columns are fixed
     */
    public Boolean getRightColumns() {
        return rightColumns;
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
