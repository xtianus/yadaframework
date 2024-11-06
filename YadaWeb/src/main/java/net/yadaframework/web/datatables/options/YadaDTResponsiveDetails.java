package net.yadaframework.web.datatables.options;

import com.fasterxml.jackson.annotation.JsonInclude;

import net.yadaframework.core.YadaFluentBase;

/**
 * Class representing detailed options for responsive behavior in DataTables.
 *
 * @see <a href="https://datatables.net/reference/option/responsive.details">DataTables Responsive Details Reference</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class YadaDTResponsiveDetails extends YadaFluentBase<YadaDTResponsive> {
    private String display;
    private String renderer;
    private String target;
    private String type;

    public YadaDTResponsiveDetails(YadaDTResponsive parent) {
        super(parent);
    }

    // Fluent Setters prefixed with "dt"
    /**
     * Sets how the display of the child row details will be controlled.
     *
     * @param display Details display control configuration
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/responsive.details.display">DataTables details.display Reference</a>
     */
    public YadaDTResponsiveDetails dtDisplay(String display) {
        this.display = display;
        return this;
    }

    /**
     * Sets the function that will be used to render the child row when the table is responsive.
     *
     * @param renderer Details renderer function
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/responsive.details.renderer">DataTables details.renderer Reference</a>
     */
    public YadaDTResponsiveDetails dtRenderer(String renderer) {
        this.renderer = renderer;
        return this;
    }

    /**
     * Sets the target element for child row display when the table is responsive.
     *
     * @param target Details target configuration
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/responsive.details.target">DataTables details.target Reference</a>
     */
    public YadaDTResponsiveDetails dtTarget(String target) {
        this.target = target;
        return this;
    }

    /**
     * Sets the type of control for displaying the child rows.
     *
     * @param type Details type configuration
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/responsive.details.type">DataTables details.type Reference</a>
     */
    public YadaDTResponsiveDetails dtType(String type) {
        this.type = type;
        return this;
    }

    // Getters
    public String getDisplay() {
        return display;
    }

    public String getRenderer() {
        return renderer;
    }

    public String getTarget() {
        return target;
    }

    public String getType() {
        return type;
    }
}
