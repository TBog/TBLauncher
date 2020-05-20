package rocks.tbog.tblauncher.dataprovider;

import android.content.ComponentName;
import android.content.Context;

import androidx.annotation.WorkerThread;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import rocks.tbog.tblauncher.db.AppRecord;
import rocks.tbog.tblauncher.entry.AppEntry;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.normalizer.StringNormalizer;
import rocks.tbog.tblauncher.searcher.Searcher;
import rocks.tbog.tblauncher.utils.FuzzyScore;
import rocks.tbog.tblauncher.utils.UserHandleCompat;

public class AppCacheProvider implements IProvider {

    final private Context context;
    final private HashMap<String, AppRecord> apps;

    public AppCacheProvider(Context context, HashMap<String, AppRecord> cachedApps) {
        this.context = context;
        apps = cachedApps;
    }

    @WorkerThread
    @Override
    public void requestResults(String query, Searcher searcher) {
        StringNormalizer.Result queryNormalized = StringNormalizer.normalizeWithResult(query, false);

        if (queryNormalized.codePoints.length == 0) {
            return;
        }

        FuzzyScore fuzzyScore = new FuzzyScore(queryNormalized.codePoints);
        FuzzyScore.MatchInfo matchInfo;
        boolean match;

        ArrayList<AppEntry> pojos = new ArrayList<>(apps.size());

        // convert from AppRecord to AppEntry
        for ( AppRecord rec : apps.values() ) {
            UserHandleCompat user = UserHandleCompat.fromComponentName(context, rec.componentName);
            String id = AppEntry.SCHEME + rec.componentName;
            ComponentName cn = UserHandleCompat.unflattenComponentName(rec.componentName);
            AppEntry app = new AppEntry(id, cn.getPackageName(), cn.getClassName(), user);
            pojos.add(app);

            if (rec.hasCustomName())
                app.setName(rec.displayName);
            else
                app.setName(user.getBadgedLabelForUser(context, rec.displayName));
            if (rec.hasCustomIcon())
                app.setCustomIcon(rec.dbId);
            //app.setTags(tagsHandler.getTags(app.id));
        }

        for (AppEntry pojo : pojos) {
            if (pojo.isExcluded()) {
                continue;
            }

            matchInfo = fuzzyScore.match(pojo.normalizedName.codePoints);
            match = matchInfo.match;
            pojo.setRelevance(matchInfo.score);

            // check relevance for tags
//            if (pojo.getNormalizedTags() != null) {
//                matchInfo = fuzzyScore.match(pojo.getNormalizedTags().codePoints);
//                if (matchInfo.match && (!match || matchInfo.score > pojo.relevance)) {
//                    match = true;
//                    pojo.relevance = matchInfo.score;
//                }
//            }

            if (match && !searcher.addResult(pojo)) {
                return;
            }
        }
    }

    @Override
    public void reload() {

    }

    @Override
    public boolean isLoaded() {
        return false;
    }

    @Override
    public boolean mayFindById(String id) {
        return false;
    }

    @Override
    public EntryItem findById(String id) {
        return null;
    }

    @Override
    public List<? extends EntryItem> getPojos() {
        return null;
    }
}
