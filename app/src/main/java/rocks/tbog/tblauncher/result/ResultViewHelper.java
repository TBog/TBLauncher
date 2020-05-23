package rocks.tbog.tblauncher.result;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Pair;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import java.lang.ref.WeakReference;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.normalizer.StringNormalizer;
import rocks.tbog.tblauncher.utils.FuzzyScore;
import rocks.tbog.tblauncher.utils.UIColors;

public final class ResultViewHelper {

    final static Executor iconAsyncExecutor = Executors.newSingleThreadExecutor();

    private ResultViewHelper() {
        // this is a static class
    }

    /**
     * Highlight text
     *
     * @param normalized the mapping and code points of the text
     * @param text       the visible text that gets highlighted
     * @param fuzzyScore function to compute matched sequences //TODO: send the FuzzyScore.MatchInfo
     * @param view       TextView that gets the highlighted text
     * @param context    used for getting colors
     * @return if the text got any matches
     */
    public static boolean displayHighlighted(StringNormalizer.Result normalized, String text, FuzzyScore fuzzyScore,
                                             TextView view, Context context) {
        FuzzyScore.MatchInfo matchInfo = fuzzyScore.match(normalized.codePoints);

        if (!matchInfo.match) {
            view.setText(text);
            return false;
        }

        SpannableString enriched = new SpannableString(text);
        int primaryColor = UIColors.getPrimaryColor(context);

        for (Pair<Integer, Integer> position : matchInfo.getMatchedSequences()) {
            enriched.setSpan(
                    new ForegroundColorSpan(primaryColor),
                    normalized.mapPosition(position.first),
                    normalized.mapPosition(position.second),
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
            );
        }
        view.setText(enriched);
        return true;
    }

    public static void setIconAsync(@NonNull EntryItem entry, @NonNull AsyncSetDrawable task) {
        task.executeOnExecutor(iconAsyncExecutor, entry);
    }

    public static abstract class AsyncSetDrawable extends AsyncTask<EntryItem, Void, Drawable> {
        final WeakReference<ImageView> weakImage;

        protected AsyncSetDrawable(@NonNull ImageView image) {
            super();
            if ( image.getTag() instanceof AsyncSetDrawable )
                ((AsyncSetDrawable)image.getTag()).cancel(true);
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
        }
    }

}
