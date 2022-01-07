package rocks.tbog.tblauncher.customicon;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.drawable.DrawableUtils;

public class SearchEntryPage extends CustomShapePage {
    final String mEntryName;

    SearchEntryPage(CharSequence name, View view, String entryName) {
        super(name, view);
        mEntryName = entryName;
        mScale = DrawableUtils.getScaleToFit(mShape);
    }

    @Override
    void setupView(@NonNull Context context, @Nullable OnItemClickListener iconClickListener, @Nullable OnItemClickListener iconLongClickListener) {
        super.setupView(context, iconClickListener, iconLongClickListener);

        final Drawable originalDrawable;
        // default icon
        {
            Drawable drawable = ContextCompat.getDrawable(context, R.drawable.ic_search);
            originalDrawable = drawable;
            ShapedIconInfo iconInfo = new DefaultIconInfo(context.getString(R.string.default_static_icon, mEntryName), drawable);
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
