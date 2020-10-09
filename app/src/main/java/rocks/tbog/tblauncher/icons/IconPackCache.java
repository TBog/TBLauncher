package rocks.tbog.tblauncher.icons;

import androidx.annotation.NonNull;
import androidx.collection.LruCache;

public class IconPackCache {
    private LruCache<String, IconPackXML> mCache = new LruCache<>(16);

    @NonNull
    public IconPackXML getIconPack(String packageName)
    {
        IconPackXML pack = mCache.get(packageName);
        if (pack == null)
        {
            pack = new IconPackXML(packageName);
            mCache.put(packageName, pack);
        }
        return pack;
    }

    public void clearCache() {
        mCache.evictAll();
    }
}
