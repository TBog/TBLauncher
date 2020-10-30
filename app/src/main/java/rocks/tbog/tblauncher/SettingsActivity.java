package rocks.tbog.tblauncher;

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
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;

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

import rocks.tbog.tblauncher.db.XmlImport;
import rocks.tbog.tblauncher.preference.ChooseColorDialog;
import rocks.tbog.tblauncher.preference.ConfirmDialog;
import rocks.tbog.tblauncher.preference.CustomDialogPreference;
import rocks.tbog.tblauncher.preference.EditSearchEnginesPreferenceDialog;
import rocks.tbog.tblauncher.preference.QuickListPreferenceDialog;
import rocks.tbog.tblauncher.preference.SliderDialog;
import rocks.tbog.tblauncher.utils.FileUtils;
import rocks.tbog.tblauncher.utils.SystemUiVisibility;
import rocks.tbog.tblauncher.utils.UIColors;
import rocks.tbog.tblauncher.utils.UISizes;

public class SettingsActivity extends AppCompatActivity implements PreferenceFragmentCompat.OnPreferenceStartScreenCallback/*, PreferenceFragmentCompat.OnPreferenceStartFragmentCallback*/ {

    private final static String PREF_THAT_REQUIRE_LAYOUT_UPDATE = "result-list-rounded search-bar-rounded search-bar-gradient";
    private static final int FILE_SELECT_XML_SET = 63;
    private static final int FILE_SELECT_XML_OVERWRITE = 62;
    private static final int FILE_SELECT_XML_APPEND = 61;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
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

        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                setTitle(R.string.menu_popup_launcher_settings);
            }
        });

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (getSupportFragmentManager().popBackStackImmediate()) {
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
        String key = preferenceScreen.getKey();
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        SettingsFragment fragment = new SettingsFragment();
        Bundle args = new Bundle();
        args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, key);
        fragment.setArguments(args);
        ft.replace(R.id.settings_container, fragment, key);
        ft.addToBackStack(key);
        ft.commit();
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
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
                XmlImport.settingsXml(this, uri, method);
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
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

            // import settings
            {
                Preference pref = findPreference("import-settings-set");
                if (pref != null)
                    pref.setOnPreferenceClickListener(preference -> {
                        FileUtils.chooseFile(requireActivity(), FILE_SELECT_XML_SET);
                        return true;
                    });
                pref = findPreference("import-settings-overwrite");
                if (pref != null)
                    pref.setOnPreferenceClickListener(preference -> {
                        FileUtils.chooseFile(requireActivity(), FILE_SELECT_XML_OVERWRITE);
                        return true;
                    });
                pref = findPreference("import-settings-append");
                if (pref != null)
                    pref.setOnPreferenceClickListener(preference -> {
                        FileUtils.chooseFile(requireActivity(), FILE_SELECT_XML_APPEND);
                        return true;
                    });
            }

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());

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
            UIColors.setStatusBarColor((SettingsActivity) getActivity(), UIColors.setAlpha(color, alpha));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                View view = getView();
                if (view != null) {
                    if (sharedPreferences.getBoolean("black-notification-icons", false)) {
                        SystemUiVisibility.setLightStatusBar(view);
                        setActionBarTextColor(0xFF000000);
                    } else {
                        SystemUiVisibility.clearLightStatusBar(view);
                        setActionBarTextColor(0xFFffffff);
                    }
                }
            } else {
                if (UIColors.luminance(color) > .5)
                    setActionBarTextColor(0xFF000000);
                else
                    setActionBarTextColor(0xFFffffff);
            }
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
                    case "notification-bar-color":
                    case "search-bar-color":
                    case "result-list-color":
                    case "result-highlight-color":
                    case "result-text-color":
                    case "result-text2-color":
                    case "contact-action-color":
                    case "search-bar-text-color":
                    case "search-bar-icon-color":
                    case "quick-list-toggle-color":
                    case "quick-list-color":
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
                        dialogFragment = SliderDialog.newInstance(key);
                        break;
                    case "exit-app":
                    case "reset-default-launcher":
                    case "export-tags":
                    case "export-favs":
                    case "export-apps":
                    case "export-interface":
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
                    default:
                        throw new RuntimeException("CustomDialogPreference \"" + key + "\" has no dialog defined");
                }
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

            if (PREF_THAT_REQUIRE_LAYOUT_UPDATE.contains(key))
                TBApplication.getApplication(activity).requireLayoutUpdate();

            // rebind and relayout all visible views because I can't find how to rebind only the current view
            getListView().getAdapter().notifyDataSetChanged();

            TBApplication.liveWallpaper(activity).onPrefChanged(sharedPreferences, key);

            switch (key) {
                case "notification-bar-color":
                case "notification-bar-alpha": {
                    int color = UIColors.getColor(sharedPreferences, "notification-bar-color");
                    int alpha = UIColors.getAlpha(sharedPreferences, "notification-bar-alpha");
                    UIColors.setStatusBarColor(activity, UIColors.setAlpha(color, alpha));
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                        if (UIColors.luminance(color) > .5)
                            setActionBarTextColor(0xFF000000);
                        else
                            setActionBarTextColor(0xFFffffff);
                    }
                    break;
                }
                case "black-notification-icons":
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        View view = getView();
                        if (view == null)
                            break;
                        if (sharedPreferences.getBoolean("black-notification-icons", false)) {
                            SystemUiVisibility.setLightStatusBar(view);
                            setActionBarTextColor(0xFF000000);
                        } else {
                            SystemUiVisibility.clearLightStatusBar(view);
                            setActionBarTextColor(0xFFffffff);
                        }
                    }
                    break;
                case "quick-list-color":
                    // static entities will change color based on luminance
                    // fallthrough
                case "quick-list-toggle-color":
                    // toggle animation is also caching the color
                    TBApplication.quickList(activity).onFavoritesChanged();
                    // fallthrough
                case "result-highlight-color":
                case "result-text-color":
                case "result-text2-color":
                case "contact-action-color":
                case "search-bar-text-color":
                case "search-bar-icon-color":
                    UIColors.resetCache();
                    break;
                case "result-text-size":
                case "result-text2-size":
                case "result-icon-size":
                    UISizes.resetCache();
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
                    TBApplication.iconsHandler(activity).onPrefChanged(sharedPreferences);
                    TBApplication.drawableCache(activity).clearCache();
                    TBApplication.quickList(activity).onFavoritesChanged();
                    break;
                case "tags-enabled": {
                    boolean useTags = sharedPreferences.getBoolean("tags-enabled", true);
                    SwitchPreference preference = findPreference("fuzzy-search-tags");
                    if (preference != null)
                        preference.setChecked(useTags);
                    else
                        sharedPreferences.edit().putBoolean("fuzzy-search-tags", useTags).apply();
                    break;
                }
                case "quick-list-text-visible":
                case "quick-list-icons-visible":
                case "quick-list-show-badge":
                    TBApplication.quickList(activity).onFavoritesChanged();
                    break;
                case "cache-drawable":
                case "cache-half-apps":
                    TBApplication.drawableCache(activity).onPrefChanged(activity, sharedPreferences);
                    break;
                case "enable-search":
                case "enable-url":
                case "enable-calculator":
                    TBApplication.dataHandler(activity).reloadProviders();
                    break;
            }
        }

        private void setActionBarTextColor(int color) {
            Activity activity = getActivity();
            ActionBar actionBar = activity instanceof AppCompatActivity
                    ? ((AppCompatActivity) activity).getSupportActionBar()
                    : null;
            CharSequence title = actionBar != null ? actionBar.getTitle() : null;
            if (title == null)
                return;

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
    }
}