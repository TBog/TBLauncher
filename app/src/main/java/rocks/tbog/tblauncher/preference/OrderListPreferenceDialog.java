package rocks.tbog.tblauncher.preference;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.PreferenceDialogFragmentCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.dataprovider.TagsProvider;
import rocks.tbog.tblauncher.entry.TagEntry;
import rocks.tbog.tblauncher.utils.KeyboardDialogBuilder;
import rocks.tbog.tblauncher.utils.PrefOrderedListHelper;
import rocks.tbog.tblauncher.utils.Utilities;
import rocks.tbog.tblauncher.utils.ViewHolderAdapter;
import rocks.tbog.tblauncher.utils.ViewHolderListAdapter;

public class OrderListPreferenceDialog extends PreferenceDialogFragmentCompat {

    protected static final String ARG_TAG_ICONS = "showTagIcons";

    private static final String SAVE_STATE_VALUES = "OrderListPreferenceDialogFragment.values";
    private static final String SAVE_STATE_CHANGED = "OrderListPreferenceDialogFragment.changed";
    private static final String SAVE_STATE_ENTRIES = "OrderListPreferenceDialogFragment.entries";
    private static final String SAVE_STATE_ENTRY_VALUES = "OrderListPreferenceDialogFragment.entryValues";

    private final HashSet<String> mNewValues = new HashSet<>();
    boolean mPreferenceChanged;
    private CharSequence[] mEntries;
    private CharSequence[] mEntryValues;

    public static OrderListPreferenceDialog newInstance(String key, boolean showTagIcons) {
        final OrderListPreferenceDialog fragment = new OrderListPreferenceDialog();
        final Bundle b = new Bundle(2);
        b.putString(ARG_KEY, key);
        b.putBoolean(ARG_TAG_ICONS, showTagIcons);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            final MultiSelectListPreference preference = getListPreference();

            if (preference.getEntries() == null || preference.getEntryValues() == null) {
                throw new IllegalStateException("OrderListPreferenceDialog requires an entries array and an entryValues array.");
            }

            mNewValues.clear();
            mNewValues.addAll(preference.getValues());
            mPreferenceChanged = false;
            mEntries = preference.getEntries();
            mEntryValues = preference.getEntryValues();
        } else {
            mNewValues.clear();
            mNewValues.addAll(savedInstanceState.getStringArrayList(SAVE_STATE_VALUES));
            mPreferenceChanged = savedInstanceState.getBoolean(SAVE_STATE_CHANGED, false);
            mEntries = savedInstanceState.getCharSequenceArray(SAVE_STATE_ENTRIES);
            mEntryValues = savedInstanceState.getCharSequenceArray(SAVE_STATE_ENTRY_VALUES);
        }

