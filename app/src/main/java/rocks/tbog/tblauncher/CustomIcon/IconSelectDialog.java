package rocks.tbog.tblauncher.CustomIcon;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
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
import androidx.annotation.StringRes;
import androidx.viewpager.widget.ViewPager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import rocks.tbog.tblauncher.IconsHandler;
import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.db.DBHelper;
import rocks.tbog.tblauncher.db.ShortcutRecord;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.entry.ShortcutEntry;
import rocks.tbog.tblauncher.entry.StaticEntry;
import rocks.tbog.tblauncher.icons.IconPack;
import rocks.tbog.tblauncher.shortcut.ShortcutUtil;
import rocks.tbog.tblauncher.ui.DialogFragment;
import rocks.tbog.tblauncher.ui.LinearAdapter;
import rocks.tbog.tblauncher.ui.ListPopup;
import rocks.tbog.tblauncher.utils.UserHandleCompat;
import rocks.tbog.tblauncher.utils.Utilities;

public class IconSelectDialog extends DialogFragment<Drawable> {
    private Drawable mSelectedDrawable = null;
    private ImageView mPreview;
    private ViewPager mViewPager;
    private CustomShapePage mCustomShapePage = null;

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

                mCustomShapePage = addSystemPage(inflater, mViewPager, cn, userHandle, pageName);
            } else if (args.containsKey("entryId")) {
                String entryId = args.getString("entryId", "");
                EntryItem entryItem = TBApplication.dataHandler(context).getPojo(entryId);
                if (!(entryItem instanceof StaticEntry)) {
                    dismiss();
                } else {
                    StaticEntry staticEntry = (StaticEntry) entryItem;
                    String pageName = context.getString(R.string.tab_static_icons);
                    mCustomShapePage = addStaticEntryPage(inflater, mViewPager, staticEntry, pageName);
                }
            } else if (args.containsKey("shortcutId")) {
                String packageName = args.getString("packageName", "");
                String shortcutData = args.getString("shortcutData", "");
                ShortcutRecord shortcutRecord = null;
                List<ShortcutRecord> shortcutRecordList = DBHelper.getShortcutsNoIcons(context, packageName);
                for (ShortcutRecord rec : shortcutRecordList)
                    if (shortcutData.equals(rec.infoData)) {
                        shortcutRecord = rec;
                        break;
                    }
                if (shortcutRecord == null) {
                    dismiss();
                } else {
                    mCustomShapePage = addShortcutPage(inflater, mViewPager, shortcutRecord, shortcutRecord.displayName);
                }

            }
        }

        ArrayList<Pair<String, String>> iconPacks;
        // add icon packs
        {
            IconsHandler iconsHandler = TBApplication.iconsHandler(context);
            Map<String, String> iconPackNames = iconsHandler.getIconPackNames();
            iconPacks = new ArrayList<>(iconPackNames.size());
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
                addIconPackPage(inflater, mViewPager, packName, packPackageName);
            }
        }
        pageAdapter.notifyDataSetChanged();

        pageAdapter.setupPageView(context, (adapter, v, position) -> {
            if (adapter instanceof IconAdapter) {
                IconData item = ((IconAdapter) adapter).getItem(position);
                mSelectedDrawable = item.getIcon();
                mPreview.setImageDrawable(mSelectedDrawable);
            } else if (adapter instanceof CustomShapePage.ShapedIconAdapter) {
                CustomShapePage.ShapedIconInfo item = ((CustomShapePage.ShapedIconAdapter) adapter).getItem(position);
                mSelectedDrawable = item.getIcon();
                mPreview.setImageDrawable(item.getPreview());
            }
        }, (adapter, v, position) -> {
            if (adapter instanceof IconAdapter) {
                IconData item = ((IconAdapter) adapter).getItem(position);
                getIconPackMenu(item).show(v);
            }
        });

        if (mCustomShapePage instanceof SystemPage) {
            ((SystemPage) mCustomShapePage).loadIconPackIcons(iconPacks);
        }

        return view;
    }

    public void addIconPackPage(@NonNull LayoutInflater inflater, ViewGroup container, String packName, String packPackageName) {
        View view = inflater.inflate(R.layout.dialog_icon_select_page, container, false);
        IconPackPage page = new IconPackPage(packName, packPackageName, view);
        PageAdapter adapter = (PageAdapter) mViewPager.getAdapter();
        adapter.addPage(page);
    }

    public SystemPage addSystemPage(LayoutInflater inflater, ViewPager container, ComponentName cn, UserHandleCompat userHandle, String pageName) {
        View view = inflater.inflate(R.layout.dialog_custom_shape_icon_select_page, container, false);
        SystemPage page = new SystemPage(pageName, view, cn, userHandle);
        PageAdapter adapter = (PageAdapter) mViewPager.getAdapter();
        adapter.addPage(page);
        return page;
    }

    public StaticEntryPage addStaticEntryPage(LayoutInflater inflater, ViewPager container, StaticEntry staticEntry, String pageName) {
        View view = inflater.inflate(R.layout.dialog_custom_shape_icon_select_page, container, false);
        StaticEntryPage page = new StaticEntryPage(pageName, view, staticEntry);
        PageAdapter adapter = (PageAdapter) mViewPager.getAdapter();
        adapter.addPage(page);
        return page;
    }

    public ShortcutPage addShortcutPage(LayoutInflater inflater, ViewPager container, ShortcutRecord shortcutRecord, String pageName) {
        View view = inflater.inflate(R.layout.dialog_custom_shape_icon_select_page, container, false);
        ShortcutPage page = new ShortcutPage(pageName, view, shortcutRecord);
        PageAdapter adapter = (PageAdapter) mViewPager.getAdapter();
        adapter.addPage(page);
        return page;
    }

    private ListPopup getIconPackMenu(IconData iconData) {
        final Context ctx = getContext();
        LinearAdapter adapter = new LinearAdapter();

        adapter.add(new LinearAdapter.ItemTitle(iconData.drawableInfo.getDrawableName()));
        adapter.add(new LinearAdapter.Item(ctx, R.string.choose_icon_menu_add));
        adapter.add(new LinearAdapter.Item(ctx, R.string.choose_icon_menu_add2));

        return ListPopup.create(ctx, adapter)
                .setModal(true)
                .setOnItemClickListener((a, v, pos) -> {
                    LinearAdapter.MenuItem item = ((LinearAdapter) a).getItem(pos);
                    @StringRes int stringId = 0;
                    if (item instanceof LinearAdapter.Item) {
                        stringId = ((LinearAdapter.Item) a.getItem(pos)).stringId;
                    }
                    if (stringId == R.string.choose_icon_menu_add2) {
                        if (mCustomShapePage != null)
                            mCustomShapePage.addIcon(iconData.drawableInfo.getDrawableName(), iconData.getIcon());
                        // set the first page as current
                        mViewPager.setCurrentItem(0);
                    } else if (stringId == R.string.choose_icon_menu_add) {
                        if (mCustomShapePage != null)
                            mCustomShapePage.addIcon(iconData.drawableInfo.getDrawableName(), iconData.getIcon());
                    }
                });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments() != null ? getArguments() : new Bundle();
        if (args.containsKey("componentName"))
            customIconApp(args);
        else if (args.containsKey("entryId"))
            customIconStaticEntry(args);
        else if (args.containsKey("shortcutId"))
            customIconShortcut(args);

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

        IconsHandler iconsHandler = TBApplication.getApplication(context).iconsHandler();
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

    private void customIconShortcut(Bundle args) {
        Context context = requireContext();

        String shortcutId = args.getString("shortcutId", "");

        EntryItem entryItem = TBApplication.dataHandler(context).getPojo(shortcutId);
        if (!(entryItem instanceof ShortcutEntry)) {
            dismiss();
            return;
        }
        ShortcutEntry shortcutEntry = (ShortcutEntry) entryItem;

        // Preview
        Utilities.setIconAsync(mPreview, shortcutEntry::getIcon);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
//        IconsHandler iconsHandler = TBApplication.getApplication(requireContext()).getIconsHandler();
//        IconPackXML iconPack = iconsHandler.getCustomIconPack();
//        String packageName = iconPack != null ? iconPack.getPackPackageName() : null;

        PageAdapter adapter = (PageAdapter) mViewPager.getAdapter();
        if (adapter != null) {
            int selectedPage = mViewPager.getCurrentItem();
//            if (packageName != null) {
//                int idx = 0;
//                for (PageAdapter.Page page : adapter.getPageIterable()) {
//                    if (page instanceof IconPackPage)
//                        if (packageName.equals(((IconPackPage) page).packageName)) {
//                            selectedPage = idx;
//                            break;
//                        }
//                    idx += 1;
//                }
//            }
//            mViewPager.setCurrentItem(selectedPage);
            // allow the adapter to load as needed
            mViewPager.addOnPageChangeListener(adapter);
            // make sure we load the selected page
            adapter.onPageSelected(selectedPage);
        }
    }
}
