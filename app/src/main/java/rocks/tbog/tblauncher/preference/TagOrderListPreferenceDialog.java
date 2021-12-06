package rocks.tbog.tblauncher.preference;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.DialogPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;

import java.util.ArrayList;
import java.util.List;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.dataprovider.TagsProvider;
import rocks.tbog.tblauncher.entry.ActionEntry;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.entry.TagEntry;
import rocks.tbog.tblauncher.utils.PrefCache;
import rocks.tbog.tblauncher.utils.PrefOrderedListHelper;
import rocks.tbog.tblauncher.utils.Utilities;

public class TagOrderListPreferenceDialog extends OrderListPreferenceDialog {

    private static final String SAVE_STATE_UNTAGGED_IDX = "TagOrderListPreferenceDialogFragment.untaggedIdx";

    private int mUntaggedIndex = 0;

    public static TagOrderListPreferenceDialog newInstance(String key) {
        final TagOrderListPreferenceDialog fragment = new TagOrderListPreferenceDialog();
        final Bundle b = new Bundle(2);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            // find the index of the action entry (show/untagged)
            for (int i = 0; i < mEntryValues.length; i++) {
                CharSequence entryValue = mEntryValues[i];
                if (entryValue.toString().startsWith(ActionEntry.SCHEME)) {
                    mUntaggedIndex = i;
                    break;
                }
            }
        } else {
            mUntaggedIndex = savedInstanceState.getInt(SAVE_STATE_UNTAGGED_IDX);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SAVE_STATE_UNTAGGED_IDX, mUntaggedIndex);
    }

    @Nullable
    private EditTextPreference getUntaggedIndexPreference() {
        final DialogPreference.TargetFragment fragment = (DialogPreference.TargetFragment) getTargetFragment();
        if (fragment == null)
            return null;
        Preference pref = fragment.findPreference("tags-menu-untagged-index");
        return pref instanceof EditTextPreference ? (EditTextPreference) pref : null;
    }

    @Override
    protected ArrayList<ListEntry> generateEntryList() {
        final int entryCount = mEntryValues.length;

        Context context = getContext();
        boolean bAddUntagged = context != null && PrefCache.showTagsMenuUntagged(context);
        EntryItem untaggedEntry = TBApplication.dataHandler(context).getPojo(ActionEntry.SCHEME + "show/untagged");
        if (!(untaggedEntry instanceof ActionEntry))
            bAddUntagged = false;

        ArrayList<ListEntry> entryArrayList = new ArrayList<>(entryCount + (bAddUntagged ? 1 : 0));

        for (int i = 0; i < entryCount; i += 1) {
            String value = mEntryValues[i].toString();
            String tagId = TagsProvider.getTagId(value);
            ListEntry listEntry = new ListEntry(mEntries[i], tagId);
            entryArrayList.add(listEntry);
        }

        if (bAddUntagged) {
            EditTextPreference pref = getUntaggedIndexPreference();
            if (pref != null) {
                int idx;
                try {
                    idx = Integer.parseInt(pref.getText());
                } catch (Exception ignored) {
                    idx = 0;
                }
                if (idx > entryArrayList.size())
                    idx = entryArrayList.size();
                int size = context.getResources().getDimensionPixelSize(R.dimen.icon_preview_size);
                Drawable icon = ((ActionEntry) untaggedEntry).getIconDrawable(context);
                icon.setBounds(0, 0, size, size);

                int layoutDirection = context.getResources().getConfiguration().getLayoutDirection();
                CharSequence label = Utilities.addDrawableBeforeString(untaggedEntry.getName(), icon, layoutDirection);
                entryArrayList.add(idx, new ListEntry(label, untaggedEntry.id));
            }
        }

        return entryArrayList;
    }

    @Override
    protected void generateNewValues(List<ListEntry> list) {
        mPreferenceChanged = true;
        mNewValues.clear();
        int ord = 0;
        for (int idx = 0, listSize = list.size(); idx < listSize; idx++) {
            String id = list.get(idx).value;
            if (id.startsWith(TagEntry.SCHEME)) {
                String tagName = id.substring(TagEntry.SCHEME.length());
                mNewValues.add(PrefOrderedListHelper.makeOrderedValue(tagName, ord++));
            } else if (id.startsWith(ActionEntry.SCHEME)) {
                // assume it's "show/untagged"
                mUntaggedIndex = idx;
            }
        }
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult && mPreferenceChanged) {
            EditTextPreference pref = getUntaggedIndexPreference();
            if (pref != null)
                pref.setText(Integer.toString(mUntaggedIndex));
        }
        super.onDialogClosed(positiveResult);
    }
}
