package net.yadaframework.web.datatables.proxy;

import java.util.List;
import java.util.Locale;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.servlet.http.HttpServletRequest;
import net.yadaframework.exceptions.YadaInvalidUsageException;
import net.yadaframework.persistence.YadaDataTableDao;
import net.yadaframework.persistence.YadaDataTableQueryPolicy;
import net.yadaframework.web.datatables.YadaDataTable;
import net.yadaframework.web.datatables.YadaDataTableConfigurer;
import net.yadaframework.web.datatables.config.YadaDataTableHTML;
import net.yadaframework.web.datatables.options.YadaDTColumns;
import net.yadaframework.web.datatables.options.YadaDTOptions;

/**
 * This class implements the methods needed for <b>internal use</b> 
 * so that they don't pollute the fluent interface.
 */
public class YadaDataTableProxy extends YadaDataTable {
	@JsonIgnore private volatile YadaDataTableQueryPolicy queryPolicy; // Derived once from the configuration

	public YadaDataTableProxy(String id, Locale locale) {
		super(id, locale);
	}
	
	public String getSecurityAsPath() {
		return securityAsPath;
	}
	
	public void setSecurityAsPath(String securityAsPath) {
		this.securityAsPath = securityAsPath;
	}
	
	/**
	 * Set the configurer used to create this instance, for internal use.
	 * @param configurer
	 */
	public void setConfigurer(YadaDataTableConfigurer configurer) {
		this.configurer = configurer;
	}

	/**
	 * Gets the configurer used to create this instance, for internal use.
	 */
	public Object getConfigurer() {
		return configurer;
	}

	public YadaDTOptions getOptions() {
		return options;
	}
	
	public String getId() {
		return id;
	}
	
	public YadaDataTableHTML getStructure() {
		// We use the trick of using the first getter to make sure that prepareConfiguration() is called before serialization
		prepareConfiguration();
		return yadaDataTableHTML;
	}
	
	public String getAjaxUrl() {
		return ajaxUrl;
	}
	
	public String getLanguageUrl() {
		String languageUrl = null;
		if (yadaDataTableLanguage != null) {
			languageUrl = yadaDataTableLanguage.getLanguageUrl(locale.getLanguage());
		}
		return languageUrl;
	}

	public Class getEntityClass() {
		return entityClass;
	}

	/**
	 * Returns the server-side query policy of this table, derived from its configuration: one column for each
	 * configured column name (or data path when the name is not set), searchable and orderable unless switched off
	 * with dtSearchableOff() or dtOrderableOff(). Selection and command columns, and columns whose data is a
	 * javascript function without a name, are not part of it. A column whose path is not a persistent field of the
	 * entity class (a getter, a computed value, a subclass-only attribute) is output-only, with a warning in the log.
	 * @return the policy to pass to {@link YadaDataTableDao#getConvertedJsonPage(net.yadaframework.web.YadaDatatablesRequest, Class, Locale, YadaDataTableQueryPolicy)}
	 * @throws YadaInvalidUsageException when the entity class is not set or a column path is not valid for it
	 */
	@JsonIgnore // Never serialized to the page
	public YadaDataTableQueryPolicy getQueryPolicy() {
		YadaDataTableQueryPolicy result = queryPolicy;
		if (result == null) {
			if (entityClass == null) {
				throw new YadaInvalidUsageException("Entity class must be set in dataTableId '{}'", id);
			}
			prepareConfiguration();
			YadaDataTableQueryPolicy.Builder builder = YadaDataTableQueryPolicy.forConfiguredTable(id, entityClass);
			List<YadaDTColumns> columns = ((YadaDTOptionsProxy) options).getColumns();
			if (columns != null) {
				for (YadaDTColumns column : columns) {
					YadaDTColumnsProxy columnProxy = (YadaDTColumnsProxy) column;
					String name = StringUtils.trimToNull(columnProxy.getName());
					Object data = columnProxy.getData();
					String path = name;
					if (path == null && data instanceof String dataString) {
						path = StringUtils.trimToNull(dataString);
						if (path != null && path.endsWith("()")) {
							path = null; // Javascript function: no entity value
						}
					}
					if (path == null || YadaDataTableDao.COLUMN_COMMAND.equals(path) || YadaDataTableDao.COLUMN_SELECTION.equals(path)) {
						continue;
					}
					builder.column(path, !Boolean.FALSE.equals(columnProxy.isSearchable()), !Boolean.FALSE.equals(columnProxy.isOrderable()));
				}
			}
			result = builder.build();
			queryPolicy = result;
		}
		return result;
	}
}
