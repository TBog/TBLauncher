package rocks.tbog.tblauncher;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.IdRes;
import androidx.preference.PreferenceManager;

import rocks.tbog.tblauncher.ui.CutoutFactory;
import rocks.tbog.tblauncher.utils.SystemUiVisibility;
import rocks.tbog.tblauncher.utils.UIColors;

public class CustomizeUI {
    private TBLauncherActivity mTBLauncherActivity;
    private SharedPreferences mPref;
    private ImageView mNotificationBackground;
    private View mSearchBarContainer;

    @SuppressWarnings("TypeParameterUnusedInFormals")
    private <T extends View> T findViewById(@IdRes int id) {
        return mTBLauncherActivity.findViewById(id);
    }

    public void onCreateActivity(TBLauncherActivity tbLauncherActivity) {
        mTBLauncherActivity = tbLauncherActivity;
        mPref = PreferenceManager.getDefaultSharedPreferences(tbLauncherActivity);

        mNotificationBackground = findViewById(R.id.notificationBackground);
        mSearchBarContainer = findViewById(R.id.searchBarContainer);
    }

    public void onResume() {
        setNotificationBarColor();
        setSearchBarColor();
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

    private void setSearchBarColor() {
        int color = UIColors.getColor(mPref, "search-bar-color");
        int alpha = UIColors.getAlpha(mPref, "search-bar-alpha");
        boolean gradient = mPref.getBoolean("search-bar-gradient", true);
        if (gradient)
            mSearchBarContainer.setBackgroundResource(R.drawable.search_bar_background);
        else
            mSearchBarContainer.setBackground(new ColorDrawable(Color.WHITE));
        mSearchBarContainer.getBackground().setColorFilter(new PorterDuffColorFilter(UIColors.setAlpha(color, alpha), PorterDuff.Mode.MULTIPLY));
    }
}
