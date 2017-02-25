package net.yadaframework.web;

import java.util.HashMap;

/**
 * This is a map because Spring wouldn't map it from the request otherwise
 *
 */
@SuppressWarnings("serial")
public class YadaDatatablesColumnSearch extends HashMap<String, Object> {
	
	/**
	 * 
	 * @return Search value to apply, can be null
	 */
	public String getValue() {
		return (String) this.get("value");
	}
	
	/**
	 *  
	 * @return true if the filter should be treated as a regular expression for advanced searching, false otherwise.
	 */
	public boolean isRegex() {
		return "true".equals(this.get("regex"));
	}
}