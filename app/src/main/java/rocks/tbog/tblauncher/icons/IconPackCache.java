package rocks.tbog.tblauncher.icons;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.LruCache;

import java.lang.ref.SoftReference;
import java.util.HashMap;

import rocks.tbog.tblauncher.TBApplication;

public class IconPackCache {
    private final SoftReferenceCache<String, IconPackXML> mCache = new SoftReferenceCache<>();

    @NonNull
    public IconPackXML getIconPack(String packageName) {
        IconPackXML pack = mCache.get(packageName);
        if (pack == null) {
            pack = new IconPackXML(packageName);
            mCache.put(packageName, pack);
        }
        return pack;
    }

    public void clearCache(TBApplication app) {
        mCache.evictAll();
        IconPackXML customIconPack = app.iconsHandler().getCustomIconPack();
        if (customIconPack != null)
            mCache.put(customIconPack.getPackPackageName(), customIconPack);
    }


    /**
     * SoftReferenceCache
     *
     * @param <K> The type of the key's.
     * @param <V> The type of the value's.
     */
    static class SoftReferenceCache<K, V> {
        private final HashMap<K, SoftReference<V>> mCache = new HashMap<>();

        /**
         * Put a new item in the cache. This item can be gone after a GC run.
         *
         * @param key   The key of the value.
         * @param value The value to store.
         */
        public void put(K key, V value) {
            mCache.put(key, new SoftReference<>(value));
        }

        /**
         * Retrieve a value from the cache (if available).
         *
         * @param key The key to look for.
         * @return The value if it's found. Return null if the key-value pair is not stored yet or the GC has removed the value from memory.
         */
        @Nullable
        public V get(K key) {
            V value = null;

            SoftReference<V> reference = mCache.get(key);

            if (reference != null) {
                value = reference.get();
            }

            if (value == null)
                mCache.remove(key);

            return value;
        }

        public void evictAll() {
            mCache.clear();
        }
    }
}
