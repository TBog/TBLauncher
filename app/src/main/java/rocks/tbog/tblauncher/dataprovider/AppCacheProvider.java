package rocks.tbog.tblauncher.dataprovider;

import android.content.ComponentName;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import rocks.tbog.tblauncher.db.AppRecord;
import rocks.tbog.tblauncher.entry.AppEntry;
import rocks.tbog.tblauncher.normalizer.StringNormalizer;
import rocks.tbog.tblauncher.searcher.Searcher;
import rocks.tbog.tblauncher.utils.FuzzyScore;
import rocks.tbog.tblauncher.utils.UserHandleCompat;

public class AppCacheProvider implements IProvider<AppEntry> {

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

        //FuzzyScore fuzzyScore = new FuzzyScore(queryNormalized.codePoints);
        //FuzzyScore.MatchInfo matchInfo;
        //boolean match;

        ArrayList<AppEntry> pojos = new ArrayList<>(apps.size());

        // convert from AppRecord to AppEntry
        for (AppRecord rec : apps.values()) {
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

        FuzzyScore fuzzyScore = new FuzzyScore(queryNormalized.codePoints);

        AppProvider.checkAppResults(pojos, fuzzyScore, searcher);
    }

    public void reload(boolean cancelCurrentLoadTask) {
    }

    @Override
    public boolean isLoaded() {
        return true;
    }

    @Override
    public void setDirty() {
        // do nothing, we already have the full list of items
    }

    @Override
    public int getLoadStep() {
        return LOAD_STEP_1;
    }

    @Override
    public boolean mayFindById(@NonNull String id) {
        return false;
    }

    @Override
    public AppEntry findById(@NonNull String id) {
        return null;
    }

    @Override
    public List<AppEntry> getPojos() {
        return null;
    }
}
