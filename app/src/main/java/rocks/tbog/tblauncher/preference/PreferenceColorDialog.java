package rocks.tbog.tblauncher.preference;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.transition.Fade;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.asynclayoutinflater.view.AsyncLayoutInflater;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.preference.DialogPreference;

import net.mm2d.color.chooser.ColorChooserDialog;
import net.mm2d.color.chooser.ColorChooserView;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.databinding.Mm2dCcColorChooserBinding;
import rocks.tbog.tblauncher.utils.Timer;
import rocks.tbog.tblauncher.utils.UIColors;

public class PreferenceColorDialog extends BasePreferenceDialog {
    private static final String TAG = PreferenceColorDialog.class.getSimpleName();
    private ColorChooserView mChooseView = null;
    private int mInitialTab;
    private int mInitialColor;

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

        if (savedInstanceState != null) {
            mInitialTab = savedInstanceState.getInt(ColorChooserDialog.KEY_INITIAL_TAB, ColorChooserDialog.TAB_PALETTE);
            mInitialColor = savedInstanceState.getInt(ColorChooserDialog.KEY_INITIAL_COLOR, color);
        } else {
            mInitialTab = ColorChooserDialog.TAB_PALETTE;
            mInitialColor = color;
        }
    }

    @Override
    protected View onCreateDialogView(@NonNull Context context) {
        Log.d(TAG, "onCreateDialogView start");
        var t = Timer.startNano();

        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(com.google.android.material.R.attr.alertDialogTheme, outValue, true);
        var themeWrapper = new ContextThemeWrapper(context, outValue.resourceId);

        var inflater = LayoutInflater.from(themeWrapper);
        @SuppressLint("InflateParams")
        View view = inflater.inflate(R.layout.dialog_preference_color_chooser, null, false);

        final ConstraintLayout rootLayout = view instanceof ConstraintLayout ? (ConstraintLayout) view : null;
        if (rootLayout == null)
            return view;

        new AsyncLayoutInflater(themeWrapper).inflate(R.layout.mm2d_cc_color_chooser, rootLayout, (v, resid, parent) -> {
            mChooseView = Mm2dCcColorChooserBinding.bind(v).getRoot();
            mChooseView.setId(View.generateViewId());
            mChooseView.setVisibility(View.GONE);
            rootLayout.addView(mChooseView);
            t.stop();
            Log.d(TAG, "onCreateDialogView finished " + t);

            // initialize color-chooser
            mChooseView.setCurrentItem(mInitialTab);
            mChooseView.init(mInitialColor);
            mChooseView.setWithAlpha(getPreference().getKey().endsWith("-argb"));

            // set new constraints to hide loading and place the button bar below the color-chooser
            var constraintSet = new ConstraintSet();
            constraintSet.clone(rootLayout);
            constraintSet.setVisibility(R.id.iconLoadingBar, ConstraintSet.GONE);
            constraintSet.setVisibility(mChooseView.getId(), ConstraintSet.VISIBLE);
            constraintSet.constrainHeight(mChooseView.getId(), 0);
            constraintSet.constrainDefaultHeight(mChooseView.getId(), ConstraintSet.MATCH_CONSTRAINT_WRAP);
            constraintSet.connect(mChooseView.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
            constraintSet.connect(mChooseView.getId(), ConstraintSet.BOTTOM, R.id.buttonPanel, ConstraintSet.TOP);
            constraintSet.connect(R.id.buttonPanel, ConstraintSet.TOP, mChooseView.getId(), ConstraintSet.BOTTOM);
            constraintSet.constrainDefaultHeight(mChooseView.getId(), ConstraintSet.MATCH_CONSTRAINT_WRAP);
            constraintSet.constrainHeight(mChooseView.getId(), ConstraintSet.MATCH_CONSTRAINT);

            // set transition without the button bar
            var transition = new TransitionSet();
            transition.setOrdering(TransitionSet.ORDERING_TOGETHER);
            transition.addTransition(new Fade(Fade.OUT));
            transition.addTransition(new Fade(Fade.IN));
            transition.setInterpolator(new AccelerateInterpolator());
            transition.excludeTarget(R.id.buttonPanel, true);

            // start the transition
            TransitionManager.beginDelayedTransition(parent, transition);
            constraintSet.applyTo(rootLayout);
        });

        return rootLayout;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ColorChooserDialog.KEY_INITIAL_TAB, mInitialTab = mChooseView.getCurrentItem());
        outState.putInt(ColorChooserDialog.KEY_INITIAL_COLOR, mInitialColor = mChooseView.getColor());
    }
}
