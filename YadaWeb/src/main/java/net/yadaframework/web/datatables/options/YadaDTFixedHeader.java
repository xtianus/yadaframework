
package net.yadaframework.web.datatables.options;

import net.yadaframework.core.YadaFluentBase;

/**
 * Represents fixed header configuration for DataTables.
 * 
 * @see <a href="https://datatables.net/reference/option/fixedHeader">FixedHeader</a>
 */
public class YadaDTFixedHeader extends YadaFluentBase<YadaDTOptions> {
    private Boolean header;
    private Boolean footer;

    YadaDTFixedHeader(YadaDTOptions parent) {
        super(parent);
    }

    /**
     * Set whether the header is fixed.
     * 
     * @param header if true, the header is fixed
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/fixedHeader.header">header</a>
     */
    public YadaDTFixedHeader dtHeader(Boolean header) {
        this.header = header;
        return this;
    }

    /**
     * Get whether the header is fixed.
     * 
     * @return true if the header is fixed
     */
    public Boolean getHeader() {
        return header;
    }

    /**
     * Set whether the footer is fixed.
     * 
     * @param footer if true, the footer is fixed
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/fixedHeader.footer">footer</a>
     */
    public YadaDTFixedHeader dtFooter(Boolean footer) {
        this.footer = footer;
        return this;
    }

    /**
     * Get whether the footer is fixed.
     * 
     * @return true if the footer is fixed
     */
    public Boolean getFooter() {
        return footer;
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
