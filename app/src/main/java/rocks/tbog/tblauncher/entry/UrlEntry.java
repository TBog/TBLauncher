package rocks.tbog.tblauncher.entry;

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

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.result.ResultViewHelper;
import rocks.tbog.tblauncher.utils.UIColors;
import rocks.tbog.tblauncher.utils.Utilities;

public final class UrlEntry extends SearchEntry {
    public static final String SCHEME = "url://";
    public final String query;
    public final String url;

    public UrlEntry(String query, String url) {
        super(SCHEME + url);
        this.query = query;
        this.url = url;
    }

    @Override
    public String getHistoryId() {
        // Search POJO should not appear in history
        return "";
    }

    @Override
    public void displayResult(@NonNull View view, int drawFlags) {
        Context context = view.getContext();
        TextView nameView = view.findViewById(android.R.id.text1);
        nameView.setTextColor(UIColors.getResultTextColor(view.getContext()));
        if (Utilities.checkFlag(drawFlags, FLAG_DRAW_NAME)) {
            String text = String.format(context.getString(R.string.ui_item_visit), getName());
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
            nameView.setVisibility(View.VISIBLE);
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
        Uri uri = Uri.parse(url);
        Intent search = new Intent(Intent.ACTION_VIEW, uri);
        search.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(search);
        } catch (ActivityNotFoundException e) {
            Log.w("SearchResult", "Unable to run search for url: " + url);
        }
    }
}
