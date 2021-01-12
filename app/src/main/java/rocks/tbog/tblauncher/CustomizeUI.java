package rocks.tbog.tblauncher;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.PaintDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import rocks.tbog.tblauncher.ui.CutoutFactory;
import rocks.tbog.tblauncher.ui.SearchEditText;
import rocks.tbog.tblauncher.utils.SystemUiVisibility;
import rocks.tbog.tblauncher.utils.UIColors;
import rocks.tbog.tblauncher.utils.UISizes;
import rocks.tbog.tblauncher.utils.Utilities;

public class CustomizeUI {
    private TBLauncherActivity mTBLauncherActivity;
    private SharedPreferences mPref = null;
    private ImageView mNotificationBackground;
    private ViewGroup mSearchBarContainer;
    private SearchEditText mSearchBar;
    private ImageView mLauncherButton;
    private ImageView mMenuButton;
    private ImageView mClearButton;
    private View mResultLayout;

    @SuppressWarnings("TypeParameterUnusedInFormals")
    private <T extends View> T findViewById(@IdRes int id) {
        return mTBLauncherActivity.findViewById(id);
    }

    public void onCreateActivity(TBLauncherActivity tbLauncherActivity) {
        mTBLauncherActivity = tbLauncherActivity;
        mPref = PreferenceManager.getDefaultSharedPreferences(tbLauncherActivity);

        mNotificationBackground = findViewById(R.id.notificationBackground);
        mSearchBarContainer = findViewById(R.id.searchBarContainer);
        mSearchBar = mSearchBarContainer.findViewById(R.id.launcherSearch);
        mLauncherButton = mSearchBarContainer.findViewById(R.id.launcherButton);
        mMenuButton = mSearchBarContainer.findViewById(R.id.menuButton);
        mClearButton = mSearchBarContainer.findViewById(R.id.clearButton);
        mResultLayout = findViewById(R.id.resultLayout);
    }

    public void onResume() {
        setNotificationBarColor();
        setSearchBarPref();
        setResultListPref(mResultLayout);
        TBApplication.quickList(getContext()).onResume(mPref);
    }

