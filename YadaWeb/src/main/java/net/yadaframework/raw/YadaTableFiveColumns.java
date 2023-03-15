package net.yadaframework.raw;

import java.util.HashMap;
import java.util.Map;

/**
 * Implements a table with five columns: four keys and one value. The purpose is to return the fifth value given the first four ones.
 * Can't have rows with the same keys.
 * @param <K1> the type of column 1
 * @param <K2> the type of column 2
 * @param <K3> the type of column 3
 * @param <K4> the type of column 4
 * @param <V> the type of the value
 */
public class YadaTableFiveColumns<K1, K2, K3, K4, V> {
	Map<K1, YadaTableFourColumns<K2, K3, K4, V>> col1 = new HashMap<>();

	/**
	 * Add a new row to the table. Any value can be null.
	 */
	public void put(K1 key1, K2 key2, K3 key3, K4 key4, V value) {
		YadaTableFourColumns<K2, K3, K4, V> col2 = col1.get(key1);
		if (col2==null) {
			col2 = new YadaTableFourColumns<>();
			col1.put(key1, col2);
		}
		col2.put(key2, key3, key4, value);
	}
	
	/**
	 * Get the value of the last column given the first ones
	 * @param key1 can be null
	 * @param key2 can be null
	 * @param key3 can be null
	 * @param key4 can be null
	 * @return the value of column 5, or null
	 */
	public V get(K1 key1, K2 key2, K3 key3, K4 key4) {
		YadaTableFourColumns<K2, K3, K4, V> col2 = col1.get(key1);
		if (col2!=null) {
			return col2.get(key2, key3, key4);
		}
		return null;
	}
	
}
