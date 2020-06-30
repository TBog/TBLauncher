package rocks.tbog.tblauncher;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.LruCache;

import java.util.List;

public class DrawableCache {
    private static final String TAG = "DrawCache";
    private boolean mEnabled = true;
    private LruCache<String, DrawableInfo> mCache = new LruCache<>(16);

    public void setSize(int maxSize) {
        mCache.resize(maxSize);
    }

    private static class DrawableInfo {

        final Drawable drawable;

        DrawableInfo(Drawable drawable) {
            this.drawable = drawable;
        }

    }

    public void cacheDrawable(@NonNull String name, @Nullable Drawable drawable) {
        if (drawable == null) {
            mCache.remove(name);
            return;
        }
        if (!mEnabled)
            return;
        DrawableInfo info = new DrawableInfo(drawable);
        mCache.put(name, info);
    }

    @Nullable
    public Drawable getCachedDrawable(@NonNull String name) {
        DrawableInfo info = mCache.get(name);
        if (info != null)
            return info.drawable;
        return null;
    }

    public void clearCache() {
        mCache.evictAll();
    }

    public void onPrefChanged(Context ctx, SharedPreferences pref) {
        boolean enabled = pref.getBoolean("cache-drawable", true);
        if (enabled != mEnabled) {
            mEnabled = enabled;
            clearCache();
        }
        boolean halfSize = pref.getBoolean("cache-half-apps", true);
        List<?> apps = TBApplication.dataHandler(ctx).getApplications();
        int size = apps == null ? 0 : apps.size();
        size = size < 16 ? 16 : halfSize ? (size / 2) : (size * 115 / 100);
        Log.i(TAG, "Cache size: " + size);
        mCache.resize(size);
    }
}
