package net.yadaframework.web.datatables.options.proxy;

import java.util.List;

import net.yadaframework.web.datatables.options.YadaDTColumns;
import net.yadaframework.web.datatables.options.YadaDTOptions;

/**
 * This class implements the needed methods for <b>internal use</b> 
 * so that the fluent interface is not polluted by them.
 */
public class YadaDTColumnsProxy extends YadaDTColumns {

	public YadaDTColumnsProxy(YadaDTOptions parent) {
		super(parent);
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
     * @return The data source for the column.
     * @see <a href="https://datatables.net/reference/option/columns.data">DataTables Reference: columns.data</a>
     */
    public String getData() {
        return data;
    }

    /**
     * @return The default content for the column if the data source is null.
     * @see <a href="https://datatables.net/reference/option/columns.defaultContent">DataTables Reference: columns.defaultContent</a>
     */
    public String getDefaultContent() {
        return defaultContent;
    }

    /**
     * @return The field to be edited in Editor for this column.
     * @see <a href="https://datatables.net/reference/option/columns.editField">DataTables Reference: columns.editField</a>
     */
    public String getEditField() {
        return editField;
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

}
