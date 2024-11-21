package net.yadaframework.raw;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implements a table with four columns: three keys and one value. The purpose is to return the fourth value given the first three ones.
 * Can't have rows with the same keys.
 * @param <K1> the type of column 1
 * @param <K2> the type of column 2
 * @param <K3> the type of column 3
 * @param <V> the type of the value
 * @see {@link YadaLookupTable} for any number of keys
 */
public class YadaLookupTableFour<K1, K2, K3, V> {
    // Use ConcurrentHashMap for thread safety
    private final Map<K1, YadaLookupTableThree<K2, K3, V>> col1 = new ConcurrentHashMap<>();

    /**
     * Add a new row to the table. No parameter can be null.
     */
    public void put(K1 key1, K2 key2, K3 key3, V value) {
        // Ensure the YadaLookupTableThree is present for key1 in a thread-safe manner
        col1.computeIfAbsent(key1, k -> new YadaLookupTableThree<>()).put(key2, key3, value);
    }

    /**
     * Get the value of the last column given the first ones
	 * @param key1 value of the first column
	 * @param key2 value of the second column
     * @param key3 value of the third column
     * @return the value of column 4, or null if not found.
     */
    public V get(K1 key1, K2 key2, K3 key3) {
        YadaLookupTableThree<K2, K3, V> col2 = col1.get(key1);
        if (col2 != null) {
            return col2.get(key2, key3);
        }
        return null;
    }
    
	/**
	 * Get a table of all the rows that match the first column value, excluding the first column.
	 * @param key1 the value of the first column
	 * @return a table of the following columns for the given value of the first column, or null if not found.
	 */
	public YadaLookupTableThree<K2, K3, V> getSubtable(K1 key1) {
		return col1.get(key1);
	}
    
	/**
	 * Clear the table
	 */
	public void clear() {
		// No need to clear the inner tables because they become unreferenced
		col1.clear();
	}
}
