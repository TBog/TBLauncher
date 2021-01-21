package rocks.tbog.tblauncher;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import java.io.File;

import rocks.tbog.tblauncher.db.XmlImport;
import rocks.tbog.tblauncher.preference.BaseListPreferenceDialog;
import rocks.tbog.tblauncher.preference.ChooseColorDialog;
import rocks.tbog.tblauncher.preference.ConfirmDialog;
import rocks.tbog.tblauncher.preference.CustomDialogPreference;
import rocks.tbog.tblauncher.preference.EditSearchEnginesPreferenceDialog;
import rocks.tbog.tblauncher.preference.EditSearchHintPreferenceDialog;
import rocks.tbog.tblauncher.preference.QuickListPreferenceDialog;
import rocks.tbog.tblauncher.preference.SliderDialog;
import rocks.tbog.tblauncher.ui.PleaseWaitDialog;
import rocks.tbog.tblauncher.utils.FileUtils;
import rocks.tbog.tblauncher.utils.PrefCache;
import rocks.tbog.tblauncher.utils.SystemUiVisibility;
import rocks.tbog.tblauncher.utils.UIColors;
import rocks.tbog.tblauncher.utils.UISizes;
import rocks.tbog.tblauncher.utils.UITheme;
import rocks.tbog.tblauncher.utils.Utilities;

public class SettingsActivity extends AppCompatActivity implements PreferenceFragmentCompat.OnPreferenceStartScreenCallback/*, PreferenceFragmentCompat.OnPreferenceStartFragmentCallback*/ {

    private final static String PREF_THAT_REQUIRE_LAYOUT_UPDATE = "result-list-rounded search-bar-rounded search-bar-gradient";
    private static final int FILE_SELECT_XML_SET = 63;
    private static final int FILE_SELECT_XML_OVERWRITE = 62;
    private static final int FILE_SELECT_XML_APPEND = 61;

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
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
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
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
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
        finish();
        startActivity(new Intent(this, getClass()));
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
            if (count == 0) {
                setTitle(R.string.menu_popup_launcher_settings);
            }
            return true;
        }
        return super.onSupportNavigateUp();
    }

