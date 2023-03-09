package net.yadaframework.raw;

import java.util.HashMap;
import java.util.Map;

/**
 * Implements a table with 6 columns. The purpose is to return the last value given the first ones.
 * @param <K1> the type of column 1
 * @param <K2> the type of column 2
 * @param <K3> the type of column 3
 * @param <K4> the type of column 4
 * @param <K5> the type of column 5
 * @param <K6> the type of column 6
 */
public class YadaTableSixColumns<K1, K2, K3, K4, K5, K6> {
	Map<K1, Map<K2, Map<K3, Map<K4, Map<K5, K6>>>>> col1 = new HashMap<>();

	/**
	 * Add a new row to the table. Any value can be null.
	 * @param v1
	 * @param v2
	 * @param v3
	 * @param v4
	 * @param v5
	 * @param v6
	 */
	public void add(K1 v1, K2 v2, K3 v3, K4 v4, K5 v5, K6 v6) {
		Map<K2, Map<K3, Map<K4, Map<K5, K6>>>> col2 = col1.get(v1);
		if (col2==null) {
			col2 = new HashMap<>();
			col1.put(v1, col2);
		}
		Map<K3,Map<K4, Map<K5, K6>>> col3 = col2.get(v2);
		if (col3==null) {
			col3 = new HashMap<>();
			col2.put(v2, col3);
		}
		Map<K4, Map<K5, K6>> col4 = col3.get(v3);
		if (col4==null) {
			col4 = new HashMap<>();
			col3.put(v3, col4);
		}
		Map<K5, K6> col5 = col4.get(v4);
		if (col5==null) {
			col5 = new HashMap<>();
			col4.put(v4, col5);
		}
		col5.put(v5, v6);
	}
	
	/**
	 * Get the value of the last column given the first ones
	 * @param v1 can be null
	 * @param v2 can be null
	 * @param v3 can be null
	 * @param v4 can be null
	 * @param v5 can be null
	 * @return the value of column 6, or null
	 */
	public K6 get(K1 v1, K2 v2, K3 v3, K4 v4, K5 v5) {
		Map<K2, Map<K3, Map<K4, Map<K5, K6>>>> col2 = col1.get(v1);
		if (col2!=null) {
			Map<K3, Map<K4, Map<K5, K6>>> col3 = col2.get(v2);
			if (col3!=null) {
				Map<K4, Map<K5, K6>> col4 = col3.get(v3);
				if (col4!=null) {
					Map<K5, K6> col5 = col4.get(v4);
					if (col5!=null) {
						return col5.get(v5);
					}
				}
			}
		}
		return null;
	}
	
}
