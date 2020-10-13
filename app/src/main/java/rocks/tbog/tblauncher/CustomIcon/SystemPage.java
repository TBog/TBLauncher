package rocks.tbog.tblauncher.CustomIcon;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Pair;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import rocks.tbog.tblauncher.IconsHandler;
import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.icons.IconPackXML;
import rocks.tbog.tblauncher.icons.SystemIconPack;
import rocks.tbog.tblauncher.ui.CodePointDrawable;
import rocks.tbog.tblauncher.ui.FourCodePointDrawable;
import rocks.tbog.tblauncher.ui.TwoCodePointDrawable;
import rocks.tbog.tblauncher.utils.DrawableUtils;
import rocks.tbog.tblauncher.utils.UserHandleCompat;
import rocks.tbog.tblauncher.utils.Utilities;
import rocks.tbog.tblauncher.utils.ViewHolderAdapter;

public class SystemPage extends PageAdapter.Page {
    private ComponentName componentName;
    private UserHandleCompat userHandle;
    private IconPackXML mShownIconPack;
    private GridView mGridView;

    SystemPage(CharSequence name, View view, ComponentName cn, UserHandleCompat uh) {
        super(name, view);
        componentName = cn;
        userHandle = uh;
        mShownIconPack = TBApplication.iconsHandler(view.getContext()).getCustomIconPack();
    }

