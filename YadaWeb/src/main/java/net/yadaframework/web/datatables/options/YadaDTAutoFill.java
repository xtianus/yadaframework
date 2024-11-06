
package net.yadaframework.web.datatables.options;

import net.yadaframework.core.YadaFluentBase;

/**
 * Represents AutoFill configuration for DataTables.
 * 
 * @see <a href="https://datatables.net/reference/option/autoFill">AutoFill</a>
 */
public class YadaDTAutoFill extends YadaFluentBase<YadaDTOptions> {
    private Boolean enable;
    private Boolean update;

    YadaDTAutoFill(YadaDTOptions parent) {
        super(parent);
    }

    /**
     * Set whether AutoFill is enabled.
     * 
     * @param enable if true, AutoFill is enabled
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/autoFill.enable">enable</a>
     */
    public YadaDTAutoFill dtEnable(Boolean enable) {
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
    public YadaDTAutoFill dtUpdate(Boolean update) {
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
    public YadaDTOptions parent() {
        return super.parent;
    }
}
