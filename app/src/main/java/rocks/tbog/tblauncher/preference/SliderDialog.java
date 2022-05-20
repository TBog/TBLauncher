package rocks.tbog.tblauncher.preference;

import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.preference.DialogPreference;

import rocks.tbog.tblauncher.R;

public class SliderDialog extends BasePreferenceDialog {

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
            default:
                break;
        }

        // because we can't set minimum below API 26 we make our own
        int minValue = 0;
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
            case "quick-list-height":
                minValue = 2;
                seekBar.setMax(seekBar.getMax() - minValue);
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
            default:
                break;
        }

        // set slider value
        int seekBarProgress = (Integer) preference.getValue() - minValue;
        seekBar.setProgress(seekBarProgress);

        // make change listener
        TextView text2 = root.findViewById(android.R.id.text2);
        ProgressChanged listener = new ProgressChanged(minValue, text2, (progress) -> {
            CustomDialogPreference pref = ((CustomDialogPreference) SliderDialog.this.getPreference());
            pref.setValue(progress);
        });

        // update display value
        listener.onProgressChanged(seekBar, seekBarProgress, false);

        // set change listener
        seekBar.setOnSeekBarChangeListener(listener);
    }

    interface ValueChanged {
        void valueChanged(int newValue);
    }

    private static class ProgressChanged implements SeekBar.OnSeekBarChangeListener {
        private final int offset;
        private final TextView textView;
        private final ValueChanged listener;

        public ProgressChanged(int offset, TextView textView, ValueChanged listener) {
            this.offset = offset;
            this.textView = textView;
            this.listener = listener;
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            progress += offset;
            textView.setText(textView.getResources().getString(R.string.value, progress));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // do nothing
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            int progress = seekBar.getProgress();
            progress += offset;
            listener.valueChanged(progress);
        }
    }
}
