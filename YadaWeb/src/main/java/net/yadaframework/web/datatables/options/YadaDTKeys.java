
package net.yadaframework.web.datatables.options;

import net.yadaframework.core.YadaFluentBase;

/**
 * Represents key table configuration for DataTables.
 * 
 * @see <a href="https://datatables.net/reference/option/keys">Keys</a>
 */
class YadaDTKeys extends YadaFluentBase<YadaDataTableOptions> {
    private Boolean clipboard;
    private Boolean columns;

    YadaDTKeys(YadaDataTableOptions parent) {
        super(parent);
    }

    /**
     * Set whether clipboard interaction is enabled.
     * 
     * @param clipboard if true, clipboard interaction is enabled
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/keys.clipboard">clipboard</a>
     */
    public YadaDTKeys dtClipboard(Boolean clipboard) {
        this.clipboard = clipboard;
        return this;
    }

    /**
     * Get whether clipboard interaction is enabled.
     * 
     * @return true if clipboard interaction is enabled
     */
    public Boolean getClipboard() {
        return clipboard;
    }

    /**
     * Set whether column key interaction is enabled.
     * 
     * @param columns if true, column key interaction is enabled
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/keys.columns">columns</a>
     */
    public YadaDTKeys dtColumns(Boolean columns) {
        this.columns = columns;
        return this;
    }

    /**
     * Get whether column key interaction is enabled.
     * 
     * @return true if column key interaction is enabled
     */
    public Boolean getColumns() {
        return columns;
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
