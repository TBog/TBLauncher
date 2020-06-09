package rocks.tbog.tblauncher.entry;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import java.net.URISyntaxException;
import java.util.List;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.db.DBHelper;
import rocks.tbog.tblauncher.result.ResultAdapter;
import rocks.tbog.tblauncher.result.ResultViewHelper;
import rocks.tbog.tblauncher.ui.LinearAdapter;
import rocks.tbog.tblauncher.ui.ListPopup;

public final class ShortcutEntry extends EntryWithTags {

    public static final String SCHEME = "shortcut://";
    public static final String OREO_PREFIX = "oreo-shortcut/";

    private final int dbId;
    public final String packageName;
    public final String intentUri;// TODO: 15/10/18 Use boolean instead of prefix for Oreo shortcuts

    public ShortcutEntry(String id, int dbId, String packageName, String intentUri) {
        super(id);

        this.dbId = dbId;
        this.packageName = packageName;
        this.intentUri = intentUri;
    }

    /**
     * Oreo shortcuts do not have a real intentUri, instead they have a shortcut id
     * and the Android system is responsible for safekeeping the Intent
     */
    public boolean isOreoShortcut() {
        return intentUri.contains(ShortcutEntry.OREO_PREFIX);
    }

    public String getOreoId() {
        // Oreo shortcuts encode their id in the unused intentUri field
        return intentUri.replace(ShortcutEntry.OREO_PREFIX, "");
    }

    public Bitmap getIcon(Context context) {
        byte[] iconBlob = DBHelper.getShortcutIcon(context, this.dbId);

        if (iconBlob == null) {
            return null;
        }

        return BitmapFactory.decodeByteArray(iconBlob, 0, iconBlob.length);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Result methods
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public int getResultLayout() {
        return R.layout.item_shortcut;
    }

    @Override
    public void displayResult(@NonNull View view) {
        Context context = view.getContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        TextView shortcutName = view.findViewById(R.id.item_app_name);

        ResultViewHelper.displayHighlighted(relevanceSource, normalizedName, getName(), relevance, shortcutName);

        TextView tagsView = view.findViewById(R.id.item_app_tag);

        // Hide tags view if tags are empty
        if (getTags().isEmpty()) {
            tagsView.setVisibility(View.GONE);
        } else if (ResultViewHelper.displayHighlighted(relevanceSource, getTags(), relevance, tagsView, context)
                || prefs.getBoolean("tags-enabled", true)) {
            tagsView.setVisibility(View.VISIBLE);
        } else {
            tagsView.setVisibility(View.GONE);
        }

        final ImageView shortcutIcon = view.findViewById(R.id.item_shortcut_icon);
        final ImageView appIcon = view.findViewById(R.id.item_app_icon);

        // Retrieve package icon for this shortcut
        final PackageManager packageManager = context.getPackageManager();
        Drawable appDrawable = null;
        try {
            Intent intent = Intent.parseUri(intentUri, 0);
            List<ResolveInfo> packages = packageManager.queryIntentActivities(intent, 0);
            if (packages.size() > 0) {
                ResolveInfo mainPackage = packages.get(0);
                String packageName = mainPackage.activityInfo.applicationInfo.packageName;
                String activityName = mainPackage.activityInfo.name;
                ComponentName className = new ComponentName(packageName, activityName);
                appDrawable = context.getPackageManager().getActivityIcon(className);
            } else {
                // Can't make sense of the intent URI (Oreo shortcut, or a shortcut from an activity that was removed from an installed app)
                // Retrieve app icon
                try {
                    appDrawable = packageManager.getApplicationIcon(packageName);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
            }
        } catch (PackageManager.NameNotFoundException | URISyntaxException e) {
            Log.e("Shortcut", "get shortcut icon", e);
            return;
        }

        if (prefs.getBoolean("icons-visible", true)) {
            Bitmap icon = getIcon(context);
            if (icon != null) {
                BitmapDrawable drawable = new BitmapDrawable(context.getResources(), icon);
                shortcutIcon.setImageDrawable(drawable);
                appIcon.setImageDrawable(appDrawable);
            } else {
                // No icon for this shortcut, use app icon
                shortcutIcon.setImageDrawable(appDrawable);
                appIcon.setImageResource(android.R.drawable.ic_menu_send);
            }
            if (!prefs.getBoolean("subicon-visible", true)) {
                appIcon.setVisibility(View.GONE);
            }
        } else {
            appIcon.setImageDrawable(null);
            shortcutIcon.setImageDrawable(null);
        }
    }

    @Override
    ListPopup buildPopupMenu(Context context, LinearAdapter adapter, ResultAdapter parent, View parentView) {
        adapter.add(new LinearAdapter.Item(context, R.string.menu_remove_shortcut));
        return inflatePopupMenu(adapter, context);
    }

    @Override
    boolean popupMenuClickHandler(@NonNull Context context, @NonNull LinearAdapter.MenuItem item, int stringId) {
        switch (stringId) {
            case R.string.menu_remove_shortcut:
                return true;
        }
        return super.popupMenuClickHandler(context, item, stringId);
    }
}
