
package net.yadaframework.web.datatables.options;

import net.yadaframework.core.YadaFluentBase;

/**
 * Represents ARIA language configuration for DataTables.
 * 
 * @see <a href="https://datatables.net/reference/option/language.aria">Language Aria</a>
 */
class YadaDTLanguageAria extends YadaFluentBase<YadaDTLanguage> {
    private String sortAscending;
    private String sortDescending;

    public YadaDTLanguageAria(YadaDTLanguage parent) {
        super(parent);
    }

    /**
     * Set the ARIA label for sorting in ascending order.
     * 
     * @param sortAscending the ARIA label for ascending sorting
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/language.aria.sortAscending">sortAscending</a>
     */
    public YadaDTLanguageAria sortAscending(String sortAscending) {
        this.sortAscending = sortAscending;
        return this;
    }

    /**
     * Get the ARIA label for sorting in ascending order.
     * 
     * @return the ARIA label for ascending sorting
     */
    public String getSortAscending() {
        return sortAscending;
    }

    /**
     * Set the ARIA label for sorting in descending order.
     * 
     * @param sortDescending the ARIA label for descending sorting
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/language.aria.sortDescending">sortDescending</a>
     */
    public YadaDTLanguageAria sortDescending(String sortDescending) {
        this.sortDescending = sortDescending;
        return this;
    }

    /**
     * Get the ARIA label for sorting in descending order.
     * 
     * @return the ARIA label for descending sorting
     */
    public String getSortDescending() {
        return sortDescending;
    }

    /**
     * Get the parent DataTable options.
     * 
     * @return the parent DataTable options
     */
    public YadaDTLanguage parent() {
        return super.parent;
    }
}