    @Override
    void setupView(@NonNull Context context, @Nullable OnItemClickListener iconClickListener) {
        mGridView = pageView.findViewById(R.id.iconGrid);
        SystemPageAdapter adapter = new SystemPageAdapter(SystemPageViewHolder.class, iconClickListener);
        mGridView.setAdapter(adapter);

        ProgressBar mIconLoadingBar = pageView.findViewById(R.id.iconLoadingBar);
        mIconLoadingBar.setVisibility(View.GONE);

        TextView textView = pageView.findViewById(android.R.id.text1);
        textView.setVisibility(View.GONE);

        TextView mSearch = pageView.findViewById(R.id.search);
        mSearch.setHint(R.string.static_icon_letters_label);
        mSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                generateTextIcons(s);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        ArraySet<Bitmap> dSet = new ArraySet<>(3);

        IconsHandler iconsHandler = TBApplication.getApplication(context).getIconsHandler();
        // add default icon
        {
            Drawable drawable = iconsHandler.getDrawableIconForPackage(componentName, userHandle);

            //checkDuplicateDrawable(dSet, drawable);

            SystemIconInfo iconInfo = new DefaultIconInfo(drawable);
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
                    for (int shape : DrawableUtils.SHAPE_LIST) {
                        Drawable shapedDrawable = DrawableUtils.applyIconMaskShape(context, drawable, shape, true);
                        String name = context.getString(R.string.custom_icon_activity_shaped, DrawableUtils.shapeName(context, shape));
                        addIconPackOption(name, shapedDrawable, adapter);
                    }
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
                    for (int shape : DrawableUtils.SHAPE_LIST) {
                        Drawable shapedDrawable = DrawableUtils.applyIconMaskShape(context, drawable, shape, true);
                        String name = context.getString(R.string.custom_icon_application_shaped, DrawableUtils.shapeName(context, shape));
                        addIconPackOption(name, shapedDrawable, adapter);
                    }
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

        // this will call generateTextIcons
        //mSearch.setText(pageName);
    }

    private void generateTextIcons(CharSequence text) {
        Context ctx = mGridView.getContext();
        SystemPageAdapter adapter = (SystemPageAdapter) mGridView.getAdapter();

        // remove all TextDrawable icons
        for (Iterator<SystemIconInfo> iterator = adapter.mList.iterator(); iterator.hasNext(); ) {
            SystemIconInfo info = iterator.next();
            if (info instanceof TextIconInfo)
                iterator.remove();
        }
        adapter.notifyDataSetChanged();

        int length = Utilities.codePointsLength(text);
        for (int shape : DrawableUtils.SHAPE_LIST) {
            if (length >= 1) {
                Drawable icon = new CodePointDrawable(text);
                Drawable shapedIcon = DrawableUtils.applyIconMaskShape(ctx, icon, shape, true);
                adapter.addItem(new TextIconInfo(shapedIcon));
            }
            if (length >= 2) {
                Drawable icon = TwoCodePointDrawable.fromText(text, false);
                Drawable shapedIcon = DrawableUtils.applyIconMaskShape(ctx, icon, shape, true);
                adapter.addItem(new TextIconInfo(shapedIcon));
            }
            if (length >= 2) {
                Drawable icon = TwoCodePointDrawable.fromText(text, true);
                Drawable shapedIcon = DrawableUtils.applyIconMaskShape(ctx, icon, shape, true);
                adapter.addItem(new TextIconInfo(shapedIcon));
            }
            if (length >= 3) {
                Drawable icon = FourCodePointDrawable.fromText(text, true);
                Drawable shapedIcon = DrawableUtils.applyIconMaskShape(ctx, icon, shape, true);
                adapter.addItem(new TextIconInfo(shapedIcon));
            }
            if (length >= 3) {
                Drawable icon = FourCodePointDrawable.fromText(text, false);
                Drawable shapedIcon = DrawableUtils.applyIconMaskShape(ctx, icon, shape, true);
                adapter.addItem(new TextIconInfo(shapedIcon));
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

    private static void addQuickOption(@StringRes int textId, Drawable drawable, SystemPageAdapter adapter) {
        if (!(drawable instanceof BitmapDrawable))
            return;

        SystemIconInfo iconInfo = new SystemIconInfo(drawable);
        iconInfo.textId = textId;
        adapter.addItem(iconInfo);
    }

    private static void addIconPackOption(String packName, Drawable drawable, SystemPageAdapter adapter) {
        if (!(drawable instanceof BitmapDrawable))
            return;

        IconPackIconInfo iconInfo = new IconPackIconInfo(packName, drawable);
        adapter.addItem(iconInfo);
    }

    public void loadIconPackIcons(List<Pair<String, String>> iconPacks) {
        if (iconPacks.isEmpty())
            return;

        final SystemIconInfo placeholderItem = new SystemIconInfo(null);
        placeholderItem.textId = R.string.icon_pack_loading;
        {
            SystemPageAdapter adapter = (SystemPageAdapter) mGridView.getAdapter();
            adapter.addItem(placeholderItem);
        }
        final HashMap<String, Drawable> options = new HashMap<>(iconPacks.size());
        Utilities.runAsync(() -> {
            for (Pair<String, String> packInfo : iconPacks) {
                String packPackageName = packInfo.first;
                String packName = packInfo.second;
                Activity activity = Utilities.getActivity(mGridView);
                if (activity != null) {
                    IconPackXML pack = TBApplication.iconPackCache(activity).getIconPack(packPackageName);
                    pack.load(activity.getPackageManager());
                    Drawable drawable = pack.getComponentDrawable(activity, componentName, userHandle);
                    options.put(packName, drawable);
                } else {
                    break;
                }
            }
        }, () -> {
            Activity activity = Utilities.getActivity(mGridView);
            if (activity != null) {
                SystemPageAdapter adapter = (SystemPageAdapter) mGridView.getAdapter();
                adapter.removeItem(placeholderItem);
                for (Map.Entry<String, Drawable> entry : options.entrySet())
                    addIconPackOption(entry.getKey(), entry.getValue(), adapter);
            }
        });
    }

    static class TextIconInfo extends IconPackIconInfo {

        TextIconInfo(Drawable icon) {
            super("", icon);
        }
    }

    static class DefaultIconInfo extends SystemIconInfo {

        DefaultIconInfo(Drawable icon) {
            super(icon);
        }

        @Override
        Drawable getIcon() {
            return null;
        }
    }

    static class IconPackIconInfo extends SystemIconInfo {
        final CharSequence name;

        IconPackIconInfo(CharSequence name, Drawable icon) {
            super(icon);
            this.name = name;
        }
    }

    static class SystemIconInfo {
        final Drawable iconDrawable;
        @StringRes
        int textId;

        SystemIconInfo(Drawable icon) {
            iconDrawable = icon;
        }

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
            // set icon
            icon.setImageDrawable(content.getPreview());
            icon.setVisibility(content.getPreview() == null ? View.GONE : View.VISIBLE);

            //set text
            if (content instanceof IconPackIconInfo)
                text1.setText(((IconPackIconInfo) content).name);
            else
                text1.setText(content.textId);

            // setOnClickListener when we have an icon
            if (systemPageAdapter.mIconClickListener != null && content.getPreview() != null)
                root.setOnClickListener(v -> systemPageAdapter.mIconClickListener.onItemClick(adapter, v, position));
        }
    }

    static class SystemPageAdapter extends ViewHolderAdapter<SystemIconInfo, SystemPageViewHolder> {
        ArrayList<SystemIconInfo> mList = new ArrayList<>();
        @Nullable
        private OnItemClickListener mIconClickListener;

        protected SystemPageAdapter(@NonNull Class<SystemPageViewHolder> viewHolderClass, @Nullable OnItemClickListener iconClickListener) {
            super(viewHolderClass, R.layout.item_grid);
            mIconClickListener = iconClickListener;
        }

        void addItem(SystemIconInfo item) {
            mList.add(item);
            notifyDataSetChanged();
        }

        void removeItem(SystemIconInfo item) {
            mList.remove(item);
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
