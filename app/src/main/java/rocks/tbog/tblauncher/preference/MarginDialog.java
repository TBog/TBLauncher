package rocks.tbog.tblauncher.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.DialogPreference;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.ui.CustomizeMarginView;
import rocks.tbog.tblauncher.utils.UIColors;
import rocks.tbog.tblauncher.utils.UISizes;

public class MarginDialog extends BasePreferenceDialog {
    private static final String TAG = MarginDialog.class.getSimpleName();
    private final MutableLiveData<Float> offsetX = new MutableLiveData<>();
    private final MutableLiveData<Float> offsetY = new MutableLiveData<>();

    public static MarginDialog newInstance(String key) {
        MarginDialog fragment = new MarginDialog();
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

            editor.apply();
        }
    }

    @Override
    protected void onBindDialogView(View root) {
        super.onBindDialogView(root);

        DialogPreference preference = getPreference();
        final String keyX = preference.getKey();
        if (!keyX.endsWith("-dx"))
            throw new IllegalStateException("pref key `" + keyX + "` must end with `-dx`");
        final String keyY = keyX.replace("-dx", "-dy");

        SharedPreferences sharedPreferences = preference.getSharedPreferences();
        if (sharedPreferences == null) {
            Log.e(TAG, "getSharedPreferences == null for preference `" + keyX + "`");
            return;
        }
        var prefMap = sharedPreferences.getAll();

        CustomizeMarginView viewXY = root.findViewById(R.id.viewXY);
        TextView textValueXY = root.findViewById(R.id.textValueXY);

        // initialize LiveData
        {
            var value = prefMap.get(keyX);
            offsetX.setValue(value instanceof Float ? (float) value : 0f);
        }
        {
            var value = prefMap.get(keyY);
            offsetY.setValue(value instanceof Float ? (float) value : 0f);
        }

        final Context ctx = requireContext();

        {
            Float dx = offsetX.getValue();
            Float dy = offsetY.getValue();
            if (dx == null)
                dx = 0f;
            if (dy == null)
                dy = 0f;
            viewXY.setOffsetValues(UISizes.dp2px_float(ctx, dx), UISizes.dp2px_float(ctx, dy));
        }

        // initialize preview
        {
            int color1;
            int color2;
            switch (keyX) {
                case "result-list-margin-offset-dx":
                    color1 = UIColors.getResultListBackground(ctx);
                    color2 = UIColors.getResultListRipple(ctx);
                    final var margin = UISizes.getResultListMargin(ctx);
                    viewXY.setMarginParameters(margin.exactCenterX(), margin.exactCenterY());
                    break;
                default:
                    color1 = 0;
                    color2 = 0;
                    break;
            }
            if (color1 != 0 || color2 != 0) {
                color1 = UIColors.setAlpha(color1, 0xFF);
                color2 = UIColors.setAlpha(color2, 0xFF);
                viewXY.setPreviewColors(color1, color2);
            }
        }

        viewXY.setOnOffsetChanged((dx, dy) -> {
            offsetX.postValue(UISizes.px2dp_float(viewXY.getContext(), dx));
            offsetY.postValue(UISizes.px2dp_float(viewXY.getContext(), dy));
        });

        MediatorLiveData<LiveMarginParameters> dataMerge = new MediatorLiveData<>();
        dataMerge.addSource(offsetX, aFloat -> dataMerge.setValue(new LiveMarginParameters(aFloat, offsetY.getValue())));
        dataMerge.addSource(offsetY, aFloat -> dataMerge.setValue(new LiveMarginParameters(offsetX.getValue(), aFloat)));

        dataMerge.observe(getDialogLifecycleOwner(), marginParameters -> {
            final float dx = marginParameters.dx;
            final float dy = marginParameters.dy;
            textValueXY.setText(getResources().getString(R.string.value_float_xy, dx, dy));
        });
    }

    private static class LiveMarginParameters {
        Float dx;
        Float dy;

        public LiveMarginParameters(Float dx, Float dy) {
            this.dx = dx;
            this.dy = dy;
        }
    }
}
