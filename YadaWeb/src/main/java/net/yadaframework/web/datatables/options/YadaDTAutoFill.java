
package net.yadaframework.web.datatables.options;

import net.yadaframework.core.YadaFluentBase;

/**
 * Represents AutoFill configuration for DataTables.
 * 
 * @see <a href="https://datatables.net/reference/option/autoFill">AutoFill</a>
 */
class YadaDTAutoFill extends YadaFluentBase<YadaDataTableOptions> {
    private Boolean enable;
    private Boolean update;

    public YadaDTAutoFill(YadaDataTableOptions parent) {
        super(parent);
    }

    /**
     * Set whether AutoFill is enabled.
     * 
     * @param enable if true, AutoFill is enabled
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/autoFill.enable">enable</a>
     */
    public YadaDTAutoFill enable(Boolean enable) {
        this.enable = enable;
        return this;
    }

    /**
     * Get whether AutoFill is enabled.
     * 
     * @return true if AutoFill is enabled
     */
    public Boolean getEnable() {
        return enable;
    }

    /**
     * Set whether AutoFill will update the data.
     * 
     * @param update if true, AutoFill will update the data
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/autoFill.update">update</a>
     */
    public YadaDTAutoFill update(Boolean update) {
        this.update = update;
        return this;
    }

    /**
     * Get whether AutoFill will update the data.
     * 
     * @return true if AutoFill will update the data
     */
    public Boolean getUpdate() {
        return update;
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
