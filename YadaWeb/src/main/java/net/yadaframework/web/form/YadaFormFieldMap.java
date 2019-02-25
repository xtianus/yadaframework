package net.yadaframework.web.form;

import java.util.HashMap;
import java.util.Map;

/**
 * Generic form bean for String values.
 * 
 * 
 *
 */
public class YadaFormFieldMap {
	public Map<String, String> fieldMap = new HashMap<>();

	public Map<String, String> getMap() {
		return fieldMap;
	}
}
