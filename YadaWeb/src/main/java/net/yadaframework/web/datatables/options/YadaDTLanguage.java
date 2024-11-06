
package net.yadaframework.web.datatables.options;

import net.yadaframework.core.YadaFluentBase;

/**
 * Represents language configuration for DataTables.
 * 
 * @see <a href="https://datatables.net/reference/option/language">Language</a>
 */
@Deprecated // To be deleted because the url is set via YadaDataTable.dtLanguageObj()
public class YadaDTLanguage extends YadaFluentBase<YadaDTOptions> {
    private String url;

    YadaDTLanguage(YadaDTOptions parent) {
        super(parent);
    }
    
    /**
     * Set the URL for the language file.
     * @return this instance for method chaining
     * @see <a href="https://datatables.net/reference/option/language.url">langiage.url</a>
     */
    public YadaDTLanguage dtUrl(String url) {
		this.url = url;
		return this;
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
