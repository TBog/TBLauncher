package rocks.tbog.tblauncher.preference;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

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
                ((TextView) root.findViewById(android.R.id.text1)).setText(R.string.shadow_offset);
                break;
            case "result-shadow-radius":
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
                incrementByFloat = .1f;
                minValue = (int) (-5 / incrementByFloat);
                seekBar.setMax((int) (5 / incrementByFloat) - minValue);
                break;
            case "result-shadow-radius":
                incrementByFloat = .1f;
                seekBar.setMax((int) (10 / incrementByFloat) - minValue);
                break;
            default:
                break;
        }

        // set slider value
        final int seekBarProgress;
        if (preference.getValue() instanceof Integer) {
            seekBarProgress = (Integer) preference.getValue() - minValue;
        } else if (preference.getValue() instanceof Float) {
            float incrementBy = incrementByFloat != null ? incrementByFloat : 1f;
            seekBarProgress = Math.round(((Float) preference.getValue()) / incrementBy) - minValue;
        } else {
            seekBarProgress = 0;
        }
        seekBar.setProgress(seekBarProgress);

        final TextView text2 = root.findViewById(android.R.id.text2);
        final ProgressChanged<?> listener;

        // default change listener uses integers
        if (incrementByFloat == null) {
            listener = new ProgressChangedInt(minValue, text2, (integer) -> {
                CustomDialogPreference pref = ((CustomDialogPreference) SliderDialog.this.getPreference());
                pref.setValue(integer);
            });
        } else {
            listener = new ProgressChangedFloat(minValue, incrementByFloat, text2, (aFloat) -> {
                CustomDialogPreference pref = ((CustomDialogPreference) SliderDialog.this.getPreference());
                pref.setValue(aFloat);
            });
        }

        // update display value
        listener.onProgressChanged(seekBar, seekBarProgress, false);

        // set change listener
        seekBar.setOnSeekBarChangeListener(listener);
    }

    interface ValueChanged<T> {
        void valueChanged(T newValue);
    }

    private static abstract class ProgressChanged<T> implements SeekBar.OnSeekBarChangeListener {
        protected final int offset;
        protected final TextView textView;
        protected final ValueChanged<T> listener;

        public ProgressChanged(int offset, TextView textView, ValueChanged<T> listener) {
            this.offset = offset;
            this.textView = textView;
            this.listener = listener;
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // do nothing
        }
    }

    private static class ProgressChangedInt extends ProgressChanged<Integer> {

        public ProgressChangedInt(int offset, TextView textView, ValueChanged<Integer> listener) {
            super(offset, textView, listener);
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            final int newValue = progress + offset;
            textView.setText(textView.getResources().getString(R.string.value, newValue));
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            int progress = seekBar.getProgress();
            progress += offset;
            listener.valueChanged(progress);
        }
    }

    private static class ProgressChangedFloat extends ProgressChanged<Float> {
        protected float incrementBy;

        public ProgressChangedFloat(int offset, float incrementBy, TextView textView, ValueChanged<Float> listener) {
            super(offset, textView, listener);
            this.incrementBy = incrementBy;
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            final float newValue = (progress + offset) * incrementBy;
            textView.setText(textView.getResources().getString(R.string.value_float, newValue));
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            float progress = seekBar.getProgress();
            progress = (progress + offset) * incrementBy;
            listener.valueChanged(progress);
        }
    }
}
