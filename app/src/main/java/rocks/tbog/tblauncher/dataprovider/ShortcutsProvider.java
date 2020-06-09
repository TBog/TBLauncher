package rocks.tbog.tblauncher.dataprovider;

import android.widget.Toast;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.entry.EntryWithTags;
import rocks.tbog.tblauncher.entry.ShortcutEntry;
import rocks.tbog.tblauncher.loader.LoadShortcutsEntryItem;
import rocks.tbog.tblauncher.normalizer.StringNormalizer;
import rocks.tbog.tblauncher.searcher.Searcher;
import rocks.tbog.tblauncher.utils.FuzzyScore;

public class ShortcutsProvider extends Provider<ShortcutEntry> {
    private static boolean notifiedKissNotDefaultLauncher = false;

    @Override
    public void reload() {
        super.reload();
        // If the user tries to add a new shortcut, but KISS isn't the default launcher
        // AND the services are not running (low memory), then we won't be able to
        // spawn a new service on Android 8.1+.

        try {
            this.initialize(new LoadShortcutsEntryItem(this));
        } catch (IllegalStateException e) {
            if (!notifiedKissNotDefaultLauncher) {
                // Only display this message once per process
                Toast.makeText(this, R.string.unable_to_initialize_shortcuts, Toast.LENGTH_LONG).show();
            }
            notifiedKissNotDefaultLauncher = true;
            e.printStackTrace();
        }
    }

    @Override
    public void requestResults(String query, Searcher searcher) {
        StringNormalizer.Result queryNormalized = StringNormalizer.normalizeWithResult(query, false);

        if (queryNormalized.codePoints.length == 0) {
            return;
        }

        FuzzyScore fuzzyScore = new FuzzyScore(queryNormalized.codePoints);
        FuzzyScore.MatchInfo matchInfo;
        boolean match;

        for (ShortcutEntry pojo : pojos) {
            matchInfo = fuzzyScore.match(pojo.normalizedName.codePoints);
            match = matchInfo.match;
            pojo.setRelevance(pojo.normalizedName, matchInfo);

            if (searcher.tagsEnabled()) {
                // check relevance for tags
                for (EntryWithTags.TagDetails tag : pojo.getTags()) {
                    matchInfo = fuzzyScore.match(tag.normalized.codePoints);
                    if (matchInfo.match && (!match || matchInfo.score > pojo.getRelevance())) {
                        match = true;
                        pojo.setRelevance(tag.normalized, matchInfo);
                    }
                }
            }

            if (match && !searcher.addResult(pojo)) {
                return;
            }
        }
    }

    public EntryItem findByName(String name) {
        for (EntryItem pojo : pojos) {
            if (pojo.getName().equals(name))
                return pojo;
        }
        return null;
    }
}
