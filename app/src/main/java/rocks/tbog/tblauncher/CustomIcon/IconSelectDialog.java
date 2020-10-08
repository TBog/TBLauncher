package rocks.tbog.tblauncher.CustomIcon;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import rocks.tbog.tblauncher.IconsHandler;
import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.entry.StaticEntry;
import rocks.tbog.tblauncher.icons.IconPack;
import rocks.tbog.tblauncher.icons.IconPackXML;
import rocks.tbog.tblauncher.ui.DialogFragment;
import rocks.tbog.tblauncher.utils.UserHandleCompat;
import rocks.tbog.tblauncher.utils.Utilities;

public class IconSelectDialog extends DialogFragment<Drawable> {
    private Drawable mSelectedDrawable = null;
    private ImageView mPreview;
    private ViewPager mViewPager;

    @Override
    protected int layoutRes() {
        return R.layout.dialog_icon_select;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (view == null)
            return null;
        Context context = requireContext();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            view.setClipToOutline(true);
        }

        mPreview = view.findViewById(R.id.preview);
        mViewPager = view.findViewById(R.id.viewPager);

        PageAdapter pageAdapter = new PageAdapter();
        mViewPager.setAdapter(pageAdapter);

        // add system icons
        {
            Bundle args = getArguments() != null ? getArguments() : new Bundle();
            if (args.containsKey("componentName")) {
                String name = args.getString("componentName", "");
                String entryName = args.getString("entryName", "");
                String pageName = context.getString(R.string.tab_app_icons, entryName);

                ComponentName cn = UserHandleCompat.unflattenComponentName(name);
                UserHandleCompat userHandle = UserHandleCompat.fromComponentName(context, name);

                pageAdapter.addSystemPage(inflater, mViewPager, cn, userHandle, pageName);
            } else if (args.containsKey("entryId")) {
                String entryId = args.getString("entryId", "");
                EntryItem entryItem = TBApplication.dataHandler(context).getPojo(entryId);
                if (!(entryItem instanceof StaticEntry)) {
                    dismiss();
                } else {
                    StaticEntry staticEntry = (StaticEntry) entryItem;
                    String pageName = context.getString(R.string.tab_static_icons);
                    pageAdapter.addSystemPage(inflater, mViewPager, staticEntry, pageName);
                }
            }
        }

        // add icon packs
        {
            IconsHandler iconsHandler = TBApplication.iconsHandler(context);
            Map<String, String> iconPackNames = iconsHandler.getIconPackNames();
            ArrayList<Pair<String, String>> iconPacks = new ArrayList<>(iconPackNames.size());
            for (Map.Entry<String, String> packInfo : iconPackNames.entrySet())
                iconPacks.add(new Pair<>(packInfo.getKey(), packInfo.getValue()));
            IconPack<?> iconPack = iconsHandler.getCustomIconPack();
            String selectedPackPackageName = iconPack != null ? iconPack.getPackPackageName() : "";
            Collections.sort(iconPacks, (o1, o2) -> {
                if (selectedPackPackageName.equals(o1.first))
                    return -1;
                if (selectedPackPackageName.equals(o2.first))
                    return 1;
                return o1.second.compareTo(o2.second);
            });
            for (Pair<String, String> packInfo : iconPacks) {
                String packPackageName = packInfo.first;
                String packName = packInfo.second;
                if (selectedPackPackageName.equals(packPackageName))
                    packName = context.getString(R.string.selected_pack, packName);

                // add page to ViewPager
                pageAdapter.addIconPack(inflater, mViewPager, packName, packPackageName);
            }
        }
        pageAdapter.notifyDataSetChanged();

