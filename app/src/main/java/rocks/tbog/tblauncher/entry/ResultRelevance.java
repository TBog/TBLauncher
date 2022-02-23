package rocks.tbog.tblauncher.entry;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import rocks.tbog.tblauncher.normalizer.StringNormalizer;
import rocks.tbog.tblauncher.utils.FuzzyScore;

public class ResultRelevance implements Comparable<ResultRelevance> {
    // How relevant is this record? The higher, the most probable it will be displayed
    public FuzzyScore.MatchInfo relevance = null;
    // Pointer to the normalizedName that the above relevance was calculated, used for highlighting
    public StringNormalizer.Result relevanceSource = null;

    public int getRelevance() {
        return relevance == null ? 0 : relevance.score;
    }

    public void setRelevance(@NonNull StringNormalizer.Result normalizedName, @Nullable FuzzyScore.MatchInfo matchInfo) {
        relevanceSource = normalizedName;
        relevance = matchInfo != null ? new FuzzyScore.MatchInfo(matchInfo) : new FuzzyScore.MatchInfo();
    }

    public void boostRelevance(int boost) {
        if (relevance != null)
            relevance.score += boost;
    }

    public void resetRelevance() {
        this.relevanceSource = null;
        this.relevance = null;
    }

    @Override
    public int compareTo(ResultRelevance o) {
        int difference = getRelevance() - o.getRelevance();
        if (difference == 0)
            if (relevanceSource != null && o.relevanceSource != null)
                return relevanceSource.compareTo(o.relevanceSource);
        return difference;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ResultRelevance))
            return false;
        return compareTo((ResultRelevance) o) == 0;
    }
}
