package net.yadaframework.web.datatables.options;

import com.fasterxml.jackson.annotation.JsonInclude;

import net.yadaframework.core.YadaFluentBase;

/**
 * Class representing responsive options for DataTables.
 *
 * @see <a href="https://datatables.net/reference/option/responsive">DataTables Responsive Reference</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class YadaDTResponsive extends YadaFluentBase<YadaDTOptions> {
    private String breakpoints;
    private String orthogonal;
    private Boolean details;
    private YadaDTResponsiveDetails yadaDTResponsiveDetails;

    public YadaDTResponsive(YadaDTOptions parent) {
        super(parent);
    }

    // Fluent Setters prefixed with "dt"
    /**
     * Sets the breakpoints at which the table will change its size for responsive behavior.
     * 
     * @param breakpoints Breakpoints configuration
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/responsive.breakpoints">DataTables breakpoints Reference</a>
     */
    public YadaDTResponsive dtBreakpoints(String breakpoints) {
        this.breakpoints = breakpoints;
        return this;
    }

    /**
     * Sets the orthogonal data request for responsive handling.
     * 
     * @param orthogonal Orthogonal data configuration
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/responsive.orthogonal">DataTables orthogonal Reference</a>
     */
    public YadaDTResponsive dtOrthogonal(String orthogonal) {
        this.orthogonal = orthogonal;
        return this;
    }

    // Getters
    public String getBreakpoints() {
        return breakpoints;
    }

    public Object getDetails() {
        return details;
    }

    public String getOrthogonal() {
        return orthogonal;
    }
    

    /**
     * Sets the details option for child row display control when the table is responsive.
     * 
     * @param details Details configuration
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/responsive.details">DataTables details Reference</a>
     */
    public YadaDTResponsiveDetails dtDetails() {
        if (this.yadaDTResponsiveDetails == null) {
            this.yadaDTResponsiveDetails = new YadaDTResponsiveDetails(this);
        }
        return this.yadaDTResponsiveDetails;
    }

    /**
     * Sets the details option for child row display control when the table is responsive.
     * 
     * @param details Details configuration
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/responsive.details">DataTables details Reference</a>
     */
    public YadaDTResponsive dtDetails(Boolean details) {
        this.details = details;
        return this;
    }
    
}
