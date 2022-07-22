package rocks.tbog.tblauncher.preference;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.DialogPreference;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.utils.PrefCache;

public class SliderDialog extends BasePreferenceDialog {

    private static final String TAG = SliderDialog.class.getSimpleName();

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
        SharedPreferences sharedPreferences = preference.getSharedPreferences();
        if (sharedPreferences == null) {
            Log.e(TAG, "getSharedPreferences == null for preference `" + key + "`");
            return;
        }
        {
            Object value = sharedPreferences.getAll().get(key);
            if (value != null)
                preference.setValue(value);
        }
        SeekBar seekBar = root.findViewById(R.id.seekBar); // seekBar default minimum is set to 0
        if (key.endsWith("-alpha")) {
            seekBar.setMax(255);
            ((TextView) root.findViewById(android.R.id.text1)).setText(R.string.title_select_alpha);
        }

        switch (key) {
            case "search-bar-text-size":
                ((TextView) root.findViewById(android.R.id.text1)).setText(R.string.search_bar_text_size);
                break;
            case "search-bar-height":
                ((TextView) root.findViewById(android.R.id.text1)).setText(R.string.search_bar_height);
                break;
            case "quick-list-height":
                ((TextView) root.findViewById(android.R.id.text1)).setText(R.string.quick_list_height);
                break;
            case "popup-corner-radius":
            case "quick-list-radius":
                ((TextView) root.findViewById(android.R.id.text1)).setText(R.string.corner_radius);
                break;
            case "result-shadow-dx":
            case "result-shadow-dy":
            case "popup-shadow-dx":
            case "popup-shadow-dy":
            case "search-bar-shadow-dx":
            case "search-bar-shadow-dy":
                ((TextView) root.findViewById(android.R.id.text1)).setText(R.string.shadow_offset);
                break;
            case "result-shadow-radius":
            case "popup-shadow-radius":
            case "search-bar-shadow-radius":
                ((TextView) root.findViewById(android.R.id.text1)).setText(R.string.shadow_radius);
                break;
            default:
                break;
        }

        // because we can't set minimum below API 26 we make our own
        int minValue = 0;
        Float incrementByFloat = null;
        switch (key) {
            case "result-icon-size":
            case "quick-list-icon-size":
            case "tags-menu-icon-size":
                minValue = getResources().getInteger(R.integer.min_size_icon);
                seekBar.setMax(getResources().getInteger(R.integer.max_size_icon) - minValue);
                break;
            case "result-text-size":
            case "result-text2-size":
            case "search-bar-text-size":
            case "search-bar-height":
                minValue = 2;
                seekBar.setMax(seekBar.getMax() - minValue);
                break;
            case "quick-list-height":
                minValue = 2 * PrefCache.getDockRowCount(sharedPreferences);
                seekBar.setMax(100 * PrefCache.getDockRowCount(sharedPreferences) - minValue);
                break;
            case "result-history-size":
            case "result-history-adaptive":
            case "result-search-cap":
                minValue = 1;
                seekBar.setMax(1000 - minValue);
                break;
            case "icon-hue":
                minValue = -180;
                seekBar.setMax(180 - minValue);
                break;
            case "icon-scale-red":
            case "icon-scale-green":
            case "icon-scale-blue":
            case "icon-scale-alpha":
                minValue = -200;
                seekBar.setMax(200 - minValue);
                break;
            case "icon-contrast":
            case "icon-brightness":
            case "icon-saturation":
                minValue = -100;
                seekBar.setMax(100 - minValue);
                break;
            case "quick-list-columns":
                minValue = 1;
                seekBar.setMax(32 - minValue);
                break;
            case "quick-list-rows":
                minValue = 1;
                seekBar.setMax(8 - minValue);
                break;
            case "result-shadow-dx":
            case "result-shadow-dy":
            case "popup-shadow-dx":
            case "popup-shadow-dy":
            case "search-bar-shadow-dx":
            case "search-bar-shadow-dy":
                incrementByFloat = .1f;
                minValue = (int) (-5 / incrementByFloat);
                seekBar.setMax((int) (5 / incrementByFloat) - minValue);
                break;
            case "result-shadow-radius":
            case "popup-shadow-radius":
            case "search-bar-shadow-radius":
                incrementByFloat = .1f;
                seekBar.setMax((int) (10 / incrementByFloat) - minValue);
                break;
            default:
                break;
        }

        // set slider value
        setProgressFromPreference(seekBar, preference.getValue(), minValue, incrementByFloat);

        final TextView text2 = root.findViewById(android.R.id.text2);
        final SeekBarChangeListener<?> listener;

        // default change listener uses integers
        if (incrementByFloat == null) {
            listener = new SeekBarChangeListener.ProgressChangedInt(minValue, text2, (integer) -> {
                CustomDialogPreference pref = ((CustomDialogPreference) SliderDialog.this.getPreference());
                pref.setValue(integer);
            });
        } else {
            listener = new SeekBarChangeListener.ProgressChangedFloat(minValue, incrementByFloat, text2, (aFloat) -> {
                CustomDialogPreference pref = ((CustomDialogPreference) SliderDialog.this.getPreference());
                pref.setValue(aFloat);
            });
        }

        // update display value
        listener.onProgressChanged(seekBar, seekBar.getProgress(), false);

        // set change listener
        seekBar.setOnSeekBarChangeListener(listener);
    }

    public static void setProgressFromPreference(@NonNull SeekBar seekBar, @Nullable Object prefValue, int minValue, @Nullable Float incrementByFloat) {
        final int seekBarProgress;
        if (prefValue instanceof Integer) {
            seekBarProgress = (Integer) prefValue - minValue;
        } else if (prefValue instanceof Float) {
            float incrementBy = incrementByFloat != null ? incrementByFloat : 1f;
            seekBarProgress = Math.round(((Float) prefValue) / incrementBy) - minValue;
        } else {
            seekBarProgress = 0;
        }
        seekBar.setProgress(seekBarProgress);
    }
}
