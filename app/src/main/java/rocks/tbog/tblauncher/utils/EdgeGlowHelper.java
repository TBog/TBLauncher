package rocks.tbog.tblauncher.utils;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.widget.AbsListView;
import android.widget.EdgeEffect;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.widget.EdgeEffectCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * Found this in a comment from https://stackoverflow.com/questions/27342957/how-to-change-the-color-of-overscroll-edge-and-overscroll-glow
 * Taken from https://pastebin.com/TAujMUu9
 */
public final class EdgeGlowHelper {
    private static final String TAG = EdgeGlowHelper.class.getSimpleName();

    private static final Class<RecyclerView> CLASS_RECYCLER_VIEW = RecyclerView.class;
    private static final Field RECYCLER_VIEW_FIELD_EDGE_GLOW_TOP;
    private static final Field RECYCLER_VIEW_FIELD_EDGE_GLOW_LEFT;
    private static final Field RECYCLER_VIEW_FIELD_EDGE_GLOW_RIGHT;
    private static final Field RECYCLER_VIEW_FIELD_EDGE_GLOW_BOTTOM;

    private static final Class<AbsListView> CLASS_LIST_VIEW = AbsListView.class;
    private static final Field LIST_VIEW_FIELD_EDGE_GLOW_TOP;
    private static final Field LIST_VIEW_FIELD_EDGE_GLOW_BOTTOM;

    private static final Field EDGE_GLOW_FIELD_EDGE;
    private static final Field EDGE_GLOW_FIELD_GLOW;
    private static final Field EDGE_EFFECT_COMPAT_FIELD_EDGE_EFFECT;

    static {
        Field edgeGlowTop = null;
        Field edgeGlowBottom = null;
        Field edgeGlowLeft = null;
        Field edgeGlowRight = null;
        for (Field f : CLASS_RECYCLER_VIEW.getDeclaredFields()) {
            switch (f.getName()) {
                case "mTopGlow":
                    f.setAccessible(true);
                    edgeGlowTop = f;
                    break;
                case "mBottomGlow":
                    f.setAccessible(true);
                    edgeGlowBottom = f;
                    break;
                case "mLeftGlow":
                    f.setAccessible(true);
                    edgeGlowLeft = f;
                    break;
                case "mRightGlow":
                    f.setAccessible(true);
                    edgeGlowRight = f;
                    break;
                default:
                    // do nothing
                    break;
            }
        }

        RECYCLER_VIEW_FIELD_EDGE_GLOW_TOP = edgeGlowTop;
        RECYCLER_VIEW_FIELD_EDGE_GLOW_BOTTOM = edgeGlowBottom;
        RECYCLER_VIEW_FIELD_EDGE_GLOW_LEFT = edgeGlowLeft;
        RECYCLER_VIEW_FIELD_EDGE_GLOW_RIGHT = edgeGlowRight;
    }

    static {
        Field edgeGlowTop = null;
        Field edgeGlowBottom = null;
        for (Field f : CLASS_LIST_VIEW.getDeclaredFields()) {
            switch (f.getName()) {
                case "mEdgeGlowTop":
                    f.setAccessible(true);
                    edgeGlowTop = f;
                    break;
                case "mEdgeGlowBottom":
                    f.setAccessible(true);
                    edgeGlowBottom = f;
                    break;
                default:
                    // do nothing
                    break;
            }
        }

        LIST_VIEW_FIELD_EDGE_GLOW_TOP = edgeGlowTop;
        LIST_VIEW_FIELD_EDGE_GLOW_BOTTOM = edgeGlowBottom;
    }

