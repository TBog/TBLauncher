package rocks.tbog.tblauncher.dataprovider;

import android.content.Context;
import android.content.SharedPreferences;
import android.webkit.URLUtil;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.entry.SearchEngineEntry;
import rocks.tbog.tblauncher.entry.SearchEntry;
import rocks.tbog.tblauncher.entry.UrlEntry;
import rocks.tbog.tblauncher.normalizer.StringNormalizer;
import rocks.tbog.tblauncher.searcher.Searcher;
import rocks.tbog.tblauncher.utils.FuzzyScore;

public class SearchProvider extends SimpleProvider<SearchEntry> {
    private static final String URL_REGEX = "^(?:[a-z]+://)?(?:[a-z0-9-]|[^\\x00-\\x7F])+(?:[.](?:[a-z0-9-]|[^\\x00-\\x7F])+)+.*$";
    public static final Pattern urlPattern = Pattern.compile(URL_REGEX);
    private final SharedPreferences prefs;
    private final ArrayList<SearchEngineEntry> searchEngines = new ArrayList<>();
    private final Context context;

    @NonNull
    public static Set<String> getDefaultSearchProviders(Context context) {
        String[] defaultSearchProviders = context.getResources().getStringArray(R.array.defaultSearchProviders);
        return new HashSet<>(Arrays.asList(defaultSearchProviders));
    }

    public SearchProvider(Context context) {
        super();
        this.context = context.getApplicationContext();
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
        reload(false);
    }

    @Override
    public void reload(boolean cancelCurrentLoadTask) {
        searchEngines.clear();

        Set<String> availableProviders = prefs.getStringSet("available-search-providers", null);
        if (availableProviders == null)
            availableProviders = SearchProvider.getDefaultSearchProviders(context);

        for (String searchProvider : availableProviders) {
            String url = getProviderUrl(searchProvider);
            String name = getProviderName(searchProvider);
            SearchEngineEntry entry = new SearchEngineEntry(name, url);
            if (url != null)
                searchEngines.add(entry);
        }
    }

    @Override
    public boolean mayFindById(@NonNull String id) {
        return false;
    }

    @Override
    public void requestResults(String s, Searcher searcher) {
        searcher.addResult(getResults(s).toArray(new SearchEntry[0]));
    }

    @NonNull
    private ArrayList<SearchEntry> getResults(String query) {
        ArrayList<SearchEntry> records = new ArrayList<>();
        StringNormalizer.Result queryNormalized = StringNormalizer.normalizeWithResult(query, false);

        if (queryNormalized.codePoints.length == 0) {
            return records;
        }

        if (prefs.getBoolean("enable-search", true)) {
            // Get default search engine
            String defaultSearchEngine = prefs.getString("default-search-provider", "Google");
            for (SearchEngineEntry entry : searchEngines) {
                entry.setQuery(query);
                entry.setRelevance(entry.normalizedName, null);
                // Super low relevance, should never be displayed before anything
                entry.boostRelevance(-500);
                if (entry.getName().equals(defaultSearchEngine))
                    // Display default search engine slightly higher
                    entry.boostRelevance(100);

                records.add(entry);
            }
        }

        FuzzyScore fuzzyScore = new FuzzyScore(queryNormalized.codePoints);

        // Open URLs directly (if I type http://something.com for instance)
        Matcher m = urlPattern.matcher(query);
        if (m.find()) {
            String guessedUrl = URLUtil.guessUrl(query);
            if (URLUtil.isHttpUrl(guessedUrl))
                guessedUrl = "https://" + guessedUrl.substring(7);
            if (URLUtil.isValidUrl(guessedUrl)) {
                SearchEntry pojo = new UrlEntry(query, guessedUrl);
                pojo.setName(guessedUrl);
                FuzzyScore.MatchInfo matchInfo = fuzzyScore.match(pojo.normalizedName.codePoints);
                pojo.setRelevance(pojo.normalizedName, matchInfo);
                records.add(pojo);
            }
        }
        return records;
    }

    private static String getProviderUrl(String searchProvider) {
        int pos = searchProvider.indexOf("|");
        if (pos >= 0)
            return searchProvider.substring(pos + 1);
        if (URLUtil.isValidUrl(searchProvider))
            return searchProvider;
        return null;
    }

    @NonNull
    private static String getProviderName(String searchProvider) {
        int pos = searchProvider.indexOf("|");
        if (pos >= 0)
            return searchProvider.substring(0, pos);
        return "null";
    }
}