//    @Override
//    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
//        // Instantiate the new Fragment
//        final Bundle args = pref.getExtras();
//        final Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(
//                getClassLoader(),
//                pref.getFragment());
//        fragment.setArguments(args);
//        fragment.setTargetFragment(caller, 0);
//        // Replace the existing Fragment with the new Fragment
//        getSupportFragmentManager().beginTransaction()
//                .replace(R.id.settings_container, fragment)
//                .addToBackStack(null)
//                .commit();
//        return true;
//    }

    @Override
    public boolean onPreferenceStartScreen(PreferenceFragmentCompat caller, PreferenceScreen preferenceScreen) {
        final String key = preferenceScreen.getKey();
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        SettingsFragment fragment = new SettingsFragment();
        Bundle args = new Bundle();
        args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, key);
        fragment.setArguments(args);
        ft.replace(R.id.settings_container, fragment, key);
        ft.addToBackStack(key);
        ft.commit();

        setTitle(preferenceScreen.getTitle());
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            XmlImport.SettingsData.Method method = null;
            switch (requestCode) {
                case FILE_SELECT_XML_APPEND:
                    method = XmlImport.SettingsData.Method.APPEND;
                    break;
                case FILE_SELECT_XML_OVERWRITE:
                    method = XmlImport.SettingsData.Method.OVERWRITE;
                    break;
                case FILE_SELECT_XML_SET:
                    method = XmlImport.SettingsData.Method.SET;
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
                    final XmlImport.SettingsData.Method importMethod = method;
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

    public static class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
        static final String FRAGMENT_TAG = SettingsFragment.class.getName();
        private static final String DIALOG_FRAGMENT_TAG = "androidx.preference.PreferenceFragment.DIALOG";
        private static final String TAG = "Settings";

        public SettingsFragment() {
            super();
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            if ("feature-holder".equals(rootKey))
                setPreferencesFromResource(R.xml.preference_features, rootKey);
            else
                setPreferencesFromResource(R.xml.preferences, rootKey);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                removePreference("black-notification-icons");
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                removePreference("pin-auto-confirm");
            }
            ActionBar actionBar = ((SettingsActivity) requireActivity()).getSupportActionBar();
            if (actionBar != null) {
                // we can change the theme from the options menu
                removePreference("settings-theme");
            }

            final Activity activity = requireActivity();
            // import settings
            {
                Preference pref = findPreference("import-settings-set");
                if (pref != null)
                    pref.setOnPreferenceClickListener(preference -> {
                        FileUtils.chooseSettingsFile(activity, FILE_SELECT_XML_SET);
                        return true;
                    });
                pref = findPreference("import-settings-overwrite");
                if (pref != null)
                    pref.setOnPreferenceClickListener(preference -> {
                        FileUtils.chooseSettingsFile(activity, FILE_SELECT_XML_OVERWRITE);
                        return true;
                    });
                pref = findPreference("import-settings-append");
                if (pref != null)
                    pref.setOnPreferenceClickListener(preference -> {
                        FileUtils.chooseSettingsFile(activity, FILE_SELECT_XML_APPEND);
                        return true;
                    });
            }

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());

            // quick-list
            {
                Preference pref = findPreference("quick-list-enabled");
                // if we don't have the toggle in this screen we need to apply dependency by hand
                if (pref == null) {
                    // only show the category if we use the quick list
                    Preference section = findPreference("quick-list-section");
                    if (section != null)
                        section.setVisible(sharedPreferences.getBoolean("quick-list-enabled", true));
                }
            }

            final ListPreference iconsPack = findPreference("icons-pack");
            if (iconsPack != null)
                iconsPack.setEnabled(false);

            Runnable iconsPackLoad = () -> {
                if (iconsPack == null)
                    return;
                SettingsFragment.this.setListPreferenceIconsPacksData(iconsPack);
                new Handler(Looper.getMainLooper()).post(() -> iconsPack.setEnabled(true));
            };

            if (savedInstanceState == null) {
                // Run asynchronously to open settings fast
                AsyncTask.execute(iconsPackLoad);
            } else {
                // Run synchronously to ensure preferences can be restored from state
                iconsPackLoad.run();
            }

        }

        private void removePreference(String key) {
            Preference pref = findPreference(key);
            if (pref != null && pref.getParent() != null)
                pref.getParent().removePreference(pref);
        }

        private void setListPreferenceIconsPacksData(ListPreference lp) {
            Context context = getContext();
            if (context == null)
                return;
            IconsHandler iph = TBApplication.getApplication(context).iconsHandler();

            CharSequence[] entries = new CharSequence[iph.getIconPackNames().size() + 1];
            CharSequence[] entryValues = new CharSequence[iph.getIconPackNames().size() + 1];

            int i = 0;
            entries[0] = this.getString(R.string.icons_pack_default_name);
            entryValues[0] = "default";
            for (String packageIconsPack : iph.getIconPackNames().keySet()) {
                entries[++i] = iph.getIconPackNames().get(packageIconsPack);
                entryValues[i] = packageIconsPack;
            }

            lp.setEntries(entries);
            lp.setDefaultValue("default");
            lp.setEntryValues(entryValues);
        }

        @Override
        public void onResume() {
            super.onResume();
            SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
            sharedPreferences.registerOnSharedPreferenceChangeListener(this);

            int color = UIColors.getColor(sharedPreferences, "notification-bar-color");
            int alpha = UIColors.getAlpha(sharedPreferences, "notification-bar-alpha");
            SettingsActivity activity = (SettingsActivity) requireActivity();
            UIColors.setStatusBarColor(activity, UIColors.setAlpha(color, alpha));
            int actionBarTextColor = activity.getTitleColor();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                View view = getView();
                if (view != null) {
                    if (sharedPreferences.getBoolean("black-notification-icons", false)) {
                        SystemUiVisibility.setLightStatusBar(view);
                        actionBarTextColor = 0xFF000000;
                    } else {
                        SystemUiVisibility.clearLightStatusBar(view);
                        actionBarTextColor = 0xFFffffff;
                    }
                }
            } else {
                actionBarTextColor = UIColors.isColorLight(color) ? 0xFF000000 : 0xFFffffff;
            }
            setActionBarTextColor(activity, actionBarTextColor);
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onDisplayPreferenceDialog(Preference preference) {
            // Try if the preference is one of our custom Preferences
            DialogFragment dialogFragment = null;
            if (preference instanceof CustomDialogPreference) {
                // Create a new instance of CustomDialog with the key of the related Preference
                String key = preference.getKey();
                Log.d(TAG, "onDisplayPreferenceDialog " + key);
                switch (key) {
                    case "icon-background-argb":
                    case "notification-bar-color":
                    case "search-bar-color":
                    case "result-list-color":
                    case "result-ripple-color":
                    case "result-highlight-color":
                    case "result-text-color":
                    case "result-text2-color":
                    case "contact-action-color":
                    case "search-bar-text-color":
                    case "search-bar-icon-color":
                    case "quick-list-toggle-color":
                    case "quick-list-color":
                    case "quick-list-ripple-color":
                    case "popup-background-argb":
                    case "popup-border-argb":
                    case "popup-ripple-color":
                    case "popup-text-color":
                    case "popup-title-color":
                        dialogFragment = ChooseColorDialog.newInstance(key);
                        break;
                    case "notification-bar-alpha":
                    case "search-bar-alpha":
                    case "result-list-alpha":
                    case "search-bar-size":
                    case "quick-list-alpha":
                    case "quick-list-size":
                    case "result-text-size":
                    case "result-text2-size":
                    case "result-icon-size":
                    case "result-history-size":
                    case "result-history-adaptive":
                    case "result-search-cap":
                    case "icon-contrast":
                    case "icon-brightness":
                        dialogFragment = SliderDialog.newInstance(key);
                        break;
                    case "reset-preferences":
                    case "exit-app":
                    case "reset-default-launcher":
                    case "export-tags":
                    case "export-favs":
                    case "export-apps":
                    case "export-interface":
                    case "export-widgets":
                    case "export-history":
                    case "export-backup":
                    case "unlimited-search-cap":
                        dialogFragment = ConfirmDialog.newInstance(key);
                        break;
                    case "quick-list-content":
                        dialogFragment = QuickListPreferenceDialog.newInstance(key);
                        break;
                    case "reset-search-engines":
                    case "edit-search-engines":
                    case "add-search-engine":
                        dialogFragment = EditSearchEnginesPreferenceDialog.newInstance(key);
                        break;
                    case "reset-search-hint":
                    case "edit-search-hint":
                    case "add-search-hint":
                        dialogFragment = EditSearchHintPreferenceDialog.newInstance(key);
                        break;
                    default:
                        throw new RuntimeException("CustomDialogPreference \"" + key + "\" has no dialog defined");
                }
            } else if (preference instanceof ListPreference) {
                dialogFragment = BaseListPreferenceDialog.newInstance(preference.getKey());
            }

            // If it was one of our custom Preferences, show its dialog
            if (dialogFragment != null) {
                dialogFragment.setTargetFragment(this, 0);
                final FragmentManager fm = this.getParentFragmentManager();
                dialogFragment.show(fm, DIALOG_FRAGMENT_TAG);
            }
            // Could not be handled here. Try with the super method.
            else {
                super.onDisplayPreferenceDialog(preference);
            }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            SettingsActivity activity = (SettingsActivity) getActivity();
            if (activity == null)
                return;

            // rebind and relayout all visible views because I can't find how to rebind only the current view
            getListView().getAdapter().notifyDataSetChanged();

            SettingsActivity.onSharedPreferenceChanged(activity, sharedPreferences, key);
        }
    }

    private static void setActionBarTextColor(Activity activity, int color) {
        ActionBar actionBar = activity instanceof AppCompatActivity
                ? ((AppCompatActivity) activity).getSupportActionBar()
                : null;
        CharSequence title = actionBar != null ? actionBar.getTitle() : null;
        if (title == null)
            return;
        activity.setTitleColor(color);

        Drawable arrow = ContextCompat.getDrawable(activity, R.drawable.ic_arrow_back);
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

    public static void onSharedPreferenceChanged(Context context, SharedPreferences sharedPreferences, String key) {
        if (PREF_THAT_REQUIRE_LAYOUT_UPDATE.contains(key))
            TBApplication.getApplication(context).requireLayoutUpdate();

        TBApplication.liveWallpaper(context).onPrefChanged(sharedPreferences, key);

        switch (key) {
            case "notification-bar-color":
            case "notification-bar-alpha": {
                int color = UIColors.getColor(sharedPreferences, "notification-bar-color");
                int alpha = UIColors.getAlpha(sharedPreferences, "notification-bar-alpha");
                Activity activity = Utilities.getActivity(context);
                if (activity instanceof SettingsActivity) {
                    UIColors.setStatusBarColor((SettingsActivity) activity, UIColors.setAlpha(color, alpha));
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                        setActionBarTextColor(activity, UIColors.isColorLight(color) ? 0xFF000000 : 0xFFffffff);
                    }
                }
                break;
            }
            case "black-notification-icons":
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Activity activity = Utilities.getActivity(context);
                    View view = activity != null ? activity.findViewById(android.R.id.content) : null;
                    if (view == null)
                        break;
                    if (sharedPreferences.getBoolean("black-notification-icons", false)) {
                        SystemUiVisibility.setLightStatusBar(view);
                        setActionBarTextColor(activity, 0xFF000000);
                    } else {
                        SystemUiVisibility.clearLightStatusBar(view);
                        setActionBarTextColor(activity, 0xFFffffff);
                    }
                }
                break;
            case "icon-background-argb":
                TBApplication.drawableCache(context).clearCache();
                // fallthrough
            case "quick-list-color":
            case "quick-list-ripple-color":
                // static entities will change color based on luminance
                // fallthrough
            case "quick-list-toggle-color":
                // toggle animation is also caching the color
                TBApplication.quickList(context).onFavoritesChanged();
                // fallthrough
            case "result-list-color":
            case "result-ripple-color":
            case "result-list-alpha":
            case "result-highlight-color":
            case "result-text-color":
            case "result-text2-color":
            case "contact-action-color":
            case "search-bar-text-color":
            case "search-bar-icon-color":
            case "popup-background-argb":
            case "popup-border-argb":
            case "popup-ripple-color":
            case "popup-text-color":
            case "popup-title-color":
            case "icon-contrast":
            case "icon-brightness":
                UIColors.resetCache();
                break;
            case "result-text-size":
            case "result-text2-size":
            case "result-icon-size":
                UISizes.resetCache();
                break;
            case "result-history-size":
            case "result-history-adaptive":
            case "fuzzy-search-tags":
            case "result-search-cap":
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
                TBApplication.quickList(context).onFavoritesChanged();
                break;
            case "tags-enabled": {
                boolean useTags = sharedPreferences.getBoolean("tags-enabled", true);
                Activity activity = Utilities.getActivity(context);
                Fragment fragment = null;
                if (activity instanceof SettingsActivity)
                    fragment = ((SettingsActivity) activity).getSupportFragmentManager().findFragmentByTag(SettingsFragment.FRAGMENT_TAG);
                SwitchPreference preference = null;
                if (fragment instanceof SettingsFragment)
                    preference = ((SettingsFragment) fragment).findPreference("fuzzy-search-tags");
                if (preference != null)
                    preference.setChecked(useTags);
                else
                    sharedPreferences.edit().putBoolean("fuzzy-search-tags", useTags).apply();
                break;
            }
            case "quick-list-text-visible":
            case "quick-list-icons-visible":
            case "quick-list-show-badge":
                TBApplication.quickList(context).onFavoritesChanged();
                break;
            case "cache-drawable":
            case "cache-half-apps":
                TBApplication.drawableCache(context).onPrefChanged(context, sharedPreferences);
                break;
            case "enable-search":
            case "enable-url":
            case "enable-calculator":
                TBApplication.dataHandler(context).reloadProviders();
                break;
        }
    }
}