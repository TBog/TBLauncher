package rocks.tbog.tblauncher.customicon;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.drawable.DrawableUtils;
import rocks.tbog.tblauncher.handler.IconsHandler;

public class DefaultButtonPage extends CustomShapePage {
    final private int mDefaultIcon;
    @StringRes
    final private int mDefaultName;
    final private String mEntryName;

    DefaultButtonPage(CharSequence name, View view, String entryName, int defaultIcon, @StringRes int defaultName) {
        super(name, view);
        mEntryName = entryName;
        mDefaultIcon = defaultIcon;
        mDefaultName = defaultName;
        mScale = DrawableUtils.getScaleToFit(mShape);
    }

    @Override
    void setupView(@NonNull DialogFragment dialogFragment, @Nullable OnItemClickListener iconClickListener, @Nullable OnItemClickListener iconLongClickListener) {
        Context context = dialogFragment.requireContext();
        super.setupView(dialogFragment, iconClickListener, iconLongClickListener);

        final Drawable originalDrawable;
        // default icon
        {
            originalDrawable = ContextCompat.getDrawable(context, mDefaultIcon);
            IconsHandler.IconInfo iconHandlerIconInfo = new IconsHandler.IconInfo().setNonAdaptiveIcon(originalDrawable);
            ShapedIconInfo iconInfo = new DefaultIconInfo(dialogFragment.getString(mDefaultName), iconHandlerIconInfo);
            mShapedIconAdapter.addItem(iconInfo);
        }

        // customizable default icon
        {
            Drawable shapedDrawable = DrawableUtils.applyIconMaskShape(context, originalDrawable, mShape, mScale, mBackground);
            ShapedIconInfo iconInfo = new NamedIconInfo(mEntryName, shapedDrawable, originalDrawable);
            mShapedIconAdapter.addItem(iconInfo);
        }

        // this will call generateTextIcons
        mLettersView.setText(mEntryName);
    }

    static class DefaultIconInfo extends CustomShapePage.DefaultIconInfo {
        final String name;

        DefaultIconInfo(@NonNull String name, IconsHandler.IconInfo icon) {
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
