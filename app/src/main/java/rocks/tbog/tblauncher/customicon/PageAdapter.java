package rocks.tbog.tblauncher.customicon;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.viewpager.widget.ViewPager;

import java.util.ArrayList;

class PageAdapter extends androidx.viewpager.widget.PagerAdapter implements ViewPager.OnPageChangeListener {

    private final ArrayList<Page> pageList = new ArrayList<>(0);
    private int mScrollState = ViewPager.SCROLL_STATE_IDLE;

    void addPage(Page page) {
        pageList.add(page);
    }

    @NonNull
    Iterable<Page> getPageIterable() {
        return pageList;
    }

    public void setupPageView(@NonNull IconSelectDialog iconSelectDialog) {
        // touch listener
        Page.OnItemClickListener iconClickListener = (adapter, v, position) -> {
            if (adapter instanceof IconAdapter) {
                IconData item = ((IconAdapter) adapter).getItem(position);
                Drawable icon = item.getIcon();
                iconSelectDialog.setSelectedDrawable(icon, icon);
            } else if (adapter instanceof CustomShapePage.ShapedIconAdapter) {
                CustomShapePage.ShapedIconInfo item = ((CustomShapePage.ShapedIconAdapter) adapter).getItem(position);
                if (item instanceof SystemPage.PickedIconInfo) {
                    if (((SystemPage.PickedIconInfo) item).launchPicker(iconSelectDialog, v))
                        return;
                }
                iconSelectDialog.setSelectedDrawable(item.getIcon(), item.getPreview());
            }
        };
        // long touch listener
        Page.OnItemClickListener iconLongClickListener = (adapter, v, position) -> {
            if (adapter instanceof IconAdapter) {
                IconData item = ((IconAdapter) adapter).getItem(position);
                iconSelectDialog.getIconPackMenu(item).show(v);
            }
        };
        // setup pages
        for (Page page : getPageIterable())
            page.setupView(iconSelectDialog, iconClickListener, iconLongClickListener);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        //Log.d("ISDialog", String.format("onPageScrolled %d %.2f", position, positionOffset));
        if (mScrollState != ViewPager.SCROLL_STATE_SETTLING) {
            Page pageLeft = pageList.get(position);
            if (!pageLeft.bDataLoaded)
                pageLeft.loadData();
            if ((position + 1) < pageList.size()) {
                Page pageRight = pageList.get(position + 1);
                if (!pageRight.bDataLoaded)
                    pageRight.loadData();
            }
        }
    }

    @Override
    public void onPageSelected(int position) {
        //Log.d("ISDialog", String.format("onPageSelected %d", position));
        Page page = pageList.get(position);
        if (!page.bDataLoaded)
            page.loadData();
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        //Log.d("ISDialog", String.format("onPageScrollStateChanged %d", state));
        mScrollState = state;
    }

    static abstract class Page {
        final CharSequence pageName;
        final View pageView;
        boolean bDataLoaded = false;

        public interface OnItemClickListener {
            void onItemClick(Adapter adapter, View view, int position);
        }

        Page(CharSequence name, View view) {
            pageName = name;
            pageView = view;
        }

        abstract void setupView(@NonNull DialogFragment dialogFragment, @Nullable OnItemClickListener iconClickListener, @Nullable OnItemClickListener iconLongClickListener);

        public void addPickedIcon(@NonNull Drawable pickedImage, String filename) {
            // do nothing in the base class, override to handle image picked from gallery
        }

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
