package rocks.tbog.tblauncher;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.PaintDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.IdRes;
import androidx.preference.PreferenceManager;

import rocks.tbog.tblauncher.ui.CutoutFactory;
import rocks.tbog.tblauncher.ui.SearchEditText;
import rocks.tbog.tblauncher.utils.SystemUiVisibility;
import rocks.tbog.tblauncher.utils.UIColors;

public class CustomizeUI {
    private TBLauncherActivity mTBLauncherActivity;
    private SharedPreferences mPref = null;
    private ImageView mNotificationBackground;
    private ViewGroup mSearchBarContainer;
    private SearchEditText mSearchBar;
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
        mResultLayout = findViewById(R.id.resultLayout);
    }

    public void onResume() {
        setNotificationBarColor();
        setSearchBarPref();
        setResultListPref();
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
            mNotificationBackground.setImageAlpha(alpha);
            mNotificationBackground.setColorFilter(color);
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

        // set layout height
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

        // set text size
        {
            float smallSize = resources.getDimension(R.dimen.bar_text);
            float largeSize = resources.getDimension(R.dimen.large_bar_text);
            mSearchBar.setTextSize(TypedValue.COMPLEX_UNIT_PX, smallSize + (largeSize - smallSize) * percent / 100);
        }

        // color
        int color = UIColors.getColor(mPref, "search-bar-color");
        int alpha = UIColors.getAlpha(mPref, "search-bar-alpha");
        boolean gradient = mPref.getBoolean("search-bar-gradient", true);
        if (gradient) {
            mSearchBarContainer.setBackgroundResource(R.drawable.search_bar_background);
            mSearchBarContainer.getBackground().setColorFilter(new PorterDuffColorFilter(UIColors.setAlpha(color, alpha), PorterDuff.Mode.MULTIPLY));
        } else {
            if (mPref.getBoolean("search-bar-rounded", true)) {
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
    }

    private void setResultListPref() {
        int color = UIColors.getColor(mPref, "result-list-color");
        int alpha = UIColors.getAlpha(mPref, "result-list-alpha");
        Drawable drawable;
        if (mPref.getBoolean("result-list-rounded", true)) {
            drawable = new GradientDrawable();  // can't use PaintDrawable when alpha < 255, ugly big darker borders
            ((GradientDrawable) drawable).setColor(UIColors.setAlpha(color, alpha));
            ((GradientDrawable) drawable).setCornerRadius(mResultLayout.getResources().getDimension(R.dimen.result_corner_radius));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // clip list content to rounded corners
                mResultLayout.setClipToOutline(true);
            }
        } else {
            drawable = new ColorDrawable(UIColors.setAlpha(color, alpha));
        }
        //drawable.setColorFilter(new PorterDuffColorFilter(UIColors.setAlpha(color, alpha), PorterDuff.Mode.MULTIPLY));
        mResultLayout.setBackground(drawable);
    }

    public Context getContext() {
        return mTBLauncherActivity;
    }
}
