package rocks.tbog.tblauncher.dataprovider;

import android.util.Log;

import androidx.annotation.WorkerThread;

import java.util.Collection;

import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.entry.EntryWithTags;
import rocks.tbog.tblauncher.normalizer.StringNormalizer;
import rocks.tbog.tblauncher.searcher.ISearcher;
import rocks.tbog.tblauncher.searcher.ResultBuffer;
import rocks.tbog.tblauncher.utils.FuzzyScore;

public class EntryToResultUtils {
    final static String TAG = "E2R";

    interface CheckResults<T> {
        void checkResults(Collection<T> entries, FuzzyScore fuzzyScore, ISearcher searcher);
    }

    @WorkerThread
    public static <T extends EntryItem> void recursiveWordCheck(Collection<T> entries, String query, ISearcher searcher, CheckResults<T> action, Class<T> typeClass) {
        int pos = query.lastIndexOf(' ');
        if (pos > 0) {
            String queryLeft = query.substring(0, pos).trim();
            String queryRight = query.substring(pos + 1).trim();

            StringNormalizer.Result queryNormalizedRight = StringNormalizer.normalizeWithResult(queryRight, false);
            if (queryNormalizedRight.codePoints.length > 0) {
                ResultBuffer<T> buffer = new ResultBuffer<>(searcher.tagsEnabled(), typeClass);
                recursiveWordCheck(entries, queryLeft, buffer, action, typeClass);

                FuzzyScore fuzzyScoreRight = new FuzzyScore(queryNormalizedRight.codePoints);
                action.checkResults(buffer.getEntryItems(), fuzzyScoreRight, searcher);
                return;
            }
        }

        StringNormalizer.Result queryNormalized = StringNormalizer.normalizeWithResult(query, false);
        if (queryNormalized.codePoints.length == 0)
            return;

        FuzzyScore fuzzyScore = new FuzzyScore(queryNormalized.codePoints);
        action.checkResults(entries, fuzzyScore, searcher);
    }

    @WorkerThread
    public static void tagsCheckResults(Collection<? extends EntryWithTags> entries, FuzzyScore fuzzyScore, ISearcher searcher) {
        Log.d(TAG, "tagsCheckResults count=" + entries.size() + " " + fuzzyScore);

        for (EntryWithTags entry : entries) {
            if (entry.isHiddenByUser()) {
                continue;
            }

            FuzzyScore.MatchInfo scoreInfo = fuzzyScore.match(entry.normalizedName.codePoints);

            StringNormalizer.Result matchedText = entry.normalizedName;
            FuzzyScore.MatchInfo matchedInfo = FuzzyScore.MatchInfo.copyOrNewInstance(scoreInfo, null);

            if (searcher.tagsEnabled()) {
                // check relevance for tags
                for (EntryWithTags.TagDetails tag : entry.getTags()) {
                    // fuzzyScore.match will return the same object
                    scoreInfo = fuzzyScore.match(tag.normalized.codePoints);
                    if (scoreInfo.match && (!matchedInfo.match || scoreInfo.score > matchedInfo.score)) {
                        matchedText = tag.normalized;
                        matchedInfo = FuzzyScore.MatchInfo.copyOrNewInstance(scoreInfo, matchedInfo);
                    }
                }
            }

            entry.addResultMatch(matchedText, matchedInfo);

            if (matchedInfo.match && !searcher.addResult(entry)) {
                return;
            }
        }
    }

}
