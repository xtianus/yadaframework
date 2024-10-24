
package net.yadaframework.web.datatables.options;

import net.yadaframework.core.YadaFluentBase;

/**
 * Represents column configuration for DataTables.
 * 
 * @see <a href="https://datatables.net/reference/option/columns">Columns</a>
 */
@Deprecated // Deletable
class YadaDTColumn extends YadaFluentBase<YadaDataTableOptions> {
    private String title;
    private String data;

    YadaDTColumn(YadaDataTableOptions parent) {
        super(parent);
    }

    /**
     * Set the title for the column.
     * 
     * @param title the column title
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/columns.title">title</a>
     */
    public YadaDTColumn dtTitle(String title) {
        this.title = title;
        return this;
    }

    /**
     * Get the title for the column.
     * 
     * @return the column title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Set the data source for the column.
     * 
     * @param data the data source for the column
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/columns.data">data</a>
     */
    public YadaDTColumn dtData(String data) {
        this.data = data;
        return this;
    }

    /**
     * Get the data source for the column.
     * 
     * @return the data source for the column
     */
    public String getData() {
        return data;
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
