package rocks.tbog.tblauncher.result;

import android.content.Context;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.entry.EntryWithTags;
import rocks.tbog.tblauncher.entry.ResultRelevance;
import rocks.tbog.tblauncher.normalizer.StringNormalizer;
import rocks.tbog.tblauncher.utils.FuzzyScore;
import rocks.tbog.tblauncher.utils.PrefCache;
import rocks.tbog.tblauncher.utils.UIColors;
import rocks.tbog.tblauncher.utils.UISizes;
import rocks.tbog.tblauncher.utils.Utilities;

public final class ResultViewHelper {

    public final static ExecutorService EXECUTOR_LOAD_ICON = Executors.newSingleThreadExecutor();
    private static final String TAG = "RVH";

    private ResultViewHelper() {
        // this is a namespace
    }

    private static SpannableString highlightText(StringNormalizer.Result normalized, String text, FuzzyScore.MatchInfo matchInfo, int color) {
        return highlightText(normalized, text, matchInfo.getMatchedSequences(), color);
    }

    private static SpannableString highlightText(StringNormalizer.Result normalized, String text, Iterable<Pair<Integer, Integer>> matchedSequences, int color) {
        SpannableString enriched = new SpannableString(text);

        for (Pair<Integer, Integer> position : matchedSequences) {
            enriched.setSpan(
                new ForegroundColorSpan(color),
                normalized.mapPosition(position.first),
                normalized.mapPosition(position.second),
                Spannable.SPAN_INCLUSIVE_EXCLUSIVE
            );
        }

        return enriched;
    }

    /**
     * Highlight text
     *
     * @param relevance result match information
     * @param normText  we'll use this to match the result with the text we try to highlight
     * @param text      provided visible text that may need highlighting
     * @param view      TextView that gets the text
     * @return if the text got any matches
     */
    public static boolean displayHighlighted(@NonNull ResultRelevance relevance, @NonNull StringNormalizer.Result normText,
                                             @NonNull String text, @NonNull TextView view) {
        // merge all results that have the same text source
        TreeSet<Integer> matchedPositions = new TreeSet<>();
        for (ResultRelevance.ResultInfo result : relevance.getInfoList()) {
            if (result.relevance.match && result.relevanceSource.equals(normText)) {
                matchedPositions.addAll(result.relevance.matchedIndices);
            }
        }
        if (matchedPositions.isEmpty()) {
            view.setText(text);
            return false;
        }

        int color = UIColors.getResultHighlightColor(view.getContext());
        List<Pair<Integer, Integer>> matchedSequences = FuzzyScore.MatchInfo.getMatchedSequences(new ArrayList<>(matchedPositions));
        view.setText(highlightText(normText, text, matchedSequences, color));
        return true;
    }

    public static boolean displayHighlighted(@NonNull ResultRelevance relevance, Iterable<EntryWithTags.TagDetails> tags,
                                             TextView view, Context context) {
        boolean matchFound = false;
        TreeSet<Integer> matchedPositions = new TreeSet<>();
        int color = UIColors.getResultHighlightColor(context);
        SpannableStringBuilder builder = new SpannableStringBuilder();
        boolean first = true;
        for (EntryWithTags.TagDetails tag : tags) {
            if (!first)
                builder.append(" \u2223 ");
            first = false;

            matchedPositions.clear();
            // find all matched positions
            for (ResultRelevance.ResultInfo result : relevance.getInfoList()) {
                if (result.relevance.match && result.relevanceSource.equals(tag.normalized)) {
                    matchedPositions.addAll(result.relevance.matchedIndices);
                    matchFound = true;
                }
            }

            if (matchedPositions.isEmpty()) {
                // no matches found
                builder.append(tag.name);
            } else {
                // highlight found matches
                List<Pair<Integer, Integer>> matchedSequences = FuzzyScore.MatchInfo.getMatchedSequences(new ArrayList<>(matchedPositions));
                builder.append(highlightText(tag.normalized, tag.name, matchedSequences, color));
            }
        }

        view.setText(builder);

        return matchFound;
    }

