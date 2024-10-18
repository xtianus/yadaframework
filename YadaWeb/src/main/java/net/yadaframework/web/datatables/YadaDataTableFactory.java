package net.yadaframework.web.datatables;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import net.yadaframework.raw.YadaLookupTableThree;

/**
 * Returns YadaDataTable instances, either creating new ones or reusing existing ones
 */
@Service
public class YadaDataTableFactory {
	@Autowired MessageSource messageSource; 

	// id --> (locale --> instance)
	private YadaLookupTableThree<String, Locale, YadaDataTable> instancePool = new YadaLookupTableThree<String, Locale, YadaDataTable>();
	
	/**
	 * Get or create a YadaDataTable instance.
	 * @param id datatable site-wide unique id 
	 * @param locale
	 * @param configurer configuration function for new instances, e.g. <pre>table -> { table.dtOptions()... } </pre>
	 */
	public YadaDataTable getInstance(String id, Locale locale, YadaDataTableConfigurer configurer) {
		// This code could be synchronized to prevent adding identical 
		// instances but there's no harm in that so we avoid the synchronization overhead.
		YadaDataTable result = instancePool.get(id, locale);
		if (result==null) {
			result = new YadaDataTable(id, messageSource, locale);
			configurer.configure(result);
			instancePool.put(id, locale, result);
		}
		return result;
	}
}