        Log.d("pref", "OrderListPreferenceDialog " + getPreference().getKey() + "\n entries=" + Arrays.toString(mEntries) + "\n values=" + Arrays.toString(mEntryValues));
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList(SAVE_STATE_VALUES, new ArrayList<>(mNewValues));
        outState.putBoolean(SAVE_STATE_CHANGED, mPreferenceChanged);
        outState.putCharSequenceArray(SAVE_STATE_ENTRIES, mEntries);
        outState.putCharSequenceArray(SAVE_STATE_ENTRY_VALUES, mEntryValues);
    }

    private MultiSelectListPreference getListPreference() {
        return (MultiSelectListPreference) getPreference();
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        KeyboardDialogBuilder.setCustomTitle(builder, getPreference().getDialogTitle());

        if (mEntryValues.length != mEntries.length)
            throw new IllegalStateException("mEntryValues.length=" + mEntryValues.length + " mEntries.length=" + mEntries.length);

        final int entryCount = mEntryValues.length;
        ArrayList<ListEntry> entryArrayList = new ArrayList<>(entryCount);
        for (int i = 0; i < entryCount; i += 1) {
            ListEntry listEntry = new ListEntry(mEntries[i].toString(), mEntryValues[i].toString());
            entryArrayList.add(listEntry);
        }

        final Bundle args = getArguments();
        boolean tagIcons = args != null && args.getBoolean(ARG_TAG_ICONS, false);
        EntryAdapter entryAdapter = new EntryAdapter(tagIcons ? TagEntryViewHolder.class : EntryViewHolder.class, entryArrayList);
        builder.setAdapter(entryAdapter, null);

        entryAdapter.mOnMoveUpListener = (adapter, view, position) -> {
            List<ListEntry> list = adapter.getList();
            ListEntry entry = list.remove(position);
            list.add(position - 1, entry);
            adapter.notifyDataSetChanged();

            mPreferenceChanged = true;
            generateNewValues(list);
        };

        entryAdapter.mOnMoveDownListener = (adapter, view, position) -> {
            List<ListEntry> list = adapter.getList();
            ListEntry entry = list.remove(position);
            list.add(position + 1, entry);
            adapter.notifyDataSetChanged();

            mPreferenceChanged = true;
            generateNewValues(list);
        };
    }

    @Override
    public void onStart() {
        super.onStart();
        KeyboardDialogBuilder.setButtonBarBackground(requireDialog());
    }

    private void generateNewValues(List<ListEntry> list) {
        mNewValues.clear();
        int ord = 0;
        for (ListEntry entry : list) {
            mNewValues.add(PrefOrderedListHelper.makeOrderedValue(entry.value, ord++));
        }
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult && mPreferenceChanged) {
            final MultiSelectListPreference preference = getListPreference();
            Log.d("pref", "onDialogClosed " + preference.getKey() + "\n mNewValues=" + mNewValues);
            if (preference.callChangeListener(mNewValues)) {
                preference.setEntryValues(mNewValues.toArray(new CharSequence[0]));
                preference.setValues(mNewValues);
            }
        }
        mPreferenceChanged = false;
    }

    public static class ListEntry {
        final String name;
        final String value;

        public ListEntry(@NonNull String name, @NonNull String value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            ListEntry entry = (ListEntry) o;
            return name.equals(entry.name) && value.equals(entry.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, value);
        }
    }

    private static class EntryAdapter extends ViewHolderListAdapter<ListEntry, EntryViewHolder> {

        private OnItemClickListener mOnMoveUpListener = null;
        private OnItemClickListener mOnMoveDownListener = null;

        public interface OnItemClickListener {
            void onClick(EntryAdapter adapter, View view, int position);
        }

        protected EntryAdapter(@NonNull Class<? extends EntryViewHolder> viewHolderClass, @NonNull List<ListEntry> list) {
            super(viewHolderClass, R.layout.order_list_item, list);
        }

        public List<ListEntry> getList() {
            return mList;
        }
    }

    public static class EntryViewHolder extends ViewHolderAdapter.ViewHolder<ListEntry> {
        TextView textView;
        View btnUp;
        View btnDown;

        protected EntryViewHolder(View view) {
            super(view);
            textView = view.findViewById(android.R.id.text1);
            btnUp = view.findViewById(android.R.id.button1);
            btnDown = view.findViewById(android.R.id.button2);
        }

        @Override
        protected void setContent(ListEntry content, int position, @NonNull ViewHolderAdapter<ListEntry, ? extends ViewHolderAdapter.ViewHolder<ListEntry>> adapter) {
            EntryAdapter entryAdapter = (EntryAdapter) adapter;

            textView.setText(content.name);

            btnUp.setOnClickListener(v -> {
                if (entryAdapter.mOnMoveUpListener == null)
                    return;
                int pos = entryAdapter.getList().indexOf(content);
                if (pos != -1)
                    entryAdapter.mOnMoveUpListener.onClick(entryAdapter, v, pos);
            });

            btnDown.setOnClickListener(v -> {
                if (entryAdapter.mOnMoveDownListener == null)
                    return;
                int pos = entryAdapter.getList().indexOf(content);
                if (pos != -1)
                    entryAdapter.mOnMoveDownListener.onClick(entryAdapter, v, pos);
            });
        }
    }

    public static class TagEntryViewHolder extends EntryViewHolder {
        ImageView icon;

        protected TagEntryViewHolder(View view) {
            super(view);
            icon = view.findViewById(android.R.id.icon);
        }

        @Override
        protected void setContent(ListEntry content, int position, @NonNull ViewHolderAdapter<ListEntry, ? extends ViewHolderAdapter.ViewHolder<ListEntry>> adapter) {
            super.setContent(content, position, adapter);
            if (icon == null)
                return;

            icon.setVisibility(View.VISIBLE);
            TagsProvider tagsProvider = TBApplication.dataHandler(icon.getContext()).getTagsProvider();
            TagEntry tagEntry = tagsProvider != null ? tagsProvider.getTagEntry(content.name) : null;
            if (tagEntry != null)
                Utilities.setIconAsync(icon, tagEntry::getIconDrawable);
            else
                icon.setImageDrawable(null);
        }
    }
}
