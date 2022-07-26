package rocks.tbog.tblauncher.preference;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.DialogPreference;

import net.mm2d.color.chooser.ColorChooserDialog;
import net.mm2d.color.chooser.ColorChooserView;

import rocks.tbog.tblauncher.databinding.Mm2dCcColorChooserBinding;
import rocks.tbog.tblauncher.utils.UIColors;

public class PreferenceColorDialog extends BasePreferenceDialog {
    private ColorChooserView mChooseView = null;

    public static PreferenceColorDialog newInstance(String key) {
        final PreferenceColorDialog fragment = new PreferenceColorDialog();
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
    protected View onCreateDialogView(@NonNull Context context) {
        mChooseView = Mm2dCcColorChooserBinding.inflate(LayoutInflater.from(context)).getRoot();
        return mChooseView;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ColorChooserDialog.KEY_INITIAL_TAB, mChooseView.getCurrentItem());
        outState.putInt(ColorChooserDialog.KEY_INITIAL_COLOR, mChooseView.getColor());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // we expect super.onCreateDialog to call this.onCreateDialogView and initialize mChooseView
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        if (mChooseView == null)
            return dialog;

        if (savedInstanceState != null) {
            var tab = savedInstanceState.getInt(ColorChooserDialog.KEY_INITIAL_TAB, 0);
            mChooseView.setCurrentItem(tab);
            var color = savedInstanceState.getInt(ColorChooserDialog.KEY_INITIAL_COLOR, 0);
            mChooseView.init(color);
        } else {
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

            mChooseView.setCurrentItem(ColorChooserDialog.TAB_PALETTE);
            mChooseView.init((int) selectedColor);
        }
        mChooseView.setWithAlpha(getPreference().getKey().endsWith("-argb"));
        return dialog;
    }
}
