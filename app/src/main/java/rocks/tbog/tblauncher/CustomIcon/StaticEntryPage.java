package rocks.tbog.tblauncher.CustomIcon;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.entry.StaticEntry;
import rocks.tbog.tblauncher.utils.DrawableUtils;

public class StaticEntryPage extends CustomShapePage {
    private final StaticEntry mStaticEntry;

    StaticEntryPage(CharSequence name, View view, StaticEntry staticEntry) {
        super(name, view);
        mStaticEntry = staticEntry;
        mScale = DrawableUtils.getScaleToFit(mShape);
    }

    @Override
    void setupView(@NonNull Context context, @Nullable OnItemClickListener iconClickListener, @Nullable OnItemClickListener iconLongClickListener) {
        super.setupView(context, iconClickListener, iconLongClickListener);

        // default icon
        {
            Drawable drawable = mStaticEntry.getDefaultDrawable(context);
            ShapedIconInfo iconInfo = new DefaultIconInfo(context.getString(R.string.default_static_icon, mStaticEntry.getName()), drawable);
            iconInfo.textId = R.string.default_icon;
            mShapedIconAdapter.addItem(iconInfo);
        }

        // this will call generateTextIcons
        mLettersView.setText(mStaticEntry.getName());
    }

    static class DefaultIconInfo extends CustomShapePage.DefaultIconInfo {
        final String name;

        DefaultIconInfo(@NonNull String name, Drawable icon) {
            super(icon);
            this.name = name;
        }

        @Nullable
        @Override
        CharSequence getText() {
            return name;
        }
    }
}
