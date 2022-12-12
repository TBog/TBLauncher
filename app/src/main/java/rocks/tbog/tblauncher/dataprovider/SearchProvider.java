package rocks.tbog.tblauncher.dataprovider;

import android.content.Context;
import android.content.SharedPreferences;
import android.webkit.URLUtil;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArraySet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rocks.tbog.tblauncher.BuildConfig;
import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.entry.OpenUrlEntry;
import rocks.tbog.tblauncher.entry.SearchEngineEntry;
import rocks.tbog.tblauncher.entry.SearchEntry;
import rocks.tbog.tblauncher.normalizer.StringNormalizer;
import rocks.tbog.tblauncher.searcher.ISearcher;
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
        return new ArraySet<>(Arrays.asList(defaultSearchProviders));
    }

    @NonNull
    public static Set<String> getAvailableSearchProviders(Context context, SharedPreferences prefs) {
        Set<String> availableProviders = prefs.getStringSet("available-search-providers", null);
        if (availableProviders == null)
            availableProviders = SearchProvider.getDefaultSearchProviders(context);
        if (BuildConfig.DEBUG)
            return Collections.unmodifiableSet(availableProviders);
        return availableProviders;
    }

    @NonNull
    public static Set<String> getSelectedProviderNames(Context context, SharedPreferences prefs) {
        Set<String> selectedProviders = prefs.getStringSet("selected-search-provider-names", null);
        if (selectedProviders == null) {
            Set<String> availableProviders = getAvailableSearchProviders(context, prefs);
            selectedProviders = new ArraySet<>(availableProviders.size());
            for (String availableProvider : availableProviders)
                selectedProviders.add(getProviderName(availableProvider));
        }
        return selectedProviders;
    }

    @NonNull
    public static String sanitizeProviderName(@Nullable String name) {
        if (name == null)
            return "[name]";
        while (name.contains("|"))
            name = name.replace('|', ' ');
        return name;
    }

    @NonNull
    public static String sanitizeProviderUrl(@Nullable String url) {
        if (url == null)
            return "%s";
        if (!url.contains("%s"))
            return url + "%s";
        return url;
    }

    public SearchProvider(Context context, SharedPreferences sharedPreferences) {
        super();
        this.context = context.getApplicationContext();
        this.prefs = sharedPreferences;
        reload(false);
    }

    @Override
    public void reload(boolean cancelCurrentLoadTask) {
        searchEngines.clear();

        Set<String> availableSearchProviders = SearchProvider.getAvailableSearchProviders(context, prefs);
        Set<String> selectedProviderNames = SearchProvider.getSelectedProviderNames(context, prefs);

        for (String searchProvider : availableSearchProviders) {
            String name = getProviderName(searchProvider);
            if (selectedProviderNames.contains(name)) {
                String url = getProviderUrl(searchProvider);
                SearchEngineEntry entry = new SearchEngineEntry(name, url);
                if (url != null)
                    searchEngines.add(entry);
            }
        }
    }

    @Override
    public boolean mayFindById(@NonNull String id) {
        return id.startsWith(SearchEngineEntry.SCHEME);
    }

    @Override
    public SearchEntry findById(@NonNull String id) {
        for (SearchEngineEntry entry : searchEngines)
            if (entry.id.equals(id))
                return entry;
        return null;
    }

    @Override
    public void requestResults(String query, ISearcher searcher) {
        searcher.addResult(getResults(query));
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

        if (prefs.getBoolean("enable-url", true)) {
            FuzzyScore fuzzyScore = new FuzzyScore(queryNormalized.codePoints);

            // Open URLs directly (if I type http://something.com for instance)
            Matcher m = urlPattern.matcher(query);
            if (m.find()) {
                String guessedUrl = URLUtil.guessUrl(query);
                if (URLUtil.isHttpUrl(guessedUrl))
                    guessedUrl = "https://" + guessedUrl.substring(7);
                if (URLUtil.isValidUrl(guessedUrl)) {
                    SearchEntry pojo = new OpenUrlEntry(query, guessedUrl);
                    pojo.setName(guessedUrl);
                    FuzzyScore.MatchInfo matchInfo = fuzzyScore.match(pojo.normalizedName.codePoints);
                    pojo.setRelevance(pojo.normalizedName, matchInfo);
                    records.add(pojo);
                }
            }
        }
        return records;
    }

    @Nullable
    public static String getProviderUrl(@NonNull String searchProvider) {
        int pos = searchProvider.indexOf("|");
        if (pos >= 0)
            return searchProvider.substring(pos + 1);
        if (URLUtil.isValidUrl(searchProvider))
            return searchProvider;
        return null;
    }

    @NonNull
    public static String getProviderName(@NonNull String searchProvider) {
        int pos = searchProvider.indexOf("|");
        if (pos >= 0)
            return searchProvider.substring(0, pos);
        return "null";
    }

    @NonNull
    public static String makeProvider(@NonNull String name, @NonNull String url) {
        return sanitizeProviderName(name) + "|" + sanitizeProviderUrl(url);
    }
}
