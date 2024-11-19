package net.yadaframework.web.datatables.options;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import net.yadaframework.core.YadaFluentBase;
import net.yadaframework.web.YadaJsonRawStringSerializer;

/**
 * Class representing detailed options for responsive behavior in DataTables.
 *
 * @see <a href="https://datatables.net/reference/option/responsive.details">DataTables Responsive Details Reference</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class YadaDTResponsiveDetails extends YadaFluentBase<YadaDTResponsive> {
	@JsonSerialize(using = YadaJsonRawStringSerializer.class)
	@JsonProperty private String display;
	@JsonSerialize(using = YadaJsonRawStringSerializer.class)
	@JsonProperty private String renderer;
	@JsonProperty private Object target; // String or Integer
	@JsonProperty private String type;

    public YadaDTResponsiveDetails(YadaDTResponsive parent) {
        super(parent);
    }

    // Fluent Setters prefixed with "dt"
    /**
     * The function given is responsible for showing and hiding the child data.
     *
     * @param display Details display control configuration:
     * <ul>
     * <li>"DataTable.Responsive.display.childRowImmediate" will show the child rows immediately without need of clicking on the control</li>
     * <li>"DataTable.Responsive.display.modal()" will show the child rows in a modal dialog</li>
     * <ul>
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
     * Column for child row display control when using column details type.
     * This column is the one where the user needs to click in order to show the hidden details.
     *
     * @param target Column index to which the show / hide control should be attached. 
     * 				This can be >=0 to count columns from the left, or <0 to count from the right.
     * @return this instance for method chaining
     * @see #dtTypeColumn()
     * @see <a href="https://datatables.net/reference/option/responsive.details.target">DataTables details.target Reference</a>
     */
    public YadaDTResponsiveDetails dtTarget(Integer target) {
        this.target = target;
        return this;
    }
    
    /**
     * Selector for child row display control when using column details type.
     *
     * @param target jQuery selector to determine what element(s) will activate the show / hide 
     * 			control for the details child rows. This provides the ability to use any element 
     * 			in a table - for example you can use the whole row, or a single img element in the row.
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/responsive.details.target">DataTables details.target Reference</a>
     */
    public YadaDTResponsiveDetails dtTarget(String target) {
    	this.target = target;
    	return this;
    }

    /**
     * Sets the "column" type of control for displaying the child rows i.e.
     * use a whole column to display the control element.
     * This means that you need to click on a cell to show the hidden columns
     * and the cell is in the column set with dtTarget().
     * @return this instance for method chaining
     * @see #dtTarget(Integer)
     * @see #dtTypeNone(Integer)
     * @see <a href="https://datatables.net/reference/option/responsive.details.type">DataTables details.type Reference</a>
     */
    public YadaDTResponsiveDetails dtTypeColumn() {
        this.type = "column";
        return this;
    }
    
    /**
     * Don't show the show / hide icons.
     *
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/responsive.details.type">DataTables details.type Reference</a>
     */
    public YadaDTResponsiveDetails dtTypeNone() {
    	this.type = "none";
    	return this;
    }
    
}