    public static <E extends EntryItem, T extends AsyncSetEntryDrawable<E>> void setIconAsync(int drawFlags, @NonNull E entry, @NonNull ImageView iconView, @NonNull Class<T> asyncSetEntryIconClass, @NonNull Class<E> entryItemClass) {
        String cacheId = entry.getIconCacheId();
        if (cacheId.equals(iconView.getTag(R.id.tag_cacheId)) && !Utilities.checkFlag(drawFlags, EntryItem.FLAG_RELOAD))
            return;

        if (!Utilities.checkFlag(drawFlags, EntryItem.FLAG_DRAW_NO_CACHE)) {
            Drawable cache = TBApplication.drawableCache(iconView.getContext()).getCachedDrawable(cacheId);
            if (cache != null) {
                Log.d(TAG, "cache found, view=" + Integer.toHexString(iconView.hashCode()) + " entry=" + entry.getName() + " cacheId=" + cacheId);
                // found the icon in cache
                iconView.setImageDrawable(cache);
                iconView.setTag(R.id.tag_cacheId, cacheId);
                iconView.setTag(R.id.tag_iconTask, null);
                // continue to run the async task only if FLAG_RELOAD set
                if (!Utilities.checkFlag(drawFlags, EntryItem.FLAG_RELOAD))
                    return;
            }
        }

        // Below we have 2 methods for getting rid of `entryItemClass` parameter

        /* METHOD 1: Get the actual type of EntryItem from template; this may be faster after the first run, but it needs further profiling
        T task;
        var superClass = asyncSetEntryIconClass.getGenericSuperclass();
        Class<?> entryClass = EntryItem.class;
        if (superClass instanceof ParameterizedType) {
            var actualTypeArguments = ((ParameterizedType) superClass).getActualTypeArguments();
            if (actualTypeArguments.length == 1)
                entryClass = (Class<?>) actualTypeArguments[0];
        }
        // make new task instance from class asyncSetEntryIconClass
        try {
            var constructor = asyncSetEntryIconClass.getConstructor(ImageView.class, int.class, entryClass);
            task = constructor.newInstance(iconView, drawFlags, entry);
        } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            Log.e(TAG, "new <? extends AsyncSetEntryDrawable>, ?=" + asyncSetEntryIconClass.getName(), e);
            return;
        }
        //*/

        /* METHOD 2: Find a constructor testing arguments by hand because we don't know the actual type of the EntryItem from asyncSetEntryIconClass
        T task = null;
        @SuppressWarnings("unchecked")
        Constructor<T>[] declaredConstructors = (Constructor<T>[]) asyncSetEntryIconClass.getDeclaredConstructors();
        // find and call constructor for template class
        for (Constructor<T> constructor : declaredConstructors) {
            var paramTypes = constructor.getParameterTypes();
            if (paramTypes.length == 3
                && paramTypes[0] == ImageView.class
                && paramTypes[1] == int.class
                && paramTypes[2].isAssignableFrom(entry.getClass())) {
                try {
                    task = constructor.newInstance(iconView, drawFlags, entry);
                } catch (ReflectiveOperationException e) {
                    Log.e(TAG, "new " + constructor, e);
                    return;
                }
                break;
            }
        }
        if (task == null) {
            Log.e(TAG, "constructor not found for " + asyncSetEntryIconClass.getName() + "\n declaredConstructors=" + Arrays.toString(declaredConstructors));
            return;
        }
        //*/

        // make new task instance from class `asyncSetEntryIconClass` using `entryItemClass`
        T task;
        Constructor<T> constructor = null;
        try {
            constructor = asyncSetEntryIconClass.getConstructor(ImageView.class, int.class, entryItemClass);
            task = constructor.newInstance(iconView, drawFlags, entry);
        } catch (ReflectiveOperationException e) {
            if (constructor != null)
                Log.e(TAG, "new " + constructor, e);
            else
                Log.e(TAG, "constructor not found for `" + asyncSetEntryIconClass.getName() + "` and entry `" + entry.getClass() + "`\n declaredConstructors=" + Arrays.toString(asyncSetEntryIconClass.getDeclaredConstructors()));
            return;
        }

        // run the async task
        task.execute();
    }

