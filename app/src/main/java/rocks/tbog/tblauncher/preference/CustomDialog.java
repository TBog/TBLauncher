package rocks.tbog.tblauncher.preference;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.preference.DialogPreference;
import androidx.preference.PreferenceDialogFragmentCompat;

import com.android.colorpicker.ColorPickerPalette;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.utils.UIColors;

public class CustomDialog extends PreferenceDialogFragmentCompat {

    private ColorPickerPalette mPalette;

    public static CustomDialog newInstance(String key) {
        final CustomDialog fragment = new CustomDialog();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);

        return fragment;
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (!positiveResult)
            return;
        DialogPreference dialogPreference = getPreference();
        if (!(dialogPreference instanceof CustomDialogPreference))
            return;
        CustomDialogPreference preference = (CustomDialogPreference) dialogPreference;
        // save data when user clicked OK
        if ("notification-bar-color".equals(preference.getKey())) {
            preference.persistValueIfAllowed();
        }
    }

    @Override
    protected View onCreateDialogView(Context context) {
        View root = super.onCreateDialogView(context);
        DialogPreference preference = getPreference();
        final String key = preference.getKey();
        // initialize layout data
        if ("notification-bar-color".equals(key)) {
            // initialize value
            ((CustomDialogPreference) preference).setValue(preference.getSharedPreferences().getInt(key, UIColors.COLOR_DEFAULT));

            mPalette = root.findViewById(R.id.colorPicker);
            mPalette.init(ColorPickerPalette.SIZE_SMALL, 4, color -> {
                CustomDialogPreference pref = ((CustomDialogPreference) CustomDialog.this.getPreference());
                pref.setValue(color);
                //pref.persistValueIfAllowed();
                CustomDialog.this.drawPalette();
                //CustomDialog.this.dismiss();
            });
        }
        return root;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        DialogPreference preference = getPreference();
        // initialize layout data
        if ("notification-bar-color".equals(preference.getKey())) {
//            // Calculate number of swatches to display
//            int swatchSize = mPalette.getResources().getDimensionPixelSize(R.dimen.color_swatch_small);
//            mPalette.init(ColorPickerPalette.SIZE_SMALL, (view.getWidth() - (swatchSize * 2 / 3)) / swatchSize, mPalette.mOnColorSelectedListener);
            drawPalette();
        }
    }

    private void drawPalette() {
        if (mPalette != null) {
            Object selectedColor = ((CustomDialogPreference) getPreference()).getValue();
            if (!(selectedColor instanceof Integer))
                selectedColor = 0;
            mPalette.drawPalette(UIColors.COLOR_LIST, (int) selectedColor);
        }
    }

}
