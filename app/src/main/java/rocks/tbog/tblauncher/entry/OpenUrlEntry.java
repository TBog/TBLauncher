package rocks.tbog.tblauncher.entry;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;

import rocks.tbog.tblauncher.R;

public final class OpenUrlEntry extends UrlEntry {
    public static final String SCHEME = "url://";

    public OpenUrlEntry(String query, String url) {
        super(SCHEME + url, url);
        this.query = query;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Result methods
    ///////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    protected String getResultText(Context context) {
        return String.format(context.getString(R.string.ui_item_visit), getName());
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