    private void setNotificationBarColor() {
        int color = UIColors.getColor(mPref, "notification-bar-color");
        int alpha = UIColors.getAlpha(mPref, "notification-bar-alpha");
        boolean gradient = mPref.getBoolean("notification-bar-gradient", true);

        if (gradient) {
            int size = CutoutFactory.StatusBarCutout.getStatusBarHeight(mTBLauncherActivity);
            ViewGroup.LayoutParams params = mNotificationBackground.getLayoutParams();
            if (params != null) {
                params.height = size;
                mNotificationBackground.setLayoutParams(params);
            }
            Utilities.setColorFilterMultiply(mNotificationBackground, UIColors.setAlpha(color, alpha));
            UIColors.setStatusBarColor(mTBLauncherActivity, 0x00000000);
        } else {
            mNotificationBackground.setVisibility(View.GONE);
            UIColors.setStatusBarColor(mTBLauncherActivity, UIColors.setAlpha(color, alpha));
        }

        // Notification drawer icon color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View view = mTBLauncherActivity.getWindow().getDecorView();
            if (mPref.getBoolean("black-notification-icons", false)) {
                SystemUiVisibility.setLightStatusBar(view);
            } else {
                SystemUiVisibility.clearLightStatusBar(view);
            }
        }

    }

    private void setSearchBarPref() {
        Resources resources = mSearchBarContainer.getResources();

        // size
        int percent = mPref.getInt("search-bar-size", 0);

        // layout height
        {
            int smallSize = resources.getDimensionPixelSize(R.dimen.bar_height);
            int largeSize = resources.getDimensionPixelSize(R.dimen.large_bar_height);
            ViewGroup.LayoutParams params = mSearchBarContainer.getLayoutParams();
            if (params instanceof LinearLayout.LayoutParams) {
                params.height = smallSize + (largeSize - smallSize) * percent / 100;
                mSearchBarContainer.setLayoutParams(params);
            } else {
                throw new IllegalStateException("mSearchBarContainer has the wrong layout params");
            }
        }

        // text size
        {
            float smallSize = resources.getDimension(R.dimen.small_bar_text);
            float largeSize = resources.getDimension(R.dimen.large_bar_text);
            mSearchBar.setTextSize(TypedValue.COMPLEX_UNIT_PX, smallSize + (largeSize - smallSize) * percent / 100);
        }

        // text color
        {
            int searchTextColor = UIColors.getSearchTextColor(mSearchBar.getContext());
            int searchHintColor = UIColors.setAlpha(searchTextColor, 0xBB);
            mSearchBar.setTextColor(searchTextColor);
            mSearchBar.setHintTextColor(searchHintColor);
        }

        // icon color
        {
            int searchIconColor = UIColors.getSearchIconColor(mLauncherButton.getContext());
            Utilities.setColorFilterMultiply(mLauncherButton, searchIconColor);
            Utilities.setColorFilterMultiply(mMenuButton, searchIconColor);
            Utilities.setColorFilterMultiply(mClearButton, searchIconColor);
        }

        // background color
        int color = UIColors.getColor(mPref, "search-bar-color");
        int alpha = UIColors.getAlpha(mPref, "search-bar-alpha");
        if (mPref.getBoolean("search-bar-gradient", true)) {
            mSearchBarContainer.setBackgroundResource(R.drawable.search_bar_background);
            Utilities.setColorFilterMultiply(mSearchBarContainer.getBackground(), UIColors.setAlpha(color, alpha));
        } else if (mPref.getBoolean("search-bar-rounded", true)) {
            PaintDrawable drawable = new PaintDrawable();
            drawable.getPaint().setColor(UIColors.setAlpha(color, alpha));
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mSearchBarContainer.getLayoutParams();
            drawable.setCornerRadius(resources.getDimension(R.dimen.bar_corner_radius));
            mSearchBarContainer.setBackground(drawable);
            int margin = (int) (params.height * .25f);
            params.setMargins(margin, 0, margin, margin);
        } else
            mSearchBarContainer.setBackground(new ColorDrawable(UIColors.setAlpha(color, alpha)));
    }

    public void setResultListPref(View resultLayout) {
        int background = UIColors.getResultListBackground(mPref);
        Drawable drawable;
        if (mPref.getBoolean("result-list-rounded", true)) {
            drawable = new GradientDrawable();  // can't use PaintDrawable when alpha < 255, ugly big darker borders
            ((GradientDrawable) drawable).setColor(background);
            ((GradientDrawable) drawable).setCornerRadius(resultLayout.getResources().getDimension(R.dimen.result_corner_radius));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // clip list content to rounded corners
                resultLayout.setClipToOutline(true);
            }
        } else {
            drawable = new ColorDrawable(background);
        }
        resultLayout.setBackground(drawable);

        if (resultLayout instanceof AbsListView) {
            setListViewSelectorPref((AbsListView) resultLayout, true);
            setListViewScrollbarPref(resultLayout);
        } else {
            View list = resultLayout.findViewById(R.id.resultList);
            if (list instanceof AbsListView) {
                setListViewSelectorPref((AbsListView) list, false);
                setListViewScrollbarPref(list);
            }
        }
    }

    public void setListViewSelectorPref(AbsListView listView, boolean borderless) {
        int touchColor = UIColors.getResultListRipple(listView.getContext());
        Drawable selector = getSelectorDrawable(listView, touchColor, borderless);
        listView.setSelector(selector);
    }

    public Drawable getSelectorDrawable(View view, int color, boolean borderless) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Drawable mask = borderless ? null : new ColorDrawable(Color.WHITE);
            Drawable content = borderless ? null : view.getBackground();
            return new RippleDrawable(ColorStateList.valueOf(color), content, mask);
        } else {
            ColorDrawable stateColor = new ColorDrawable(color);

            StateListDrawable stateListDrawable = new StateListDrawable();
            stateListDrawable.addState(new int[]{android.R.attr.state_selected}, stateColor);
            stateListDrawable.addState(new int[]{android.R.attr.state_focused}, stateColor);
            stateListDrawable.addState(new int[]{android.R.attr.state_pressed}, stateColor);
            stateListDrawable.addState(new int[]{}, new ColorDrawable(Color.TRANSPARENT));
            stateListDrawable.setEnterFadeDuration(300);
            stateListDrawable.setExitFadeDuration(100);
            return stateListDrawable;
        }
    }

    public void setListViewScrollbarPref(View listView) {
        int color = UIColors.getResultListRipple(listView.getContext());
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{color & 0xffffff, color, color});
        drawable.setCornerRadius(UISizes.dp2px(listView.getContext(), 3));
        drawable.setSize(UISizes.dp2px(listView.getContext(), 4), drawable.getIntrinsicHeight());

        Utilities.setVerticalScrollbarThumbDrawable(listView, drawable);
    }

    public Context getContext() {
        return mTBLauncherActivity;
    }

    @NonNull
    public Drawable getPopupBackgroundDrawable() {
        Context ctx = getContext();
        int border = UISizes.dp2px(ctx, 1);
        int r = ctx.getResources().getDimensionPixelSize(R.dimen.popup_corner_radius);
        float[] corners = new float[]{r, r, r, r, r, r, r, r};

        GradientDrawable gradient = new GradientDrawable();
        gradient.setCornerRadii(corners);
        gradient.setStroke(border, UIColors.getPopupBorderColor(ctx));
        gradient.setColor(UIColors.getPopupBackgroundColor(ctx));

        return gradient;
    }
}
