package rocks.tbog.tblauncher.customicon;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.drawable.DrawableUtils;

public class ButtonPage extends CustomShapePage {
    final private int mDefaultIcon;

    ButtonPage(CharSequence name, View view, int defaultIcon) {
        super(name, view);
        mDefaultIcon = defaultIcon;
        mScale = DrawableUtils.getScaleToFit(mShape);
    }

    @Override
    void setupView(@NonNull DialogFragment dialogFragment, @Nullable OnItemClickListener iconClickListener, @Nullable OnItemClickListener iconLongClickListener) {
        Context context = dialogFragment.requireContext();
        super.setupView(dialogFragment, iconClickListener, iconLongClickListener);

        final Drawable originalDrawable;
        // default icon
        {
            Drawable drawable = ContextCompat.getDrawable(context, mDefaultIcon);
            originalDrawable = drawable;
            ShapedIconInfo iconInfo = new DefaultIconInfo(dialogFragment.getString(R.string.default_icon), drawable);
            mShapedIconAdapter.addItem(iconInfo);
        }

        // customizable default icon
        {
            Drawable shapedDrawable = DrawableUtils.applyIconMaskShape(context, originalDrawable, mShape, mScale, mBackground);
            ShapedIconInfo iconInfo = new NamedIconInfo("", shapedDrawable, originalDrawable);
            mShapedIconAdapter.addItem(iconInfo);
        }

        // this will call generateTextIcons
        mLettersView.setText("");
    }

    static class DefaultIconInfo extends CustomShapePage.DefaultIconInfo {
        final String name;

        DefaultIconInfo(@NonNull String name, Drawable icon) {
            super(icon);
            this.name = name;
            textId = R.string.default_icon;
        }

        @Nullable
        @Override
        CharSequence getText() {
            return name;
        }
    }
}
