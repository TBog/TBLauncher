package rocks.tbog.tblauncher;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.collection.ArraySet;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import rocks.tbog.tblauncher.dataprovider.FavProvider;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.entry.StaticEntry;
import rocks.tbog.tblauncher.icons.IconPack;
import rocks.tbog.tblauncher.icons.IconPackXML;
import rocks.tbog.tblauncher.icons.SystemIconPack;
import rocks.tbog.tblauncher.normalizer.StringNormalizer;
import rocks.tbog.tblauncher.result.ResultViewHelper;
import rocks.tbog.tblauncher.ui.DialogFragment;
import rocks.tbog.tblauncher.utils.DrawableUtils;
import rocks.tbog.tblauncher.utils.FuzzyScore;
import rocks.tbog.tblauncher.utils.UserHandleCompat;
import rocks.tbog.tblauncher.utils.Utilities;

public class CustomIconDialog extends DialogFragment<Drawable> {
    private final List<IconData> mIconData = new ArrayList<>();
    private Drawable mSelectedDrawable = null;
    private ProgressBar mIconLoadingBar;
    private GridView mIconGrid;
    private TextView mSearch;
    private ImageView mPreview;
    private LinearLayout mIconPackList;
    private IconPackXML mShownIconPack;
    private Utilities.AsyncRun mLoadIconPackTask = null;

    @Override
    protected int layoutRes() {
        return R.layout.custom_icon_dialog;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            view.setClipToOutline(true);
        }

        mIconLoadingBar = view.findViewById(R.id.iconLoadingBar);

        mIconPackList = view.findViewById(R.id.iconPackList);
        populateIconPackList();

        mIconGrid = view.findViewById(R.id.iconGrid);
        IconAdapter iconAdapter = new IconAdapter(mIconData);
        mIconGrid.setAdapter(iconAdapter);

        iconAdapter.setOnItemClickListener((adapter, v, position) -> {
            mSelectedDrawable = adapter.getItem(position).getIcon();
            mPreview.setImageDrawable(mSelectedDrawable);
        });
        iconAdapter.setOnItemLongClickListener(((adapter, v, position) -> {
            String name = adapter.getItem(position).drawableInfo.getDrawableName();
            displayToast(v, name);
        }));

