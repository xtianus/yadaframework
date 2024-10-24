package net.yadaframework.raw;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implements a table with six columns: five keys and one value. The purpose is to return the sixth value given the first five ones.
 * Can't have rows with the same keys.
 * @param <K1> the type of column 1
 * @param <K2> the type of column 2
 * @param <K3> the type of column 3
 * @param <K4> the type of column 4
 * @param <K5> the type of column 5
 * @param <V> the type of the value
 * @see {@link YadaLookupTable} for any number of keys
 */
public class YadaLookupTableSix<K1, K2, K3, K4, K5, V> {
	Map<K1, YadaLookupTableFive<K2, K3, K4, K5, V>> col1 = new ConcurrentHashMap<>();

	/**
	 * Add a new row to the table. No parameter can be null.
	 */
	public void put(K1 key1, K2 key2, K3 key3, K4 key4, K5 key5, V value) {
        // Ensure the YadaLookupTableThree is present for key1 in a thread-safe manner
        col1.computeIfAbsent(key1, k -> new YadaLookupTableFive<>()).put(key2, key3, key4, key5, value);
	}
	
	/**
	 * Get the value of the last column given the first ones
	 * @param key1 can be null
	 * @param key2 can be null
	 * @param key3 can be null
	 * @param key4 can be null
	 * @param key5 can be null
	 * @return the value of column 6, or null if not found.
	 */
	public V get(K1 key1, K2 key2, K3 key3, K4 key4, K5 key5) {
		YadaLookupTableFive<K2, K3, K4, K5, V> col2 = col1.get(key1);
		if (col2!=null) {
			return col2.get(key2, key3, key4, key5);
		}
		return null;
	}
	
}
