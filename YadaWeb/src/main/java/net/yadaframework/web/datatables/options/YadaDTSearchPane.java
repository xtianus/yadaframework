package net.yadaframework.web.datatables.options;

import java.util.ArrayList;
import java.util.List;

import net.yadaframework.core.YadaFluentBase;

/**
 * Represents a search pane in DataTables.
 * 
 * @see <a href="https://datatables.net/reference/option/searchPanes.panes">SearchPanes panes</a>
 */
class YadaDTSearchPane extends YadaFluentBase<YadaDTSearchPanes> {
    private String className;
    private String dtOpts;
    private String header;
    private List<YadaDTSearchPaneOption> options;

    public YadaDTSearchPane(YadaDTSearchPanes parent) {
        super(parent);
    }

    /**
     * Set the CSS class name for the search pane.
     * 
     * @param className the CSS class name
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/searchPanes.panes.className">className</a>
     */
    public YadaDTSearchPane className(String className) {
        this.className = className;
        return this;
    }

    /**
     * Get the CSS class name for the search pane.
     * 
     * @return the CSS class name
     */
    public String getClassName() {
        return className;
    }

    /**
     * Set additional DataTables options for the search pane.
     * 
     * @param dtOpts additional DataTables options
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/searchPanes.panes.dtOpts">dtOpts</a>
     */
    public YadaDTSearchPane dtOpts(String dtOpts) {
        this.dtOpts = dtOpts;
        return this;
    }

    /**
     * Get additional DataTables options for the search pane.
     * 
     * @return the additional DataTables options
     */
    public String getDtOpts() {
        return dtOpts;
    }

    /**
     * Set the header text for the search pane.
     * 
     * @param header the header text
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/searchPanes.panes.header">header</a>
     */
    public YadaDTSearchPane header(String header) {
        this.header = header;
        return this;
    }

    /**
     * Get the header text for the search pane.
     * 
     * @return the header text
     */
    public String getHeader() {
        return header;
    }

    /**
     * Set the list of options for the search pane.
     * 
     * @param options a list of YadaDTSearchPaneOption instances
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/searchPanes.panes.options">options</a>
     */
    public YadaDTSearchPane options(List<YadaDTSearchPaneOption> options) {
        this.options = options;
        return this;
    }

    /**
     * Get the list of options for the search pane.
     * 
     * @return the list of YadaDTSearchPaneOption instances
     */
    public List<YadaDTSearchPaneOption> getOptions() {
        return options;
    }

    /**
     * Add a new option to the list of options for the search pane.
     * 
     * @return a new YadaDTSearchPaneOption instance
     */
    public YadaDTSearchPaneOption options() {
        if (this.options == null) {
            this.options = new ArrayList<>();
        }
        YadaDTSearchPaneOption newOption = new YadaDTSearchPaneOption(this);
        this.options.add(newOption);
        return newOption;
    }

    /**
     * Get the parent search panes instance.
     * 
     * @return the parent search panes
     */
    public YadaDTSearchPanes parent() {
        return super.parent;
    }
}
