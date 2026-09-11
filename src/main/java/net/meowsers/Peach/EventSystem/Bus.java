package net.meowsers.Peach.EventSystem;

import net.meowsers.Peach.Serialization.PeachSerializer;

import java.util.HashMap;
import java.util.Map;

public class Bus {

    public static final Map<String, Object> DATA = new HashMap<>();

    /**
     * Stores an object in the bus.
     */
    public static void put(String key, Object value) {
        DATA.put(key, value);
    }

    /**
     * Retrieves an object from the bus, automatically casting it to the target type.
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(String key) {
        return (T) DATA.get(key);
    }

    /**
     * Retrieves an object with a fallback default value if the key doesn't exist.
     */
    @SuppressWarnings("unchecked")
    public static <T> T getOrDefault(String key, T defaultValue) {
        return (T) DATA.getOrDefault(key, defaultValue);
    }

    /**
     * Removes an entry from the bus.
     */
    public static void remove(String key) {
        DATA.remove(key);
    }

    /**
     * Checks if a key exists in the bus.
     */
    public static boolean has(String key) {
        return DATA.containsKey(key);
    }

    public static void save(String filePath) {
        PeachSerializer.saveHashMap(filePath, DATA);
    }

    public static void load(String filePath, boolean overwrite) {
        Map<String, Object> loaded = PeachSerializer.loadHashMap(filePath, String.class, Object.class);
        if (loaded != null) {
            if (overwrite) {
                DATA.clear();
            }
            DATA.putAll(loaded);
        }
    }

    public static void load(String filePath) {
        load(filePath, true);
    }
}