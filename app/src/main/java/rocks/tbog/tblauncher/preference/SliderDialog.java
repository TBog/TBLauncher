package rocks.tbog.tblauncher.preference;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;

import androidx.preference.DialogPreference;
import androidx.preference.PreferenceDialogFragmentCompat;

import rocks.tbog.tblauncher.R;

public class SliderDialog extends PreferenceDialogFragmentCompat {

    public static SliderDialog newInstance(String key) {
        SliderDialog fragment = new SliderDialog();
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
        preference.persistValueIfAllowed();
    }

    @Override
    protected View onCreateDialogView(Context context) {
        View root = super.onCreateDialogView(context);
        CustomDialogPreference preference = (CustomDialogPreference) getPreference();
        final String key = preference.getKey();

        // initialize value
        preference.setValue(preference.getSharedPreferences().getInt(key, 255));

        SeekBar seekBar = root.findViewById(R.id.seekBar); // seekBar default minimum is set to 0
        if ("notification-bar-alpha".equals(key))
            seekBar.setMax(255);

        seekBar.setProgress((Integer) preference.getValue());
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                if (fromUser) {
//                    CustomDialogPreference pref = ((CustomDialogPreference) SliderDialog.this.getPreference());
//                    pref.setValue(progress);
//                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                CustomDialogPreference pref = ((CustomDialogPreference) SliderDialog.this.getPreference());
                pref.setValue(progress);
            }
        });

        return root;
    }
}
