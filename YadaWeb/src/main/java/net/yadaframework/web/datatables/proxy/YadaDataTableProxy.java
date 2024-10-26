package net.yadaframework.web.datatables.proxy;

import java.util.Locale;

import net.yadaframework.web.datatables.YadaDataTable;
import net.yadaframework.web.datatables.config.YadaDataTableHTML;
import net.yadaframework.web.datatables.options.YadaDTOptions;

/**
 * This class implements the methods needed for <b>internal use</b> 
 * so that they don't pollute the fluent interface.
 */
public class YadaDataTableProxy extends YadaDataTable {

	public YadaDataTableProxy(String id, Locale locale) {
		super(id, locale);
	}
	
	public YadaDTOptions getOptions() {
		// We use the trick of using the first getter to make sure that prepareConfiguration() is called before serialization
		prepareConfiguration();
		return options;
	}
	
	public String getId() {
		return id;
	}
	
	public YadaDataTableHTML getHTML() {
		return yadaDataTableHTML;
	}
	
	public String getAjaxUrl() {
		return ajaxUrl;
	}
}
