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
     * Add a new row to the table. Any value can be null.
     */
    public void put(K1 key1, K2 key2, K3 key3, V value) {
        // Ensure the YadaLookupTableThree is present for key1 in a thread-safe manner
        col1.computeIfAbsent(key1, k -> new YadaLookupTableThree<>()).put(key2, key3, value);
    }

    /**
     * Get the value of the last column given the first ones
     * @param key1 can be null
     * @param key2 can be null
     * @param key3 can be null
     * @return the value of column 4, or null
     */
    public V get(K1 key1, K2 key2, K3 key3) {
        YadaLookupTableThree<K2, K3, V> col2 = col1.get(key1);
        if (col2 != null) {
            return col2.get(key2, key3);
        }
        return null;
    }
}
