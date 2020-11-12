package rocks.tbog.tblauncher.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.core.app.ShareCompat;
import androidx.core.content.FileProvider;
import androidx.core.content.res.ResourcesCompat;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.concurrent.Executor;

import rocks.tbog.tblauncher.result.ResultViewHelper;
import rocks.tbog.tblauncher.ui.CutoutFactory;
import rocks.tbog.tblauncher.ui.ICutout;

public class Utilities {
    final static Executor EXECUTOR_RUN_ASYNC = AsyncTask.THREAD_POOL_EXECUTOR;
    private final static int[] ON_SCREEN_POS = new int[2];
    private final static Rect ON_SCREEN_RECT = new Rect();

    // https://stackoverflow.com/questions/3035692/how-to-convert-a-drawable-to-a-bitmap
    @NonNull
    public static Bitmap drawableToBitmap(@Nullable Drawable drawable) {
        Bitmap bitmap;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if (drawable == null || drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        if (drawable != null) {
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
        } else {
            canvas.drawRGB(255, 255, 255);
        }
        return bitmap;
    }

    /**
     * Returns a drawable suitable for the all apps view. If the package or the resource do not
     * exist, it returns null.
     */
    public static Drawable createIconDrawable(Intent.ShortcutIconResource iconRes, Context context) {
        PackageManager packageManager = context.getPackageManager();
        // the resource
        try {
            Resources resources = packageManager.getResourcesForApplication(iconRes.packageName);
            final int id = resources.getIdentifier(iconRes.resourceName, null, null);
            return ResourcesCompat.getDrawableForDensity(resources, id, 0, null);
        } catch (Exception e) {
            // Icon not found.
        }
        return null;
    }

    /**
     * Returns a drawable which is of the appropriate size to be displayed as an icon
     */
    public static Drawable createIconDrawable(Bitmap icon, Context context) {
        return new BitmapDrawable(context.getResources(), icon);
    }

    @NonNull
    public static ICutout getNotchCutout(Activity activity) {
        ICutout cutout;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            cutout = CutoutFactory.getForAndroidPie(activity);
        else
            cutout = CutoutFactory.getByManufacturer(activity, Build.MANUFACTURER);

        return cutout == null ? CutoutFactory.getNoCutout() : cutout;
    }

    public static void setIconAsync(@NonNull ImageView image, @NonNull GetDrawable callback) {
        new Utilities.AsyncSetDrawable(image) {
            @Override
            protected Drawable getDrawable(Context context) {
                return callback.getDrawable(context);
            }
        }.executeOnExecutor(ResultViewHelper.EXECUTOR_LOAD_ICON);
    }

    public static void setIntentSourceBounds(@NonNull Intent intent, @NonNull View v) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            v.getLocationOnScreen(ON_SCREEN_POS);
            ON_SCREEN_RECT.set(ON_SCREEN_POS[0], ON_SCREEN_POS[1], ON_SCREEN_POS[0] + v.getWidth(), ON_SCREEN_POS[1] + v.getHeight());
            intent.setSourceBounds(ON_SCREEN_RECT);
        }
    }

    @Nullable
    public static Rect getOnScreenRect(@Nullable View v) {
        if (v == null)
            return null;
        v.getLocationOnScreen(ON_SCREEN_POS);
        ON_SCREEN_RECT.set(ON_SCREEN_POS[0], ON_SCREEN_POS[1], ON_SCREEN_POS[0] + v.getWidth(), ON_SCREEN_POS[1] + v.getHeight());
        return ON_SCREEN_RECT;
    }

    public static boolean checkFlag(int flags, int flagToCheck) {
        return (flags & flagToCheck) == flagToCheck;
    }

    public static boolean checkAnyFlag(int flags, int anyFlag) {
        return (flags & anyFlag) != 0;
    }

    /**
     * Return a valid activity or null given a view
     *
     * @param view any view of an activity
     * @return an activity or null
     */
    @Nullable
    public static Activity getActivity(@Nullable View view) {
        return view != null ? getActivity(view.getContext()) : null;
    }

    /**
     * Return a valid activity or null given a context
     *
     * @param ctx context
     * @return an activity or null
     */
    @Nullable
    public static Activity getActivity(@Nullable Context ctx) {
        while (ctx instanceof ContextWrapper) {
            if (ctx instanceof Activity) {
                Activity act = (Activity) ctx;
                if (act.isFinishing() || act.isDestroyed())
                    return null;
                return act;
            }
            ctx = ((ContextWrapper) ctx).getBaseContext();
        }
        return null;
    }

    public static void positionToast(@NonNull Toast toast, @NonNull View anchor, int offsetX, int offsetY) {
        // toasts are positioned relatively to decor view, views relatively to their parents, we have to gather additional data to have a common coordinate system
        Rect rect = new Rect();
        anchor.getWindowVisibleDisplayFrame(rect);

        // covert anchor view absolute position to a position which is relative to decor view
        int[] viewLocation = new int[2];
        anchor.getLocationOnScreen(viewLocation);
        int viewLeft = viewLocation[0] - rect.left;
        int viewTop = viewLocation[1] - rect.top;

        // measure toast to center it relatively to the anchor view
        DisplayMetrics metrics = new DisplayMetrics();
        anchor.getDisplay().getMetrics(metrics);
        int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(metrics.widthPixels, View.MeasureSpec.UNSPECIFIED);
        int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(metrics.heightPixels, View.MeasureSpec.UNSPECIFIED);
        toast.getView().measure(widthMeasureSpec, heightMeasureSpec);
        int toastWidth = toast.getView().getMeasuredWidth();

        // compute toast offsets
        int toastX = viewLeft + (anchor.getWidth() - toastWidth) / 2 + offsetX;
        int toastY = viewTop + anchor.getHeight() + offsetY;

        toast.setGravity(Gravity.START | Gravity.TOP, toastX, toastY);
    }

    public static Utilities.AsyncRun runAsync(@NonNull AsyncRun.Run background, @Nullable AsyncRun.Run after) {
        return (Utilities.AsyncRun) new Utilities.AsyncRun(background, after).executeOnExecutor(EXECUTOR_RUN_ASYNC);
    }

    public static void setColorFilterMultiply(@NonNull ImageView imageView, int color) {
        setColorFilterMultiply(imageView.getDrawable(), color);
    }

    public static void setColorFilterMultiply(@Nullable Drawable drawable, int color) {
        if (drawable == null)
            return;
        ColorFilter cf = new PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY);
        drawable.setColorFilter(cf);
    }

    @SuppressLint("ObsoleteSdkInt")
    public static void expandNotificationsPanel(Activity activity) {
        @SuppressLint("WrongConstant")
        Object statusBarService = activity.getSystemService("statusbar");
        if (statusBarService != null) {
            try {
                Class<?> statusbarManager = Class.forName("android.app.StatusBarManager");
                Method expand;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    expand = statusbarManager.getMethod("expandNotificationsPanel");
                } else {
                    expand = statusbarManager.getMethod("expand");
                }
                expand.invoke(statusBarService);
            } catch (Exception ignored) {
            }
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    public static void expandSettingsPanel(Activity activity) {
        @SuppressLint("WrongConstant")
        Object statusBarService = activity.getSystemService("statusbar");
        if (statusBarService != null) {
            try {
                Class<?> statusbarManager = Class.forName("android.app.StatusBarManager");
                Method expand;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    expand = statusbarManager.getMethod("expandSettingsPanel");
                } else {
                    expand = statusbarManager.getMethod("expand");
                }
                expand.invoke(statusBarService);
            } catch (Exception ignored) {
            }
        }
    }

    public static int getNextCodePointIndex(CharSequence s, int startPosition) {
        int codePoint = Character.codePointAt(s, startPosition);
        int next = startPosition + Character.charCount(codePoint);

        if (next < s.length()) {
            // skip next character if it's not helpful
            codePoint = Character.codePointAt(s, next);
            boolean skip = codePoint == 0x200D;
            skip = skip || Character.UnicodeBlock.of(codePoint) == Character.UnicodeBlock.VARIATION_SELECTORS;
            if (skip)
                return getNextCodePointIndex(s, next);
        }

        return next;
    }

    public static int codePointsLength(CharSequence s) {
        int n = 0;
        for (int i = 0; i < s.length(); ) {
            int codePoint = Character.codePointAt(s, i);
            i += Character.charCount(codePoint);
            // skip this if it's ZERO WIDTH JOINER
            if (codePoint == 0x200D)
                continue;
            if (Character.UnicodeBlock.of(codePoint) == Character.UnicodeBlock.VARIATION_SELECTORS)
                continue;
            ++n;
        }
        return n;
    }

    @Nullable
    public static byte[] decodeIcon(@Nullable String text, @Nullable String encoding) {
        if (text != null) {
            text = text.trim();
            int size = text.length();
            if (encoding == null || "base64".equals(encoding)) {
                byte[] base64enc = new byte[size];
                for (int i = 0; i < size; i += 1) {
                    char c = text.charAt(i);
                    base64enc[i] = (byte) (c & 0xff);
                }
                return Base64.decode(base64enc, Base64.NO_WRAP);
            }
        }
        return null;
    }


    public interface GetDrawable {
        @Nullable
        Drawable getDrawable(@NonNull Context context);
    }

    public static abstract class AsyncSetDrawable extends AsyncTask<Void, Void, Drawable> {
        protected final WeakReference<ImageView> weakImage;

        protected AsyncSetDrawable(@NonNull ImageView image) {
            super();
            if (image.getTag() instanceof ResultViewHelper.AsyncSetEntryDrawable)
                ((ResultViewHelper.AsyncSetEntryDrawable) image.getTag()).cancel(true);
            image.setTag(this);
            image.setImageResource(android.R.color.transparent);
            this.weakImage = new WeakReference<>(image);
        }

        @Override
        protected Drawable doInBackground(Void... voids) {
            ImageView image = weakImage.get();
            Activity act = Utilities.getActivity(image);
            if (isCancelled() || act == null || image.getTag() != this) {
                weakImage.clear();
                return null;
            }

            Context ctx = image.getContext();
            return getDrawable(ctx);
        }

        @WorkerThread
        protected abstract Drawable getDrawable(Context context);

        @Override
        protected void onPostExecute(Drawable drawable) {
            ImageView image = weakImage.get();
            Activity act = Utilities.getActivity(image);
            if (act == null || drawable == null) {
                weakImage.clear();
                return;
            }
            image.setImageDrawable(drawable);
            image.setTag(null);
        }
    }

    public static class AsyncRun extends AsyncTask<Void, Void, Void> {
        private final Run mBackground;
        private final Run mAfter;

        public interface Run {
            void run(@NonNull Utilities.AsyncRun task);
        }

        public AsyncRun(@NonNull Run background, @Nullable Run after) {
            super();
            mBackground = background;
            mAfter = after;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            mBackground.run(this);
            return null;
        }

        @Override
        protected void onCancelled(Void aVoid) {
            if (mAfter != null)
                mAfter.run(this);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (mAfter != null)
                mAfter.run(this);
        }

        public boolean cancel() {
            return cancel(false);
        }
    }
}
