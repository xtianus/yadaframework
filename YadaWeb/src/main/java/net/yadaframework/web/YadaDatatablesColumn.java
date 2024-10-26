package net.yadaframework.web;

import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;

/**
 * This is a map because Spring wouldn't map it from the request otherwise
 *
 */
@SuppressWarnings("serial")
public class YadaDatatablesColumn extends HashMap<String, Object> {
	
	public YadaDatatablesColumn() {
		this.put("search", new YadaDatatablesColumnSearch());
	}
	
	public YadaDatatablesColumnSearch getSearch() {
		return (YadaDatatablesColumnSearch) this.get("search");
	}
	
	/**
	 * Returns the column data if any, the column name otherwise.
	 * @return data or name or null
	 */
	// Never used
	public String getDataOrName() {
		String result = StringUtils.trimToNull(getData());
		if (result==null) {
			result = StringUtils.trimToNull(getName());
		}
		return result;
	}
	
	/**
	 * Returns the column name if any, the column data otherwise.
	 * @return name or data or null
	 */
	public String getNameOrData() {
		String result = StringUtils.trimToNull(getName());
		if (result==null) {
			result = StringUtils.trimToNull(getData());
		}
		return result;
	}
	
	/**
	 * 
	 * @return Column's data source, as defined by columns.data
	 */
	public String getData() {
		return (String) this.get("data");
	}
	
	/**
	 * 
	 * @return Column's name, as defined by columns.name
	 */
	public String getName() {
		return (String) this.get("name");
	}
	
	/**
	 * 
	 * @return Flag to indicate if this column is searchable (true) or not (false). This is controlled by columns.searchable
	 */
	public boolean isSearchable() {
		return "true".equals(this.get("searchable"));
	}
	
	/**
	 * 
	 * @return Flag to indicate if this column is orderable (true) or not (false). This is controlled by columns.orderable
	 */
	public boolean isOrderable() {
		return "true".equals(this.get("orderable"));
	}
	
}