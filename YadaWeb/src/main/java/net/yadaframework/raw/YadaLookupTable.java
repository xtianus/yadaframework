package net.yadaframework.raw;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lookup table with any number of keys. The key and the value can be of a specified type.
 * @param <K> the type of the keys, can be Object for no type checking (i.e. to use any different objects types as keys)
 * @param <V> the type of the value, can be Object to store any different object types in different rows
 * @see {@link YadaLookupTableThree} to use two keys of different type
 * @see {@link YadaLookupTableFour} to use three keys of different type
 * @see {@link YadaLookupTableFive} to use four keys of different type
 * @see {@link YadaLookupTableSix} to use five keys of different type
 */
public class YadaLookupTable<K, V> {
    private final Map<Object, Object> table = new ConcurrentHashMap<>();

    /**
     * Add a new row to the table using variable-length keys.
     * Note that the value comes before the keys.
     * @param value The value to insert.
     * @param keys  The keys to use for indexing, provided as varargs.
     */
	@SafeVarargs
    public final void put(V value, K... keys) {
        Map<Object, Object> currentMap = table;

        // Traverse the map using the keys, creating new maps if necessary
        for (int i = 0; i < keys.length - 1; i++) {
            currentMap = (Map<Object, Object>) currentMap.computeIfAbsent(keys[i], k -> new ConcurrentHashMap<>());
        }

        // Insert the value at the last key
        currentMap.put(keys[keys.length - 1], value);
    }

    /**
     * Retrieve the value from the table given the keys.
     * @param keys The keys to use for lookup.
     * @return The value corresponding to the keys, or null if not found.
     */
    @SafeVarargs
    public final V get(K... keys) {
        Map<Object, Object> currentMap = table;

        // Traverse the map using the keys
        for (int i = 0; i < keys.length - 1; i++) {
            currentMap = (Map<Object, Object>) currentMap.get(keys[i]);
            if (currentMap == null) {
                return null;
            }
        }

        // Return the value at the final key
        return (V) currentMap.get(keys[keys.length - 1]);
    }
}

