package net.yadaframework.web.datatables.options;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import net.yadaframework.components.YadaUtil;
import net.yadaframework.core.YadaFluentBase;

/**
 * Class representing responsive options for DataTables.
 *
 * @see <a href="https://datatables.net/reference/option/responsive">DataTables Responsive Reference</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class YadaDTResponsive extends YadaFluentBase<YadaDTOptions> {
	@JsonProperty private List<YadaDTBreakpoint> breakpoints;
	@JsonProperty private Object details;
	@JsonProperty private String orthogonal;

    public YadaDTResponsive(YadaDTOptions parent) {
        super(parent);
    }

    // Fluent Setters prefixed with "dt"
    /**
     * Add one breakpoint at which the table will change its size for responsive behavior.
     * Should normally be called multiple times.
     * @return A new breakpoint definition. 
     * @see <a href="https://datatables.net/reference/option/responsive.breakpoints">DataTables breakpoints Reference</a>
     */
    public YadaDTBreakpoint dtBreakpointsObj() {
    	this.breakpoints = YadaUtil.lazyUnsafeInit(this.breakpoints);
        YadaDTBreakpoint newBreakpoint = new YadaDTBreakpoint(this);
        this.breakpoints.add(newBreakpoint);
        return newBreakpoint;
    }

    /**
     * Disable the child rows completely (columns will simply be removed 
     * and their content not be accessible other than through the DataTables API).
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/responsive.details">DataTables details Reference</a>
     */
    public YadaDTResponsive dtDetailsFalse() {
    	this.details = false;
    	return this;
    }

    /**
     * Set the orthogonal data request type for the hidden information display.
     * 
     * @param orthogonal Orthogonal data configuration, e.g. "responsive"
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/responsive.orthogonal">DataTables orthogonal Reference</a>
     */
    public YadaDTResponsive dtOrthogonal(String orthogonal) {
        this.orthogonal = orthogonal;
        return this;
    }

    /**
     * Enable and configure the child rows shown by Responsive for collapsed tables.
     * 
     * @return the instance of YadaDTResponsiveDetails for further configuration
     * @see <a href="https://datatables.net/reference/option/responsive.details">DataTables details Reference</a>
     */
    public YadaDTResponsiveDetails dtDetailsObj() {
    	this.details = YadaUtil.lazyUnsafeInit(this.details, () -> new YadaDTResponsiveDetails(this));
        return (YadaDTResponsiveDetails) this.details;
    }
    
}
