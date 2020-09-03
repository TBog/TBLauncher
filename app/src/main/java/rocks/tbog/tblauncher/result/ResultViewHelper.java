package rocks.tbog.tblauncher.result;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.Pair;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.entry.EntryWithTags;
import rocks.tbog.tblauncher.normalizer.StringNormalizer;
import rocks.tbog.tblauncher.utils.FuzzyScore;
import rocks.tbog.tblauncher.utils.UIColors;
import rocks.tbog.tblauncher.utils.Utilities;

public final class ResultViewHelper {

    private final static Executor iconAsyncExecutor = Executors.newSingleThreadExecutor();
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
        if (!Utilities.checkFlag(drawFlags, EntryItem.FLAG_DRAW_NO_CACHE)) {
            Drawable cache = TBApplication.drawableCache(appIcon.getContext()).getCachedDrawable(entry.id);
            if (cache != null) {
                // found the icon in cache
                appIcon.setImageDrawable(cache);
                appIcon.setTag(entry.id);
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
            Log.e(TAG, "new <? extends AsyncSetEntryDrawable>", e);
            return;
        }
        task.executeOnExecutor(iconAsyncExecutor);
    }

    public static abstract class AsyncSetEntryDrawable extends AsyncTask<Void, Void, Drawable> {
        private final WeakReference<ImageView> weakImage;
        protected final String cacheId;
        protected int drawFlags;
        protected EntryItem entryItem;

        public AsyncSetEntryDrawable(@NonNull ImageView image, int drawFlags, @NonNull EntryItem entryItem) {
            super();
            cacheId = entryItem.id;

            Object tag = image.getTag();
            image.setTag(this);
            boolean keepIcon = false;
            if (tag instanceof AsyncSetEntryDrawable) {
                AsyncSetEntryDrawable task = (AsyncSetEntryDrawable) tag;
                task.cancel(false);
                // if the old task was loading the same entry we can keep the icon while we refresh it
                keepIcon = entryItem.equals(task.entryItem);
            } else if (tag instanceof String) {
                // if the tag equals cacheId then we can keep the icon while we refresh it
                keepIcon = tag.equals(cacheId);
            }
            if (!keepIcon)
                image.setImageResource(android.R.color.transparent);
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
        protected Drawable doInBackground(Void... voids) {
            ImageView image = getImageView();
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
            ImageView image = getImageView();
            if (image == null || drawable == null) {
                weakImage.clear();
                return;
            }
            image.setImageDrawable(drawable);
            image.setTag(cacheId);
            if (cacheId != null && !Utilities.checkFlag(drawFlags, EntryItem.FLAG_DRAW_NO_CACHE))
                TBApplication.drawableCache(image.getContext()).cacheDrawable(cacheId, drawable);
        }
    }

}
