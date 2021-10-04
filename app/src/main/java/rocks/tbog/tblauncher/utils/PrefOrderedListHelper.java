package rocks.tbog.tblauncher.utils;

import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class PrefOrderedListHelper {
    private static final String TAG = "Order";

    /**
     * Synchronize the toggle list with the order list. Remove toggled off entries and add at the end new ones.
     *
     * @param sharedPreferences we get the list from here and apply the changes to
     * @param listKey           preference key of the list
     * @param orderKey          preference key of the order list
     */
    public static void syncOrderedList(@NonNull SharedPreferences sharedPreferences, @NonNull String listKey, @NonNull String orderKey) {
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
            String name = getOrderedValueName(value);
            if (listSet.remove(name)) {
                newValues.add(makeOrderedValue(name, idx++));
            }
        }

        // add at the end all the new values
        for (String name : listSet)
            newValues.add(makeOrderedValue(name, idx++));

        Set<String> newOrderSet = new HashSet<>(newValues);
        if (!newOrderSet.equals(orderSet))
            sharedPreferences.edit().putStringSet(orderKey, newOrderSet).apply();
    }

    public static List<String> getOrderedList(@NonNull SharedPreferences sharedPreferences, @NonNull String listKey, @NonNull String orderKey) {
        syncOrderedList(sharedPreferences, listKey, orderKey);
        Set<String> orderSet = sharedPreferences.getStringSet(orderKey, Collections.emptySet());
        List<String> orderValues = new ArrayList<>(orderSet);
        Collections.sort(orderValues);
        return orderValues;
    }

    public static String getOrderedValueName(String value) {
        int pos = value.indexOf(". ");
        pos = pos >= 0 ? pos + 2 : 0;
        return value.substring(pos);
    }

    public static int getOrderedValueIndex(String value) {
        int pos = value.indexOf(". ");
        int order = 0;
        if (pos > 0) {
            String hexOrder = value.substring(0, pos);
            try {
                order = Integer.parseInt(hexOrder, 16);
            } catch (Exception e) {
                Log.e(TAG, "parse `" + hexOrder + "` in base 16", e);
            }
        } else {
            Log.e(TAG, "invalid ordered value `" + value + "`");
        }
        return order;
    }

    public static String makeOrderedValue(String name, int position) {
        return String.format(Locale.US, "%08x. %s", position, name);
    }

    public static ArrayList<String> getOrderedArrayList(CharSequence[] entryValues) {
        ArrayList<String> orderedValues = new ArrayList<>(entryValues.length);
        int ord = 0;
        for (CharSequence value : entryValues) {
            String orderedValue = makeOrderedValue(value.toString(), ord);
            orderedValues.add(orderedValue);
            ord += 1;
        }
        return orderedValues;
    }

    public static ArrayList<String> getOrderedArrayList(List<String> entryValues) {
        ArrayList<String> orderedValues = new ArrayList<>(entryValues.size());
        int ord = 0;
        for (String value : entryValues) {
            String orderedValue = makeOrderedValue(value, ord);
            orderedValues.add(orderedValue);
            ord += 1;
        }
        return orderedValues;
    }
}
