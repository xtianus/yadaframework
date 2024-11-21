package net.yadaframework.components;

import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.Nullable;
import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.exceptions.YadaInvalidUsageException;
import net.yadaframework.raw.YadaLookupTableThree;
import net.yadaframework.web.datatables.YadaDataTableConfigurer;
import net.yadaframework.web.datatables.proxy.YadaDataTableProxy;

/**
 * Returns YadaDataTable instances, either creating new ones or reusing existing ones.
 * There's one instance for each id and locale because data could be fetched using locale-dependent keys.
 * Using a singleton is more performant, allows reusing it in other requests and enables the implementation via a proxy that simplifies the fluent interface.
 */
@Service
public class YadaDataTableFactory {
	private final transient Logger log = LoggerFactory.getLogger(getClass());
	@Autowired private YadaConfiguration config;

	// private Map<String, YadaDataTable> instancePool = new ConcurrentHashMap<>(); // id --> instance
	
	// id --> (locale --> instance)
	private YadaLookupTableThree<String, Locale, YadaDataTableProxy> instancePool = new YadaLookupTableThree<String, Locale, YadaDataTableProxy>();

	/**
	 * Get an existing YadaDataTable instance identified by id and locale.
	 * @param id datatable site-wide unique id. Careful that different site pages can't share the same id or the singleton would be shared. 
	 * @param the locale to which this table instance applies. If null, the configured default locale is used if any.
	 * @return an existing instance
	 * @throws YadaInvalidUsageException if the instance is not found
	 */
	public YadaDataTableProxy getSingleton(String id, @Nullable Locale locale) {
		locale = getNormalizedLocale(locale);
		YadaDataTableProxy result = instancePool.get(id, locale);
		if (result == null) {
			// It could be that the instance is in the pool but with a different locale
			// in which case we log and continue.
			// This could actually be a valid use case when the same ajax method is called with different
			// datatable ids and one of them used a null Locale when creating the singleton.
			Map<Locale, YadaDataTableProxy> localeToTable = instancePool.getSubtable(id);
			if (localeToTable!=null && !localeToTable.isEmpty()) {
				Locale localeOrig = locale;
				locale = localeToTable.keySet().iterator().next();
				log.debug("YadaDataTable instance not found for id '{}' locale '{}', using locale '{}'", id, localeOrig, locale);
				result = localeToTable.get(locale);
			}
		}
		if (result == null) {
			log.error("YadaDataTable instance not found for id '{}' locale '{}'", id, locale);
			throw new YadaInvalidUsageException("YadaDataTable instance not found for id '{}' locale '{}'", id, locale);
		}
		return result;
	}
	
	/**
	 * Get or create a YadaDataTable instance identified by id, when no i18n is needed.
	 * @param id datatable site-wide unique id. Careful that different site pages can't share the same id or the singleton would be the same. 
	 * @param configurer configuration function for new instances, e.g. <pre>table -> { table.dtOptionsObj()... } </pre>
	 * @return a newly initialized instance or an existing one
	 */
	public YadaDataTableProxy getSingleton(String id, YadaDataTableConfigurer configurer) {
		return getSingleton(id, null, configurer);
	}
	
	/**
	 * Get or create a YadaDataTable instance identified by id.
	 * @param id datatable site-wide unique id. Careful that different site pages can't share the same id or the singleton would be the same. 
	 * @param the locale to which this table instance applies. If null, the configured default locale is used if any.
	 * @param configurer configuration function for new instances, e.g. <pre>table -> { table.dtOptionsObj()... } </pre>
	 * @return a newly initialized instance or an existing one
	 */
	public YadaDataTableProxy getSingleton(String id, @Nullable Locale locale, YadaDataTableConfigurer configurer) {
		// This code could be synchronized to prevent adding identical 
		// instances but there's no harm in that so we avoid the synchronization overhead.
		locale = getNormalizedLocale(locale);
		YadaDataTableProxy result = instancePool.get(id, locale);
		
		// This is useful when the configurer is edited while the server is running and we don't want to restart to get the new singleton
		if (config.isDevelopmentEnvironment() && result!=null) {
			// Check that the configurer has not been changed in the meantime
			if (configurer.hashCode() != result.getConfigurer().hashCode()) {
				log.warn("YadaDataTableConfigurer has changed for id '{}' locale '{}'", id, locale);
				result = null; // The configurer has changed so create a new singleton
			}
		}
		
		if (result==null) {
			log.debug("Creating new YadaDataTable instance for id '{}' locale '{}'", id, locale);
			result = new YadaDataTableProxy(id, locale);
			configurer.configure(result);
			result.setConfigurer(configurer); // Used in development to check for changes
			instancePool.put(id, locale, result);
		}
		return result;
	}
	
	private Locale getNormalizedLocale(Locale locale) {
		if (locale==null) {
			locale = config.getDefaultLocale();
			if (locale==null) {
				locale = Locale.getDefault(); // Fall back to platform default
			}
		}
		return locale;
	}
	
}
