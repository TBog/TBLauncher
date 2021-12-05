package rocks.tbog.tblauncher.preference;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
import rocks.tbog.tblauncher.utils.KeyboardDialogBuilder;
import rocks.tbog.tblauncher.utils.PrefOrderedListHelper;
import rocks.tbog.tblauncher.utils.ViewHolderAdapter;
import rocks.tbog.tblauncher.utils.ViewHolderListAdapter;

public class OrderListPreferenceDialog extends PreferenceDialogFragmentCompat {

    private static final String SAVE_STATE_VALUES = "OrderListPreferenceDialogFragment.values";
    private static final String SAVE_STATE_CHANGED = "OrderListPreferenceDialogFragment.changed";
    private static final String SAVE_STATE_ENTRIES = "OrderListPreferenceDialogFragment.entries";
    private static final String SAVE_STATE_ENTRY_VALUES = "OrderListPreferenceDialogFragment.entryValues";

    protected final HashSet<String> mNewValues = new HashSet<>();
    boolean mPreferenceChanged;
    protected CharSequence[] mEntries;
    protected CharSequence[] mEntryValues;

    public static OrderListPreferenceDialog newInstance(String key) {
        final OrderListPreferenceDialog fragment = new OrderListPreferenceDialog();
        final Bundle b = new Bundle(2);
        b.putString(ARG_KEY, key);
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

    protected ArrayList<ListEntry> generateEntryList() {
        final int entryCount = mEntryValues.length;
        ArrayList<ListEntry> entryArrayList = new ArrayList<>(entryCount);
        for (int i = 0; i < entryCount; i += 1) {
            ListEntry listEntry = new ListEntry(mEntries[i], mEntryValues[i].toString());
            entryArrayList.add(listEntry);
        }
        return entryArrayList;
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        KeyboardDialogBuilder.setCustomTitle(builder, getPreference().getDialogTitle());

        if (mEntryValues.length != mEntries.length)
            throw new IllegalStateException("mEntryValues.length=" + mEntryValues.length + " mEntries.length=" + mEntries.length);

        ArrayList<ListEntry> entryArrayList = generateEntryList();
        EntryAdapter entryAdapter = new EntryAdapter(EntryViewHolder.class, entryArrayList);
        builder.setAdapter(entryAdapter, null);

        entryAdapter.mOnMoveUpListener = (adapter, view, position) -> {
            if (position <= 0)
                return;

            List<ListEntry> list = adapter.getList();
            ListEntry entry = list.remove(position);
            list.add(position - 1, entry);
            adapter.notifyDataSetChanged();

            generateNewValues(list);
        };

        entryAdapter.mOnMoveDownListener = (adapter, view, position) -> {
            if ((position + 1) >= adapter.getCount())
                return;

            List<ListEntry> list = adapter.getList();
            ListEntry entry = list.remove(position);
            list.add(position + 1, entry);
            adapter.notifyDataSetChanged();

            generateNewValues(list);
        };
    }

    @Override
    public void onStart() {
        super.onStart();
        KeyboardDialogBuilder.setButtonBarBackground(requireDialog());
    }

    protected void generateNewValues(List<ListEntry> list) {
        mPreferenceChanged = true;
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
        final CharSequence name;
        final String value;

        public ListEntry(@NonNull CharSequence name, @NonNull String value) {
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

            if (position == 0)
                btnUp.setVisibility(View.INVISIBLE);
            else {
                btnUp.setVisibility(View.VISIBLE);
                btnUp.setOnClickListener(v -> {
                    if (entryAdapter.mOnMoveUpListener == null)
                        return;
                    int pos = entryAdapter.getList().indexOf(content);
                    if (pos != -1)
                        entryAdapter.mOnMoveUpListener.onClick(entryAdapter, v, pos);
                });
            }

            if (position == (adapter.getCount() - 1))
                btnDown.setVisibility(View.INVISIBLE);
            else {
                btnDown.setVisibility(View.VISIBLE);
                btnDown.setOnClickListener(v -> {
                    if (entryAdapter.mOnMoveDownListener == null)
                        return;
                    int pos = entryAdapter.getList().indexOf(content);
                    if (pos != -1)
                        entryAdapter.mOnMoveDownListener.onClick(entryAdapter, v, pos);
                });
            }
        }
    }
}
