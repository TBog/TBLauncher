package rocks.tbog.tblauncher.settings;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.collection.ArraySet;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.TBLauncherActivity;
import rocks.tbog.tblauncher.db.ExportedData;
import rocks.tbog.tblauncher.db.XmlImport;
import rocks.tbog.tblauncher.ui.dialog.PleaseWaitDialog;
import rocks.tbog.tblauncher.utils.FileUtils;
import rocks.tbog.tblauncher.utils.PrefCache;
import rocks.tbog.tblauncher.utils.PrefOrderedListHelper;
import rocks.tbog.tblauncher.utils.SystemUiVisibility;
import rocks.tbog.tblauncher.utils.UIColors;
import rocks.tbog.tblauncher.utils.UISizes;
import rocks.tbog.tblauncher.utils.UITheme;
import rocks.tbog.tblauncher.utils.Utilities;

public class SettingsActivity extends AppCompatActivity implements PreferenceFragmentCompat.OnPreferenceStartScreenCallback, SharedPreferences.OnSharedPreferenceChangeListener/*, PreferenceFragmentCompat.OnPreferenceStartFragmentCallback*/ {

    private final static String INTENT_EXTRA_BACK_STACK_TAGS = "backStackTagList";

    private final static ArraySet<String> PREF_THAT_REQUIRE_LAYOUT_UPDATE = new ArraySet<>(Arrays.asList(
        "result-list-argb", "result-ripple-color", "result-list-radius", "result-list-row-height",
        "notification-bar-argb", "notification-bar-gradient", "black-notification-icons",
        "navigation-bar-argb",
        "search-bar-height", "search-bar-text-size", "search-bar-radius", "search-bar-gradient", "search-bar-at-bottom",
        "search-bar-argb", "search-bar-text-color", "search-bar-icon-color",
        "search-bar-ripple-color", "search-bar-cursor-argb", "enable-suggestions-keyboard",
        "search-bar-layout", "quick-list-position"
    ));

    public static final int FILE_SELECT_XML_SET = 63;
    public static final int FILE_SELECT_XML_OVERWRITE = 62;
    public static final int FILE_SELECT_XML_APPEND = 61;
    public static final int ENABLE_DEVICE_ADMIN = 60;
    private static final String TAG = "SettAct";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        int theme = UITheme.getSettingsTheme(this);
        if (theme != UITheme.ID_NULL)
            setTheme(theme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (savedInstanceState == null) {
            // Create the fragment only when the activity is created for the first time.
            // ie. not after orientation changes
            Fragment fragment = getSupportFragmentManager().findFragmentByTag(SettingsFragment.FRAGMENT_TAG);
            if (fragment == null) {
                fragment = new SettingsFragment();
            }

            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_container, fragment, SettingsFragment.FRAGMENT_TAG)
                .commit();

            restoreBackStack();
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        applyNotificationBarColor(sharedPreferences, this);
    }

    @Override
    public void onPause() {
        super.onPause();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    private void restoreBackStack() {
        Intent intent = getIntent();
        if (intent == null)
            return;
        ArrayList<String> backStackEntryList = intent.getStringArrayListExtra(INTENT_EXTRA_BACK_STACK_TAGS);
        if (backStackEntryList != null)
            for (String key : backStackEntryList)
                if (key != null)
                    addToBackStack(key);
    }

    private void addToBackStack(@NonNull String key) {
        View page = findViewById(R.id.settings_page);
        if (page != null)
            page.setVisibility(View.VISIBLE);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        SettingsFragment fragment = new SettingsFragment();
        Bundle args = new Bundle();
        args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, key);
        fragment.setArguments(args);
        ft.replace(R.id.settings_page, fragment, key);
        ft.addToBackStack(key);
        ft.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        String[] themeNames = getResources().getStringArray(R.array.settingsThemeEntries);
        for (String name : themeNames)
            menu.add(name);
        return true;
    }

