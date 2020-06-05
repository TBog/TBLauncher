package rocks.tbog.tblauncher.preference;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.preference.DialogPreference;
import androidx.preference.PreferenceDialogFragmentCompat;

import java.util.Map;

import rocks.tbog.tblauncher.R;

public class SliderDialog extends PreferenceDialogFragmentCompat {

    protected TextView mTextView2;

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

//        Map<String, Object> map = (Map<String, Object>) preference.getSharedPreferences().getAll();
//        for (Map.Entry<String, Object> entry : map.entrySet()) {
//            String value;
//            if (entry.getValue() instanceof Integer)
//                value = Integer.toString((Integer) entry.getValue());
//            else if (entry.getValue() instanceof String)
//                value = (String) entry.getValue();
//            else
//                value = entry.getValue().toString();
//
//            Log.d("Pref", "pref[ `" + entry.getKey() + "` ]= `" + value + "`");
//        }

        // initialize value
        preference.setValue(preference.getSharedPreferences().getInt(key, 255));

        SeekBar seekBar = root.findViewById(R.id.seekBar); // seekBar default minimum is set to 0
        if (key.endsWith("-alpha"))
            seekBar.setMax(255);
        seekBar.setProgress((Integer) preference.getValue());

        switch (key) {
            case "search-bar-size":
                ((TextView) root.findViewById(android.R.id.text1)).setText(R.string.search_bar_size);
                break;
            case "quick-list-size":
                ((TextView) root.findViewById(android.R.id.text1)).setText(R.string.quick_list_size);
                break;
        }

        mTextView2 = root.findViewById(android.R.id.text2);
        mTextView2.setText(mTextView2.getResources().getString(R.string.value, seekBar.getProgress()));

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mTextView2.setText(mTextView2.getResources().getString(R.string.value, progress));
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
