package net.yadaframework.web.datatables.options;

import net.yadaframework.core.YadaFluentBase;

/**
 * Represents a search column in DataTables.
 */
public class YadaDTSearchCol extends YadaFluentBase<YadaDTOptions> {
    private String column;
    private String search;

    YadaDTSearchCol(YadaDTOptions parent) {
        super(parent);
    }

    /**
     * Set the column to be searched.
     * 
     * @param column the column name or index
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/columns.searchable">column</a>
     */
    public YadaDTSearchCol dtColumn(String column) {
        this.column = column;
        return this;
    }

    /**
     * Get the column to be searched.
     * 
     * @return the column name or index
     */
    public String getColumn() {
        return column;
    }

    /**
     * Set the search term for the column.
     * 
     * @param search the search term
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/columns.search">search</a>
     */
    public YadaDTSearchCol dtSearch(String search) {
        this.search = search;
        return this;
    }

    /**
     * Get the search term for the column.
     * 
     * @return the search term
     */
    public String getSearch() {
        return search;
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
