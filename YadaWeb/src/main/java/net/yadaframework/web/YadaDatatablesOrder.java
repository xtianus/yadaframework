package net.yadaframework.web;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a map because Spring wouldn't map it from the request otherwise
 *
 */
@SuppressWarnings("serial")
public class YadaDatatablesOrder extends HashMap<String, String> {
	private final transient Logger log = LoggerFactory.getLogger(getClass());
	
	public final static String ASC = "asc";
	public final static String DESC = "desc";
	
	/**
	 * Column number to which ordering should be applied. This is an index reference to the columns array of information that is also submitted to the server.
	 * @return column number, or -1 if no column has been set
	 */
	public int getColumnIndex() {
		try {
			return Integer.parseInt(this.get("column"));
		} catch (NumberFormatException e) {
			log.debug("Not a valid integer: {} - (returning -1)", this.get("column"));
			return -1;
		}
	}
	
	/**
	 * 
	 * @return Ordering direction for this column. It will be asc or desc to indicate ascending ordering or descending ordering, respectively.
	 */
	public String getDir() {
		return (String) this.get("dir");
	}
	
	
}