package rocks.tbog.tblauncher.utils;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.lang.ref.WeakReference;

import rocks.tbog.tblauncher.result.ResultViewHelper;
import rocks.tbog.tblauncher.ui.CutoutFactory;
import rocks.tbog.tblauncher.ui.ICutout;

public class Utilities {
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
            return resources.getDrawableForDensity(id, 0);
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
        }.execute();
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

    @Nullable
    public static Activity getActivity(@Nullable Context ctx) {
        while (ctx instanceof ContextWrapper) {
            if (ctx instanceof Activity)
                return (Activity) ctx;
            ctx = ((ContextWrapper) ctx).getBaseContext();
        }
        return null;
    }

    public static void positionToast(Toast toast, View anchor, int offsetX, int offsetY) {
        Activity activity = Utilities.getActivity(anchor.getContext());
        if (activity != null)
            positionToast(toast, anchor, activity.getWindow(), offsetX, offsetY);
    }

    public static void positionToast(Toast toast, View anchor, Window window, int offsetX, int offsetY) {
        // toasts are positioned relatively to decor view, views relatively to their parents, we have to gather additional data to have a common coordinate system
        Rect rect = new Rect();
        window.getDecorView().getWindowVisibleDisplayFrame(rect);

        // covert anchor view absolute position to a position which is relative to decor view
        int[] viewLocation = new int[2];
        anchor.getLocationOnScreen(viewLocation);
        int viewLeft = viewLocation[0] - rect.left;
        int viewTop = viewLocation[1] - rect.top;

        // measure toast to center it relatively to the anchor view
        DisplayMetrics metrics = new DisplayMetrics();
        window.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(metrics.widthPixels, View.MeasureSpec.UNSPECIFIED);
        int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(metrics.heightPixels, View.MeasureSpec.UNSPECIFIED);
        toast.getView().measure(widthMeasureSpec, heightMeasureSpec);
        int toastWidth = toast.getView().getMeasuredWidth();

        // compute toast offsets
        int toastX = viewLeft + (anchor.getWidth() - toastWidth) / 2 + offsetX;
        int toastY = viewTop + anchor.getHeight() + offsetY;

        toast.setGravity(Gravity.START | Gravity.TOP, toastX, toastY);
    }

    public static Utilities.AsyncRun runAsync(Runnable background, Runnable after) {
        return (Utilities.AsyncRun) new Utilities.AsyncRun(background, after).execute();
    }

    public interface GetDrawable {
        @Nullable
        Drawable getDrawable(@NonNull Context context);
    }

    public static abstract class AsyncSetDrawable extends AsyncTask<Void, Void, Drawable> {
        final WeakReference<ImageView> weakImage;

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
            if (isCancelled() || image == null || image.getTag() != this) {
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
            if (image == null || drawable == null) {
                weakImage.clear();
                return;
            }
            image.setImageDrawable(drawable);
            image.setTag(null);
        }
    }

    public static class AsyncRun extends AsyncTask<Void, Void, Void> {
        private final Runnable mBackground;
        private final Runnable mAfter;

        public AsyncRun(@NonNull Runnable background, @Nullable Runnable after) {
            super();
            mBackground = background;
            mAfter = after;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            mBackground.run();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (mAfter != null)
                mAfter.run();
        }
    }
}
