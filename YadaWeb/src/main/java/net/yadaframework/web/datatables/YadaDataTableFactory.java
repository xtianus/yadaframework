package net.yadaframework.web.datatables;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.raw.YadaLookupTableThree;

/**
 * Returns YadaDataTable instances, either creating new ones or reusing existing ones
 */
@Service
public class YadaDataTableFactory {
	private final transient Logger log = LoggerFactory.getLogger(getClass());
	private @Autowired YadaConfiguration config; 
	private @Autowired MessageSource messageSource; 

	// id --> (locale --> instance)
	private YadaLookupTableThree<String, Locale, YadaDataTable> instancePool = new YadaLookupTableThree<String, Locale, YadaDataTable>();
	
	/**
	 * Get or create a YadaDataTable instance identified by id and locale.
	 * Using a singleton is more performant and allows reusing it in other requests.
	 * For simple use cases use the constructor {@link YadaDataTable#YadaDataTable(String, YadaConfiguration, MessageSource, Locale)}
	 * @param id datatable site-wide unique id 
	 * @param locale of the current user
	 * @param configurer configuration function for new instances, e.g. <pre>table -> { table.dtOptions()... } </pre>
	 * @return a newly initialized instance or an existing one
	 */
	public YadaDataTable getSingleton(String id, Locale locale, YadaDataTableConfigurer configurer) {
		// This code could be synchronized to prevent adding identical 
		// instances but there's no harm in that so we avoid the synchronization overhead.
		YadaDataTable result = instancePool.get(id, locale);
		if (result==null) {
			result = new YadaDataTable(id, config, messageSource, locale);
			configurer.configure(result);
			instancePool.put(id, locale, result);
		}
		return result;
	}
	
	/**
	 * Empties the cached instances. Use only for debugging purposes (i.e. to re-run the configurer).
	 */
	public synchronized void clear() {
		if (!config.isDevelopmentEnvironment()) {
			log.error("The 'YadaDataTableFactory.clear()' method should only be used for debugging purposes");
		}
		instancePool = new YadaLookupTableThree<String, Locale, YadaDataTable>();
		log.warn("Cleared YadaDataTable instances");
	}
}
