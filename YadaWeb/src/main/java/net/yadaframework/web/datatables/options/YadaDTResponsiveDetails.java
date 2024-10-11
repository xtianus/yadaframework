
package net.yadaframework.web.datatables.options;

import net.yadaframework.core.YadaFluentBase;

/**
 * Represents responsive details configuration for DataTables.
 * 
 * @see <a href="https://datatables.net/reference/option/responsive.details">Responsive Details</a>
 */
class YadaDTResponsiveDetails extends YadaFluentBase<YadaDataTableOptions> {
    private Boolean type;
    private String renderer;

    public YadaDTResponsiveDetails(YadaDataTableOptions parent) {
        super(parent);
    }

    /**
     * Set the type of responsive details.
     * 
     * @param type the type of responsive details
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/responsive.details.type">type</a>
     */
    public YadaDTResponsiveDetails type(Boolean type) {
        this.type = type;
        return this;
    }

    /**
     * Get the type of responsive details.
     * 
     * @return the type of responsive details
     */
    public Boolean getType() {
        return type;
    }

    /**
     * Set the renderer for responsive details.
     * 
     * @param renderer the renderer function
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/responsive.details.renderer">renderer</a>
     */
    public YadaDTResponsiveDetails renderer(String renderer) {
        this.renderer = renderer;
        return this;
    }

    /**
     * Get the renderer for responsive details.
     * 
     * @return the renderer function
     */
    public String getRenderer() {
        return renderer;
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
