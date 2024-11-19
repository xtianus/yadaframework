
package net.yadaframework.web.datatables.options;

import com.fasterxml.jackson.annotation.JsonInclude;

import net.yadaframework.exceptions.YadaInvalidUsageException;

/**
 * Represents column definition configuration for DataTables.
 * 
 * @see <a href="https://datatables.net/reference/option/columnDefs">ColumnDefs</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class YadaDTColumnDef extends YadaDTColumns {
    // protected String stringTarget;
    // protected int[] intTarget;
    protected Object targets;

    protected YadaDTColumnDef(YadaDTOptions parent) {
        super(parent);
    }

    /**
     * Set the target for the column definition.
     * 
     * @param columnIndex the target column or an array of target columns:
	       <ul>
	       <li>0 or a positive integer - column index counting from the left</li>
		   <li>a negative integer - column index counting from the right</li>
	       </ul>
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/columnDefs.targets">targets</a>
     */
    public YadaDTColumnDef dtTargets(int ... columnIndex) {
    	if (targets != null) {
	    	throw new YadaInvalidUsageException("Cannot set targets more than once");
    	}
        this.targets = columnIndex;
        return this;
    }
    
    /**
     * Set the target for the column definition.
     * 
     * @param cssSelector a CSS selector - columns that match the selector will be used
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/columnDefs.targets">targets</a>
     */
    public YadaDTColumnDef dtTargetsCss(String cssSelector) {
    	if (targets != null) {
	    	throw new YadaInvalidUsageException("Cannot set targets more than once");
    	}
    	this.targets = cssSelector;
    	return this;
    }
    
    /**
     * Set the target for the column definition.
     * 
     * @param columnName the name of the column, without ":name" at the end
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/columnDefs.targets">targets</a>
     */
    public YadaDTColumnDef dtTargetsName(String columnName) {
    	if (targets != null) {
	    	throw new YadaInvalidUsageException("Cannot set targets more than once");
    	}
    	this.targets = columnName + ":name";
    	return this;
    }
    
    /**
     * Target all columns.
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/columnDefs.targets">targets</a>
     */
    public YadaDTColumnDef dtTargetsAll() {
    	if (targets != null) {
	    	throw new YadaInvalidUsageException("Cannot set targets more than once");
    	}
    	this.targets = "_all";
    	return this;
    }
    
}