    public static void applyPreferences(int drawFlags, TextView nameView, ImageView iconView) {
        Context ctx = nameView.getContext();

        nameView.setTextColor(UIColors.getResultTextColor(ctx));
        nameView.setTextSize(TypedValue.COMPLEX_UNIT_PX, UISizes.getResultTextSize(ctx));

        if (Utilities.checkAnyFlag(drawFlags, EntryItem.FLAG_DRAW_LIST | EntryItem.FLAG_DRAW_GRID)) {
            ViewGroup.LayoutParams params = iconView.getLayoutParams();
            int size = UISizes.getResultIconSize(ctx);
            if (params.width != size || params.height != size) {
                params.width = size;
                params.height = size;
                iconView.setLayoutParams(params);
            }
        } else if (Utilities.checkFlag(drawFlags, EntryItem.FLAG_DRAW_QUICK_LIST)) {
            ViewGroup.LayoutParams params = iconView.getLayoutParams();
            if (params instanceof ConstraintLayout.LayoutParams) {
                ConstraintLayout.LayoutParams cParams = (ConstraintLayout.LayoutParams) params;
                int size = UISizes.getDockMaxIconSize(ctx);
                if (cParams.matchConstraintMaxWidth != size || cParams.matchConstraintMaxHeight != size) {
                    cParams.matchConstraintMaxWidth = size;
                    cParams.matchConstraintMaxHeight = size;
                    iconView.setLayoutParams(params);
                }
            }
        }

//        if (Utilities.checkFlag(drawFlags, EntryItem.FLAG_DRAW_LIST))
//        {
//            int[] colors = new int[] {0xFF000000, 0xFF00ff00, 0xFF000000};
//            GradientDrawable bkg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors);
//            iconView.setBackground(bkg);
//        }
    }

    public static void applyPreferences(int drawFlags, TextView nameView, TextView tagsView, ImageView iconView) {
        applyPreferences(drawFlags, nameView, iconView);

        Context ctx = tagsView.getContext();

        tagsView.setTextColor(UIColors.getResultText2Color(ctx));
        tagsView.setTextSize(TypedValue.COMPLEX_UNIT_PX, UISizes.getResultText2Size(ctx));
    }

    public static void applyListRowPreferences(ViewGroup rowView) {
        // set result list item height
        Context ctx = rowView.getContext();
        int rowHeight = UISizes.getResultListRowHeight(ctx);
        ViewGroup.LayoutParams params = rowView.getLayoutParams();
        if (params.height != rowHeight) {
            params.height = rowHeight;
            rowView.setLayoutParams(params);
        }
    }

    @Nullable
    private static ColorFilter getColorFilter(@NonNull Context context, int drawFlags) {
        final ColorFilter colorFilter;
        if (Utilities.checkFlag(drawFlags, EntryItem.FLAG_DRAW_QUICK_LIST))
            colorFilter = UIColors.colorFilterQuickIcon(context);
        else
            colorFilter = UIColors.colorFilter(context);
        return colorFilter;
    }

    @Nullable
    public static ColorFilter setIconColorFilter(@NonNull ImageView icon, int drawFlags) {
        ColorFilter colorFilter = getColorFilter(icon.getContext(), drawFlags);
        icon.setColorFilter(colorFilter);
        return colorFilter;
    }

    public static void removeIconColorFilter(@NonNull ImageView icon) {
        icon.clearColorFilter();
    }

    public static void setLoadingIcon(@NonNull ImageView image) {
        @DrawableRes
        int drawableId = PrefCache.getLoadingIconRes(image.getContext());
        image.setImageResource(drawableId);
        Utilities.startAnimatable(image);
    }
}
