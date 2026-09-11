package net.meowsers.Peach.GameEngine;

import net.meowsers.Peach.Utils.Disposable;
import net.meowsers.Peach.Utils.PeachException;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.Map;

public class Assets {

    // Central cache map keyed by normalized file path
    private static final Map<String, Object> CACHE = new ConcurrentHashMap<>();

    /**
     * Retrieves an asset from the cache, or loads it using the provided loader function if missing.
     *
     * @param path   The normalized file path / key
     * @param loader Factory function to load the asset if not cached (e.g. Texture::new)
     * @param <T>    Asset type
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(String path, Function<String, T> loader) {
        String key = normalizePath(path);

        return (T) CACHE.computeIfAbsent(key, k -> {
            T asset = loader.apply(k);
            if (asset == null) {
                throw new PeachException("Failed to load asset at path: " + k);
            }
            return asset;
        });
    }

    public static boolean contains(String path) {
        return CACHE.containsKey(normalizePath(path));
    }

    public static void unload(String path) {
        String key = normalizePath(path);
        Object asset = CACHE.remove(key);
        if (asset instanceof Disposable disposable) {
            disposable.dispose();
        }
    }

    public static void clear() {
        for (Object asset : CACHE.values()) {
            if (asset instanceof Disposable disposable) {
                disposable.dispose();
            }
        }
        CACHE.clear();
    }

    private static String normalizePath(String path) {
        return path.replace('\\', '/').trim();
    }
}