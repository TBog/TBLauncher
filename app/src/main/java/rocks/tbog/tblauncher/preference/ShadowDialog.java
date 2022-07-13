package rocks.tbog.tblauncher.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.DialogPreference;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.ui.CustomizeShadowView;
import rocks.tbog.tblauncher.utils.UIColors;

public class ShadowDialog extends BasePreferenceDialog {
    private static final String TAG = ShadowDialog.class.getSimpleName();
    private final MutableLiveData<Float> offsetX = new MutableLiveData<>();
    private final MutableLiveData<Float> offsetY = new MutableLiveData<>();
    private final MutableLiveData<Float> radius = new MutableLiveData<>();

    public static ShadowDialog newInstance(String key) {
        ShadowDialog fragment = new ShadowDialog();
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
        final var offsetXValue = offsetX.getValue();
        if (offsetXValue != null)
            preference.persistValueIfAllowed(offsetXValue);

        SharedPreferences sharedPreferences = preference.getSharedPreferences();
        var editor = sharedPreferences != null ? sharedPreferences.edit() : null;
        if (editor != null) {
            final var offsetYValue = offsetY.getValue();
            if (offsetYValue != null) {
                final String key = preference.getKey().replace("-dx", "-dy");
                editor.putFloat(key, offsetYValue);
            }

            final var radiusValue = radius.getValue();
            if (radiusValue != null) {
                final String key = preference.getKey().replace("-dx", "-radius");
                editor.putFloat(key, radiusValue);
            }

            editor.apply();
        }
    }

    @Override
    protected void onBindDialogView(View root) {
        super.onBindDialogView(root);

        CustomDialogPreference preference = (CustomDialogPreference) getPreference();
        final String keyX = preference.getKey();
        if (!keyX.endsWith("-dx"))
            throw new IllegalStateException("pref key `" + keyX + "` must end with `-dx`");
        final String keyY = keyX.replace("-dx", "-dy");
        final String keyR = keyX.replace("-dx", "-radius");
        final String keyC = keyX.replace("-dx", "-color");

        SharedPreferences sharedPreferences = preference.getSharedPreferences();
        if (sharedPreferences == null) {
            Log.e(TAG, "getSharedPreferences == null for preference `" + keyX + "`");
            return;
        }
        var prefMap = sharedPreferences.getAll();

        CustomizeShadowView viewXY = root.findViewById(R.id.viewXY);
        TextView textValueXY = root.findViewById(R.id.textValueXY);
        SeekBar seekBar = root.findViewById(R.id.seekBar);
        TextView textValueSlider = root.findViewById(R.id.textValueSlider);

        // initialize LiveData
        {
            var value = prefMap.get(keyX);
            offsetX.setValue(value instanceof Float ? (float) value : 0f);
        }
        {
            var value = prefMap.get(keyY);
            offsetY.setValue(value instanceof Float ? (float) value : 0f);
        }
        {
            var value = prefMap.get(keyR);
            radius.setValue(value instanceof Float ? (float) value : 0f);
        }

        // get additional data
        final int shadowColor;
        {
            var value = prefMap.get(keyC);
            shadowColor = value instanceof Integer ? (int) value : 0;
        }

        // set view parameters
        //setShadowParameters(viewXY, shadowColor);

        final Context ctx = getContext();

        // initialize shadow preview
        int color1, color2, textColor;
        switch (keyX) {
            case "result-shadow-dx":
                color1 = UIColors.getResultListBackground(ctx);
                color2 = UIColors.getResultListRipple(ctx);
                textColor = UIColors.getResultTextColor(ctx);
                break;
            case "popup-shadow-dx":
                color1 = UIColors.getPopupBackgroundColor(ctx);
                color2 = UIColors.getPopupRipple(ctx);
                textColor = UIColors.getPopupTextColor(ctx);
                break;
            case "search-bar-shadow-dx":
                color1 = UIColors.getColor(sharedPreferences, "search-bar-argb");
                color2 = UIColors.getColor(sharedPreferences, "search-bar-ripple-color");
                textColor = UIColors.getSearchTextColor(ctx);
                break;
            default:
                color1 = 0;
                color2 = 0;
                textColor = 0;
                break;
        }
        if (color1 != 0) {
            color1 = UIColors.setAlpha(color1, 0xFF);
            color2 = UIColors.setAlpha(color2, 0xFF);
            viewXY.setBackgroundParameters(color1, color2);
        }
        if (textColor != 0)
            viewXY.setTextParameters(null, textColor);

        viewXY.setOnOffsetChanged((dx, dy) -> {
            offsetX.postValue(dx);
            offsetY.postValue(dy);
        });

        int minValue = 0;
        float incrementByFloat = .1f;

        MediatorLiveData<LiveShadowParameters> dataMerge = new MediatorLiveData<>();
        dataMerge.addSource(offsetX, aFloat -> dataMerge.setValue(new LiveShadowParameters(radius.getValue(), aFloat, offsetY.getValue())));
        dataMerge.addSource(offsetY, aFloat -> dataMerge.setValue(new LiveShadowParameters(radius.getValue(), offsetX.getValue(), aFloat)));
        dataMerge.addSource(radius, aFloat -> dataMerge.setValue(new LiveShadowParameters(aFloat, offsetX.getValue(), offsetY.getValue())));

        dataMerge.observe(getDialogLifecycleOwner(), liveShadowParameters -> {
            final float r = liveShadowParameters.radius;
            final float dx = liveShadowParameters.dx;
            final float dy = liveShadowParameters.dy;
            viewXY.setShadowParameters(r, dx, dy, shadowColor);
            textValueXY.setText(getResources().getString(R.string.value_float_xy, dx, dy));
            textValueSlider.setText(getResources().getString(R.string.value_float, r));
        });

        // set slider value
        SliderDialog.setProgressFromPreference(seekBar, radius.getValue(), minValue, incrementByFloat);

        // set seek bar change listener
        seekBar.setOnSeekBarChangeListener(new SeekBarChangeListener(radius, minValue, incrementByFloat));
    }

    private static class SeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {
        final MutableLiveData<Float> variable;
        final int minValue;
        final float incrementByFloat;

        public SeekBarChangeListener(MutableLiveData<Float> var, int min, float inc) {
            variable = var;
            minValue = min;
            incrementByFloat = inc;
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            final float newValue = (progress + minValue) * incrementByFloat;
            variable.postValue(newValue);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // do nothing
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            // do nothing
        }
    }

    private static class LiveShadowParameters {
        Float dx, dy, radius;

        public LiveShadowParameters(Float radius, Float dx, Float dy) {
            this.dx = dx;
            this.dy = dy;
            this.radius = radius;
        }
    }
}
