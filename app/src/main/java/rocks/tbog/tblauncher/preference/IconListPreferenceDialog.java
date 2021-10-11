package rocks.tbog.tblauncher.preference;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceDialogFragmentCompat;

import java.util.ArrayList;
import java.util.List;

import rocks.tbog.tblauncher.drawable.DrawableUtils;
import rocks.tbog.tblauncher.utils.KeyboardDialogBuilder;
import rocks.tbog.tblauncher.utils.UISizes;
import rocks.tbog.tblauncher.utils.ViewHolderAdapter;
import rocks.tbog.tblauncher.utils.ViewHolderListAdapter;

public class IconListPreferenceDialog extends PreferenceDialogFragmentCompat {

    private static final String SAVE_STATE_INDEX = "IconListPreferenceDialog.index";
    private static final String SAVE_STATE_ENTRIES = "IconListPreferenceDialog.entries";
    private static final String SAVE_STATE_ENTRY_VALUES = "IconListPreferenceDialog.entryValues";

    private int mClickedDialogEntryIndex = -1;
    private CharSequence[] mEntries;
    private CharSequence[] mEntryValues;

    public static IconListPreferenceDialog newInstance(String key) {
        final IconListPreferenceDialog fragment = new IconListPreferenceDialog();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            final ListPreference preference = getListPreference();

            if (preference.getEntries() == null || preference.getEntryValues() == null) {
                throw new IllegalStateException("ListPreference requires an entries array and an entryValues array.");
            }

            mClickedDialogEntryIndex = preference.findIndexOfValue(preference.getValue());
            mEntries = preference.getEntries();
            mEntryValues = preference.getEntryValues();
        } else {
            mClickedDialogEntryIndex = savedInstanceState.getInt(SAVE_STATE_INDEX, 0);
            mEntries = savedInstanceState.getCharSequenceArray(SAVE_STATE_ENTRIES);
            mEntryValues = savedInstanceState.getCharSequenceArray(SAVE_STATE_ENTRY_VALUES);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SAVE_STATE_INDEX, mClickedDialogEntryIndex);
        outState.putCharSequenceArray(SAVE_STATE_ENTRIES, mEntries);
        outState.putCharSequenceArray(SAVE_STATE_ENTRY_VALUES, mEntryValues);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        ArrayList<IconEntry> list = new ArrayList<>(mEntries.length);
        for (int i = 0; i < mEntries.length; i++) {
            list.add(new IconEntry(mEntries[i], mEntryValues[i]));
        }

        builder.setSingleChoiceItems(
                new IconAdapter(android.R.layout.select_dialog_singlechoice, list),
                mClickedDialogEntryIndex,
                (dialog, which) -> {
                    mClickedDialogEntryIndex = which;

                    // Clicking on an item simulates the positive button click
                    IconListPreferenceDialog.this.onClick(dialog, DialogInterface.BUTTON_POSITIVE);
                    // and dismisses the dialog.
                    dialog.dismiss();
                });

        builder.setPositiveButton(null, null);
        KeyboardDialogBuilder.setCustomTitle(builder, getPreference().getDialogTitle());
    }

    @Override
    public void onStart() {
        super.onStart();
        KeyboardDialogBuilder.setButtonBarBackground(requireDialog());
    }

    private ListPreference getListPreference() {
        return (ListPreference) getPreference();
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult && mClickedDialogEntryIndex >= 0) {
            String value = mEntryValues[mClickedDialogEntryIndex].toString();
            final ListPreference preference = getListPreference();
            if (preference.callChangeListener(value)) {
                preference.setValue(value);
            }
        }
    }

    public static class IconEntry {
        private final CharSequence name;
        private final CharSequence value;

        public IconEntry(CharSequence name, CharSequence value) {
            this.name = name;
            this.value = value;
        }
    }

    private static class IconAdapter extends ViewHolderListAdapter<IconEntry, EntryViewHolder> {
        protected IconAdapter(int listItemLayout, @NonNull List<IconEntry> list) {
            super(EntryViewHolder.class, listItemLayout, list);
        }
    }

    public static class EntryViewHolder extends ViewHolderAdapter.ViewHolder<IconEntry> {
        TextView textView;

        protected EntryViewHolder(View view) {
            super(view);
            textView = view.findViewById(android.R.id.text1);
        }

        @Override
        protected void setContent(IconEntry content, int position, @NonNull ViewHolderAdapter<IconEntry, ? extends ViewHolderAdapter.ViewHolder<IconEntry>> adapter) {
            Context context = textView.getContext();
            Drawable drawable = new ColorDrawable(0xFF7f7f7f);
            for (int shape : DrawableUtils.SHAPE_LIST) {
                //String name = DrawableUtils.shapeName(context, shape);
                if (Integer.toString(shape).equals(content.value.toString())) {
                    Drawable shapedDrawable;
                    if (shape == DrawableUtils.SHAPE_NONE) {
                        shapedDrawable = new ColorDrawable(Color.TRANSPARENT);
                    } else {
                        shapedDrawable = DrawableUtils.applyIconMaskShape(context, drawable, shape);
                    }
                    drawable = shapedDrawable;
                    break;
                }
            }
            int size = UISizes.getResultIconSize(context);
            drawable.setBounds(0, 0, size, size);
            textView.setCompoundDrawables(drawable, null, null, null);
            textView.setCompoundDrawablePadding(UISizes.sp2px(context, 5));
            textView.setText(content.name);
        }
    }
}
