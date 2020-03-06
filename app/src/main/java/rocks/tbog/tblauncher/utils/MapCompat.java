package rocks.tbog.tblauncher.utils;

import androidx.annotation.NonNull;

import java.util.Map;

public class MapCompat {

    public static <K, V> V getOrDefault(@NonNull Map<K, V> map, K key, V defaultValue) {
        V v = map.get(key);
        return ((v != null) || map.containsKey(key)) ? v : defaultValue;
    }
}