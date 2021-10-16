package rocks.tbog.tblauncher.entry;

import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.result.ResultViewHelper;
import rocks.tbog.tblauncher.utils.UIColors;
import rocks.tbog.tblauncher.utils.Utilities;

public final class SearchEngineEntry extends SearchEntry {
    public static final String SCHEME = "search-engine://";
    private final String url;

    public SearchEngineEntry(String engineName, String engineUrl) {
        super(SCHEME + engineName);
        url = engineUrl;
        setName(engineName, false);
    }

    @Override
    public void displayResult(@NonNull View view, int drawFlags) {
        Context context = view.getContext();
        TextView nameView = view.findViewById(android.R.id.text1);
        nameView.setTextColor(UIColors.getResultTextColor(view.getContext()));
        if (Utilities.checkFlag(drawFlags, FLAG_DRAW_NAME)) {
            String text = String.format(context.getString(R.string.ui_item_search), getName(), query);
            int pos = text.indexOf(query);
            if (pos >= 0) {
                int color = UIColors.getResultHighlightColor(context);
                SpannableString enriched = new SpannableString(text);
                enriched.setSpan(
                        new ForegroundColorSpan(color),
                        pos,
                        pos + query.length(),
                        Spannable.SPAN_INCLUSIVE_EXCLUSIVE
                );
                nameView.setText(enriched);
            } else {
                nameView.setText(text);
            }
        } else {
            nameView.setVisibility(View.GONE);
        }

        ImageView appIcon = view.findViewById(android.R.id.icon);
        if (Utilities.checkFlag(drawFlags, FLAG_DRAW_ICON)) {
            ResultViewHelper.setIconColorFilter(appIcon, drawFlags);
            appIcon.setVisibility(View.VISIBLE);
            Drawable icon = getApplicationIconForUrl(context, url);
            if (icon != null) {
                appIcon.setImageDrawable(icon);
            } else {
                appIcon.setImageResource(R.drawable.ic_search);
            }
        } else {
            appIcon.setImageDrawable(null);
            appIcon.setVisibility(View.GONE);
        }

        ResultViewHelper.applyPreferences(drawFlags, nameView, appIcon);
    }

    @Override
    public void doLaunch(@NonNull View v, int flags) {
        Context context = v.getContext();
        if (isGoogleSearch(url)) {
            try {
                Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(SearchManager.QUERY, query); // query contains search string
                context.startActivity(intent);
                return;
            } catch (ActivityNotFoundException e) {
                // Google app not found, fall back to default method
            }
        }
        String encodedQuery;
        try {
            encodedQuery = URLEncoder.encode(query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            encodedQuery = URLEncoder.encode(query);
        }
        String urlWithQuery = url.replaceAll("%s|\\{q\\}", encodedQuery);
        Uri uri = Uri.parse(urlWithQuery);
        Intent search = new Intent(Intent.ACTION_VIEW, uri);
        search.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(search);
        } catch (ActivityNotFoundException e) {
            Log.w("SearchResult", "Unable to run search for url: " + url);
        }
    }
}
