package rocks.tbog.tblauncher.preference;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.preference.DialogPreference;

import net.mm2d.color.chooser.DialogView;

import rocks.tbog.tblauncher.utils.UIColors;

public class ChooseColorDialog extends BasePreferenceDialog {
    private DialogView mChooseView = null;

    public static ChooseColorDialog newInstance(String key) {
        final ChooseColorDialog fragment = new ChooseColorDialog();
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
        if (mChooseView != null) {
            preference.setValue(mChooseView.getColor());
        }

        preference.persistValueIfAllowed();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // the DialogPreference has no value set, get the one from SharedPreferences
        DialogPreference dialogPreference = getPreference();
        int color = dialogPreference.getSharedPreferences().getInt(dialogPreference.getKey(), UIColors.COLOR_DEFAULT);
        if (dialogPreference instanceof CustomDialogPreference) {
            CustomDialogPreference preference = (CustomDialogPreference) dialogPreference;
            preference.setValue(color);
        }
    }

    @Override
    protected View onCreateDialogView(Context context) {
        mChooseView = new DialogView(context);

        Object selectedColor = null;
        {
            DialogPreference dialogPreference = getPreference();
            if (dialogPreference instanceof CustomDialogPreference) {
                CustomDialogPreference preference = (CustomDialogPreference) dialogPreference;
                selectedColor = preference.getValue();
            }
        }
        if (!(selectedColor instanceof Integer)) {
            selectedColor = UIColors.COLOR_DEFAULT;
        }

        mChooseView.init((int) selectedColor, this);
        mChooseView.setWithAlpha(getPreference().getKey().endsWith("-argb"));
        mChooseView.addObserver(color -> {
            DialogPreference dialogPreference = getPreference();
            if (!(dialogPreference instanceof CustomDialogPreference))
                return;
            CustomDialogPreference preference = (CustomDialogPreference) dialogPreference;
            preference.setValue(color);
        }, this);

        return mChooseView;
    }
}
