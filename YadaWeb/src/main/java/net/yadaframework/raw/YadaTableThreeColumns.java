package net.yadaframework.raw;

import java.util.HashMap;
import java.util.Map;

/**
 * Implements a table with three columns: two keys and one value. The purpose is to return the third value given the first two ones.
 * Can't have rows with the same keys.
 * @param <K1> the type of column 1
 * @param <K2> the type of column 2
 * @param <V> the type of the value
 */
public class YadaTableThreeColumns<K1, K2, V> {
	Map<K1, Map<K2, V>> col1 = new HashMap<>();

	/**
	 * Add a new row to the table. Any value can be null.
	 */
	public void put(K1 key1, K2 key2, V value) {
		Map<K2, V> col2 = col1.get(key1);
		if (col2==null) {
			col2 = new HashMap<>();
			col1.put(key1, col2);
		}
		col2.put(key2, value);
	}
	
	/**
	 * Get the value of the last column given the first ones
	 * @param key1 can be null
	 * @param key2 can be null
	 * @return the value of column 3, or null
	 */
	public V get(K1 key1, K2 key2) {
		Map<K2, V> col2 = col1.get(key1);
		if (col2!=null) {
			return col2.get(key2);
		}
		return null;
	}
	
}
