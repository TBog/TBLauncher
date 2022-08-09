package rocks.tbog.tblauncher.settings;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.collection.ArraySet;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rocks.tbog.tblauncher.BuildConfig;
import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.dataprovider.ShortcutsProvider;
import rocks.tbog.tblauncher.dataprovider.TagsProvider;
import rocks.tbog.tblauncher.drawable.SizeWrappedDrawable;
import rocks.tbog.tblauncher.entry.AppEntry;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.entry.StaticEntry;
import rocks.tbog.tblauncher.entry.TagEntry;
import rocks.tbog.tblauncher.handler.IconsHandler;
import rocks.tbog.tblauncher.preference.BaseListPreferenceDialog;
import rocks.tbog.tblauncher.preference.BaseMultiSelectListPreferenceDialog;
import rocks.tbog.tblauncher.preference.ConfirmDialog;
import rocks.tbog.tblauncher.preference.ContentLoadHelper;
import rocks.tbog.tblauncher.preference.CustomDialogPreference;
import rocks.tbog.tblauncher.preference.EditSearchEnginesPreferenceDialog;
import rocks.tbog.tblauncher.preference.EditSearchHintPreferenceDialog;
import rocks.tbog.tblauncher.preference.IconListPreferenceDialog;
import rocks.tbog.tblauncher.preference.OrderListPreferenceDialog;
import rocks.tbog.tblauncher.preference.PreferenceColorDialog;
import rocks.tbog.tblauncher.preference.QuickListPreferenceDialog;
import rocks.tbog.tblauncher.preference.ShadowDialog;
import rocks.tbog.tblauncher.preference.SliderDialog;
import rocks.tbog.tblauncher.preference.TagOrderListPreferenceDialog;
import rocks.tbog.tblauncher.utils.FileUtils;
import rocks.tbog.tblauncher.utils.MimeTypeUtils;
import rocks.tbog.tblauncher.utils.UIColors;
import rocks.tbog.tblauncher.utils.UISizes;
import rocks.tbog.tblauncher.utils.Utilities;

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String FRAGMENT_TAG = SettingsFragment.class.getName();
    private static final String DIALOG_FRAGMENT_TAG = "androidx.preference.PreferenceFragment.DIALOG";
    private static final String TAG = "Settings";

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
        "dm-widget-back",
        "dm-search-open-result"
    ));

    private static Pair<CharSequence[], CharSequence[]> AppToRunListContent = null;
    private static Pair<CharSequence[], CharSequence[]> ShortcutToRunListContent = null;
    private static Pair<CharSequence[], CharSequence[]> EntryToShowListContent = null;
    private static ContentLoadHelper.OrderedMultiSelectListData TagsMenuContent = null;
    private static ContentLoadHelper.OrderedMultiSelectListData ResultPopupContent = null;
    private static Pair<CharSequence[], CharSequence[]> MimeTypeListContent = null;

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
        if (!BuildConfig.SHOW_RATE_APP) {
            removePreference("rate-app");
        }
        if (!BuildConfig.SHOW_PRIVACY_POLICY) {
            removePreference("privacy-policy");
        }
        // set app name and version
        {
            Preference appVer = findPreference("app-version");
            if (appVer != null) {
                var version = appVer.getContext().getString(R.string.app_version, BuildConfig.VERSION_NAME);
                var appName = appVer.getContext().getText(R.string.app_name);
                String appStore;
                switch (BuildConfig.FLAVOR) {
                    case "playstore":
                        appStore = "Google Play";
                        break;
                    case "fdroid":
                        appStore = "F-Droid";
                        break;
                    case "github":
                        appStore = "GitHub";
                        break;
                    default:
                        throw new IllegalStateException("Undefined flavor");
                }
                var summary = appVer.getContext().getString(R.string.app_version_summary, appName, appStore);
                appVer.setTitle(version);
                appVer.setSummary(summary);

                // add link to the launcher webpage if app not installed from a store
                if (!BuildConfig.SHOW_RATE_APP) {
                    appVer.setEnabled(true);
                }
            }
        }

        final Activity activity = requireActivity();

        // set activity title as the preference screen title
        activity.setTitle(getPreferenceScreen().getTitle());

        ActionBar actionBar = ((SettingsActivity) activity).getSupportActionBar();
        if (actionBar != null) {
            // we can change the theme from the options menu
            removePreference("settings-theme");
        }

        setupButtonActions(activity);

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

        onCreateAsyncLoad(context, sharedPreferences, savedInstanceState);
    }

    private void setupButtonActions(@NonNull Activity activity) {
        // import settings
        {
            Preference pref = findPreference("import-settings-set");
            if (pref != null)
                pref.setOnPreferenceClickListener(preference -> {
                    FileUtils.chooseSettingsFile(activity, SettingsActivity.FILE_SELECT_XML_SET);
                    return true;
                });
            pref = findPreference("import-settings-overwrite");
            if (pref != null)
                pref.setOnPreferenceClickListener(preference -> {
                    FileUtils.chooseSettingsFile(activity, SettingsActivity.FILE_SELECT_XML_OVERWRITE);
                    return true;
                });
            pref = findPreference("import-settings-append");
            if (pref != null)
                pref.setOnPreferenceClickListener(preference -> {
                    FileUtils.chooseSettingsFile(activity, SettingsActivity.FILE_SELECT_XML_APPEND);
                    return true;
                });
        }
    }

    private void onCreateAsyncLoad(@NonNull Context context, @NonNull SharedPreferences sharedPreferences, @Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            initAppToRunLists(context, sharedPreferences);
            initShortcutToRunLists(context, sharedPreferences);
            initEntryToShowLists(context, sharedPreferences);
            initTagsMenuList(context, sharedPreferences);
            initResultPopupList(context, sharedPreferences);
            initMimeTypes(context);
        } else {
            synchronized (SettingsFragment.class) {
                if (AppToRunListContent == null)
                    AppToRunListContent = generateAppToRunListContent(context);
                if (ShortcutToRunListContent == null)
                    ShortcutToRunListContent = generateShortcutToRunListContent(context);
                if (EntryToShowListContent == null)
                    EntryToShowListContent = generateEntryToShowListContent(context);
                if (TagsMenuContent == null)
                    TagsMenuContent = ContentLoadHelper.generateTagsMenuContent(context, sharedPreferences);
                if (ResultPopupContent == null)
                    ResultPopupContent = ContentLoadHelper.generateResultPopupContent(context, sharedPreferences);
                if (MimeTypeListContent == null)
                    MimeTypeListContent = generateMimeTypeListContent(context);

                for (String gesturePref : PREF_LISTS_WITH_DEPENDENCY) {
                    updateAppToRunList(sharedPreferences, gesturePref);
                    updateShortcutToRunList(sharedPreferences, gesturePref);
                    updateEntryToShowList(sharedPreferences, gesturePref);
                }
                TagsMenuContent.setMultiListValues(findPreference("tags-menu-list"));
                TagsMenuContent.setOrderedListValues(findPreference("tags-menu-order"));
                ResultPopupContent.setOrderedListValues(findPreference("result-popup-order"));
                ContentLoadHelper.setMultiListValues(findPreference("selected-contact-mime-types"), MimeTypeListContent, null);
            }
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
                synchronized (SettingsFragment.class) {
                    if (AppToRunListContent == null)
                        AppToRunListContent = content;
                }
            }, t -> updateLists.run());
        } else {
            updateLists.run();
        }
    }

    private void initShortcutToRunLists(@NonNull Context context, @NonNull SharedPreferences sharedPreferences) {
        final Runnable updateLists = () -> {
            for (String gesturePref : PREF_LISTS_WITH_DEPENDENCY)
                updateShortcutToRunList(sharedPreferences, gesturePref);
        };
        if (ShortcutToRunListContent == null) {
            Utilities.runAsync(getLifecycle(), t -> {
                Pair<CharSequence[], CharSequence[]> content = generateShortcutToRunListContent(context);
                synchronized (SettingsFragment.this) {
                    if (ShortcutToRunListContent == null)
                        ShortcutToRunListContent = content;
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
                synchronized (SettingsFragment.class) {
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
            synchronized (SettingsFragment.class) {
                if (TagsMenuContent != null) {
                    TagsMenuContent.setMultiListValues(findPreference("tags-menu-list"));
                    TagsMenuContent.setOrderedListValues(findPreference("tags-menu-order"));
                }
            }
        };

        if (TagsMenuContent == null) {
            Utilities.runAsync(getLifecycle(), t -> {
                ContentLoadHelper.OrderedMultiSelectListData content = ContentLoadHelper.generateTagsMenuContent(context, sharedPreferences);
                synchronized (SettingsFragment.class) {
                    if (TagsMenuContent == null) {
                        TagsMenuContent = content;
                    }
                }
            }, t -> setTagsMenuValues.run());
        } else {
            setTagsMenuValues.run();
        }
    }

    private void initResultPopupList(@NonNull Context context, @NonNull SharedPreferences sharedPreferences) {
        final Runnable setResultPopupValues = () -> {
            synchronized (SettingsFragment.class) {
                if (ResultPopupContent != null)
                    ResultPopupContent.setOrderedListValues(findPreference("result-popup-order"));
            }
        };

        if (ResultPopupContent == null) {
            Utilities.runAsync(getLifecycle(), t -> {
                ContentLoadHelper.OrderedMultiSelectListData content = ContentLoadHelper.generateResultPopupContent(context, sharedPreferences);
                synchronized (SettingsFragment.class) {
                    if (ResultPopupContent == null) {
                        ResultPopupContent = content;
                    }
                }
            }, t -> setResultPopupValues.run());
        } else {
            setResultPopupValues.run();
        }
    }

    private void initMimeTypes(@NonNull Context context) {
        // get all supported mime types
        final Runnable setMimeTypeValues = () -> {
            synchronized (SettingsFragment.class) {
                if (MimeTypeListContent != null)
                    ContentLoadHelper.setMultiListValues(findPreference("selected-contact-mime-types"), MimeTypeListContent, null);
            }
        };

        if (MimeTypeListContent == null) {
            Utilities.runAsync(getLifecycle(), t -> {
                Pair<CharSequence[], CharSequence[]> content = generateMimeTypeListContent(context);
                synchronized (SettingsFragment.class) {
                    if (MimeTypeListContent == null)
                        MimeTypeListContent = content;
                }
            }, t -> setMimeTypeValues.run());
        } else {
            setMimeTypeValues.run();
        }
    }

    private void tintPreferenceIcons(Preference preference, int color) {
        Drawable icon = preference.getIcon();
        if (icon != null) {
            // workaround to set drawable size
            {
                int size = UISizes.getResultIconSize(preference.getContext());
                icon = new SizeWrappedDrawable(icon, size);
            }
            icon.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY));
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
        if (sharedPreferences != null) {
            sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
        if (sharedPreferences != null) {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        synchronized (SettingsFragment.class) {
            AppToRunListContent = null;
            ShortcutToRunListContent = null;
            EntryToShowListContent = null;
            TagsMenuContent = null;
            ResultPopupContent = null;
        }
    }

    @Override
    public void onDisplayPreferenceDialog(@NonNull Preference preference) {
        // Try if the preference is one of our custom Preferences
        DialogFragment dialogFragment = null;
        if (preference instanceof CustomDialogPreference) {
            // Create a new instance of CustomDialog with the key of the related Preference
            String key = preference.getKey();
            Log.d(TAG, "onDisplayPreferenceDialog " + key);
            switch (key) {
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
                    dialogFragment = null;
            }
            if (dialogFragment == null) {
                @LayoutRes
                int dialogLayout = ((CustomDialogPreference) preference).getDialogLayoutResource();
                if (dialogLayout == 0) {
                    if (key.endsWith("-color") || key.endsWith("-argb"))
                        dialogFragment = PreferenceColorDialog.newInstance(key);
                } else if (R.layout.pref_slider == dialogLayout) {
                    dialogFragment = SliderDialog.newInstance(key);
                } else if (R.layout.pref_shadow == dialogLayout) {
                    dialogFragment = ShadowDialog.newInstance(key);
                } else if (R.layout.pref_confirm == dialogLayout) {
                    dialogFragment = ConfirmDialog.newInstance(key);
                }
            }
            if (dialogFragment == null)
                throw new IllegalArgumentException("CustomDialogPreference \"" + key + "\" has no dialog defined");
        } else if (preference instanceof ListPreference) {
            String key = preference.getKey();
            switch (key) {
                case "adaptive-shape":
                case "contacts-shape":
                case "shortcut-shape":
                case "icons-pack":
                    dialogFragment = IconListPreferenceDialog.newInstance(key);
                    break;
                default:
                    dialogFragment = BaseListPreferenceDialog.newInstance(key);
                    break;
            }
        } else if (preference instanceof MultiSelectListPreference) {
            String key = preference.getKey();
            if ("tags-menu-order".equals(key)) {
                dialogFragment = TagOrderListPreferenceDialog.newInstance(key);
            } else if ("result-popup-order".equals(key)) {
                dialogFragment = OrderListPreferenceDialog.newInstance(key);
            } else {
                dialogFragment = BaseMultiSelectListPreferenceDialog.newInstance(key);
            }
        }

        // If it was one of our custom Preferences, show its dialog
        if (dialogFragment != null) {
            final FragmentManager fm = this.getParentFragmentManager();
            // check if dialog is already showing
            if (fm.findFragmentByTag(DIALOG_FRAGMENT_TAG) != null) {
                return;
            }
            dialogFragment.setTargetFragment(this, 0);
            dialogFragment.show(fm, DIALOG_FRAGMENT_TAG);
        }
        // Could not be handled here. Try with the super method.
        else {
            super.onDisplayPreferenceDialog(preference);
        }
    }

    private static Pair<CharSequence[], CharSequence[]> generateAppToRunListContent(@NonNull Context context) {
        List<AppEntry> appEntryList = TBApplication.appsHandler(context).getApplications();
        Collections.sort(appEntryList, AppEntry.NAME_COMPARATOR);
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

    private static Pair<CharSequence[], CharSequence[]> generateShortcutToRunListContent(@NonNull Context context) {
        ShortcutsProvider shortcutsProvider = TBApplication.dataHandler(context).getShortcutsProvider();
        List<? extends EntryItem> shortcutList = shortcutsProvider == null ? null : shortcutsProvider.getPojos();
        if (shortcutList == null)
            return new Pair<>(new CharSequence[0], new CharSequence[0]);
        // copy list in order to sort it
        shortcutList = new ArrayList<>(shortcutList);
        Collections.sort(shortcutList, EntryItem.NAME_COMPARATOR);
        final int entryCount = shortcutList.size();
        CharSequence[] entries = new CharSequence[entryCount];
        CharSequence[] entryValues = new CharSequence[entryCount];
        for (int idx = 0; idx < entryCount; idx++) {
            EntryItem shortcutEntry = shortcutList.get(idx);
            entries[idx] = shortcutEntry.getName();
            entryValues[idx] = shortcutEntry.id;
        }
        return new Pair<>(entries, entryValues);
    }

    private static Pair<CharSequence[], CharSequence[]> generateEntryToShowListContent(@NonNull Context context) {
        final List<StaticEntry> tagList;

        final TBApplication app = TBApplication.getApplication(context);
        final TagsProvider tagsProvider = app.getDataHandler().getTagsProvider();
        if (tagsProvider != null) {
            ArrayList<String> tagNames = new ArrayList<>(app.tagsHandler().getValidTags());
            Collections.sort(tagNames);
            tagList = new ArrayList<>(tagNames.size());
            for (String tagName : tagNames) {
                TagEntry tagEntry = tagsProvider.getTagEntry(tagName);
                tagList.add(tagEntry);
            }
        } else {
            tagList = Collections.emptyList();
        }

        final CharSequence[] entries;
        final CharSequence[] entryValues;
        if (tagList.isEmpty()) {
            entries = new CharSequence[]{context.getString(R.string.no_tags)};
            entryValues = new CharSequence[]{""};
        } else {
            return ContentLoadHelper.generateStaticEntryList(context, tagList);
        }
        return new Pair<>(entries, entryValues);
    }

    private static Pair<CharSequence[], CharSequence[]> generateMimeTypeListContent(@NonNull Context context) {
        Set<String> supportedMimeTypes = MimeTypeUtils.getSupportedMimeTypes(context);
        Map<String, String> labels = TBApplication.mimeTypeCache(context).getUniqueLabels(context, supportedMimeTypes);

        String[] mimeTypes = labels.keySet().toArray(new String[0]);
        Arrays.sort(mimeTypes);

        CharSequence[] mimeLabels = new CharSequence[mimeTypes.length];
        for (int index = 0; index < mimeTypes.length; index += 1) {
            mimeLabels[index] = labels.get(mimeTypes[index]);
        }
        return new Pair<>(mimeTypes, mimeLabels);
    }

    private void updateListPrefDependency(@NonNull String dependOnKey, @Nullable String dependOnValue, @NonNull String enableValue, @NonNull String listKey, @Nullable Pair<CharSequence[], CharSequence[]> listContent) {
        Preference prefEntryToRun = findPreference(listKey);
        if (prefEntryToRun instanceof ListPreference) {
            synchronized (SettingsFragment.class) {
                if (listContent != null) {
                    CharSequence[] entries = listContent.first;
                    CharSequence[] entryValues = listContent.second;
                    ((ListPreference) prefEntryToRun).setEntries(entries);
                    ((ListPreference) prefEntryToRun).setEntryValues(entryValues);
                    prefEntryToRun.setVisible(enableValue.equals(dependOnValue));
                    return;
                }
            }
        }
        if (prefEntryToRun == null) {
            // the ListPreference for selecting an app is missing. Remove the option to run an app.
            Preference pref = findPreference(dependOnKey);
            if (pref instanceof ListPreference) {
                removeEntryValueFromListPreference(enableValue, (ListPreference) pref);
            }
        } else {
            Log.w(TAG, "ListPreference `" + listKey + "` can't be updated");
            prefEntryToRun.setVisible(false);
        }
    }

    private void updateAppToRunList(@NonNull SharedPreferences sharedPreferences, String key) {
        updateListPrefDependency(key, sharedPreferences.getString(key, null), "runApp", key + "-app-to-run", AppToRunListContent);
    }

    private void updateShortcutToRunList(@NonNull SharedPreferences sharedPreferences, String key) {
        updateListPrefDependency(key, sharedPreferences.getString(key, null), "runShortcut", key + "-shortcut-to-run", ShortcutToRunListContent);
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
        if (PREF_LISTS_WITH_DEPENDENCY.contains(key)) {
            updateAppToRunList(sharedPreferences, key);
            updateShortcutToRunList(sharedPreferences, key);
            updateEntryToShowList(sharedPreferences, key);
        }

        // rebind and relayout all visible views because I can't find how to rebind only the current view
        getListView().getAdapter().notifyDataSetChanged();

        synchronized (SettingsFragment.class) {
            if (TagsMenuContent != null) {
                if ("tags-menu-list".equals(key) || "tags-menu-order".equals(key)) {
                    TagsMenuContent.reloadOrderedValues(sharedPreferences, this, "tags-menu-order");
                } else if ("result-popup-order".equals(key)) {
                    ResultPopupContent.reloadOrderedValues(sharedPreferences, this, "result-popup-order");
                }
            }
        }
    }
}
