package rocks.tbog.tblauncher.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityOptions;
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
import android.graphics.drawable.Animatable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.GestureDetectorCompat;
import androidx.lifecycle.Lifecycle;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import rocks.tbog.tblauncher.WorkAsync.AsyncTask;
import rocks.tbog.tblauncher.WorkAsync.RunnableTask;
import rocks.tbog.tblauncher.WorkAsync.TaskRunner;
import rocks.tbog.tblauncher.result.ResultViewHelper;
import rocks.tbog.tblauncher.ui.CutoutFactory;
import rocks.tbog.tblauncher.ui.ICutout;

public class Utilities {
    public final static ExecutorService EXECUTOR_RUN_ASYNC;
    private final static int[] ON_SCREEN_POS = new int[2];
    private final static Rect ON_SCREEN_RECT = new Rect();
    private static final String TAG = "TBUtil";

    private static final int CORE_POOL_SIZE = 1;
    private static final int MAXIMUM_POOL_SIZE = 10;
    private static final int KEEP_ALIVE_SECONDS = 3;
    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        public Thread newThread(Runnable r) {
            return new Thread(r, "UtilAsync #" + mCount.getAndIncrement());
        }
    };

    static {
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(), sThreadFactory);
        threadPoolExecutor.setRejectedExecutionHandler((runnable, executor) -> {
            Log.w(TAG, "task rejected");
            if (!executor.isShutdown()) {
                runnable.run();
            }
        });
        EXECUTOR_RUN_ASYNC = threadPoolExecutor;
    }

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
        TaskRunner.executeOnExecutor(ResultViewHelper.EXECUTOR_LOAD_ICON,
                new Utilities.AsyncSetDrawable(image) {
                    @Override
                    protected Drawable getDrawable(Context context) {
                        return callback.getDrawable(context);
                    }
                }
        );
    }

    public static void setViewAsync(@NonNull View image, @NonNull GetDrawable cbGet, @NonNull SetDrawable cbSet) {
        TaskRunner.executeOnExecutor(ResultViewHelper.EXECUTOR_LOAD_ICON,
                new Utilities.AsyncViewSet(image) {
                    @Override
                    protected Drawable getDrawable(Context context) {
                        return cbGet.getDrawable(context);
                    }

                    @Override
                    protected void setDrawable(@NonNull View view, @NonNull Drawable drawable) {
                        cbSet.setDrawable(view, drawable);
                    }
                }
        );
    }

    public static void setIntentSourceBounds(@NonNull Intent intent, @Nullable View v) {
        if (v == null)
            return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            v.getLocationOnScreen(ON_SCREEN_POS);
            ON_SCREEN_RECT.set(ON_SCREEN_POS[0], ON_SCREEN_POS[1], ON_SCREEN_POS[0] + v.getWidth(), ON_SCREEN_POS[1] + v.getHeight());
            intent.setSourceBounds(ON_SCREEN_RECT);
        }
    }

    @Nullable
    public static Bundle makeStartActivityOptions(@Nullable View source) {
        if (source == null)
            return null;
        Bundle opts = null;
        // If we got an icon, we create options to get a nice animation
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            opts = ActivityOptions.makeClipRevealAnimation(source, 0, 0, source.getMeasuredWidth(), source.getMeasuredHeight()).toBundle();
        }
        if (opts == null) {
            opts = ActivityOptions.makeScaleUpAnimation(source, 0, 0, source.getMeasuredWidth(), source.getMeasuredHeight()).toBundle();
        }
        return opts;
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

    public static RunnableTask runAsync(@NonNull Lifecycle lifecycle, @NonNull TaskRunner.AsyncRunnable background, @NonNull TaskRunner.AsyncRunnable after) {
        RunnableTask task = TaskRunner.newTask(lifecycle, background, after);
        EXECUTOR_RUN_ASYNC.execute(task);
        return task;
    }

    public static RunnableTask runAsync(@NonNull TaskRunner.AsyncRunnable background, @NonNull TaskRunner.AsyncRunnable after) {
        RunnableTask task = TaskRunner.newTask(background, after);
        EXECUTOR_RUN_ASYNC.execute(task);
        return task;
    }

    public static void runAsync(@NonNull Runnable background) {
        EXECUTOR_RUN_ASYNC.execute(background);
    }

    public static <I, O> void executeAsync(@NonNull AsyncTask<I, O> task) {
        TaskRunner.executeOnExecutor(EXECUTOR_RUN_ASYNC, task);
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
                expand.setAccessible(true);
                expand.invoke(statusBarService);
            } catch (Exception ignored) {
            }
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    public static void expandSettingsPanel(Activity activity) {
        boolean expandCalled = false;
        @SuppressLint("WrongConstant")
        Object statusBarService = activity.getSystemService("statusbar");
        if (statusBarService != null) {
            try {
                Class<?> statusbarManager = Class.forName("android.app.StatusBarManager");
                Method expand;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    expand = statusbarManager.getMethod("expandSettingsPanel");
                    expand.setAccessible(true);
                    expand.invoke(statusBarService);
                    expandCalled = true;
                }
            } catch (Exception ignored) {
            }
        }
        if (!expandCalled) {
            Intent settings = new Intent(android.provider.Settings.ACTION_SETTINGS);
            activity.startActivity(settings);
        }
    }

    public static void setVerticalScrollbarThumbDrawable(View scrollView, Drawable drawable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            scrollView.setVerticalScrollbarThumbDrawable(drawable);
        } else {
            try {
                //noinspection JavaReflectionMemberAccess
                Field mScrollCacheField = View.class.getDeclaredField("mScrollCache");
                mScrollCacheField.setAccessible(true);
                Object mScrollCache = mScrollCacheField.get(scrollView);
                Field scrollBarField = mScrollCache.getClass().getDeclaredField("scrollBar");
                scrollBarField.setAccessible(true);
                Object scrollBar = scrollBarField.get(mScrollCache);
                Method method = scrollBar.getClass().getDeclaredMethod("setVerticalThumbDrawable", Drawable.class);
                method.setAccessible(true);
                method.invoke(scrollBar, drawable);
            } catch (Exception ignored) {
            }
        }
    }

    public static boolean classContainsDeclaredField(@NonNull Class<?> objectClass, @NonNull String fieldName) {
        for (Field field : objectClass.getDeclaredFields()) {
            if (field.getName().equals(fieldName)) {
                return true;
            }
        }
        return false;
    }

    public static void setTextCursorDrawable(@NonNull TextView editText, Drawable drawable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            editText.setTextCursorDrawable(drawable);
        } else {
            boolean setResToNull = false;
            if (classContainsDeclaredField(TextView.class, "mCursorDrawable")) {
                try {
                    Field fmCursorDrawable = TextView.class.getDeclaredField("mCursorDrawable");
                    fmCursorDrawable.setAccessible(true);
                    fmCursorDrawable.set(editText, drawable);
                    setResToNull = true;
                } catch (Throwable t) {
                    Log.w(TAG, "set TextView mCursorDrawable", t);
                }
            }
            if (classContainsDeclaredField(TextView.class, "mCursorDrawableRes")) {
                try {
                    Field fmCursorDrawableRes = TextView.class.getDeclaredField("mCursorDrawableRes");
                    fmCursorDrawableRes.setAccessible(true);
                    if (setResToNull)
                        fmCursorDrawableRes.setInt(editText, 0);
                    else if (fmCursorDrawableRes.getInt(editText) == 0) {
                        // this resource will not get used, we just need something != 0
                        int res = android.R.drawable.divider_horizontal_dark;
                        fmCursorDrawableRes.setInt(editText, res);
                    }
                } catch (Throwable t) {
                    Log.w(TAG, "set TextView mCursorDrawableRes", t);
                }
            }
            //https://github.com/aosp-mirror/platform_frameworks_base/blob/c46c4a6765196bcabf3ea89771a1f9067b22baad/core/java/android/widget/TextView.java#L4587
            if (classContainsDeclaredField(TextView.class, "mEditor")) {
                Object mEditor = null;
                try {
                    Field fmEditor = TextView.class.getDeclaredField("mEditor");
                    fmEditor.setAccessible(true);
                    mEditor = fmEditor.get(editText);
                } catch (Throwable t) {
                    Log.w(TAG, "get TextView mEditor", t);
                }
                if (mEditor == null)
                    return;
                if (classContainsDeclaredField(mEditor.getClass(), "mCursorDrawable")) {
                    try {
                        Field fmCursorDrawable = mEditor.getClass().getDeclaredField("mCursorDrawable");
                        fmCursorDrawable.setAccessible(true);
                        fmCursorDrawable.set(mEditor, new Drawable[]{drawable, drawable});
                    } catch (Throwable t) {
                        Log.w(TAG, "set Editor mCursorDrawable[2]", t);
                    }
                }
            }
        }
    }

    private static Drawable getDrawableFromTextViewEditor(@NonNull TextView view, @NonNull String editorField) {
        Drawable drawable = null;
        Object editor = null;
        try {
            Field f_editor = TextView.class.getDeclaredField("mEditor");
            f_editor.setAccessible(true);
            editor = f_editor.get(view);
        } catch (Throwable t) {
            Log.w(TAG, "get Editor from " + view.getClass(), t);
        }
        if (editor != null && classContainsDeclaredField(editor.getClass(), editorField)) {
            try {
                Field f_handle = editor.getClass().getDeclaredField(editorField);
                f_handle.setAccessible(true);
                if (f_handle.getType().isArray()) {
                    Object drawables = f_handle.get(editor);
                    drawable = ((Drawable[]) drawables)[0];
                } else {
                    drawable = (Drawable) f_handle.get(editor);
                }
            } catch (Throwable t) {
                Log.w(TAG, "get `" + editorField + "` from " + editor.getClass(), t);
            }
        }
        return drawable;
    }

    @Nullable
    private static Drawable getDrawableFromTextView(@NonNull TextView view, @NonNull String fieldName, @NonNull String editorField) {
        Context ctx = view.getContext();
        String resFieldName = fieldName + "Res";
        if (classContainsDeclaredField(TextView.class, resFieldName)) {
            try {
                Field f_res = TextView.class.getDeclaredField(resFieldName);
                f_res.setAccessible(true);
                int res = f_res.getInt(view);
                if (res != Resources.ID_NULL) {
                    Drawable drawable = AppCompatResources.getDrawable(ctx, res);
                    if (drawable != null)
                        return drawable;
                }
            } catch (Throwable t) {
                Log.w(TAG, "get `" + resFieldName + "` from " + TextView.class, t);
            }
        }
        if (classContainsDeclaredField(TextView.class, fieldName)) {
            try {
                Field f_drawable = TextView.class.getDeclaredField(fieldName);
                f_drawable.setAccessible(true);
                Drawable drawable = (Drawable) f_drawable.get(view);
                if (drawable != null)
                    return drawable;
            } catch (Throwable t) {
                Log.w(TAG, "get `" + fieldName + "` from " + TextView.class, t);
            }
        }

        return getDrawableFromTextViewEditor(view, editorField);
    }

    public static void setTextCursorColor(@NonNull TextView editText, @ColorInt int color) {
        Context ctx = editText.getContext();
        Drawable drawable = getDrawableFromTextView(editText, "mCursorDrawable", "mCursorDrawable");
        if (drawable == null) {
            drawable = new ShapeDrawable(new RectShape());
            ((ShapeDrawable) drawable).setIntrinsicWidth(UISizes.dp2px(ctx, 2));
            ((ShapeDrawable) drawable).getPaint().setColor(color);
        }
        drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        setTextCursorDrawable(editText, drawable);
    }

    private static void setTextSelectHandle(@NonNull TextView editText, @NonNull String fieldName, @NonNull String editorField, Drawable drawable) {
        String fieldNameRes = fieldName + "Res";
        if (classContainsDeclaredField(TextView.class, fieldNameRes)) {
            try {
                Field f_handleRes = TextView.class.getDeclaredField(fieldNameRes);
                f_handleRes.setAccessible(true);
                f_handleRes.setInt(editText, 0);
            } catch (Throwable t) {
                Log.w(TAG, "set `" + fieldNameRes + "` from " + editText.getClass(), t);
            }
        }
        if (classContainsDeclaredField(TextView.class, fieldName)) {
            try {
                Field f_handle = TextView.class.getDeclaredField(fieldName);
                f_handle.setAccessible(true);
                f_handle.set(editText, drawable);
            } catch (Throwable t) {
                Log.w(TAG, "set `" + fieldName + "` from " + editText.getClass(), t);
            }
        }
        if (!classContainsDeclaredField(TextView.class, "mEditor"))
            return;
        Object editor = null;
        try {
            Field f_editor = TextView.class.getDeclaredField("mEditor");
            f_editor.setAccessible(true);
            editor = f_editor.get(editText);
        } catch (Throwable t) {
            Log.w(TAG, "get Editor from " + editText.getClass(), t);
        }
        if (editor == null)
            return;
        try {
            Field f_handle = editor.getClass().getDeclaredField(editorField);
            f_handle.setAccessible(true);
            f_handle.set(editor, drawable);
        } catch (Throwable t) {
            Log.w(TAG, "set `" + editorField + "` from " + editor.getClass(), t);
        }
    }

    public static void setTextSelectHandle(@NonNull TextView editText, Drawable left, Drawable right, Drawable center) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            editText.setTextSelectHandle(center);
            editText.setTextSelectHandleLeft(left);
            editText.setTextSelectHandleRight(right);
        } else {
            setTextSelectHandle(editText, "mTextSelectHandleLeft", "mSelectHandleLeft", left);
            setTextSelectHandle(editText, "mTextSelectHandleRight", "mSelectHandleRight", right);
            setTextSelectHandle(editText, "mTextSelectHandle", "mSelectHandleCenter", center);
        }
    }

    public static void setTextSelectHandleColor(@NonNull TextView editText, @ColorInt int color) {
        Drawable drawableLeft;
        Drawable drawableRight;
        Drawable drawableCenter;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            drawableLeft = editText.getTextSelectHandleLeft();
            drawableRight = editText.getTextSelectHandleRight();
            drawableCenter = editText.getTextSelectHandle();
        } else {
            drawableLeft = getDrawableFromTextView(editText, "mTextSelectHandleLeft", "mSelectHandleLeft");
            drawableRight = getDrawableFromTextView(editText, "mTextSelectHandleRight", "mSelectHandleRight");
            drawableCenter = getDrawableFromTextView(editText, "mTextSelectHandle", "mSelectHandleCenter");
        }
        if (drawableLeft == null)
            drawableLeft = new ColorDrawable(color);
        if (drawableRight == null)
            drawableRight = new ColorDrawable(color);
        if (drawableCenter == null)
            drawableCenter = new ColorDrawable(color);
        drawableLeft.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        drawableRight.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        drawableCenter.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        setTextSelectHandle(editText, drawableLeft, drawableRight, drawableCenter);
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

    public static int codePointsLength(@Nullable CharSequence s) {
        final int length = s != null ? s.length() : 0;
        int n = 0;
        for (int i = 0; i < length; ) {
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

    public static String getSystemProperty(String property, String defaultValue) {
        try {
            @SuppressWarnings("rawtypes") @SuppressLint("PrivateApi")
            Class clazz = Class.forName("android.os.SystemProperties");
            @SuppressWarnings("unchecked")
            Method getter = clazz.getDeclaredMethod("get", String.class);
            String value = (String) getter.invoke(null, property);
            if (value != null && !value.isEmpty()) {
                return value;
            }
        } catch (Exception ignored) {
        }
        return defaultValue;
    }

    /**
     * @param resName
     * @param c
     * @return
     */
    public static int getResId(String resName, Class<?> c) {
        try {
            Field idField = c.getDeclaredField(resName);
            return idField.getInt(idField);
        } catch (Exception e) {
            Log.w(TAG, "getResId( " + resName + " )", e);
            return -1;
        }
    }

    public static void setGestureDetectorTouchSlop(GestureDetectorCompat gestureDetector, int value) {
        try {
            Field f_mImpl = GestureDetectorCompat.class.getDeclaredField("mImpl");
            f_mImpl.setAccessible(true);
            Object mImpl = f_mImpl.get(gestureDetector);
            if (mImpl == null) {
                Log.w(TAG, f_mImpl + " is null");
                return;
            }
            Class<?> c_GDCIJellybeanMr2 = null;
            Class<?> c_GDCIBase = null;
            try {
                c_GDCIJellybeanMr2 = Class.forName(GestureDetectorCompat.class.getName() + "$GestureDetectorCompatImplJellybeanMr2");
                c_GDCIBase = Class.forName(GestureDetectorCompat.class.getName() + "$GestureDetectorCompatImplBase");
            } catch (ClassNotFoundException ignored) {
            }
            if (c_GDCIJellybeanMr2 != null && c_GDCIJellybeanMr2.isInstance(mImpl)) {
                Field f_mDetector = c_GDCIJellybeanMr2.getDeclaredField("mDetector");
                f_mDetector.setAccessible(true);

                Object mDetector = f_mDetector.get(mImpl);
                if (mDetector instanceof GestureDetector)
                    setGestureDetectorTouchSlop((GestureDetector) mDetector, value);
            } else if (c_GDCIBase != null) {
                Field f_mTouchSlopSquare = c_GDCIBase.getDeclaredField("mTouchSlopSquare");
                f_mTouchSlopSquare.setAccessible(true);
                f_mTouchSlopSquare.setInt(mImpl, value * value);
            } else {
                Log.w(TAG, "not handled: " + mImpl.getClass().toString());
            }
        } catch (NoSuchFieldException | IllegalAccessException | NullPointerException e) {
            Log.w(TAG, gestureDetector.getClass().toString(), e);
        }
    }

    public static void setGestureDetectorTouchSlop(GestureDetector gestureDetector, int value) {
        try {
            Field f_mTouchSlopSquare = GestureDetector.class.getDeclaredField("mTouchSlopSquare");
            f_mTouchSlopSquare.setAccessible(true);
            f_mTouchSlopSquare.setInt(gestureDetector, value * value);
        } catch (NoSuchFieldException | IllegalAccessException | NullPointerException e) {
            Log.w(TAG, gestureDetector.toString(), e);
        }
    }

    public static void startAnimatable(ImageView image) {
        Drawable drawable = image.getDrawable();
        if (drawable instanceof Animatable)
            ((Animatable) drawable).start();
    }

    public interface GetDrawable {
        @Nullable
        Drawable getDrawable(@NonNull Context context);
    }

    public interface SetDrawable {
        void setDrawable(@NonNull View view, @NonNull Drawable drawable);
    }

    public static abstract class AsyncViewSet extends AsyncTask<Void, Drawable> {
        protected final WeakReference<View> weakView;

        protected AsyncViewSet(View view) {
            super();
            this.weakView = new WeakReference<>(view);
            if (view.getTag() instanceof AsyncViewSet)
                ((AsyncViewSet) view.getTag()).cancel(true);
            view.setTag(this);
        }

        @Override
        protected Drawable doInBackground(Void param) {
            View image = weakView.get();
            Activity act = Utilities.getActivity(image);
            if (isCancelled() || act == null || image.getTag() != this) {
                weakView.clear();
                return null;
            }

            Context ctx = image.getContext();
            return getDrawable(ctx);
        }

        @WorkerThread
        protected abstract Drawable getDrawable(Context context);

        @UiThread
        protected abstract void setDrawable(@NonNull View view, @NonNull Drawable drawable);

        @Override
        protected void onPostExecute(Drawable drawable) {
            View view = weakView.get();
            if (view.getTag() != this)
                return;
            Activity act = Utilities.getActivity(view);
            if (act == null || drawable == null) {
                weakView.clear();
                return;
            }
            setDrawable(view, drawable);
            view.setTag(null);
        }

        public void execute() {
            TaskRunner.executeOnExecutor(ResultViewHelper.EXECUTOR_LOAD_ICON, this);
        }
    }

    public static abstract class AsyncSetDrawable extends AsyncViewSet {
        protected AsyncSetDrawable(@NonNull ImageView image) {
            super(image);
            image.setImageResource(android.R.color.transparent);
        }

        @Override
        protected void setDrawable(@NonNull View image, @NonNull Drawable drawable) {
            ((ImageView) image).setImageDrawable(drawable);
        }
    }

//    public static class AsyncRun extends TaskRunner.AsyncTask<Void, Void> {
//        private final Run mBackground;
//        private final Run mAfter;
//
//        public interface Run {
//            void run(@NonNull Utilities.AsyncRun task);
//        }
//
//        public AsyncRun(@NonNull Run background, @Nullable Run after) {
//            super();
//            mBackground = background;
//            mAfter = after;
//        }
//
//        @Override
//        protected Void doInBackground(Void param) {
//            mBackground.run(this);
//            return null;
//        }
//
//        @Override
//        protected void onCancelled(Void aVoid) {
//            if (mAfter != null)
//                mAfter.run(this);
//        }
//
//        @Override
//        protected void onPostExecute(Void aVoid) {
//            if (mAfter != null)
//                mAfter.run(this);
//        }
//
//        public boolean cancel() {
//            return cancel(false);
//        }
//    }
}
