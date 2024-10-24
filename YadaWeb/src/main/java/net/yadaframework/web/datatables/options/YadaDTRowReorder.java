
package net.yadaframework.web.datatables.options;

import net.yadaframework.core.YadaFluentBase;

/**
 * Represents row reorder configuration for DataTables.
 * 
 * @see <a href="https://datatables.net/reference/option/rowReorder">RowReorder</a>
 */
class YadaDTRowReorder extends YadaFluentBase<YadaDataTableOptions> {
    private Boolean editor;
    private String selector;
    private Boolean snapX;

    YadaDTRowReorder(YadaDataTableOptions parent) {
        super(parent);
    }

    /**
     * Set the editor for row reordering.
     * 
     * @param editor the editor used for row reordering
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/rowReorder.editor">editor</a>
     */
    public YadaDTRowReorder dtEditor(Boolean editor) {
        this.editor = editor;
        return this;
    }

    /**
     * Get the editor for row reordering.
     * 
     * @return the editor used for row reordering
     */
    public Boolean getEditor() {
        return editor;
    }

    /**
     * Set the selector for row reordering.
     * 
     * @param selector the row selector for reordering
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/rowReorder.selector">selector</a>
     */
    public YadaDTRowReorder dtSelector(String selector) {
        this.selector = selector;
        return this;
    }

    /**
     * Get the selector for row reordering.
     * 
     * @return the row selector for reordering
     */
    public String getSelector() {
        return selector;
    }

    /**
     * Set whether the X-axis should snap to the row during reordering.
     * 
     * @param snapX if true, snapping on the X-axis is enabled
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/rowReorder.snapX">snapX</a>
     */
    public YadaDTRowReorder dtSnapX(Boolean snapX) {
        this.snapX = snapX;
        return this;
    }

    /**
     * Get whether the X-axis snapping is enabled during reordering.
     * 
     * @return true if X-axis snapping is enabled
     */
    public Boolean getSnapX() {
        return snapX;
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
