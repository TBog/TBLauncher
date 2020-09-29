package rocks.tbog.tblauncher.entry;

import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.result.ResultViewHelper;
import rocks.tbog.tblauncher.utils.UIColors;
import rocks.tbog.tblauncher.utils.Utilities;

public final class SearchEntry extends EntryItem {
    public static final int SEARCH_QUERY = 0;
    public static final int URL_QUERY = 1;
    public static final int CALCULATOR_QUERY = 2;

    public String query;
    public final String url;
    public final int type;

    public SearchEntry(String query, String url, int type) {
        this(url, query, url, type);
    }

    public SearchEntry(String id, String query, String url, int type) {
        super(id);
        if (type != SEARCH_QUERY && type != URL_QUERY && type != CALCULATOR_QUERY) {
            throw new IllegalArgumentException("Wrong type!");
        }

        this.query = query;
        this.url = url;
        this.type = type;
    }

    @Override
    public String getHistoryId() {
        // Search POJO should not appear in history
        return "";
    }

    @Override
    public int getResultLayout(int drawFlags) {
        return Utilities.checkFlag(drawFlags, FLAG_DRAW_LIST) ? R.layout.item_builtin :
                (Utilities.checkFlag(drawFlags, FLAG_DRAW_GRID) ? R.layout.item_grid :
                        R.layout.item_quick_list);
    }

    @Override
    public void displayResult(@NonNull View view, int drawFlags) {
        TextView nameView = view.findViewById(android.R.id.text1);
        nameView.setTextColor(UIColors.getResultTextColor(view.getContext()));
        if (Utilities.checkFlag(drawFlags, FLAG_DRAW_NAME))
            nameView.setText(getName() + ": " + query);
        else
            nameView.setVisibility(View.GONE);

        ImageView appIcon = view.findViewById(android.R.id.icon);
        if (Utilities.checkFlag(drawFlags, FLAG_DRAW_ICON)) {
            appIcon.setVisibility(View.VISIBLE);
            //ResultViewHelper.setIconAsync(drawFlags, this, appIcon, AsyncSetEntryIcon.class);
        } else {
            appIcon.setImageDrawable(null);
            appIcon.setVisibility(View.GONE);
        }

        ResultViewHelper.applyPreferences(drawFlags, nameView, appIcon);
    }

    private boolean isGoogleSearch() {
        return url.startsWith("https://encrypted.google.com");
    }

    private boolean isDuckDuckGo() {
        return url.startsWith("https://start.duckduckgo.com");
    }

    @Override
    public void doLaunch(View v) {
        Context context = v.getContext();
        SearchEntry searchPojo = this;
        switch (searchPojo.type) {
            case SearchEntry.URL_QUERY:
            case SearchEntry.SEARCH_QUERY:
                if (isGoogleSearch()) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra(SearchManager.QUERY, searchPojo.query); // query contains search string
                        context.startActivity(intent);
                        break;
                    } catch (ActivityNotFoundException e) {
                        // Google app not found, fall back to default method
                    }
                }
                String query;
                try {
                    query = URLEncoder.encode(searchPojo.query, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    query = URLEncoder.encode(searchPojo.query);
                }
                String urlWithQuery = searchPojo.url.replaceAll("%s|\\{q}", query);
                Uri uri = Uri.parse(urlWithQuery);
                Intent search = new Intent(Intent.ACTION_VIEW, uri);
                search.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    context.startActivity(search);
                } catch (android.content.ActivityNotFoundException e) {
                    Log.w("SearchResult", "Unable to run search for url: " + searchPojo.url);
                }
                break;
            case SearchEntry.CALCULATOR_QUERY:
//                ClipboardUtils.setClipboard(context, searchPojo.query.substring(searchPojo.query.indexOf("=") + 2));
//                Toast.makeText(context, R.string.copy_confirmation, Toast.LENGTH_SHORT).show();
                break;
        }
    }}
