package rocks.tbog.tblauncher.CustomIcon;

import android.content.ComponentName;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;

import java.util.ArrayList;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.entry.StaticEntry;
import rocks.tbog.tblauncher.utils.UserHandleCompat;

class PageAdapter extends androidx.viewpager.widget.PagerAdapter implements ViewPager.OnPageChangeListener {

    private ArrayList<Page> pageList = new ArrayList<>(0);

    public void addIconPackPage(@NonNull LayoutInflater inflater, ViewGroup container, String packName, String packPackageName) {
        View view = inflater.inflate(R.layout.dialog_icon_select_page, container, false);
        IconPackPage page = new IconPackPage(packName, packPackageName, view);
        pageList.add(page);
    }

    public SystemPage addSystemPage(LayoutInflater inflater, ViewPager container, ComponentName cn, UserHandleCompat userHandle, String pageName) {
        View view = inflater.inflate(R.layout.dialog_icon_select_page, container, false);
        SystemPage page = new SystemPage(pageName, view, cn, userHandle);
        pageList.add(page);
        return page;
    }

    public void addStaticEntryPage(LayoutInflater inflater, ViewPager container, StaticEntry staticEntry, String pageName) {
        View view = inflater.inflate(R.layout.dialog_static_icon_select_page, container, false);
        StaticEntryPage page = new StaticEntryPage(pageName, view, staticEntry);
        pageList.add(page);
    }

    Iterable<Page> getPageIterable() {
        return pageList;
    }

    public void setupPageView(Context context, @Nullable Page.OnItemClickListener iconClickListener) {
        for (Page page : getPageIterable())
            page.setupView(context, iconClickListener);
    }

    public void loadPageData() {
        for (Page page : getPageIterable())
            page.loadData();
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        Page pageLeft = pageList.get(position);
        if (!pageLeft.bDataLoaded)
            pageLeft.loadData();
        if ((position + 1) < pageList.size()) {
            Page pageRight = pageList.get(position + 1);
            if (!pageRight.bDataLoaded)
                pageRight.loadData();
        }
    }

    @Override
    public void onPageSelected(int position) {
        Page page = pageList.get(position);
        if (!page.bDataLoaded)
            page.loadData();
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    static abstract class Page {
        final CharSequence pageName;
        final View pageView;
        boolean bDataLoaded = false;

        public interface OnItemClickListener {
            void onItemClick(BaseAdapter adapter, View view, int position);
        }

        Page(CharSequence name, View view) {
            pageName = name;
            pageView = view;
        }

        abstract void setupView(@NonNull Context context, @Nullable OnItemClickListener iconClickListener);

        void loadData() {
            bDataLoaded = true;
        }
    }

    @Override
    public int getCount() {
        return pageList.size();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        if (!(object instanceof Page))
            throw new IllegalStateException("WTF?");
        return ((Page) object).pageView == view;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return pageList.get(position).pageName;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        Page page = pageList.get(position);
        container.addView(page.pageView);
        return page;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        if (!(object instanceof Page))
            throw new IllegalStateException("WTF?");
        Page page = (Page) object;
        container.removeView(page.pageView);
    }
}
