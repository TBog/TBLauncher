package rocks.tbog.tblauncher.result;

import android.app.Activity;
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
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.WorkAsync.AsyncTask;
import rocks.tbog.tblauncher.WorkAsync.TaskRunner;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.entry.EntryWithTags;
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
        SpannableString enriched = new SpannableString(text);

        for (Pair<Integer, Integer> position : matchInfo.getMatchedSequences()) {
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
     * @param relevance the mapping and code points of the matched text
     * @param normText  the mapping and code points of the provided text
     * @param text      provided visible text that may need highlighting
     * @param matchInfo matched sequences
     * @param view      TextView that gets the text
     * @return if the text got any matches
     */
    public static boolean displayHighlighted(@Nullable StringNormalizer.Result relevance, StringNormalizer.Result normText,
                                             String text, @Nullable FuzzyScore.MatchInfo matchInfo,
                                             TextView view) {
        if (matchInfo == null || !matchInfo.match || !normText.equals(relevance)) {
            view.setText(text);
            return false;
        }

        int color = UIColors.getResultHighlightColor(view.getContext());
        view.setText(highlightText(normText, text, matchInfo, color));

        return true;
    }

    public static boolean displayHighlighted(StringNormalizer.Result normalized, Iterable<EntryWithTags.TagDetails> tags,
                                             @Nullable FuzzyScore.MatchInfo matchInfo, TextView view, Context context) {
//        final StringBuilder debug = new StringBuilder();
//        Printer debugPrint = x -> debug.append(x).append("\n");
        boolean matchFound = false;

        int color = UIColors.getResultHighlightColor(context);
        SpannableStringBuilder builder = new SpannableStringBuilder();
        boolean first = true;
        for (EntryWithTags.TagDetails tag : tags) {
            if (!first)
                builder.append(" \u2223 ");
            first = false;
            if (matchInfo != null && matchInfo.match && tag.normalized.equals(normalized)) {
                builder.append(highlightText(tag.normalized, tag.name, matchInfo, color));
                matchFound = true;

//                debug.setLength(0);
//                TextUtils.dumpSpans(builder, debugPrint, "");
            } else {
                builder.append(tag.name);
            }
        }

        view.setText(builder);

        return matchFound;
    }

    public static void setIconAsync(int drawFlags, @NonNull EntryItem entry, @NonNull ImageView appIcon, @NonNull Class<? extends AsyncSetEntryDrawable> asyncSetEntryIconClass) {
        String cacheId = entry.getIconCacheId();
        if (cacheId.equals(appIcon.getTag(R.id.tag_cacheId)) && !Utilities.checkFlag(drawFlags, EntryItem.FLAG_RELOAD))
            return;

        if (!Utilities.checkFlag(drawFlags, EntryItem.FLAG_DRAW_NO_CACHE)) {
            Drawable cache = TBApplication.drawableCache(appIcon.getContext()).getCachedDrawable(cacheId);
            if (cache != null) {
                Log.d(TAG, "cache found, view=" + Integer.toHexString(appIcon.hashCode()) + " entry=" + entry.getName() + " cacheId=" + cacheId);
                // found the icon in cache
                appIcon.setImageDrawable(cache);
                appIcon.setTag(R.id.tag_cacheId, cacheId);
                appIcon.setTag(R.id.tag_iconTask, null);
                // continue to run the async task only if FLAG_RELOAD set
                if (!Utilities.checkFlag(drawFlags, EntryItem.FLAG_RELOAD))
                    return;
            }
        }

        // run the async task
        AsyncSetEntryDrawable task;
        try {
            Constructor<? extends AsyncSetEntryDrawable> constructor = asyncSetEntryIconClass.getConstructor(ImageView.class, int.class, EntryItem.class);
            task = constructor.newInstance(appIcon, drawFlags, entry);
        } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            Log.e(TAG, "new <? extends AsyncSetEntryDrawable>, ?=" + asyncSetEntryIconClass.getName(), e);
            return;
        }
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

    public static abstract class AsyncSetEntryDrawable extends AsyncTask<Void, Drawable> {
        private final WeakReference<ImageView> weakImage;
        protected final String cacheId;
        protected int drawFlags;
        protected EntryItem entryItem;

        public AsyncSetEntryDrawable(@NonNull ImageView image, int drawFlags, @NonNull EntryItem entryItem) {
            super();
            cacheId = entryItem.getIconCacheId();

            Object tag_cacheId = image.getTag(R.id.tag_cacheId);
            Object tag_iconTask = image.getTag(R.id.tag_iconTask);

            image.setTag(R.id.tag_cacheId, cacheId);
            image.setTag(R.id.tag_iconTask, this);

            boolean keepIcon = false;
            if (tag_iconTask instanceof AsyncSetEntryDrawable) {
                AsyncSetEntryDrawable task = (AsyncSetEntryDrawable) tag_iconTask;
                task.cancel(false);
                // if the old task was loading the same entry we can keep the icon while we refresh it
                keepIcon = entryItem.equals(task.entryItem);
            } else if (tag_cacheId instanceof String) {
                // if the tag equals cacheId then we can keep the icon while we refresh it
                keepIcon = tag_cacheId.equals(cacheId);
            }
            Log.i(TAG, "start task=" + Integer.toHexString(hashCode()) +
                " view=" + Integer.toHexString(image.hashCode()) +
                " tag_iconTask=" + (tag_iconTask != null ? Integer.toHexString(tag_iconTask.hashCode()) : "null") +
                " entry=`" + entryItem.getName() + "`" +
                " keepIcon=" + keepIcon +
                " tag_cacheId=" + tag_cacheId +
                " cacheId=" + cacheId);
            if (!keepIcon) {
                setLoadingIcon(image);
            }
            this.weakImage = new WeakReference<>(image);
            this.drawFlags = drawFlags;
            this.entryItem = entryItem;
        }

        @Nullable
        public ImageView getImageView() {
            ImageView imageView = weakImage.get();
            // make sure we have a valid activity
            Activity act = Utilities.getActivity(imageView);
            if (act == null)
                return null;
            return imageView;
        }

        @Override
        protected Drawable doInBackground(Void param) {
            ImageView image = getImageView();
            if (isCancelled() || image == null) {
                weakImage.clear();
                return null;
            }
            Context ctx = image.getContext();
            return getDrawable(ctx);
        }

        @WorkerThread
        protected abstract Drawable getDrawable(Context context);

        @UiThread
        protected void setDrawable(ImageView image, Drawable drawable) {
            image.setImageDrawable(drawable);
            image.setTag(R.id.tag_iconTask, null);
            Utilities.startAnimatable(image);
        }

        @Override
        protected void onPostExecute(Drawable drawable) {
            ImageView image = getImageView();
            if (image == null || drawable == null) {
                Log.i(TAG, "end task=" + Integer.toHexString(hashCode()) +
                    " view=" + (image == null ? "null" : Integer.toHexString(image.hashCode())) +
                    " drawable=" + drawable +
                    " cacheId=`" + cacheId + "`");
                weakImage.clear();
                return;
            }
            Object tag_cacheId = image.getTag(R.id.tag_cacheId);
            Object tag_iconTask = image.getTag(R.id.tag_iconTask);

            if (cacheId != null && !Utilities.checkFlag(drawFlags, EntryItem.FLAG_DRAW_NO_CACHE))
                TBApplication.drawableCache(image.getContext()).cacheDrawable(cacheId, drawable);

            Log.i(TAG, "end task=" + Integer.toHexString(hashCode()) +
                " view=" + Integer.toHexString(image.hashCode()) +
                " tag_iconTask=" + (tag_iconTask != null ? Integer.toHexString(tag_iconTask.hashCode()) : "null") +
                " cacheId=`" + cacheId + "`");
            if (tag_iconTask instanceof AsyncSetEntryDrawable) {
                AsyncSetEntryDrawable task = (AsyncSetEntryDrawable) tag_iconTask;
                if (!entryItem.equals(task.entryItem)) {
                    Log.d(TAG, "[task] skip reason: `" + entryItem.getName() + "` \u2260 `" + task.entryItem.getName() + "`");
                    weakImage.clear();
                    return;
                }
            } else {
                Log.d(TAG, "[task] skip reason: tag_iconTask=null entry=`" + entryItem.getName() + "`");
                weakImage.clear();
                return;
            }
            // if the cacheId changed, skip
            if (!tag_cacheId.equals(cacheId)) {
                Log.d(TAG, "[cacheId] skip reason: `" + tag_cacheId + "` \u2260 `" + cacheId + "`");
                weakImage.clear();
                return;
            }
            setDrawable(image, drawable);
        }

        @Override
        protected void onCancelled() {
            ImageView image = getImageView();
            Log.i(TAG, "cancelled task=" + Integer.toHexString(hashCode()) + " view=" + (image != null ? Integer.toHexString(image.hashCode()) : "null"));
        }

        public void execute() {
            TaskRunner.executeOnExecutor(EXECUTOR_LOAD_ICON, this);
        }
    }

}
