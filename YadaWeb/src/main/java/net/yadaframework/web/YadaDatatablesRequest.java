package net.yadaframework.web;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.yadaframework.persistence.YadaSql;

/**
 * Mapped by Spring automatically from the request
 * @see http://www.datatables.net/manual/server-side
 */
public class YadaDatatablesRequest {
	@SuppressWarnings("unused")
	private final transient Logger log = LoggerFactory.getLogger(getClass());
	
	// Draw counter. This is used by DataTables to ensure that the Ajax returns from server-side processing requests are drawn in sequence by DataTables 
	int draw;
	// Paging first record indicator. This is the start point in the current data set (0 index based - i.e. 0 is the first record).
	int start;
	// Number of records that the table can display in the current draw. It is expected that the number of records returned will be equal to this number, unless the server has fewer records to return. Note that this can be -1 to indicate that all records should be returned
	int length;
	List<YadaDatatablesColumn> columns = new ArrayList<>();
	// Global search. To be applied to all columns which have searchable as true
	YadaDatatablesColumnSearch search = new YadaDatatablesColumnSearch();
	// array defining how many columns are being ordered upon - i.e. if the array length is 1, then a single column sort is being performed, otherwise a multi-column sort is being performed.
	List<YadaDatatablesOrder> order;
	Map<String, String> extraParam = new HashMap<>();
	List<String> extraJsonAttributes = new ArrayList<>();
	
	// Output values
	long recordsTotal;
	long recordsFiltered;
	
	// Query builder to use for additional JPL filtering
	private YadaSql yadaSql = YadaSql.instance();
	
	/**
	 * Add any entity attribute you need to return in the json object. Can be a path.
	 * @param attributePath
	 */
	public void addExtraJsonAttribute(String attributePath) {
		extraJsonAttributes.add(attributePath);
	}

	/**
	 * 
	 * @return Draw counter. This is used by DataTables to ensure that the Ajax returns from server-side processing requests are drawn in sequence by DataTables 
	 */
	public int getDraw() {
		return draw;
	}
	
	public void setDraw(int draw) {
		this.draw = draw;
	}
	
	/**
	 * 
	 * @return Paging first record indicator. This is the start point in the current data set (0 index based - i.e. 0 is the first record).
	 */
	public int getStart() {
		return start;
	}
	
	public void setStart(int start) {
		this.start = start;
	}
	
	/**
	 * 
	 * @return Number of records that the table can display in the current draw. It is expected that the number of records returned will be equal to this number, unless the server has fewer records to return. Note that this can be -1 to indicate that all records should be returned
	 */
	public int getLength() {
		return length;
	}
	
	public void setLength(int length) {
		this.length = length;
	}

	public List<YadaDatatablesColumn> getColumns() {
		return columns;
	}

	public void setColumns(List<YadaDatatablesColumn> columns) {
		this.columns = columns;
	}

	/**
	 * 
	 * @return Global search. To be applied to all columns which have searchable as true
	 */
	public YadaDatatablesColumnSearch getSearch() {
		return search;
	}

	public void setSearch(YadaDatatablesColumnSearch search) {
		this.search = search;
	}

	public List<YadaDatatablesOrder> getOrder() {
		return order;
	}

	public void setOrder(List<YadaDatatablesOrder> order) {
		this.order = order;
	}

	public long getRecordsTotal() {
		return recordsTotal;
	}

	public void setRecordsTotal(long recordsTotal) {
		this.recordsTotal = recordsTotal;
	}

	public long getRecordsFiltered() {
		return recordsFiltered;
	}

	public void setRecordsFiltered(long recordsFiltered) {
		this.recordsFiltered = recordsFiltered;
	}

	/**
	 * Can be used to set join and where conditions on the query.
	 * Use "e" as the alias for the root table.
	 * E.g.: "e.user.role=4"
	 */
	public YadaSql getYadaSql() {
		return yadaSql;
	}

	public Map<String, String> getExtraParam() {
		return extraParam;
	}

	public void setExtraParam(Map<String, String> extraParam) {
		this.extraParam = extraParam;
	}

	public List<String> getExtraJsonAttributes() {
		return extraJsonAttributes;
	}

	public void setExtraJsonAttributes(List<String> extraJsonAttributes) {
		this.extraJsonAttributes = extraJsonAttributes;
	}
	
}
