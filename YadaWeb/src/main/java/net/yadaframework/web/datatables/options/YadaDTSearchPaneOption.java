package net.yadaframework.web.datatables.options;

import net.yadaframework.core.YadaFluentBase;

/**
 * Represents an option within a search pane in DataTables.
 * 
 * @see <a href="https://datatables.net/reference/option/searchPanes.panes.options">SearchPanes options</a>
 */
class YadaDTSearchPaneOption extends YadaFluentBase<YadaDTSearchPane> {
    private String className;
    private String label;
    private String value;

    public YadaDTSearchPaneOption(YadaDTSearchPane parent) {
        super(parent);
    }

    /**
     * Set the CSS class name for the search pane option.
     * 
     * @param className the CSS class name
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/searchPanes.panes.options.className">className</a>
     */
    public YadaDTSearchPaneOption className(String className) {
        this.className = className;
        return this;
    }

    /**
     * Get the CSS class name for the search pane option.
     * 
     * @return the CSS class name
     */
    public String getClassName() {
        return className;
    }

    /**
     * Set the label for the search pane option.
     * 
     * @param label the label to display
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/searchPanes.panes.options.label">label</a>
     */
    public YadaDTSearchPaneOption label(String label) {
        this.label = label;
        return this;
    }

    /**
     * Get the label for the search pane option.
     * 
     * @return the label to display
     */
    public String getLabel() {
        return label;
    }

    /**
     * Set the value for the search pane option.
     * 
     * @param value the value for the option
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/searchPanes.panes.options.value">value</a>
     */
    public YadaDTSearchPaneOption value(String value) {
        this.value = value;
        return this;
    }

    /**
     * Get the value for the search pane option.
     * 
     * @return the value of the option
     */
    public String getValue() {
        return value;
    }

    /**
     * Get the parent search pane.
     * 
     * @return the parent search pane
     */
    public YadaDTSearchPane parent() {
        return super.parent;
    }
}
