package net.yadaframework.raw;

import java.util.HashMap;
import java.util.Map;

/**
 * Implements a table with four columns: three keys and one value. The purpose is to return the fourth value given the first three ones.
 * Can't have rows with the same keys.
 * @param <K1> the type of column 1
 * @param <K2> the type of column 2
 * @param <K3> the type of column 3
 * @param <V> the type of the value
 */
public class YadaTableFourColumns<K1, K2, K3, V> {
	Map<K1, YadaTableThreeColumns<K2, K3, V>> col1 = new HashMap<>();

	/**
	 * Add a new row to the table. Any value can be null.
	 */
	public void put(K1 key1, K2 key2, K3 key3, V value) {
		YadaTableThreeColumns<K2, K3, V> col2 = col1.get(key1);
		if (col2==null) {
			col2 = new YadaTableThreeColumns<>();
			col1.put(key1, col2);
		}
		col2.put(key2, key3, value);
	}
	
	/**
	 * Get the value of the last column given the first ones
	 * @param key1 can be null
	 * @param key2 can be null
	 * @param key3 can be null
	 * @return the value of column 4, or null
	 */
	public V get(K1 key1, K2 key2, K3 key3) {
		YadaTableThreeColumns<K2, K3, V> col2 = col1.get(key1);
		if (col2!=null) {
			return col2.get(key2, key3);
		}
		return null;
	}
	
}
