package rocks.tbog.tblauncher.preference;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.StateSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ListAdapter;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceDialogFragmentCompat;

import java.util.ArrayList;
import java.util.List;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.drawable.DrawableUtils;
import rocks.tbog.tblauncher.utils.KeyboardDialogBuilder;
import rocks.tbog.tblauncher.utils.Utilities;
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

        final ListAdapter listAdapter;
        {
            final Bundle args = getArguments();
            String key = args != null ? args.getString(ARG_KEY) : null;
            final int listItemLayout = getItemLayout(builder.getContext());
            if ("adaptive-shape".equals(key))
                listAdapter = new IconAdapter(ShapeViewHolder.class, listItemLayout, list);
            else if ("icons-pack".equals(key))
                listAdapter = new IconAdapter(PackViewHolder.class, listItemLayout, list);
            else
                listAdapter = new ArrayAdapter<>(getContext(), listItemLayout, list);
        }

        builder.setSingleChoiceItems(
                listAdapter,
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

    @LayoutRes
    private static int getItemLayout(@NonNull Context context) {
        @LayoutRes final int layout;
        final Resources.Theme theme = context.getTheme();
        TypedValue res = new TypedValue();
        boolean found = theme.resolveAttribute(R.attr.singleChoiceItemLayout, res, true);
        if (found && res.resourceId != 0) {
            layout = res.resourceId;
        } else {
            TypedArray a = theme.obtainStyledAttributes(R.style.MaterialAlertDialog_MaterialComponents, new int[]{R.attr.singleChoiceItemLayout});
            layout = a.getResourceId(0, android.R.layout.simple_list_item_checked);
            a.recycle();
        }
        return layout;
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

    private static class IconAdapter extends ViewHolderListAdapter<IconEntry, ViewHolderAdapter.ViewHolder<IconEntry>> {
        protected IconAdapter(@NonNull Class<? extends ViewHolder<IconEntry>> viewHolderClass, int listItemLayout, @NonNull List<IconEntry> list) {
            super(viewHolderClass, listItemLayout, list);
        }
    }

    public static class ShapeViewHolder extends ViewHolderAdapter.ViewHolder<IconEntry> {
        private final static int[] STATE_CHECKED = new int[]{android.R.attr.state_checked};

        final TextView textView;
        int defaultColor = 0;
        int checkedColor = 0;
        int size;

        public ShapeViewHolder(View view) {
            super(view);
            textView = view.findViewById(android.R.id.text1);

            final Context context = view.getContext();

            // get color from theme
            {
                final Resources.Theme theme = context.getTheme();
                TypedValue res = new TypedValue();
                if (theme.resolveAttribute(R.attr.colorControlActivated, res, true)) {
                    if (res.type >= TypedValue.TYPE_FIRST_COLOR_INT && res.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                        checkedColor = res.data;
                    }
                }
                if (theme.resolveAttribute(R.attr.colorControlNormal, res, true)) {
                    if (res.type >= TypedValue.TYPE_FIRST_COLOR_INT && res.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                        defaultColor = res.data;
                    }
                }
            }

            if (defaultColor == 0 || checkedColor == 0) {
                ColorStateList textColorList = null;
                if (textView instanceof CheckedTextView) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        textColorList = ((CheckedTextView) textView).getCheckMarkTintList();
                    }
                }
                if (textColorList == null)
                    textColorList = textView.getTextColors();
                defaultColor = textColorList.getDefaultColor();
                checkedColor = textColorList.getColorForState(STATE_CHECKED, defaultColor);
            }

            size = context.getResources().getDimensionPixelSize(R.dimen.color_preview_size);
        }

        @Override
        protected void setContent(IconEntry content, int position, @NonNull ViewHolderAdapter<IconEntry, ? extends ViewHolderAdapter.ViewHolder<IconEntry>> adapter) {
            // set icon async
            Utilities.setViewAsync(textView,
                    context -> {
                        Drawable drawable = new ColorDrawable(defaultColor);
                        for (int shape : DrawableUtils.SHAPE_LIST) {
                            if (Integer.toString(shape).equals(content.value.toString())) {
                                Drawable shapedDrawable;
                                if (shape == DrawableUtils.SHAPE_NONE) {
                                    shapedDrawable = new ColorDrawable(Color.TRANSPARENT);
                                } else {
                                    StateListDrawable listDrawable = new StateListDrawable();
                                    Drawable checkedDrawable = new ColorDrawable(checkedColor);
                                    listDrawable.addState(STATE_CHECKED, DrawableUtils.applyIconMaskShape(context, checkedDrawable, shape));
                                    listDrawable.addState(StateSet.WILD_CARD, DrawableUtils.applyIconMaskShape(context, drawable, shape));

                                    shapedDrawable = listDrawable;
                                }
                                drawable = shapedDrawable;
                                break;
                            }
                        }
                        return drawable;
                    }, (view, drawable) -> {
                        if (!(view instanceof TextView))
                            return;
                        TextView textView = (TextView) view;

                        // compound drawables need a size
                        drawable.setBounds(0, 0, size, size);

                        // get relative because that's where the checkmark can be
                        Drawable[] cd = textView.getCompoundDrawablesRelative();
                        // set compound drawable
                        textView.setCompoundDrawablesRelative(cd[0], cd[1], drawable, cd[3]);
                    });

            // set text
            textView.setText(content.name);
        }
    }

    public static class PackViewHolder extends ViewHolderAdapter.ViewHolder<IconEntry> {
        final TextView textView;
        int size;

        public PackViewHolder(View view) {
            super(view);
            textView = view.findViewById(android.R.id.text1);
            final Context context = view.getContext();
            size = context.getResources().getDimensionPixelSize(R.dimen.color_preview_size);
        }

        @Override
        protected void setContent(IconEntry content, int position, @NonNull ViewHolderAdapter<IconEntry, ? extends ViewHolderAdapter.ViewHolder<IconEntry>> adapter) {
            // set icon async
            Utilities.setViewAsync(textView,
                    ctx -> {
                        try {
                            return ctx.getPackageManager().getApplicationIcon(content.value.toString());
                        } catch (Exception ignored) {
                        }
                        return new ColorDrawable(Color.TRANSPARENT);
                    },
                    (view, drawable) -> {
                        if (!(view instanceof TextView))
                            return;
                        drawable.setBounds(0, 0, size, size);
                        TextView textView = (TextView) view;
                        // get relative because that's where the checkmark can be
                        Drawable[] cd = textView.getCompoundDrawablesRelative();
                        // set compound drawable
                        textView.setCompoundDrawablesRelative(cd[0], cd[1], drawable, cd[3]);
                    });

            // set text
            textView.setText(content.name);
        }
    }
}