    static {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Field edge = null;
            Field glow = null;

            for (Field f : EdgeEffect.class.getDeclaredFields()) {
                switch (f.getName()) {
                    case "mEdge":
                        f.setAccessible(true);
                        edge = f;
                        break;
                    case "mGlow":
                        f.setAccessible(true);
                        glow = f;
                        break;
                    default:
                        // do nothing
                        break;
                }
            }

            EDGE_GLOW_FIELD_EDGE = edge;
            EDGE_GLOW_FIELD_GLOW = glow;
        } else {
            EDGE_GLOW_FIELD_EDGE = null;
            EDGE_GLOW_FIELD_GLOW = null;
        }
    }

    static {
        Field efc = null;
        try {
            efc = EdgeEffectCompat.class.getDeclaredField("mEdgeEffect");
        } catch (NoSuchFieldException e) {
            Log.e(TAG, "field `mEdgeEffect` not found in `EdgeEffectCompat`", e);
        }
        EDGE_EFFECT_COMPAT_FIELD_EDGE_EFFECT = efc;
    }

    public static void setEdgeGlowColor(RecyclerView recyclerView, @ColorInt int color) {
        recyclerView.setEdgeEffectFactory(new EdgeGlowFactory(color));
        try {
            Object ee;
            ee = RECYCLER_VIEW_FIELD_EDGE_GLOW_TOP.get(recyclerView);
            setEffectGlowColor(ee, color);
            ee = RECYCLER_VIEW_FIELD_EDGE_GLOW_BOTTOM.get(recyclerView);
            setEffectGlowColor(ee, color);
            ee = RECYCLER_VIEW_FIELD_EDGE_GLOW_LEFT.get(recyclerView);
            setEffectGlowColor(ee, color);
            ee = RECYCLER_VIEW_FIELD_EDGE_GLOW_RIGHT.get(recyclerView);
            setEffectGlowColor(ee, color);
        } catch (Exception e) {
            Log.e(TAG, "set RecyclerView(" + recyclerView.getClass().getSimpleName() + ") edge color to 0x" + Integer.toHexString(color).toUpperCase(), e);
        }
    }

    public static void setEdgeGlowColor(AbsListView listView, @ColorInt int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            listView.setEdgeEffectColor(color);
        } else {
            try {
                Object ee;
                ee = LIST_VIEW_FIELD_EDGE_GLOW_TOP.get(listView);
                setEffectGlowColor(ee, color);
                ee = LIST_VIEW_FIELD_EDGE_GLOW_BOTTOM.get(listView);
                setEffectGlowColor(ee, color);
            } catch (Exception e) {
                Log.e(TAG, "set AbsListView(" + listView.getClass().getSimpleName() + ") edge color to 0x" + Integer.toHexString(color).toUpperCase(), e);
            }
        }
    }

    private static void setEffectGlowColor(Object effect, @ColorInt int color) {
        Object edgeEffect = effect;
        if (edgeEffect instanceof EdgeEffectCompat) {
            // EdgeEffectCompat
            try {
                edgeEffect = EDGE_EFFECT_COMPAT_FIELD_EDGE_EFFECT.get(edgeEffect);
            } catch (IllegalAccessException e) {
                Log.e(TAG, "can't set glow color for overscroll", e);
                return;
            }
        }

        if (edgeEffect == null)
            return;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            // EdgeGlow
            try {
                final Drawable mEdge = (Drawable) EDGE_GLOW_FIELD_EDGE.get(edgeEffect);
                final Drawable mGlow = (Drawable) EDGE_GLOW_FIELD_GLOW.get(edgeEffect);
                for (Drawable drawable : Arrays.asList(mEdge, mGlow)) {
                    drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN);
                    drawable.setCallback(null); // free up any references
                }
            } catch (Exception e) {
                Log.e(TAG, "can't set glow color for overscroll", e);
            }
        } else {
            // EdgeEffect
            ((EdgeEffect) edgeEffect).setColor(color);
        }
    }

    public static class EdgeGlowFactory extends RecyclerView.EdgeEffectFactory {
        @ColorInt
        private final int mColor;

        public EdgeGlowFactory(@ColorInt int color) {
            mColor = color;
        }

        @NonNull
        @Override
        protected EdgeEffect createEdgeEffect(@NonNull RecyclerView view, int direction) {
            EdgeEffect effect = super.createEdgeEffect(view, direction);
            setEffectGlowColor(effect, mColor);
            return effect;
        }
    }
}
