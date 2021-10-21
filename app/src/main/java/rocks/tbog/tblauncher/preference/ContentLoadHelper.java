package rocks.tbog.tblauncher.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.SettingsActivity;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.TagsHandler;
import rocks.tbog.tblauncher.dataprovider.TagsProvider;
import rocks.tbog.tblauncher.entry.ActionEntry;
import rocks.tbog.tblauncher.entry.FilterEntry;
import rocks.tbog.tblauncher.entry.StaticEntry;
import rocks.tbog.tblauncher.entry.TagEntry;
import rocks.tbog.tblauncher.utils.PrefOrderedListHelper;
import rocks.tbog.tblauncher.utils.UIColors;
import rocks.tbog.tblauncher.utils.UISizes;
import rocks.tbog.tblauncher.utils.Utilities;

public class ContentLoadHelper {
    public static final CategoryItem[] RESULT_POPUP_CATEGORIES = {
            new CategoryItem(R.string.popup_title_shortcut_dynamic, "dyn_shortcut"),
            new CategoryItem(R.string.popup_title_hist_fav, "prefs"),
            new CategoryItem(R.string.popup_title_customize, "customize"),
            new CategoryItem(R.string.popup_title_link, "links"),
    };

    public static OrderedMultiSelectListData generateResultPopupContent(@NonNull Context context, @NonNull SharedPreferences sharedPreferences) {
        final HashSet<String> values = new HashSet<>(RESULT_POPUP_CATEGORIES.length);
        // get default values
        for (CategoryItem categoryItem : RESULT_POPUP_CATEGORIES) {
            categoryItem.updateText(context);
            values.add(categoryItem.value);
        }

        // get values from previous order
        final ArrayList<String> orderedValues;
        {
            Set<String> order = sharedPreferences.getStringSet("result-popup-order", Collections.emptySet());
            orderedValues = new ArrayList<>(order);
            Collections.sort(orderedValues);
        }

        // sync current categories with previous order
        ArrayList<String> newOrder = new ArrayList<>(RESULT_POPUP_CATEGORIES.length);
        for (String orderValue : orderedValues) {
            String valueName = PrefOrderedListHelper.getOrderedValueName(orderValue);
            if (values.remove(valueName))
                newOrder.add(valueName);
        }
        for (CategoryItem categoryItem : RESULT_POPUP_CATEGORIES) {
            if (values.remove(categoryItem.value))
                newOrder.add(categoryItem.value);
        }

        // make new order values
        orderedValues.clear();
        orderedValues.addAll(PrefOrderedListHelper.getOrderedArrayList(newOrder));

        // initialize entries using the ordered values
        CharSequence[] entries = new CharSequence[orderedValues.size()];
        CharSequence[] entryValues = new CharSequence[orderedValues.size()];
        for (int i = 0; i < orderedValues.size(); i += 1) {
            String orderValue = orderedValues.get(i);
            String value = PrefOrderedListHelper.getOrderedValueName(orderValue);
            for (CategoryItem categoryItem : RESULT_POPUP_CATEGORIES) {
                if (categoryItem.value.equals(value)) {
                    entries[i] = categoryItem.text;
                    entryValues[i] = categoryItem.value;
                    break;
                }
            }
        }

        return new OrderedMultiSelectListData(entries, entryValues, null, orderedValues);
    }

    public static OrderedMultiSelectListData generateTagsMenuContent(@NonNull Context context, @NonNull SharedPreferences sharedPreferences) {
        TagsHandler tagsHandler = TBApplication.tagsHandler(context);
        Set<String> validTags = tagsHandler.getValidTags();

        TagsProvider tagsProvider = TBApplication.dataHandler(context).getTagsProvider();

        Set<String> tagsMenuListValues = sharedPreferences.getStringSet("tags-menu-list", Collections.emptySet());

        ArrayList<String> prefEntries = new ArrayList<>(validTags);
        // make sure we have the selected values as entries (so the user can remove them)
        for (String tagName : tagsMenuListValues) {
            if (!validTags.contains(tagName))
                prefEntries.add(0, tagName);
        }
        // sort entries
        Collections.sort(prefEntries, String.CASE_INSENSITIVE_ORDER);

        int layoutDirection = context.getResources().getConfiguration().getLayoutDirection();
        int size = context.getResources().getDimensionPixelSize(R.dimen.icon_preview_size);

        // set preference entries
        int count = prefEntries.size();
        CharSequence[] entries = new CharSequence[count];
        for (int idx = 0; idx < count; idx += 1) {
            String tagName = prefEntries.get(idx);
            if (tagsProvider != null) {
                TagEntry tagEntry = tagsProvider.getTagEntry(tagName);
                Drawable tagIcon = tagEntry.getIconDrawable(context);
                tagIcon.setBounds(0, 0, size, size);

                SpannableString name = Utilities.addDrawableInString(tagName, tagIcon, layoutDirection);
                entries[idx] = name;
            } else {
                entries[idx] = tagName;
            }
        }

        // set preference values
        CharSequence[] entryValues = prefEntries.toArray(new String[0]);


        // set default values if we need them
        HashSet<String> defaultValues = new HashSet<>();
        for (String tagName : validTags) {
            if (defaultValues.size() >= 5)
                break;
            defaultValues.add(tagName);
        }

        Set<String> orderedValues = sharedPreferences.getStringSet("tags-menu-order", null);
        return new OrderedMultiSelectListData(entries, entryValues, defaultValues, orderedValues);
    }