        mSearch = view.findViewById(R.id.search);
        mSearch.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                // Auto left-trim text.
                if (s.length() > 0 && s.charAt(0) == ' ')
                    s.delete(0, 1);
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mSearch.post(() -> refreshList());
            }
        });
        mSearch.requestFocus();

        mPreview = view.findViewById(R.id.preview);

        Bundle args = getArguments() != null ? getArguments() : new Bundle();
        if (args.containsKey("componentName"))
            customIconApp(args);
        else if (args.containsKey("entryId"))
            customIconStaticEntry(args);

        // OK button
        {
            View button = view.findViewById(android.R.id.button1);
            button.setOnClickListener(v -> {
                onConfirm(mSelectedDrawable);
                dismiss();
            });
        }

        // CANCEL button
        {
            View button = view.findViewById(android.R.id.button2);
            button.setOnClickListener(v -> dismiss());
        }
    }

    private void customIconApp(Bundle args) {
        Context context = requireContext();

        String name = args.getString("componentName", "");
        long customIcon = args.getLong("customIcon", 0);
        if (name.isEmpty()) {
            dismiss();
            return;
        }

        IconsHandler iconsHandler = TBApplication.getApplication(context).getIconsHandler();
        ComponentName cn = UserHandleCompat.unflattenComponentName(name);
        UserHandleCompat userHandle = UserHandleCompat.fromComponentName(context, name);

        // Preview
        Utilities.setIconAsync(mPreview, ctx -> {
            Drawable drawable = customIcon != 0 ? iconsHandler.getCustomIcon(name, customIcon) : null;
            if (drawable == null)
                drawable = iconsHandler.getDrawableIconForPackage(cn, userHandle);
            return drawable;
        });
    }

    private void customIconStaticEntry(Bundle args) {
        Context context = requireContext();

        String entryId = args.getString("entryId", "");
        EntryItem entryItem = TBApplication.dataHandler(context).getPojo(entryId);
        if (!(entryItem instanceof StaticEntry)) {
            dismiss();
            return;
        }
        StaticEntry staticEntry = (StaticEntry) entryItem;

        // Preview
        Utilities.setIconAsync(mPreview, staticEntry::getIconDrawable);
    }

    private void displayToast(View v, CharSequence message) {
        Window window = requireDialog().getWindow();
        if (window == null || v == null)
            return;
        Toast toast = Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT);
        Utilities.positionToast(toast, v, window, 0, 0);
        toast.show();
    }

    private void populateIconPackList() {
        mIconPackList.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(getContext());
        Map<String, String> iconPacks = TBApplication.iconsHandler(getContext()).getIconPackNames();
        for (Map.Entry<String, String> packInfo : iconPacks.entrySet()) {
            String packageName = packInfo.getKey();
            String packName = packInfo.getValue();
            View packView = inflater.inflate(R.layout.item_quick_list, mIconPackList, false);
            mIconPackList.addView(packView);

            ImageView icon = packView.findViewById(android.R.id.icon);
            TextView name = packView.findViewById(android.R.id.text1);

            name.setText(packName);

            Utilities.setIconAsync(icon, (ctx) -> {
                Drawable drawable = null;
                try {
                    drawable = ctx.getPackageManager().getApplicationIcon(packageName);
                } catch (PackageManager.NameNotFoundException ignored) {
                }
                return drawable;
            });

            packView.setTag(packageName);
            packView.setOnClickListener((v) -> {
                String tag = v.getTag().toString();
                setShownIconPack(tag);
            });
        }

        if (mIconPackList.getChildCount() == 0)
            mIconPackList.setVisibility(View.GONE);
        else
            mIconPackList.requestLayout();
    }

    private void refreshQuickList() {
        Context context = requireContext();
        View view = getView();
        if (view == null)
            return;

        Bundle args = getArguments() != null ? getArguments() : new Bundle();
        if (args.containsKey("componentName")) {
            String name = args.getString("componentName", "");

            IconsHandler iconsHandler = TBApplication.getApplication(context).getIconsHandler();
            ComponentName cn = UserHandleCompat.unflattenComponentName(name);
            UserHandleCompat userHandle = UserHandleCompat.fromComponentName(context, name);

            //TODO: move this in an async task
            setQuickList(iconsHandler, view, cn, userHandle);
        } else if (args.containsKey("entryId")) {
            String entryId = args.getString("entryId", "");
            EntryItem entryItem = TBApplication.dataHandler(context).getPojo(entryId);
            if (!(entryItem instanceof StaticEntry)) {
                dismiss();
                return;
            }
            StaticEntry staticEntry = (StaticEntry) entryItem;
            setQuickList(view, staticEntry);
        }
    }

    private void setQuickList(View view, StaticEntry staticEntry) {
        Context context = view.getContext();
        ViewGroup quickList = view.findViewById(R.id.quickList);
        quickList.removeViews(1, quickList.getChildCount() - 1);

        // add default icon
        {
            Drawable drawable = staticEntry.getDefaultDrawable(context);

            ImageView icon = quickList.findViewById(android.R.id.icon);
            icon.setImageDrawable(drawable);
            icon.setOnClickListener(v -> {
                mSelectedDrawable = null;
                mPreview.setImageDrawable(((ImageView) v).getDrawable());
            });
            ((TextView) quickList.findViewById(android.R.id.text1)).setText(R.string.default_icon);
        }
    }

    private void setQuickList(IconsHandler iconsHandler, View view, ComponentName cn, UserHandleCompat userHandle) {
        Context context = view.getContext();
        ViewGroup quickList = view.findViewById(R.id.quickList);
        quickList.removeViews(1, quickList.getChildCount() - 1);

        ArraySet<Bitmap> dSet = new ArraySet<>(3);

        // add default icon
        {
            Drawable drawable = iconsHandler.getDrawableIconForPackage(cn, userHandle);

            //checkDuplicateDrawable(dSet, drawable);

            ImageView icon = quickList.findViewById(android.R.id.icon);
            icon.setImageDrawable(drawable);
            icon.setOnClickListener(v -> {
                mSelectedDrawable = null;
                mPreview.setImageDrawable(((ImageView) v).getDrawable());
            });
            ((TextView) quickList.findViewById(android.R.id.text1)).setText(R.string.default_icon);
        }

        IconPackXML iconPack = mShownIconPack;
        SystemIconPack sysPack = iconsHandler.getSystemIconPack();

        // add getActivityIcon(componentName)
        {
            Drawable drawable = null;
            try {
                drawable = context.getPackageManager().getActivityIcon(cn);
            } catch (PackageManager.NameNotFoundException ignored) {
            }
            if (drawable != null) {
                if (checkDuplicateDrawable(dSet, drawable)) {
                    addQuickOption(R.string.custom_icon_activity, drawable, quickList);
                    if (iconPack != null && iconPack.hasMask())
                        addQuickOption(R.string.custom_icon_activity_with_pack, iconPack.applyBackgroundAndMask(context, drawable, true), quickList);
                    addQuickOption(R.string.custom_icon_activity_adaptive, sysPack.applyBackgroundAndMask(context, drawable, true), quickList);
                    if (!DrawableUtils.isAdaptiveIconDrawable(drawable))
                        addQuickOption(R.string.custom_icon_activity_adaptive_fill, sysPack.applyBackgroundAndMask(context, drawable, false), quickList);
                }
            }
        }

        // add getApplicationIcon(packageName)
        {
            Drawable drawable = null;
            try {
                drawable = context.getPackageManager().getApplicationIcon(cn.getPackageName());
            } catch (PackageManager.NameNotFoundException ignored) {
            }
            if (drawable != null) {
                if (checkDuplicateDrawable(dSet, drawable)) {
                    addQuickOption(R.string.custom_icon_application, drawable, quickList);
                    if (iconPack != null && iconPack.hasMask())
                        addQuickOption(R.string.custom_icon_application_with_pack, iconPack.applyBackgroundAndMask(context, drawable, true), quickList);
                    addQuickOption(R.string.custom_icon_application_adaptive, sysPack.applyBackgroundAndMask(context, drawable, true), quickList);
                    if (!DrawableUtils.isAdaptiveIconDrawable(drawable))
                        addQuickOption(R.string.custom_icon_application_adaptive_fill, sysPack.applyBackgroundAndMask(context, drawable, false), quickList);
                }
            }
        }

        // add Activity BadgedIcon
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            LauncherApps launcher = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
            assert launcher != null;
            List<LauncherActivityInfo> icons = launcher.getActivityList(cn.getPackageName(), userHandle.getRealHandle());
            for (LauncherActivityInfo info : icons) {
                Drawable drawable = info.getBadgedIcon(0);

                if (drawable != null) {
                    if (checkDuplicateDrawable(dSet, drawable)) {
                        addQuickOption(R.string.custom_icon_badged, drawable, quickList);
                        if (iconPack != null && iconPack.hasMask())
                            addQuickOption(R.string.custom_icon_badged_with_pack, iconPack.applyBackgroundAndMask(context, drawable, true), quickList);
                        addQuickOption(R.string.custom_icon_badged_adaptive, sysPack.applyBackgroundAndMask(context, drawable, true), quickList);
                        if (!DrawableUtils.isAdaptiveIconDrawable(drawable))
                            addQuickOption(R.string.custom_icon_badged_adaptive_fill, sysPack.applyBackgroundAndMask(context, drawable, false), quickList);
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


    private void addQuickOption(@StringRes int textId, Drawable drawable, ViewGroup parent) {
        if (!(drawable instanceof BitmapDrawable))
            return;

        ViewGroup layout = (ViewGroup) LayoutInflater.from(parent.getContext()).inflate(R.layout.custom_icon_quick, parent, false);
        ImageView icon = layout.findViewById(android.R.id.icon);
        TextView text = layout.findViewById(android.R.id.text1);

        icon.setImageDrawable(drawable);
        icon.setOnClickListener(v -> {
            mSelectedDrawable = ((ImageView) v).getDrawable();
            mPreview.setImageDrawable(mSelectedDrawable);
        });

        text.setText(textId);

        parent.addView(layout);
    }

    private void setShownIconPack(String packageName) {
        if (packageName == null) {
            mShownIconPack = null;
            refreshQuickList();
            refreshList();
        } else {
            if (mShownIconPack != null && packageName.equals(mShownIconPack.getPackPackageName()))
                return;
            if (mLoadIconPackTask != null)
                mLoadIconPackTask.cancel(true);

            // inform the user we are loading data
            {
                mShownIconPack = null;
                refreshList();
                mIconLoadingBar.setVisibility(View.VISIBLE);
            }

            // load the new pack
            mLoadIconPackTask = Utilities.runAsync(() -> {
                IconPackXML pack = new IconPackXML(packageName);
                pack.loadDrawables(requireContext().getPackageManager());
                mShownIconPack = pack;
            }, () -> {
                mLoadIconPackTask = null;
                refreshQuickList();
                refreshList();
            });
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        IconsHandler iconsHandler = TBApplication.getApplication(requireContext()).getIconsHandler();
        IconPackXML iconPack = iconsHandler.getCustomIconPack();
        String packName = iconPack != null ? iconPack.getPackPackageName() : null;
        if (packName == null && mIconPackList.getChildCount() > 0)
            packName = mIconPackList.getChildAt(0).getTag().toString();
        setShownIconPack(packName);
    }

    private void refreshList() {
        mIconData.clear();
        IconPackXML iconPack = mShownIconPack;
        if (iconPack != null) {
            Collection<IconPackXML.DrawableInfo> drawables = iconPack.getDrawableList();
            StringNormalizer.Result normalized = StringNormalizer.normalizeWithResult(mSearch.getText(), true);
            FuzzyScore fuzzyScore = new FuzzyScore(normalized.codePoints);
            for (IconPackXML.DrawableInfo info : drawables) {
                if (fuzzyScore.match(info.getDrawableName()).match)
                    mIconData.add(new IconData(iconPack, info));
            }
        }
        mIconLoadingBar.setVisibility(View.GONE);
        boolean showGridAndSearch = !mIconData.isEmpty() || (mSearch.length() > 0);
        mSearch.setVisibility(showGridAndSearch ? View.VISIBLE : View.GONE);
        mIconGrid.setVisibility(showGridAndSearch ? View.VISIBLE : View.GONE);
        ((BaseAdapter) mIconGrid.getAdapter()).notifyDataSetChanged();
    }

    static class IconData {
        final IconPackXML.DrawableInfo drawableInfo;
        final IconPackXML iconPack;

        IconData(IconPackXML iconPack, IconPackXML.DrawableInfo drawableInfo) {
            this.iconPack = iconPack;
            this.drawableInfo = drawableInfo;
        }

        Drawable getIcon() {
            return iconPack.getDrawable(drawableInfo);
        }
    }

    static class IconAdapter extends BaseAdapter {
        private final List<IconData> mIcons;
        private OnItemClickListener mOnItemClickListener = null;
        private OnItemClickListener mOnItemLongClickListener = null;

        public interface OnItemClickListener {
            void onItemClick(IconAdapter adapter, View view, int position);
        }

        IconAdapter(@NonNull List<IconData> objects) {
            mIcons = objects;
        }

        void setOnItemClickListener(OnItemClickListener listener) {
            mOnItemClickListener = listener;
        }

        void setOnItemLongClickListener(OnItemClickListener listener) {
            mOnItemLongClickListener = listener;
        }

        @Override
        public IconData getItem(int position) {
            return mIcons.get(position);
        }

        @Override
        public int getCount() {
            return mIcons.size();
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).hashCode();
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            final View view;
            if (convertView == null) {
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.custom_icon_item, parent, false);
            } else {
                view = convertView;
            }
            ViewHolder holder = view.getTag() instanceof ViewHolder ? (ViewHolder) view.getTag() : new ViewHolder(view);

            IconData content = getItem(position);
            holder.setContent(content);

            holder.icon.setOnClickListener(v -> {
                if (mOnItemClickListener != null)
                    mOnItemClickListener.onItemClick(IconAdapter.this, v, position);
            });
            holder.icon.setOnLongClickListener(v -> {
                if (mOnItemLongClickListener != null)
                    mOnItemLongClickListener.onItemClick(IconAdapter.this, v, position);
                return true;
            });

            return view;
        }

        static class ViewHolder {
            ImageView icon;
            AsyncLoad loader = null;

            static class AsyncLoad extends AsyncTask<IconData, Void, Drawable> {
                WeakReference<ViewHolder> holder;

                AsyncLoad(ViewHolder holder) {
                    this.holder = new WeakReference<>(holder);
                }

                @Override
                protected void onPreExecute() {
                    ViewHolder h = holder.get();
                    if (h == null || h.loader != this)
                        return;
                    h.icon.setImageDrawable(null);
                }

                @Override
                protected Drawable doInBackground(IconData... iconData) {
                    return iconData[0].getIcon();
                }

                @Override
                protected void onPostExecute(Drawable drawable) {
                    ViewHolder h = holder.get();
                    if (h == null || h.loader != this)
                        return;
                    h.loader = null;
                    h.icon.setImageDrawable(drawable);
                    h.icon.setScaleX(0f);
                    h.icon.setScaleY(0f);
                    h.icon.setRotation((drawable.hashCode() & 1) == 1 ? 180f : -180f);
                    h.icon.animate().scaleX(1f).scaleY(1f).rotation(0f).start();
                }
            }

            ViewHolder(View itemView) {
                itemView.setTag(this);
                icon = itemView.findViewById(android.R.id.icon);
            }

            public void setContent(IconData content) {
                if (loader != null)
                    loader.cancel(true);
                loader = new AsyncLoad(this);
                loader.executeOnExecutor(ResultViewHelper.EXECUTOR_LOAD_ICON, content);
            }
        }
    }
}