    @SuppressLint("ApplySharedPref")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getTitle() != null) {
            String itemName = item.getTitle().toString();

            String[] themeNames = getResources().getStringArray(R.array.settingsThemeEntries);
            String[] themeValues = getResources().getStringArray(R.array.settingsThemeValues);

            for (int themeIdx = 0; themeIdx < themeNames.length; themeIdx++) {
                String name = themeNames[themeIdx];
                if (itemName.equals(name)) {
                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
                    sharedPreferences.edit().putString("settings-theme", themeValues[themeIdx]).commit();
                    restart();
                    return true;
                }
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void restart() {
        // save backstack
        FragmentManager fm = getSupportFragmentManager();
        int backStackEntryCount = fm.getBackStackEntryCount();
        ArrayList<String> backStackTags = null;
        if (backStackEntryCount > 0) {
            backStackTags = new ArrayList<>(backStackEntryCount);
            for (int idx = 0; idx < backStackEntryCount; idx += 1) {
                FragmentManager.BackStackEntry entry = fm.getBackStackEntryAt(idx);
                String tag = entry.getName();
                backStackTags.add(tag);
            }
        }

        // close current activity
        finish();

        // start new activity
        Intent activityIntent = new Intent(this, getClass());
        if (backStackTags != null) {
            // remember the back stack pages so we can restore them
            activityIntent.putStringArrayListExtra(INTENT_EXTRA_BACK_STACK_TAGS, backStackTags);
        }
        startActivity(activityIntent);

        // set transition animation
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    protected void onTitleChanged(CharSequence title, int color) {
        super.onTitleChanged(title, color);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            if (color != 0 && !(title instanceof Spannable)) {
                SpannableString ss = new SpannableString(title);
                ss.setSpan(new ForegroundColorSpan(color), 0, title.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                actionBar.setTitle(ss);
            } else {
                actionBar.setTitle(title);
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (getSupportFragmentManager().popBackStackImmediate()) {
            final int count = getSupportFragmentManager().getBackStackEntryCount();
            CharSequence title = null;
            if (count == 0) {
                View page = findViewById(R.id.settings_page);
                if (page != null)
                    page.setVisibility(View.GONE);
            }
            if (count > 0) {
                String tag = getSupportFragmentManager().getBackStackEntryAt(count - 1).getName();
                if (tag != null) {
                    Fragment fragment = getSupportFragmentManager().findFragmentByTag(SettingsFragment.FRAGMENT_TAG);
                    if (fragment instanceof SettingsFragment) {
                        Preference preference = ((SettingsFragment) fragment).findPreference(tag);
                        if (preference != null)
                            title = preference.getTitle();
                    }
                }
            }
            if (title != null)
                setTitle(title);
            else
                setTitle(R.string.menu_popup_launcher_settings);
            return true;
        }
        return super.onSupportNavigateUp();
    }

    @Override
    public boolean onPreferenceStartScreen(@NonNull PreferenceFragmentCompat caller, @NonNull PreferenceScreen preferenceScreen) {
        // when opening a screen preference from root, start a new backstack
        if ("preferences-root".equals(caller.getPreferenceScreen().getKey())) {
            clearBackStack(getSupportFragmentManager());
        }
        final String key = preferenceScreen.getKey();
        addToBackStack(key);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult request=" + requestCode + " result=" + resultCode);
        if (requestCode == ENABLE_DEVICE_ADMIN) {
            if (resultCode != RESULT_OK) {
                Toast.makeText(this, "Failed!", Toast.LENGTH_SHORT).show();
            }
        } else if (resultCode == RESULT_OK) {
            ExportedData.Method method = null;
            switch (requestCode) {
                case FILE_SELECT_XML_APPEND:
                    method = ExportedData.Method.APPEND;
                    break;
                case FILE_SELECT_XML_OVERWRITE:
                    method = ExportedData.Method.OVERWRITE;
                    break;
                case FILE_SELECT_XML_SET:
                    method = ExportedData.Method.SET;
                    break;
            }
            if (method != null) {
                Uri uri = data != null ? data.getData() : null;
                File importedFile = FileUtils.copyFile(this, uri, "imported.xml");
                if (importedFile != null) {
                    PleaseWaitDialog dialog = new PleaseWaitDialog();
                    // set args
                    {
                        Bundle args = new Bundle();
                        //args.putString(PleaseWaitDialog.ARG_TITLE, getString(R.string.import_dialog_title));
                        args.putString(PleaseWaitDialog.ARG_DESCRIPTION, getString(R.string.import_dialog_description));
                        dialog.setArguments(args);
                    }
                    final ExportedData.Method importMethod = method;
                    dialog.setWork(() -> {
                        Activity activity = Utilities.getActivity(dialog.getContext());
                        if (activity != null) {
                            if (!XmlImport.settingsXml(activity, importedFile, importMethod)) {
                                Toast.makeText(activity, R.string.error_fail_import, Toast.LENGTH_LONG).show();
                                dialog.dismiss();
                            }
                        }
                        dialog.onWorkFinished();
                    });
                    dialog.show(getSupportFragmentManager(), "load_imported");
                } else {
                    Toast.makeText(this, R.string.error_fail_import, Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        onSharedPreferenceChanged(this, sharedPreferences, key);
    }

    /**
     * Remove all entries from the backStack of this fragmentManager.
     *
     * @param fragmentManager the fragmentManager to clear.
     */
    private void clearBackStack(FragmentManager fragmentManager) {
        View page = findViewById(R.id.settings_page);
        if (page != null)
            page.setVisibility(View.GONE);
        if (fragmentManager.getBackStackEntryCount() > 0) {
            FragmentManager.BackStackEntry entry = fragmentManager.getBackStackEntryAt(0);
            fragmentManager.popBackStack(entry.getId(), FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }

    @SuppressWarnings("deprecation")
    private static void setActionBarTextColor(Activity activity, int color) {
        ActionBar actionBar = activity instanceof AppCompatActivity
            ? ((AppCompatActivity) activity).getSupportActionBar()
            : null;
        CharSequence title = actionBar != null ? actionBar.getTitle() : null;
        if (title == null)
            return;
        activity.setTitleColor(color);

        Drawable arrow = AppCompatResources.getDrawable(activity, R.drawable.ic_arrow_back);
        if (arrow != null) {
            arrow = DrawableCompat.wrap(arrow);
            DrawableCompat.setTint(arrow, color);
            actionBar.setHomeAsUpIndicator(arrow);
        }

        SpannableString text = new SpannableString(title);
        ForegroundColorSpan[] spansToRemove = text.getSpans(0, text.length(), ForegroundColorSpan.class);
        for (ForegroundColorSpan span : spansToRemove) {
            text.removeSpan(span);
        }
        text.setSpan(new ForegroundColorSpan(color), 0, text.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        actionBar.setTitle(text);
    }

    private static void applyNotificationBarColor(@NonNull SharedPreferences sharedPreferences, @Nullable Context context) {
        int color = UIColors.getColor(sharedPreferences, "notification-bar-argb");
        // keep the bars opaque to avoid white text on white background by mistake
        int alpha = 0xFF;//UIColors.getAlpha(sharedPreferences, "notification-bar-alpha");
        Activity activity = Utilities.getActivity(context);
        if (activity instanceof SettingsActivity)
            UIColors.setStatusBarColor((SettingsActivity) activity, UIColors.setAlpha(color, alpha));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View view = activity != null ? activity.findViewById(android.R.id.content) : null;
            if (view == null && activity != null)
                view = activity.getWindow() != null ? activity.getWindow().getDecorView() : null;
            if (view != null) {
                if (sharedPreferences.getBoolean("black-notification-icons", false)) {
                    SystemUiVisibility.setLightStatusBar(view);
                } else {
                    SystemUiVisibility.clearLightStatusBar(view);
                }
            }
        }
        setActionBarTextColor(activity, UIColors.getTextContrastColor(color));
    }

    private static void applyNavigationBarColor(@NonNull SharedPreferences sharedPreferences, @Nullable Context context) {
        int color = UIColors.getColor(sharedPreferences, "navigation-bar-argb");
        Activity activity = Utilities.getActivity(context);
        if (activity instanceof SettingsActivity)
            UIColors.setNavigationBarColor((SettingsActivity) activity, color, color);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            View view = activity != null ? activity.findViewById(android.R.id.content) : null;
            if (view == null && activity != null)
                view = activity.getWindow() != null ? activity.getWindow().getDecorView() : null;
            if (view != null) {
                if (UIColors.isColorLight(color)) {
                    SystemUiVisibility.setLightNavigationBar(view);
                } else {
                    SystemUiVisibility.clearLightNavigationBar(view);
                }
            }
        }
    }

    public static void onSharedPreferenceChanged(Context context, SharedPreferences sharedPreferences, String key) {
        TBApplication app = TBApplication.getApplication(context);

        if (PREF_THAT_REQUIRE_LAYOUT_UPDATE.contains(key))
            app.requireLayoutUpdate();

        TBLauncherActivity activity = app.launcherActivity();

        if (activity != null)
            activity.liveWallpaper.onPrefChanged(sharedPreferences, key);

        switch (key) {
            case "notification-bar-argb":
            case "black-notification-icons":
                applyNotificationBarColor(sharedPreferences, context);
                break;
            case "navigation-bar-argb":
                applyNavigationBarColor(sharedPreferences, context);
                break;
            case "icon-scale-red":
            case "icon-scale-green":
            case "icon-scale-blue":
            case "icon-scale-alpha":
            case "icon-hue":
            case "icon-contrast":
            case "icon-brightness":
            case "icon-saturation":
            case "icon-background-argb":
            case "matrix-contacts":
            case "icons-visible":
                TBApplication.drawableCache(context).clearCache();
                if (activity != null)
                    activity.refreshSearchRecords();
                // fallthrough
            case "quick-list-argb":
            case "quick-list-ripple-color":
                // static entities will change color based on luminance
                // fallthrough
            case "quick-list-toggle-color":
                // toggle animation is also caching the color
                if (activity != null)
                    activity.queueDockReload();
                // fallthrough
            case "result-list-argb":
            case "result-ripple-color":
            case "result-highlight-color":
            case "result-text-color":
            case "result-text2-color":
            case "result-shadow-color":
            case "contact-action-color":
                if (activity != null)
                    activity.refreshSearchRecords();
                // fallthrough
            case "search-bar-text-color":
            case "search-bar-shadow-color":
            case "popup-background-argb":
            case "popup-border-argb":
            case "popup-ripple-color":
            case "popup-text-color":
            case "popup-title-color":
            case "popup-shadow-color":
                UIColors.resetCache();
                break;
            case "quick-list-icon-size":
                if (activity != null)
                    activity.queueDockReload();
                // fallthrough
            case "result-text-size":
            case "result-text2-size":
            case "result-icon-size":
            case "result-shadow-radius":
            case "result-shadow-dx":
            case "result-shadow-dy":
            case "result-list-row-height":
                if (activity != null)
                    activity.refreshSearchRecords();
                // fallthrough
            case "tags-menu-icon-size":
            case "search-bar-shadow-dx":
            case "search-bar-shadow-dy":
            case "search-bar-shadow-radius":
            case "popup-corner-radius":
            case "popup-shadow-dx":
            case "popup-shadow-dy":
            case "popup-shadow-radius":
                UISizes.resetCache();
                break;
            case "result-history-size":
            case "result-history-adaptive":
            case "fuzzy-search-tags":
            case "result-search-cap":
            case "tags-menu-icons":
            case "loading-icon":
            case "tags-menu-untagged":
            case "tags-menu-untagged-index":
            case "result-popup-order":
                PrefCache.resetCache();
                break;
            case "adaptive-shape":
            case "force-adaptive":
            case "force-shape":
            case "icons-pack":
            case "contact-pack-mask":
            case "contacts-shape":
            case "shortcut-pack-mask":
            case "shortcut-shape":
            case "shortcut-pack-badge-mask":
                TBApplication.iconsHandler(context).onPrefChanged(sharedPreferences);
                TBApplication.drawableCache(context).clearCache();
                if (activity != null)
                    activity.queueDockReload();
                break;
            case "tags-enabled": {
                boolean useTags = sharedPreferences.getBoolean("tags-enabled", true);
                Activity settingsActivity = Utilities.getActivity(context);
                Fragment fragment = null;
                if (settingsActivity instanceof SettingsActivity)
                    fragment = ((SettingsActivity) settingsActivity).getSupportFragmentManager().findFragmentByTag(SettingsFragment.FRAGMENT_TAG);
                SwitchPreference preference = null;
                if (fragment instanceof SettingsFragment)
                    preference = ((SettingsFragment) fragment).findPreference("fuzzy-search-tags");
                if (preference != null)
                    preference.setChecked(useTags);
                else
                    sharedPreferences.edit().putBoolean("fuzzy-search-tags", useTags).apply();
                break;
            }
            case "quick-list-enabled":
            case "quick-list-text-visible":
            case "quick-list-icons-visible":
            case "quick-list-show-badge":
            case "quick-list-columns":
            case "quick-list-rows":
            case "quick-list-rtl":
                if (activity != null)
                    activity.queueDockReload();
                break;
            case "cache-drawable":
            case "cache-half-apps":
                TBApplication.drawableCache(context).onPrefChanged(context, sharedPreferences);
                break;
            case "enable-search":
            case "enable-url":
            case "enable-calculator":
            case "enable-dial":
            case "enable-contacts":
            case "selected-contact-mime-types":
            case "shortcut-dynamic-in-results":
                TBApplication.dataHandler(context).reloadProviders();
                break;
            case "root-mode":
                if (sharedPreferences.getBoolean("root-mode", false) &&
                    !TBApplication.rootHandler(context).isRootAvailable()) {
                    //show error dialog
                    new AlertDialog.Builder(context).setMessage(R.string.root_mode_error)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            sharedPreferences.edit().putBoolean("root-mode", false).apply();
                        }).show();
                }
                TBApplication.rootHandler(context).resetRootHandler(sharedPreferences);
                break;
            case "tags-menu-list":
                PrefOrderedListHelper.syncOrderedList(sharedPreferences, "tags-menu-list", "tags-menu-order");
                break;
        }
    }
}