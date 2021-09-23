package rocks.tbog.tblauncher;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.util.Log;
import android.util.Pair;
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
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import rocks.tbog.tblauncher.dataprovider.FavProvider;
import rocks.tbog.tblauncher.db.XmlImport;
import rocks.tbog.tblauncher.drawable.SizeWrappedDrawable;
import rocks.tbog.tblauncher.entry.ActionEntry;
import rocks.tbog.tblauncher.entry.AppEntry;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.entry.FilterEntry;
import rocks.tbog.tblauncher.entry.StaticEntry;
import rocks.tbog.tblauncher.entry.TagEntry;
import rocks.tbog.tblauncher.preference.BaseListPreferenceDialog;
import rocks.tbog.tblauncher.preference.BaseMultiSelectListPreferenceDialog;
import rocks.tbog.tblauncher.preference.ChooseColorDialog;
import rocks.tbog.tblauncher.preference.ConfirmDialog;
import rocks.tbog.tblauncher.preference.CustomDialogPreference;
import rocks.tbog.tblauncher.preference.EditSearchEnginesPreferenceDialog;
import rocks.tbog.tblauncher.preference.EditSearchHintPreferenceDialog;
import rocks.tbog.tblauncher.preference.OrderListPreferenceDialog;
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

    private final static ArraySet<String> PREF_THAT_REQUIRE_LAYOUT_UPDATE = new ArraySet<>(Arrays.asList(
            "result-list-color", "result-list-alpha", "result-ripple-color", "result-list-rounded",
            "notification-bar-color", "notification-bar-alpha", "notification-bar-gradient", "black-notification-icons",
            "search-bar-size", "search-bar-rounded", "search-bar-gradient", "search-bar-at-bottom",
            "search-bar-color", "search-bar-alpha", "search-bar-text-color", "search-bar-icon-color",
            "search-bar-ripple-color", "search-bar-cursor-argb", "enable-suggestions-keyboard"
    ));
    private final static ArraySet<String> PREF_LISTS_WITH_DEPENDENCY = new ArraySet<>(Arrays.asList(
            "gesture-click",
            "gesture-double-click",
            "gesture-fling-down-left",
            "gesture-fling-down-right",
            "gesture-fling-up",
            "gesture-fling-left",
            "gesture-fling-right",
            "button-launcher",
            "button-home",
            "dm-empty-back",
            "dm-search-back",
            "dm-widget-back"
    ));

    private static final int FILE_SELECT_XML_SET = 63;
    private static final int FILE_SELECT_XML_OVERWRITE = 62;
    private static final int FILE_SELECT_XML_APPEND = 61;
    public static final int ENABLE_DEVICE_ADMIN = 60;

    /**
     * Synchronize the toggle list with the order list. Remove toggled off entries and add at the end new ones.
     *
     * @param sharedPreferences we get the list from here and apply the changes to
     * @param listKey           preference key of the list
     * @param orderKey          preference key of the order list
     */
    private static void syncOrderedList(@NonNull SharedPreferences sharedPreferences, @NonNull String listKey, @NonNull String orderKey) {
        // get list values in a set I can modify
        Set<String> listSet = new HashSet<>(sharedPreferences.getStringSet(listKey, Collections.emptySet()));
        final int listSize = listSet.size();
        // get order
        final List<String> orderValues;
        Set<String> orderSet = sharedPreferences.getStringSet(orderKey, null);
        if (orderSet == null) {
            // we don't have any order yet
            orderValues = Collections.emptyList();
        } else {
            orderValues = new ArrayList<>(orderSet);
            Collections.sort(orderValues);
        }

        // this will be the new order
        ArrayList<String> newValues = new ArrayList<>(listSize);

        // keep previous order
        int idx = 0;
        for (String value : orderValues) {
            String name = OrderListPreferenceDialog.getOrderedValueName(value);
            if (listSet.remove(name)) {
                newValues.add(OrderListPreferenceDialog.makeOrderedValue(name, idx++));
            }
        }

        // add at the end all the new values
        for (String name : listSet)
            newValues.add(OrderListPreferenceDialog.makeOrderedValue(name, idx++));

        Set<String> newOrderSet = new HashSet<>(newValues);
        if (!newOrderSet.equals(orderSet))
            sharedPreferences.edit().putStringSet(orderKey, newOrderSet).apply();
    }

    private static class TagsMenuData {
        private final CharSequence[] entries;
        private final CharSequence[] entryValues;
        private final Set<String> defaultValues;
        private final List<String> orderedValues;

        public TagsMenuData(CharSequence[] entries, CharSequence[] entryValues, Set<String> defaultValues, Set<String> orderedValues) {
            this.entries = entries;
            this.entryValues = entryValues;
            this.defaultValues = defaultValues;

            if (orderedValues == null || orderedValues.isEmpty()) {
                // if no order found
                this.orderedValues = new ArrayList<>(entryValues.length);
                int ord = 0;
                for (CharSequence value : entryValues) {
                    String orderedValue = OrderListPreferenceDialog.makeOrderedValue(value.toString(), ord);
                    this.orderedValues.add(orderedValue);
                    ord += 1;
                }
            } else {
                this.orderedValues = new ArrayList<>(orderedValues);
                // sort entries
                Collections.sort(this.orderedValues);
            }
        }

        public void reloadOrderedValues(@NonNull SharedPreferences sharedPreferences, @NonNull SettingsFragment settings) {
            orderedValues.clear();
            orderedValues.addAll(sharedPreferences.getStringSet("tags-menu-order", Collections.emptySet()));
            Collections.sort(orderedValues);
            settings.setOrderedListValues("tags-menu-order", orderedValues);
        }
    }

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
        if (requestCode == ENABLE_DEVICE_ADMIN) {
            if (resultCode != RESULT_OK) {
                Toast.makeText(this, "Failed!", Toast.LENGTH_SHORT).show();
            }
        } else if (resultCode == RESULT_OK) {
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
        private static final String FRAGMENT_TAG = SettingsFragment.class.getName();
        private static final String DIALOG_FRAGMENT_TAG = "androidx.preference.PreferenceFragment.DIALOG";
        private static final String TAG = "Settings";

        private static Pair<CharSequence[], CharSequence[]> AppToRunListContent = null;
        private static Pair<CharSequence[], CharSequence[]> EntryToShowListContent = null;
        private static TagsMenuData TagsMenuContent = null;

        public SettingsFragment() {
            super();
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            if (rootKey != null && rootKey.startsWith("feature-"))
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

            final Context context = requireContext();

            tintPreferenceIcons(getPreferenceScreen(), UIColors.getThemeColor(context, R.attr.colorAccent));

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

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

            if (savedInstanceState == null) {
                initAppToRunLists(context, sharedPreferences);
                initEntryToShowLists(context, sharedPreferences);
                initTagsMenuList(context, sharedPreferences);
            } else {
                synchronized (SettingsFragment.this) {
                    if (AppToRunListContent == null)
                        AppToRunListContent = generateAppToRunListContent(context);
                    if (EntryToShowListContent == null)
                        EntryToShowListContent = generateEntryToShowListContent(context);
                    if (TagsMenuContent == null) {
                        TagsMenuContent = generateTagsMenuContent(context, sharedPreferences);
                    }
                }
                for (String gesturePref : PREF_LISTS_WITH_DEPENDENCY) {
                    updateAppToRunList(sharedPreferences, gesturePref);
                    updateEntryToShowList(sharedPreferences, gesturePref);
                }
                setMultiListValues("tags-menu-list", TagsMenuContent.entries, TagsMenuContent.entryValues, TagsMenuContent.defaultValues);
                setOrderedListValues("tags-menu-order", TagsMenuContent.orderedValues);
            }

            final ListPreference iconsPack = findPreference("icons-pack");
            if (iconsPack != null) {
                iconsPack.setEnabled(false);

                if (savedInstanceState == null) {
                    // Run asynchronously to open settings fast
                    Utilities.runAsync(getLifecycle(),
                            t -> SettingsFragment.this.setListPreferenceIconsPacksData(iconsPack),
                            t -> iconsPack.setEnabled(true));
                } else {
                    // Run synchronously to ensure preferences can be restored from state
                    SettingsFragment.this.setListPreferenceIconsPacksData(iconsPack);
                    iconsPack.setEnabled(true);
                }
            }
        }

        private void initAppToRunLists(@NonNull Context context, @NonNull SharedPreferences sharedPreferences) {
            final Runnable updateLists = () -> {
                for (String gesturePref : PREF_LISTS_WITH_DEPENDENCY)
                    updateAppToRunList(sharedPreferences, gesturePref);
            };
            if (AppToRunListContent == null) {
                Utilities.runAsync(getLifecycle(), t -> {
                    Pair<CharSequence[], CharSequence[]> content = generateAppToRunListContent(context);
                    synchronized (SettingsFragment.this) {
                        if (AppToRunListContent == null)
                            AppToRunListContent = content;
                    }
                }, t -> updateLists.run());
            } else {
                updateLists.run();
            }
        }

        private void initEntryToShowLists(@NonNull Context context, @NonNull SharedPreferences sharedPreferences) {
            final Runnable updateLists = () -> {
                for (String gesturePref : PREF_LISTS_WITH_DEPENDENCY)
                    updateEntryToShowList(sharedPreferences, gesturePref);
            };
            if (EntryToShowListContent == null) {
                Utilities.runAsync(getLifecycle(), t -> {
                    Pair<CharSequence[], CharSequence[]> content = generateEntryToShowListContent(context);
                    synchronized (SettingsFragment.this) {
                        if (EntryToShowListContent == null)
                            EntryToShowListContent = content;
                    }
                }, t -> updateLists.run());
            } else {
                updateLists.run();
            }
        }

        private void initTagsMenuList(@NonNull Context context, @NonNull SharedPreferences sharedPreferences) {
            final Runnable setTagsMenuValues = () -> {
                setMultiListValues("tags-menu-list", TagsMenuContent.entries, TagsMenuContent.entryValues, TagsMenuContent.defaultValues);
                setOrderedListValues("tags-menu-order", TagsMenuContent.orderedValues);
            };

            if (TagsMenuContent == null) {
                Utilities.runAsync(getLifecycle(), t -> {
                    TagsMenuData content = generateTagsMenuContent(context, sharedPreferences);
                    synchronized (SettingsFragment.this) {
                        if (TagsMenuContent == null) {
                            TagsMenuContent = content;
                        }
                    }
                }, t -> setTagsMenuValues.run());
            } else {
                setTagsMenuValues.run();
            }
        }

        private void setMultiListValues(@NonNull String listKey, @NonNull CharSequence[] entries, @NonNull CharSequence[] values, @Nullable Set<String> defaultValues) {
            Preference listPref = findPreference(listKey);
            if (!(listPref instanceof MultiSelectListPreference))
                return;
            MultiSelectListPreference tagsMenuList = (MultiSelectListPreference) listPref;

            tagsMenuList.setEntries(entries);
            tagsMenuList.setEntryValues(values);
            if (defaultValues != null && tagsMenuList.getValues().isEmpty())
                tagsMenuList.setValues(defaultValues);
        }

        private void setOrderedListValues(@NonNull String orderKey, List<String> orderedValues) {
            Preference pref = findPreference(orderKey);
            if (!(pref instanceof MultiSelectListPreference))
                return;
            MultiSelectListPreference listPref = (MultiSelectListPreference) pref;

            ArrayList<String> entries = new ArrayList<>(orderedValues.size());
            for (String value : orderedValues) {
                entries.add(OrderListPreferenceDialog.getOrderedValueName(value));
            }

            listPref.setEntries(entries.toArray(new String[0]));
            listPref.setEntryValues(orderedValues.toArray(new String[0]));
        }

        private void tintPreferenceIcons(Preference preference, int color) {
            Drawable icon = preference.getIcon();
            if (icon != null) {
                // workaround to set drawable size
                {
                    int size = UISizes.getResultIconSize(preference.getContext());
                    icon = new SizeWrappedDrawable(icon, size);
                }
                icon.setColorFilter(color, PorterDuff.Mode.MULTIPLY);
                preference.setIcon(icon);
            }
            if (preference instanceof PreferenceGroup) {
                PreferenceGroup group = ((PreferenceGroup) preference);
                for (int i = 0; i < group.getPreferenceCount(); i++) {
                    tintPreferenceIcons(group.getPreference(i), color);
                }
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

            applyNotificationBarColor(sharedPreferences, requireContext());
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            synchronized (SettingsFragment.this) {
                AppToRunListContent = null;
                EntryToShowListContent = null;
                TagsMenuContent = null;
            }
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
                    case "search-bar-ripple-color":
                    case "search-bar-cursor-argb":
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
                    case "tags-menu-icon-size":
                    case "icon-scale-red":
                    case "icon-scale-green":
                    case "icon-scale-blue":
                    case "icon-scale-alpha":
                    case "icon-hue":
                    case "icon-contrast":
                    case "icon-brightness":
                    case "icon-saturation":
                    case "popup-corner-radius":
                        dialogFragment = SliderDialog.newInstance(key);
                        break;
                    case "device-admin":
                    case "reset-matrix":
                    case "reset-preferences":
                    case "reset-cached-app-icons":
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
            } else if (preference instanceof MultiSelectListPreference) {
                String key = preference.getKey();
                if ("tags-menu-order".equals(key)) {
                    dialogFragment = OrderListPreferenceDialog.newInstance(key);
                } else {
                    dialogFragment = BaseMultiSelectListPreferenceDialog.newInstance(key);
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

        private static Pair<CharSequence[], CharSequence[]> generateAppToRunListContent(@NonNull Context context) {
            List<AppEntry> appEntryList = TBApplication.dataHandler(context).getApplications();
            final int appCount = appEntryList.size();
            CharSequence[] entries = new CharSequence[appCount];
            CharSequence[] entryValues = new CharSequence[appCount];
            for (int idx = 0; idx < appCount; idx++) {
                AppEntry appEntry = appEntryList.get(idx);
                entries[idx] = appEntry.getName();
                entryValues[idx] = appEntry.getUserComponentName();
            }
            return new Pair<>(entries, entryValues);
        }

        private static Pair<CharSequence[], CharSequence[]> generateEntryToShowListContent(@NonNull Context context) {
            FavProvider favProvider = TBApplication.dataHandler(context).getFavProvider();
            final CharSequence[] entries;
            final CharSequence[] entryValues;
            if (favProvider == null || favProvider.getPojos().isEmpty()) {
                entries = new CharSequence[]{context.getString(R.string.no_favorites)};
                entryValues = new CharSequence[]{""};
            } else {
                int iconSize = UISizes.getTextAppearanceTextSize(context, android.R.attr.textAppearanceMedium);
                int tintColor = UIColors.getThemeColor(context, R.attr.colorAccent);
                VectorDrawableCompat iconTag = VectorDrawableCompat.create(context.getResources(), R.drawable.ic_tags, null);
                iconTag.setTint(tintColor);
                iconTag.setBounds(0, 0, iconSize, iconSize);

                List<EntryItem> favList = favProvider.getPojos();
                ArrayList<StaticEntry> entryToShowList = new ArrayList<>(favList.size());
                for (EntryItem entryItem : favList)
                    if (entryItem instanceof StaticEntry)
                        entryToShowList.add((StaticEntry) entryItem);
                final int size = entryToShowList.size();
                entries = new CharSequence[size];
                entryValues = new CharSequence[size];
                for (int idx = 0; idx < size; idx++) {
                    StaticEntry entry = entryToShowList.get(idx);
                    if (entry instanceof TagEntry) {
                        SpannableString name = new SpannableString("# " + entry.getName());
                        name.setSpan(new ImageSpan(iconTag), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                        entries[idx] = name;
                    } else if (entry instanceof ActionEntry || entry instanceof FilterEntry) {
                        Drawable iconAction = entry.getDefaultDrawable(context);
                        DrawableCompat.setTint(iconAction, tintColor);
                        iconAction.setBounds(0, 0, iconSize, iconSize);

                        SpannableString name = new SpannableString("# " + entry.getName());
                        name.setSpan(new ImageSpan(iconAction), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                        entries[idx] = name;
                    } else {
                        entries[idx] = entry.getName();
                    }
                    entryValues[idx] = entry.id;
                }
            }
            return new Pair<>(entries, entryValues);
        }

        private static TagsMenuData generateTagsMenuContent(@NonNull Context context, @NonNull SharedPreferences sharedPreferences) {
            TagsHandler tagsHandler = TBApplication.tagsHandler(context);
            Set<String> validTags = tagsHandler.getValidTags();

            Set<String> tagsMenuListValues = sharedPreferences.getStringSet("tags-menu-list", Collections.emptySet());

            ArrayList<String> prefEntries = new ArrayList<>(validTags);
            // make sure we have the selected values as entries (so the user can remove them)
            for (String tagName : tagsMenuListValues) {
                if (!validTags.contains(tagName))
                    prefEntries.add(0, tagName);
            }
            // sort entries
            Collections.sort(prefEntries, String.CASE_INSENSITIVE_ORDER);

            // set preference entries and values
            CharSequence[] entries = prefEntries.toArray(new String[0]);
            CharSequence[] entryValues = prefEntries.toArray(new String[0]);

            // set default values if we need them
            HashSet<String> defaultValues = new HashSet<>();
            for (String tagName : validTags) {
                if (defaultValues.size() >= 5)
                    break;
                defaultValues.add(tagName);
            }

            Set<String> orderedValues = sharedPreferences.getStringSet("tags-menu-order", null);
            return new TagsMenuData(entries, entryValues, defaultValues, orderedValues);
        }

        private void updateListPrefDependency(@NonNull String dependOnKey, @Nullable String dependOnValue, @NonNull String enableValue, @NonNull String listKey, @Nullable Pair<CharSequence[], CharSequence[]> listContent) {
            Preference prefAppToRun = findPreference(listKey);
            if (prefAppToRun instanceof ListPreference && listContent != null) {
                CharSequence[] entries = listContent.first;
                CharSequence[] entryValues = listContent.second;
                ((ListPreference) prefAppToRun).setEntries(entries);
                ((ListPreference) prefAppToRun).setEntryValues(entryValues);
                prefAppToRun.setVisible(enableValue.equals(dependOnValue));
            } else if (prefAppToRun == null) {
                // the ListPreference for selecting an app is missing. Remove the option to run an app.
                Preference pref = findPreference(dependOnKey);
                if (pref instanceof ListPreference) {
                    removeEntryValueFromListPreference(enableValue, (ListPreference) pref);
                }
            } else {
                Log.w(TAG, "ListPreference `" + listKey + "` can't be updated");
                prefAppToRun.setVisible(false);
            }
        }

        private void updateAppToRunList(@NonNull SharedPreferences sharedPreferences, String key) {
            updateListPrefDependency(key, sharedPreferences.getString(key, null), "runApp", key + "-app-to-run", AppToRunListContent);
        }

        private void updateEntryToShowList(@NonNull SharedPreferences sharedPreferences, String key) {
            updateListPrefDependency(key, sharedPreferences.getString(key, null), "showEntry", key + "-entry-to-show", EntryToShowListContent);
        }

        private static void removeEntryValueFromListPreference(@NonNull String entryValueToRemove, ListPreference listPref) {
            CharSequence[] entryValues = listPref.getEntryValues();
            int indexToRemove = -1;
            for (int idx = 0, entryValuesLength = entryValues.length; idx < entryValuesLength; idx++) {
                CharSequence entryValue = entryValues[idx];
                if (entryValueToRemove.contentEquals(entryValue)) {
                    indexToRemove = idx;
                    break;
                }
            }
            if (indexToRemove == -1)
                return;
            CharSequence[] entries = listPref.getEntries();
            final int size = entries.length;
            final int newSize = size - 1;
            CharSequence[] newEntries = new CharSequence[newSize];
            CharSequence[] newEntryValues = new CharSequence[newSize];
            if (indexToRemove > 0) {
                System.arraycopy(entries, 0, newEntries, 0, indexToRemove);
                System.arraycopy(entryValues, 0, newEntryValues, 0, indexToRemove);
            }
            if (indexToRemove < newSize) {
                System.arraycopy(entries, indexToRemove + 1, newEntries, indexToRemove, newSize - indexToRemove);
                System.arraycopy(entryValues, indexToRemove + 1, newEntryValues, indexToRemove, newSize - indexToRemove);
            }
            listPref.setEntries(newEntries);
            listPref.setEntryValues(newEntryValues);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            SettingsActivity activity = (SettingsActivity) getActivity();
            if (activity == null)
                return;

            if (PREF_LISTS_WITH_DEPENDENCY.contains(key)) {
                updateAppToRunList(sharedPreferences, key);
                updateEntryToShowList(sharedPreferences, key);
            }

            // rebind and relayout all visible views because I can't find how to rebind only the current view
            getListView().getAdapter().notifyDataSetChanged();

            SettingsActivity.onSharedPreferenceChanged(activity, sharedPreferences, key);

            if (TagsMenuContent != null) {
                if ("tags-menu-list".equals(key) || "tags-menu-order".equals(key)) {
                    TagsMenuContent.reloadOrderedValues(sharedPreferences, this);
                }
            }
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
        int color = UIColors.getColor(sharedPreferences, "notification-bar-color");
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

    public static void onSharedPreferenceChanged(Context context, SharedPreferences sharedPreferences, String key) {
        if (PREF_THAT_REQUIRE_LAYOUT_UPDATE.contains(key))
            TBApplication.getApplication(context).requireLayoutUpdate();

        TBApplication.liveWallpaper(context).onPrefChanged(sharedPreferences, key);

        switch (key) {
            case "notification-bar-color":
            case "notification-bar-alpha":
            case "black-notification-icons":
                applyNotificationBarColor(sharedPreferences, context);
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
                TBApplication.drawableCache(context).clearCache();
                TBApplication.behaviour(context).refreshSearchRecords();
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
            case "popup-background-argb":
            case "popup-border-argb":
            case "popup-ripple-color":
            case "popup-text-color":
            case "popup-title-color":
                UIColors.resetCache();
                break;
            case "result-text-size":
            case "result-text2-size":
            case "result-icon-size":
            case "tags-menu-icon-size":
            case "popup-corner-radius":
                UISizes.resetCache();
                break;
            case "result-history-size":
            case "result-history-adaptive":
            case "fuzzy-search-tags":
            case "result-search-cap":
            case "tags-menu-icons":
            case "loading-icon":
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
            case "quick-list-enabled":
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
                syncOrderedList(sharedPreferences, "tags-menu-list", "tags-menu-order");
                break;
        }
    }
}