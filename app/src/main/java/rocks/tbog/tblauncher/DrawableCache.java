package rocks.tbog.tblauncher;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.LruCache;

import java.util.Calendar;
import java.util.Collection;

public class DrawableCache {
    private static final String TAG = "DrawCache";
    private boolean mEnabled = true;
    private final LruCache<String, DrawableInfo> mCache = new LruCache<>(16);

    public void setSize(int maxSize) {
        mCache.resize(maxSize);
    }

    public void setCalendar(String cacheId) {
        synchronized (mCache) {
            DrawableInfo info = mCache.get(cacheId);
            if (info != null)
                info.setToday();
        }
    }

    @Nullable
    public Drawable getCachedDrawable(@NonNull String cacheId) {
        synchronized (mCache) {
            DrawableInfo info = mCache.get(cacheId);
            if (info != null) {
                if (info.isToday())
                    return info.drawable;
                mCache.remove(cacheId);
            }
        }
        return null;
    }

//    @Nullable
//    public DrawableInfo getCachedInfo(@NonNull String cacheId) {
//        return mCache.get(cacheId);
//    }

    public void setCachedInfo(@NonNull String cacheId, @Nullable DrawableInfo cache) {
        synchronized (mCache) {
            if (cache == null) {
                mCache.remove(cacheId);
                return;
            }
            if (!mEnabled)
                return;
            mCache.put(cacheId, cache);
        }
    }

    public void cacheDrawable(@NonNull String cacheId, @Nullable Drawable drawable) {
        synchronized (mCache) {
            if (drawable == null) {
                mCache.remove(cacheId);
                return;
            }
            if (!mEnabled)
                return;
            DrawableInfo info = new DrawableInfo(drawable);
            mCache.put(cacheId, info);
        }
    }

    public void clearCache() {
        synchronized (mCache) {
            mCache.evictAll();
        }
    }

    public void onPrefChanged(Context ctx, SharedPreferences pref) {
        boolean enabled = pref.getBoolean("cache-drawable", true);
        if (enabled != mEnabled) {
            mEnabled = enabled;
            clearCache();
        }
        boolean halfSize = pref.getBoolean("cache-half-apps", true);
        Collection<?> apps = TBApplication.appsHandler(ctx).getAllApps();
        int size = apps.size();
        size = size < 16 ? 16 : halfSize ? (size / 2) : (size * 115 / 100);
        Log.i(TAG, "Cache size: " + size);
        synchronized (mCache) {
            mCache.resize(size);
        }
    }

    public static class DrawableInfo {
        public final Drawable drawable;
        public int dayOfMonth = 0;

        public DrawableInfo(Drawable drawable) {
            this.drawable = drawable;
        }

        /**
         * Set day for cached drawable. This is a number indicating the day of the month.
         * The first day of the month has value 1.
         */
        public void setToday() {
            dayOfMonth = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
        }

        public boolean isToday() {
            if (dayOfMonth == 0)
                return true;
            return dayOfMonth == Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
        }
    }
}
