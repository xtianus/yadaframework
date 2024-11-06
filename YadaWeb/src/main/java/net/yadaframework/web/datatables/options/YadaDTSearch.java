
package net.yadaframework.web.datatables.options;

import net.yadaframework.core.YadaFluentBase;

/**
 * Represents search configuration for DataTables.
 * 
 * @see <a href="https://datatables.net/reference/option/search">Search</a>
 */
public class YadaDTSearch extends YadaFluentBase<YadaDTOptions> {
    private String regex;
    private Boolean smart;
    private Boolean caseInsensitive;
    
    YadaDTSearch(YadaDTOptions parent) {
        super(parent);
    }

    /**
     * Set whether the search uses regular expressions.
     * 
     * @param regex the regular expression search
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/search.regex">regex</a>
     */
    public YadaDTSearch dtRegex(String regex) {
        this.regex = regex;
        return this;
    }

    /**
     * Get the regular expression search setting.
     * 
     * @return the regular expression search
     */
    public String getRegex() {
        return regex;
    }

    /**
     * Set whether smart search is enabled.
     * 
     * @param smart true if smart search is enabled
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/search.smart">smart</a>
     */
    public YadaDTSearch dtSmart(Boolean smart) {
        this.smart = smart;
        return this;
    }

    /**
     * Get whether smart search is enabled.
     * 
     * @return true if smart search is enabled
     */
    public Boolean getSmart() {
        return smart;
    }

    /**
     * Set whether the search is case insensitive.
     * 
     * @param caseInsensitive true if case insensitive search is enabled
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/search.caseInsensitive">caseInsensitive</a>
     */
    public YadaDTSearch dtCaseInsensitive(Boolean caseInsensitive) {
        this.caseInsensitive = caseInsensitive;
        return this;
    }

    /**
     * Get whether the search is case insensitive.
     * 
     * @return true if case insensitive search is enabled
     */
    public Boolean getCaseInsensitive() {
        return caseInsensitive;
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
