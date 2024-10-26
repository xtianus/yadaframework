
package net.yadaframework.web.datatables.options;

import net.yadaframework.core.YadaFluentBase;

/**
 * Represents AJAX configuration for DataTables.
 * 
 * @see <a href="https://datatables.net/reference/option/ajax">Ajax</a>
 */
@Deprecated // Ajax is automatically set in Yada
public class YadaDTAjax extends YadaFluentBase<YadaDTOptions> {
    private String url;
    private String dataSrc;

    YadaDTAjax(YadaDTOptions parent) {
        super(parent);
    }

    /**
     * Set the URL for the AJAX request.
     * 
     * @param url the AJAX URL
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/ajax.url">url</a>
     */
    public YadaDTAjax dtUrl(String url) {
        this.url = url;
        return this;
    }

    /**
     * Get the URL for the AJAX request.
     * 
     * @return the AJAX URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * Set the data source for the AJAX request.
     * 
     * @param dataSrc the data source
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/ajax.dataSrc">dataSrc</a>
     */
    public YadaDTAjax dtDataSrc(String dataSrc) {
        this.dataSrc = dataSrc;
        return this;
    }

    /**
     * Get the data source for the AJAX request.
     * 
     * @return the data source
     */
    public String getDataSrc() {
        return dataSrc;
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
