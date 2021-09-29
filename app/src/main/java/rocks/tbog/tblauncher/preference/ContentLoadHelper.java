package rocks.tbog.tblauncher.preference;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.SettingsActivity;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.TagsHandler;
import rocks.tbog.tblauncher.utils.PrefOrderedListHelper;

public class ContentLoadHelper {
    private static final int[] RESULT_POPUP_CATEGORIES = {
            R.string.popup_title_hist_fav,
            R.string.popup_title_customize,
            R.string.popup_title_link,
    };

    public static OrderedMultiSelectListData generateResultPopupContent(@NonNull Context context, @NonNull SharedPreferences sharedPreferences) {
        final HashSet<String> values = new HashSet<>(RESULT_POPUP_CATEGORIES.length);
        // get default values
        for (int stringResId : RESULT_POPUP_CATEGORIES) {
            String categoryText = context.getString(stringResId);
            values.add(categoryText);
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
        for (int stringResId : RESULT_POPUP_CATEGORIES) {
            String categoryText = context.getString(stringResId);
            if (values.remove(categoryText))
                newOrder.add(categoryText);
        }

        // make new order values
        orderedValues.clear();
        orderedValues.addAll(PrefOrderedListHelper.getOrderedArrayList(newOrder));

        return new OrderedMultiSelectListData(null, null, null, orderedValues);
    }

    public static OrderedMultiSelectListData generateTagsMenuContent(@NonNull Context context, @NonNull SharedPreferences sharedPreferences) {
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
        return new OrderedMultiSelectListData(entries, entryValues, defaultValues, orderedValues);
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
        }

        public void setOrderedListValues(@Nullable Preference preference) {
            if (!(preference instanceof MultiSelectListPreference))
                return;
            MultiSelectListPreference listPref = (MultiSelectListPreference) preference;

            ArrayList<String> entries = new ArrayList<>(orderedValues.size());
            for (String value : orderedValues) {
                entries.add(PrefOrderedListHelper.getOrderedValueName(value));
            }

            listPref.setEntries(entries.toArray(new String[0]));
            listPref.setEntryValues(orderedValues.toArray(new String[0]));
        }

    }
}
