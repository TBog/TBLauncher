package rocks.tbog.tblauncher.preference;

import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.preference.DialogPreference;
import androidx.preference.PreferenceDialogFragmentCompat;

import rocks.tbog.tblauncher.R;

public class SliderDialog extends PreferenceDialogFragmentCompat {

    protected TextView mTextView2;
    protected int mSliderOffset = 0;

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
    protected void onBindDialogView(View root) {
        super.onBindDialogView(root);

        CustomDialogPreference preference = (CustomDialogPreference) getPreference();
        final String key = preference.getKey();

        // initialize value
        preference.setValue(preference.getSharedPreferences().getInt(key, 255));

        SeekBar seekBar = root.findViewById(R.id.seekBar); // seekBar default minimum is set to 0
        if (key.endsWith("-alpha"))
            seekBar.setMax(255);

        switch (key) {
            case "search-bar-size":
                ((TextView) root.findViewById(android.R.id.text1)).setText(R.string.search_bar_size);
                break;
            case "quick-list-size":
                ((TextView) root.findViewById(android.R.id.text1)).setText(R.string.quick_list_size);
                break;
        }

        // because we can't set minimum below API 26 we make our own
        switch (key) {
            case "result-text-size":
            case "result-text2-size":
            case "result-icon-size":
                mSliderOffset = 2;
                seekBar.setMax(seekBar.getMax() - mSliderOffset);
                break;
            case "result-history-size":
            case "result-history-adaptive":
            case "result-search-cap":
                mSliderOffset = 1;
                seekBar.setMax(1000 - mSliderOffset);
        }

        int seekBarProgress = (Integer) preference.getValue() - mSliderOffset;
        seekBar.setProgress(seekBarProgress);

        // update display value
        mTextView2 = root.findViewById(android.R.id.text2);
        {
            int progress = seekBarProgress;
            progress += mSliderOffset;
            mTextView2.setText(mTextView2.getResources().getString(R.string.value, progress));
        }

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progress += mSliderOffset;
                mTextView2.setText(mTextView2.getResources().getString(R.string.value, progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                progress += mSliderOffset;
                CustomDialogPreference pref = ((CustomDialogPreference) SliderDialog.this.getPreference());
                pref.setValue(progress);
            }
        });
    }
}
