package net.yadaframework.web.datatables.proxy;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import net.yadaframework.web.datatables.options.YadaDTColumns;
import net.yadaframework.web.datatables.options.YadaDTOptions;

/**
 * This class implements the methods needed for <b>internal use</b> 
 * so that they don't pollute the fluent interface.
 */
public class YadaDTColumnsProxy extends YadaDTColumns {

	public YadaDTColumnsProxy(YadaDTOptions parent) {
		super(parent);
	}
	
	// Used by YadaDataTableColumn
	public YadaDTOptions getParent() {
		return parent;
	}
	
    /**
     * @return The data source for the column.
     * @see <a href="https://datatables.net/reference/option/columns.data">DataTables Reference: columns.data</a>
     */
	// data can be null in the json
    @JsonInclude(JsonInclude.Include.ALWAYS) 
    public Object getData() {
        return data;
    }

    // Getters
    /**
     * @return The ARIA label for the column for accessibility purposes.
     * @see <a href="https://datatables.net/reference/option/columns.ariaTitle">DataTables Reference: columns.ariaTitle</a>
     */
    public String getAriaTitle() {
        return ariaTitle;
    }

    /**
     * @return The cell type for the column (e.g., "td" or "th").
     * @see <a href="https://datatables.net/reference/option/columns.cellType">DataTables Reference: columns.cellType</a>
     */
    public String getCellType() {
        return cellType;
    }

    /**
     * @return The class name to be added to all cells in the column.
     * @see <a href="https://datatables.net/reference/option/columns.className">DataTables Reference: columns.className</a>
     */
    public String getClassName() {
        return className;
    }

    /**
     * @return The padding to be applied to the content of the column.
     * @see <a href="https://datatables.net/reference/option/columns.contentPadding">DataTables Reference: columns.contentPadding</a>
     */
    public String getContentPadding() {
        return contentPadding;
    }

    /**
     * @return The function to be executed when a cell is created.
     * @see <a href="https://datatables.net/reference/option/columns.createdCell">DataTables Reference: columns.createdCell</a>
     */
    public String getCreatedCell() {
        return createdCell;
    }

    /**
     * @return The default content for the column if the data source is null.
     * @see <a href="https://datatables.net/reference/option/columns.defaultContent">DataTables Reference: columns.defaultContent</a>
     */
    public String getDefaultContent() {
        return defaultContent;
    }

    /**
     * @return The content for the footer of the column.
     * @see <a href="https://datatables.net/reference/option/columns.footer">DataTables Reference: columns.footer</a>
     */
    public String getFooter() {
        return footer;
    }

    /**
     * @return The name for the column.
     * @see <a href="https://datatables.net/reference/option/columns.name">DataTables Reference: columns.name</a>
     */
    public String getName() {
        return name;
    }

    /**
     * @return The columns to order by when this column is sorted.
     * @see <a href="https://datatables.net/reference/option/columns.orderData">DataTables Reference: columns.orderData</a>
     */
    public List<Integer> getOrderData() {
        return orderData;
    }

    /**
     * @return The ordering data type for the column.
     * @see <a href="https://datatables.net/reference/option/columns.orderDataType">DataTables Reference: columns.orderDataType</a>
     */
    public String getOrderDataType() {
        return orderDataType;
    }

    /**
     * @return The ordering sequence for the column.
     * @see <a href="https://datatables.net/reference/option/columns.orderSequence">DataTables Reference: columns.orderSequence</a>
     */
    public List<String> getOrderSequence() {
        return orderSequence;
    }

    /**
     * @return The width of the column.
     * @see <a href="https://datatables.net/reference/option/columns.width">DataTables Reference: columns.width</a>
     */
    public String getWidth() {
        return width;
    }

    /**
     * @return The title for the column.
     * @see <a href="https://datatables.net/reference/option/columns.title">DataTables Reference: columns.title</a>
     */
    public String getTitle() {
        return title;
    }

    /**
     * @return The rendering function for the column's data.
     * @see <a href="https://datatables.net/reference/option/columns.render">DataTables Reference: columns.render</a>
     */
    public String getRender() {
        return render;
    }

    /**
     * @return The responsive priority for the column.
     * @see <a href="https://datatables.net/reference/option/columns.responsivePriority">DataTables Reference: columns.responsivePriority</a>
     */
    public Integer getResponsivePriority() {
        return responsivePriority;
    }

	public Boolean isOrderable() {
		return orderable;
	}    
	
	public Boolean isSearchable() {
		return searchable;
	}
	
	public Boolean isVisible() {
		return visible;
	}


}

