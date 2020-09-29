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
import rocks.tbog.tblauncher.entry.SearchEntry;
import rocks.tbog.tblauncher.normalizer.StringNormalizer;
import rocks.tbog.tblauncher.searcher.Searcher;
import rocks.tbog.tblauncher.utils.FuzzyScore;

public class SearchProvider extends SimpleProvider<SearchEntry> {
    private static final String URL_REGEX = "^(?:[a-z]+://)?(?:[a-z0-9-]|[^\\x00-\\x7F])+(?:[.](?:[a-z0-9-]|[^\\x00-\\x7F])+)+.*$";
    public static final Pattern urlPattern = Pattern.compile(URL_REGEX);
    private final SharedPreferences prefs;

    public static Set<String> getDefaultSearchProviders(Context context) {
        String[] defaultSearchProviders = context.getResources().getStringArray(R.array.defaultSearchProviders);
        return new HashSet<>(Arrays.asList(defaultSearchProviders));
    }

    private final ArrayList<SearchEntry> searchProviders = new ArrayList<>();
    private final Context context;

    public SearchProvider(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
        reload(false);
    }

    @Override
    public void reload(boolean cancelCurrentLoadTask) {
        searchProviders.clear();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> selectedProviders = prefs.getStringSet("selected-search-provider-names", new HashSet<>(Collections.singletonList("Google")));
        Set<String> availableProviders = prefs.getStringSet("available-search-providers", SearchProvider.getDefaultSearchProviders(context));

        // Get default search engine
        String defaultSearchEngine = prefs.getString("default-search-provider", "Google");

        assert selectedProviders != null;
        assert availableProviders != null;
        assert defaultSearchEngine != null;
        for (String searchProvider : selectedProviders) {
            String url = getProviderUrl(availableProviders, searchProvider);
            SearchEntry pojo = new SearchEntry("", url, SearchEntry.SEARCH_QUERY);
            // Super low relevance, should never be displayed before anything
            pojo.boostRelevance(-500);
            if (defaultSearchEngine.equals(searchProvider))
                // Display default search engine slightly higher
                pojo.boostRelevance(100);

            pojo.setName(searchProvider, false);
            if (pojo.url != null) {
                searchProviders.add(pojo);
            }
        }
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
            for (SearchEntry pojo : searchProviders) {
                pojo.query = query;
                records.add(pojo);
            }
        }

        FuzzyScore fuzzyScore = new FuzzyScore(queryNormalized.codePoints);

        // Open URLs directly (if I type http://something.com for instance)
        Matcher m = urlPattern.matcher(query);
        if (m.find()) {
            String guessedUrl = URLUtil.guessUrl(query);
            // URLUtil returns an http URL... we'll upgrade it to HTTPS
            // to avoid security issues on open networks,
            // technological problems when using HSTS
            // and do one less redirection to https
            // (tradeoff: non https URL will break, but they shouldn't exist anymore)
            guessedUrl = guessedUrl.replace("http://", "https://");
            if (URLUtil.isValidUrl(guessedUrl)) {
                SearchEntry pojo = new SearchEntry("search://url-access", query, guessedUrl, SearchEntry.URL_QUERY);
                FuzzyScore.MatchInfo matchInfo = fuzzyScore.match(pojo.normalizedName.codePoints);
                pojo.setRelevance(pojo.normalizedName, matchInfo);
                pojo.setName(guessedUrl, false);
                records.add(pojo);
            }
        }
        return records;
    }

    @Nullable
    @SuppressWarnings("StringSplitter")
    // Find the URL associated with specified providerName
    private String getProviderUrl(Set<String> searchProviders, String searchProviderName) {
        for (String nameAndUrl : searchProviders) {
            if (nameAndUrl.contains(searchProviderName + "|")) {
                String[] arrayNameAndUrl = nameAndUrl.split("\\|");
                // sanity check
                if (arrayNameAndUrl.length == 2) {
                    return arrayNameAndUrl[1];
                }
            }
        }
        return null;
    }
}
