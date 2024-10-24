
package net.yadaframework.web.datatables.options;

import net.yadaframework.core.YadaFluentBase;

/**
 * Represents buttons configuration for DataTables.
 * 
 * @see <a href="https://datatables.net/reference/option/buttons">Buttons</a>
 */
class YadaDTButtons extends YadaFluentBase<YadaDataTableOptions> {
    private Boolean dom;
    private String button;

    YadaDTButtons(YadaDataTableOptions parent) {
        super(parent);
    }

    /**
     * Set whether the button DOM structure is used.
     * 
     * @param dom if true, button DOM structure is used
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/buttons.dom">dom</a>
     */
    public YadaDTButtons dtDom(Boolean dom) {
        this.dom = dom;
        return this;
    }

    /**
     * Get whether the button DOM structure is used.
     * 
     * @return true if the button DOM structure is used
     */
    public Boolean getDom() {
        return dom;
    }

    /**
     * Set a specific button to be added to the DOM.
     * 
     * @param button the button to add
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/buttons.button">button</a>
     */
    public YadaDTButtons dtButton(String button) {
        this.button = button;
        return this;
    }

    /**
     * Get the specific button added to the DOM.
     * 
     * @return the button added to the DOM
     */
    public String getButton() {
        return button;
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
