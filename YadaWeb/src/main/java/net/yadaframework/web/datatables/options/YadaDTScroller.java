
package net.yadaframework.web.datatables.options;

import net.yadaframework.core.YadaFluentBase;

/**
 * Represents scroller configuration for DataTables.
 * 
 * @see <a href="https://datatables.net/reference/option/scroller">Scroller</a>
 */
class YadaDTScroller extends YadaFluentBase<YadaDataTableOptions> {
    private Boolean loadingIndicator;
    private Integer displayBuffer;
    private Integer boundaryScale;
    
    YadaDTScroller(YadaDataTableOptions parent) {
        super(parent);
    }

    /**
     * Set whether the loading indicator is enabled for the scroller.
     * 
     * @param loadingIndicator true if loading indicator is enabled
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/scroller.loadingIndicator">loadingIndicator</a>
     */
    public YadaDTScroller dtLoadingIndicator(Boolean loadingIndicator) {
        this.loadingIndicator = loadingIndicator;
        return this;
    }

    /**
     * Get whether the loading indicator is enabled for the scroller.
     * 
     * @return true if loading indicator is enabled
     */
    public Boolean getLoadingIndicator() {
        return loadingIndicator;
    }

    /**
     * Set the display buffer size for the scroller.
     * 
     * @param displayBuffer the display buffer size
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/scroller.displayBuffer">displayBuffer</a>
     */
    public YadaDTScroller dtDisplayBuffer(Integer displayBuffer) {
        this.displayBuffer = displayBuffer;
        return this;
    }

    /**
     * Get the display buffer size for the scroller.
     * 
     * @return the display buffer size
     */
    public Integer getDisplayBuffer() {
        return displayBuffer;
    }

    /**
     * Set the boundary scale factor for the scroller.
     * 
     * @param boundaryScale the boundary scale factor
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/scroller.boundaryScale">boundaryScale</a>
     */
    public YadaDTScroller dtBoundaryScale(Integer boundaryScale) {
        this.boundaryScale = boundaryScale;
        return this;
    }

    /**
     * Get the boundary scale factor for the scroller.
     * 
     * @return the boundary scale factor
     */
    public Integer getBoundaryScale() {
        return boundaryScale;
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
