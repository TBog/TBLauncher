package rocks.tbog.tblauncher.result;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.util.Pair;
import android.util.Printer;
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
     * @param relevance  the mapping and code points of the matched text
     * @param normText   the mapping and code points of the provided text
     * @param text       provided visible text that may need highlighting
     * @param matchInfo  matched sequences
     * @param view       TextView that gets the text
     * @return if the text got any matches
     */
    public static boolean displayHighlighted(@Nullable StringNormalizer.Result relevance, StringNormalizer.Result normText,
                                             String text, @Nullable FuzzyScore.MatchInfo matchInfo,
                                             TextView view) {
        if (matchInfo == null || !matchInfo.match || !normText.equals(relevance)) {
            view.setText(text);
            return false;
        }

        int color = UIColors.getPrimaryColor(view.getContext());
        view.setText(highlightText(normText, text, matchInfo, color));

        return true;
    }

    public static boolean displayHighlighted(StringNormalizer.Result normalized, Iterable<EntryWithTags.TagDetails> tags,
                                             @Nullable FuzzyScore.MatchInfo matchInfo, TextView view, Context context) {
//        final StringBuilder debug = new StringBuilder();
//        Printer debugPrint = x -> debug.append(x).append("\n");
        boolean matchFound = false;

        int color = UIColors.getPrimaryColor(context);
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

    public static void setIconAsync(@NonNull EntryItem entry, @NonNull ImageView appIcon, @NonNull Class<? extends AsyncSetEntryDrawable> asyncSetEntryIconClass) {
        Drawable cache = TBApplication.drawableCache(appIcon.getContext()).getCachedDrawable(entry.id);
        if (cache != null) {
            appIcon.setImageDrawable(cache);
            return;
        }

        AsyncSetEntryDrawable task;
        try {
            Constructor<? extends AsyncSetEntryDrawable> constructor = asyncSetEntryIconClass.getConstructor(ImageView.class);
            task = constructor.newInstance(appIcon);
        } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            Log.e(TAG, "new <? extends AsyncSetEntryDrawable>", e);
            return;
        }
        task.executeOnExecutor(iconAsyncExecutor, entry);
    }

    public static abstract class AsyncSetEntryDrawable extends AsyncTask<EntryItem, Void, Drawable> {
        final WeakReference<ImageView> weakImage;
        private String cacheId = null;

        public AsyncSetEntryDrawable(@NonNull ImageView image) {
            super();
            if (image.getTag() instanceof AsyncSetEntryDrawable)
                ((AsyncSetEntryDrawable) image.getTag()).cancel(true);
            image.setTag(this);
            image.setImageResource(android.R.color.transparent);
            this.weakImage = new WeakReference<>(image);
        }

        @Override
        protected Drawable doInBackground(EntryItem... entries) {
            ImageView image = weakImage.get();
            if (isCancelled() || image == null || image.getTag() != this) {
                weakImage.clear();
                return null;
            }
            EntryItem entry = entries[0];
            cacheId = entry.id;
            Context ctx = image.getContext();
            return getDrawable(entry, ctx);
        }

        @WorkerThread
        protected abstract Drawable getDrawable(EntryItem entry, Context context);

        @Override
        protected void onPostExecute(Drawable drawable) {
            ImageView image = weakImage.get();
            if (image == null || drawable == null) {
                weakImage.clear();
                return;
            }
            image.setImageDrawable(drawable);
            image.setTag(null);
            if (cacheId != null)
                TBApplication.drawableCache(image.getContext()).cacheDrawable(cacheId, drawable);
        }
    }

}
