package rocks.tbog.tblauncher.quicklist;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.PagerAdapter;

import java.util.ArrayList;

class ViewPagerAdapter extends PagerAdapter {
    private final ArrayList<PageInfo> mPages;

    ViewPagerAdapter(@NonNull ArrayList<PageInfo> pages) {
        mPages = pages;
    }

    @Override
    public int getCount() {
        return mPages.size();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        if (object instanceof PageInfo)
            return ((PageInfo) object).view == view;
        return false;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return mPages.get(position).title;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        final PageInfo pageInfo = mPages.get(position);
        container.addView(pageInfo.view);
        return pageInfo;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        if (object instanceof PageInfo)
            container.removeView(((PageInfo) object).view);
        else
            container.removeView(mPages.get(position).view);
    }

    public static class PageInfo {
        private final String title;
        private final View view;

        public PageInfo(String title, View view) {
            this.title = title;
            this.view = view;
        }

        public View getView() {
            return view;
        }
    }
}