    public static Pair<CharSequence[], CharSequence[]> generateStaticEntryList(@NonNull Context context, @NonNull List<StaticEntry> entryToShowList) {
        final int size = entryToShowList.size();
        final CharSequence[] entries = new CharSequence[size];
        final CharSequence[] entryValues = new CharSequence[size];
        boolean iconTagLoaded = false;
        VectorDrawableCompat iconTag = null;
        int layoutDirection = context.getResources().getConfiguration().getLayoutDirection();
        int iconSize = UISizes.getTextAppearanceTextSize(context, android.R.attr.textAppearanceMedium);
        int tintColor = UIColors.getThemeColor(context, R.attr.colorAccent);

        for (int idx = 0; idx < size; idx++) {
            StaticEntry entry = entryToShowList.get(idx);
            if (entry instanceof TagEntry) {
                if (!iconTagLoaded) {
                    iconTagLoaded = true;
                    iconTag = VectorDrawableCompat.create(context.getResources(), R.drawable.ic_tags, null);
                    if (iconTag != null) {
                        iconTag.setTint(tintColor);
                        iconTag.setBounds(0, 0, iconSize, iconSize);
                    }
                }
                if (iconTag == null) {
                    entries[idx] = entry.getName();
                } else {
                    SpannableString name = Utilities.addDrawableInString(entry.getName(), iconTag, layoutDirection);
                    entries[idx] = name;
                }
            } else if (entry instanceof ActionEntry || entry instanceof FilterEntry) {
                Drawable iconAction = entry.getDefaultDrawable(context);
                if (iconAction == null) {
                    entries[idx] = entry.getName();
                } else {
                    DrawableCompat.setTint(iconAction, tintColor);
                    iconAction.setBounds(0, 0, iconSize, iconSize);

                    SpannableString name = Utilities.addDrawableInString(entry.getName(), iconAction, layoutDirection);
                    entries[idx] = name;
                }
            } else {
                entries[idx] = entry.getName();
            }
            entryValues[idx] = entry.id;
        }
        return new Pair<>(entries, entryValues);
    }

    public static class CategoryItem {
        /**
         * String resource used when inflating the popup menu.
         * Currently we use this to generate the preference menu as well
         */
        @StringRes
        public final int textId;

        /**
         * Value stored in the preference. Must not change with language.
         */
        public final String value;

        /**
         * String to be used for the preference menu.
         */
        private String text = null;

        public CategoryItem(int textId, String value) {
            this.textId = textId;
            this.value = value;
        }

        /**
         * Using context generate the string for the preference menu
         *
         * @param context so we can get the string from the resource id
         */
        public void updateText(@NonNull Context context) {
            text = context.getString(textId);
        }
    }

    public static class OrderedMultiSelectListData {
        private final CharSequence[] entries;
        private final CharSequence[] entryValues;
        private final Set<String> defaultValues;
        private final ArrayList<String> orderedValues;

        public OrderedMultiSelectListData(CharSequence[] entries, CharSequence[] entryValues, Set<String> defaultValues, @Nullable Collection<String> orderedValues) {
            this.entries = entries;
            this.entryValues = entryValues;
            this.defaultValues = defaultValues;

            if (orderedValues == null || orderedValues.isEmpty()) {
                // if no order found
                this.orderedValues = PrefOrderedListHelper.getOrderedArrayList(entryValues);
            } else {
                this.orderedValues = new ArrayList<>(orderedValues);
                // sort entries
                Collections.sort(this.orderedValues);
            }
        }

        public void reloadOrderedValues(@NonNull SharedPreferences sharedPreferences, @NonNull SettingsActivity.SettingsFragment settings, String orderKey) {
            orderedValues.clear();
            orderedValues.addAll(sharedPreferences.getStringSet(orderKey, Collections.emptySet()));
            Collections.sort(orderedValues);
            setOrderedListValues(settings.findPreference(orderKey));
        }

        public void setMultiListValues(@Nullable Preference preference) {
            if (!(preference instanceof MultiSelectListPreference))
                return;
            MultiSelectListPreference multiSelectList = (MultiSelectListPreference) preference;

            if (entries != null)
                multiSelectList.setEntries(entries);
            if (entryValues != null)
                multiSelectList.setEntryValues(entryValues);
            if (defaultValues != null && multiSelectList.getValues().isEmpty())
                multiSelectList.setValues(defaultValues);

            Log.d("pref", "setMultiListValues " + preference.getKey() + "\n entries=" + Arrays.toString(entries) + "\n values=" + Arrays.toString(entryValues));
        }

        public void setOrderedListValues(@Nullable Preference preference) {
            if (!(preference instanceof MultiSelectListPreference))
                return;
            MultiSelectListPreference listPref = (MultiSelectListPreference) preference;

            ArrayList<CharSequence> orderedEntries = new ArrayList<>(orderedValues.size());
            ArrayList<CharSequence> orderedEntryValues = new ArrayList<>(orderedValues.size());
            for (String orderedValue : orderedValues) {
                String value = PrefOrderedListHelper.getOrderedValueName(orderedValue);
                for (int i = 0; i < entryValues.length; i += 1) {
                    if (entryValues[i].equals(value)) {
                        orderedEntries.add(entries[i]);
                        orderedEntryValues.add(entryValues[i]);
                        break;
                    }
                }
            }

            listPref.setEntries(orderedEntries.toArray(new CharSequence[0]));
            listPref.setEntryValues(orderedEntryValues.toArray(new CharSequence[0]));
            Log.d("pref", "setOrderedListValues " + listPref.getKey() + "\n entries=" + orderedEntries + "\n values=" + orderedValues);
        }

        public List<String> getOrderedListValues() {
            return orderedValues;
        }
    }
}