        pageAdapter.setupPageView(context, (adapter, v, position) -> {
            if (adapter instanceof IconAdapter) {
                IconData item = ((IconAdapter) adapter).getItem(position);
                mSelectedDrawable = item.getIcon();
                mPreview.setImageDrawable(mSelectedDrawable);
            } else if (adapter instanceof SystemPage.SystemPageAdapter) {
                SystemPage.SystemIconInfo item = ((SystemPage.SystemPageAdapter) adapter).getItem(position);
                mSelectedDrawable = item.getIcon();
                mPreview.setImageDrawable(item.getPreview());
            }
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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

//    private void populateIconPackList() {
//        mIconPackList.removeAllViews();
//
//        LayoutInflater inflater = LayoutInflater.from(getContext());
//        Map<String, String> iconPacks = TBApplication.iconsHandler(getContext()).getIconPackNames();
//        for (Map.Entry<String, String> packInfo : iconPacks.entrySet()) {
//            String packageName = packInfo.getKey();
//            String packName = packInfo.getValue();
//            View packView = inflater.inflate(R.layout.item_quick_list, mIconPackList, false);
//            mIconPackList.addView(packView);
//
//            ImageView icon = packView.findViewById(android.R.id.icon);
//            TextView name = packView.findViewById(android.R.id.text1);
//
//            name.setText(packName);
//
//            Utilities.setIconAsync(icon, (ctx) -> {
//                Drawable drawable = null;
//                try {
//                    drawable = ctx.getPackageManager().getApplicationIcon(packageName);
//                } catch (PackageManager.NameNotFoundException ignored) {
//                }
//                return drawable;
//            });
//
//            packView.setTag(packageName);
//            packView.setOnClickListener((v) -> {
//                String tag = v.getTag().toString();
//                setShownIconPack(tag);
//            });
//        }
//
//        if (mIconPackList.getChildCount() == 0)
//            mIconPackList.setVisibility(View.GONE);
//        else
//            mIconPackList.requestLayout();
//    }

//    private void refreshQuickList() {
//        Context context = requireContext();
//        View view = getView();
//        if (view == null)
//            return;
//
//        Bundle args = getArguments() != null ? getArguments() : new Bundle();
//        if (args.containsKey("componentName")) {
//            String name = args.getString("componentName", "");
//
//            IconsHandler iconsHandler = TBApplication.getApplication(context).getIconsHandler();
//            ComponentName cn = UserHandleCompat.unflattenComponentName(name);
//            UserHandleCompat userHandle = UserHandleCompat.fromComponentName(context, name);
//
//            //TODO: move this in an async task
//            setQuickList(iconsHandler, view, cn, userHandle);
//        } else if (args.containsKey("entryId")) {
//            String entryId = args.getString("entryId", "");
//            EntryItem entryItem = TBApplication.dataHandler(context).getPojo(entryId);
//            if (!(entryItem instanceof StaticEntry)) {
//                dismiss();
//                return;
//            }
//            StaticEntry staticEntry = (StaticEntry) entryItem;
//            setQuickList(view, staticEntry);
//        }
//    }

//    private void setQuickList(View view, StaticEntry staticEntry) {
//        Context context = view.getContext();
//        ViewGroup quickList = view.findViewById(R.id.quickList);
//        quickList.removeViews(1, quickList.getChildCount() - 1);
//
//        // add default icon
//        {
//            Drawable drawable = staticEntry.getDefaultDrawable(context);
//
//            ImageView icon = quickList.findViewById(android.R.id.icon);
//            icon.setImageDrawable(drawable);
//            icon.setOnClickListener(v -> {
//                mSelectedDrawable = null;
//                mPreview.setImageDrawable(((ImageView) v).getDrawable());
//            });
//            ((TextView) quickList.findViewById(android.R.id.text1)).setText(R.string.default_icon);
//        }
//    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        IconsHandler iconsHandler = TBApplication.getApplication(requireContext()).getIconsHandler();
        IconPackXML iconPack = iconsHandler.getCustomIconPack();
        String packageName = iconPack != null ? iconPack.getPackPackageName() : null;

        PageAdapter adapter = (PageAdapter) mViewPager.getAdapter();
        if (adapter != null) {
            int selectedPage = mViewPager.getCurrentItem();
            if (packageName != null) {
                int idx = 0;
                for (PageAdapter.Page page : adapter.getPageIterable()) {
                    if (page instanceof IconPackPage)
                        if (packageName.equals(((IconPackPage) page).packageName)) {
                            selectedPage = idx;
                            break;
                        }
                    idx += 1;
                }
            }
            mViewPager.setCurrentItem(selectedPage);
            // allow the adapter to load as needed
            mViewPager.addOnPageChangeListener(adapter);
            // make sure we load the selected page
            adapter.onPageSelected(selectedPage);
        }
    }
}
