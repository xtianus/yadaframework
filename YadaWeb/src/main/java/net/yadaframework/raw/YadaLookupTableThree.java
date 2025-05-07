package net.yadaframework.raw;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implements a table with three columns: two keys and one value. The purpose is to return the third value given the first two ones.
 * Can't have rows with the same keys.
 * @param <K1> the type of column 1
 * @param <K2> the type of column 2
 * @param <V> the type of the value
 * @see {@link YadaLookupTable} for any number of keys
 */
public class YadaLookupTableThree<K1, K2, V> {
	Map<K1, Map<K2, V>> col1 = new ConcurrentHashMap<>();

	/**
	 * Add a new row to the table. No parameter can be null.
	 */
	public void put(K1 key1, K2 key2, V value) {
		col1.computeIfAbsent(key1, k -> new ConcurrentHashMap<>()).put(key2, value);
		}
	
	/**
	 * Get the value of the last column given the first ones
	 * @param key1 value of the first column
	 * @param key2 value of the second column
	 * @return the value of column 3, or null if not found.
	 */
	public V get(K1 key1, K2 key2) {
		Map<K2, V> col2 = col1.get(key1);
		if (col2!=null) {
			return col2.get(key2);
		}
		return null;
	}
	
	/**
	 * Get a table of all the rows that match the first column value, excluding the first column.
	 * @param key1 the value of the first column
	 * @return a map of the following columns for the given value of the first column, or null if not found.
	 */
	public Map<K2, V> getSubtable(K1 key1) {
		return col1.get(key1);
	}
	
	/**
	 * Clear the table
	 */
	public void clear() {
		col1.clear();
	}
	
	public boolean isEmpty() {
		return col1.isEmpty();
	}
	
	/**
	 * Returns a Collection of the values contained in this table (i.e. the last column). 
	 * The collection is not backed by the table, so changes to the table are not reflected in the collection.
	 */
	public Collection<V> values() {
		List <V> result = new ArrayList<>();
		for (Map<K2, V> col2 : col1.values()) {
			result.addAll(col2.values());
		}
		return result;
	}
	
}
