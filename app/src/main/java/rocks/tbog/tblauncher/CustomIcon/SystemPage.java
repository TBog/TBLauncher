package rocks.tbog.tblauncher.CustomIcon;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.collection.ArraySet;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import rocks.tbog.tblauncher.IconsHandler;
import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.icons.IconPackXML;
import rocks.tbog.tblauncher.icons.SystemIconPack;
import rocks.tbog.tblauncher.utils.DrawableUtils;
import rocks.tbog.tblauncher.utils.UserHandleCompat;
import rocks.tbog.tblauncher.utils.ViewHolderAdapter;

public class SystemPage extends PageAdapter.Page {
    ComponentName componentName;
    UserHandleCompat userHandle;
    private IconPackXML mShownIconPack;

    SystemPage(CharSequence name, View view, ComponentName cn, UserHandleCompat uh) {
        super(name, view);
        componentName = cn;
        userHandle = uh;
        mShownIconPack = TBApplication.iconsHandler(view.getContext()).getCustomIconPack();
    }

    @Override
    void setupView(@NonNull Context context, @Nullable OnItemClickListener iconClickListener) {
        ProgressBar mIconLoadingBar = pageView.findViewById(R.id.iconLoadingBar);
        mIconLoadingBar.setVisibility(View.GONE);

        TextView textView = pageView.findViewById(android.R.id.text1);
        textView.setVisibility(View.GONE);

        TextView mSearch = pageView.findViewById(R.id.search);
        mSearch.setVisibility(View.GONE);

        GridView mGridView = pageView.findViewById(R.id.iconGrid);
        SystemPageAdapter adapter = new SystemPageAdapter(SystemPageViewHolder.class, iconClickListener);
        mGridView.setAdapter(adapter);

        ArraySet<Bitmap> dSet = new ArraySet<>(3);

        IconsHandler iconsHandler = TBApplication.getApplication(context).getIconsHandler();
        // add default icon
        {
            Drawable drawable = iconsHandler.getDrawableIconForPackage(componentName, userHandle);

            //checkDuplicateDrawable(dSet, drawable);

            SystemIconInfo iconInfo = new DefaultIconInfo();
            iconInfo.iconDrawable = drawable;
            iconInfo.textId = R.string.default_icon;
            adapter.addItem(iconInfo);
        }

        IconPackXML iconPack = mShownIconPack;
        SystemIconPack sysPack = iconsHandler.getSystemIconPack();

        // add getActivityIcon(componentName)
        {
            Drawable drawable = null;
            try {
                drawable = context.getPackageManager().getActivityIcon(componentName);
            } catch (PackageManager.NameNotFoundException ignored) {
            }
            if (drawable != null) {
                if (checkDuplicateDrawable(dSet, drawable)) {
                    addQuickOption(R.string.custom_icon_activity, drawable, adapter);
                    if (iconPack != null && iconPack.hasMask())
                        addQuickOption(R.string.custom_icon_activity_with_pack, iconPack.applyBackgroundAndMask(context, drawable, true), adapter);
                    addQuickOption(R.string.custom_icon_activity_adaptive, sysPack.applyBackgroundAndMask(context, drawable, true), adapter);
                    if (!DrawableUtils.isAdaptiveIconDrawable(drawable))
                        addQuickOption(R.string.custom_icon_activity_adaptive_fill, sysPack.applyBackgroundAndMask(context, drawable, false), adapter);
                }
            }
        }

        // add getApplicationIcon(packageName)
        {
            Drawable drawable = null;
            try {
                drawable = context.getPackageManager().getApplicationIcon(componentName.getPackageName());
            } catch (PackageManager.NameNotFoundException ignored) {
            }
            if (drawable != null) {
                if (checkDuplicateDrawable(dSet, drawable)) {
                    addQuickOption(R.string.custom_icon_application, drawable, adapter);
                    if (iconPack != null && iconPack.hasMask())
                        addQuickOption(R.string.custom_icon_application_with_pack, iconPack.applyBackgroundAndMask(context, drawable, true), adapter);
                    addQuickOption(R.string.custom_icon_application_adaptive, sysPack.applyBackgroundAndMask(context, drawable, true), adapter);
                    if (!DrawableUtils.isAdaptiveIconDrawable(drawable))
                        addQuickOption(R.string.custom_icon_application_adaptive_fill, sysPack.applyBackgroundAndMask(context, drawable, false), adapter);
                }
            }
        }

        // add Activity BadgedIcon
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            LauncherApps launcher = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
            assert launcher != null;
            List<LauncherActivityInfo> icons = launcher.getActivityList(componentName.getPackageName(), userHandle.getRealHandle());
            for (LauncherActivityInfo info : icons) {
                Drawable drawable = info.getBadgedIcon(0);

                if (drawable != null) {
                    if (checkDuplicateDrawable(dSet, drawable)) {
                        addQuickOption(R.string.custom_icon_badged, drawable, adapter);
                        if (iconPack != null && iconPack.hasMask())
                            addQuickOption(R.string.custom_icon_badged_with_pack, iconPack.applyBackgroundAndMask(context, drawable, true), adapter);
                        addQuickOption(R.string.custom_icon_badged_adaptive, sysPack.applyBackgroundAndMask(context, drawable, true), adapter);
                        if (!DrawableUtils.isAdaptiveIconDrawable(drawable))
                            addQuickOption(R.string.custom_icon_badged_adaptive_fill, sysPack.applyBackgroundAndMask(context, drawable, false), adapter);
                    }
                }
            }
        }
    }

    private boolean checkDuplicateDrawable(ArraySet<Bitmap> set, Drawable drawable) {
        Bitmap b = null;
        if (drawable instanceof BitmapDrawable)
            b = ((BitmapDrawable) drawable).getBitmap();

        if (set.contains(b))
            return false;

        set.add(b);
        return true;
    }

    private void addQuickOption(@StringRes int textId, Drawable drawable, SystemPageAdapter adapter) {
        if (!(drawable instanceof BitmapDrawable))
            return;

//        ViewGroup layout = (ViewGroup) LayoutInflater.from(adapter.getContext()).inflate(R.layout.custom_icon_quick, adapter, false);
//        ImageView icon = layout.findViewById(android.R.id.icon);
//        TextView text = layout.findViewById(android.R.id.text1);
//
//        icon.setImageDrawable(drawable);
//        icon.setOnClickListener(v -> {
//            mSelectedDrawable = ((ImageView) v).getDrawable();
//            mPreview.setImageDrawable(mSelectedDrawable);
//        });
//
//        text.setText(textId);
//
//        adapter.addView(layout);

        SystemIconInfo iconInfo = new SystemIconInfo();
        iconInfo.iconDrawable = drawable;
        iconInfo.textId = textId;
        adapter.addItem(iconInfo);
    }

    static class DefaultIconInfo extends SystemIconInfo {
        @Override
        Drawable getIcon() {
            return null;
        }
    }

    static class SystemIconInfo {
        Drawable iconDrawable;
        @StringRes
        int textId;

        Drawable getIcon() {
            return iconDrawable;
        }

        public Drawable getPreview() {
            return iconDrawable;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SystemIconInfo that = (SystemIconInfo) o;
            return Objects.equals(iconDrawable, that.iconDrawable) &&
                    Objects.equals(textId, that.textId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(iconDrawable, textId);
        }
    }

    public static class SystemPageViewHolder extends ViewHolderAdapter.ViewHolder<SystemIconInfo> {
        View root;
        ImageView icon;
        TextView text1;

        public SystemPageViewHolder(View view) {
            super(view);
            root = view;
            icon = view.findViewById(android.R.id.icon);
            text1 = view.findViewById(android.R.id.text1);
        }

        @Override
        protected void setContent(SystemIconInfo content, int position, @NonNull ViewHolderAdapter<SystemIconInfo, ? extends ViewHolderAdapter.ViewHolder<SystemIconInfo>> adapter) {
            SystemPageAdapter systemPageAdapter = (SystemPageAdapter) adapter;
            icon.setImageDrawable(content.iconDrawable);
            text1.setText(content.textId);
            if (systemPageAdapter.mIconClickListener != null)
                root.setOnClickListener(v -> systemPageAdapter.mIconClickListener.onItemClick(adapter, v, position));
        }
    }

    static class SystemPageAdapter extends ViewHolderAdapter<SystemIconInfo, SystemPageViewHolder> {
        ArrayList<SystemIconInfo> mList = new ArrayList<>();
        @Nullable
        OnItemClickListener mIconClickListener;

        protected SystemPageAdapter(@NonNull Class<SystemPageViewHolder> viewHolderClass, @Nullable OnItemClickListener iconClickListener) {
            super(viewHolderClass, R.layout.item_grid);
            //super(viewHolderClass, R.layout.custom_icon_quick);
            mIconClickListener = iconClickListener;
        }

        void addItem(SystemIconInfo item) {
            mList.add(item);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mList.size();
        }

        @Override
        public SystemIconInfo getItem(int position) {
            return mList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).hashCode();
        }
    }
}
