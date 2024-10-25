package net.yadaframework.web.datatables;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.yadaframework.core.YadaConfiguration;

/**
 * Returns YadaDataTable instances, either creating new ones or reusing existing ones
 */
@Service
public class YadaDataTableFactory {
	private final transient Logger log = LoggerFactory.getLogger(getClass());

	private Map<String, YadaDataTable> instancePool = new ConcurrentHashMap<>(); // id --> instance
	
	/**
	 * Get or create a YadaDataTable instance identified by id.
	 * Using a singleton is more performant and allows reusing it in other requests.
	 * For simple use cases use the constructor {@link YadaDataTable#YadaDataTable(String, YadaConfiguration)}
	 * @param id datatable site-wide unique id 
	 * @param datasource the URL where data is fetched from (without language in the path). Can contain thymeleaf expressions
	 * @param configurer configuration function for new instances, e.g. <pre>table -> { table.dtHTML()... } </pre>
	 * @return a newly initialized instance or an existing one
	 */
	public YadaDataTable getSingleton(String id, String datasource, YadaDataTableConfigurer configurer) {
		// This code could be synchronized to prevent adding identical 
		// instances but there's no harm in that so we avoid the synchronization overhead.
		YadaDataTable result = instancePool.get(id);
		if (result==null) {
			result = new YadaDataTable(id, datasource);
			configurer.configure(result);
			instancePool.put(id, result);
		}
		return result;
	}
	
//	/**
//	 * Empties the cached instances. Use only for debugging purposes (i.e. to re-run the configurer).
//	 */
//	public synchronized void clear() {
//		if (!config.isDevelopmentEnvironment()) {
//			log.error("The 'YadaDataTableFactory.clear()' method should only be used for debugging purposes");
//		}
//		instancePool = new YadaLookupTableThree<String, Locale, YadaDataTable>();
//		log.warn("Cleared YadaDataTable instances");
//	}
}
