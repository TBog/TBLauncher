package rocks.tbog.tblauncher.entry;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

import rocks.tbog.tblauncher.result.AsyncSetEntryDrawable;
import rocks.tbog.tblauncher.result.ResultViewHelper;
import rocks.tbog.tblauncher.utils.UIColors;
import rocks.tbog.tblauncher.utils.Utilities;

public abstract class UrlEntry extends SearchEntry {

    public final String url;
    private static final ArrayList<Pair<String, String>> APP4URL;

    static {
        APP4URL = new ArrayList<>(5);
        APP4URL.add(new Pair<>("https://encrypted.google.com", "com.google.android.googlequicksearchbox"));
        APP4URL.add(new Pair<>("https://play.google.com/store", "com.android.vending"));
        APP4URL.add(new Pair<>("https://start.duckduckgo.com", "com.duckduckgo.mobile.android"));
        APP4URL.add(new Pair<>("https://www.google.com/maps", "com.google.android.apps.maps"));
        APP4URL.add(new Pair<>("https://www.youtube.com", "com.google.android.youtube"));
    }

    public UrlEntry(@NonNull String id, @NonNull String url) {
        super(id);
        this.url = url;
    }

    @Override
    public String getHistoryId() {
        // Search POJO should not appear in history
        return "";
    }

    @Nullable
    protected static Drawable getApplicationIconForUrl(@NonNull Context context, @Nullable String url) {
        if (url == null || url.isEmpty())
            return null;
        for (Pair<String, String> pair : APP4URL) {
            if (url.startsWith(pair.first)) {
                try {
                    return context.getPackageManager().getApplicationIcon(pair.second);
                } catch (PackageManager.NameNotFoundException ignored) {
                }
            }

        }
        return null;
    }

    protected static boolean isGoogleSearch(String url) {
        return url.startsWith("https://encrypted.google.com");
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Result methods
    ///////////////////////////////////////////////////////////////////////////////////////////////
    protected abstract String getResultText(Context context);

    @Override
    public Drawable getDefaultDrawable(Context context) {
        Drawable appIcon = getApplicationIconForUrl(context, url);
        if (appIcon != null)
            return appIcon;
        return super.getDefaultDrawable(context);
    }

    @Override
    public void displayResult(@NonNull View view, int drawFlags) {
        Context context = view.getContext();
        TextView nameView = view.findViewById(android.R.id.text1);
        nameView.setTextColor(UIColors.getResultTextColor(view.getContext()));
        if (Utilities.checkFlag(drawFlags, FLAG_DRAW_NAME)) {
            String text = getResultText(context);
            int pos = text.lastIndexOf(query);
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
            ResultViewHelper.setIconAsync(drawFlags, this, appIcon, AsyncSetUrlEntryIcon.class);
        } else {
            appIcon.setImageDrawable(null);
            appIcon.setVisibility(View.GONE);
        }

        ResultViewHelper.applyPreferences(drawFlags, nameView, appIcon);
        if (Utilities.checkFlag(drawFlags, FLAG_DRAW_LIST))
            ResultViewHelper.applyListRowPreferences((ViewGroup) view);
    }

    public static class AsyncSetUrlEntryIcon extends AsyncSetEntryDrawable {
        public AsyncSetUrlEntryIcon(@NonNull ImageView image, int drawFlags, @NonNull EntryItem entryItem) {
            super(image, drawFlags, entryItem);
        }

        @Override
        public Drawable getDrawable(Context context) {
            UrlEntry urlEntry = (UrlEntry) entryItem;
            return urlEntry.getIconDrawable(context);
        }
    }
}
