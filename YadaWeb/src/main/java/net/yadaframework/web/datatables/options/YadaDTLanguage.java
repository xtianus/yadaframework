
package net.yadaframework.web.datatables.options;

import net.yadaframework.core.YadaFluentBase;

/**
 * Represents language configuration for DataTables.
 * 
 * @see <a href="https://datatables.net/reference/option/language">Language</a>
 */
public class YadaDTLanguage extends YadaFluentBase<YadaDTOptions> {
    private String emptyTable;
    private String zeroRecords;

    YadaDTLanguage(YadaDTOptions parent) {
        super(parent);
    }

    /**
     * Set the language for empty table display.
     * 
     * @param emptyTable the message to display when the table is empty
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/language.emptyTable">emptyTable</a>
     */
    public YadaDTLanguage dtEmptyTable(String emptyTable) {
        this.emptyTable = emptyTable;
        return this;
    }

    /**
     * Get the language for empty table display.
     * 
     * @return the message displayed when the table is empty
     */
    public String getEmptyTable() {
        return emptyTable;
    }

    /**
     * Set the language for zero records found.
     * 
     * @param zeroRecords the message to display when no records are found
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/language.zeroRecords">zeroRecords</a>
     */
    public YadaDTLanguage dtZeroRecords(String zeroRecords) {
        this.zeroRecords = zeroRecords;
        return this;
    }

    /**
     * Get the language for zero records found.
     * 
     * @return the message displayed when no records are found
     */
    public String getZeroRecords() {
        return zeroRecords;
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
