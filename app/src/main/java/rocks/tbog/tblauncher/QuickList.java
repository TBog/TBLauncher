package rocks.tbog.tblauncher;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

import rocks.tbog.tblauncher.dataprovider.AppProvider;
import rocks.tbog.tblauncher.dataprovider.ContactsProvider;
import rocks.tbog.tblauncher.entry.AppEntry;
import rocks.tbog.tblauncher.entry.ContactEntry;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.utils.UIColors;

public class QuickList {
    private TBLauncherActivity mTBLauncherActivity;
    private boolean mIsEnabled = true;
    private LinearLayout mQuickList;
    private boolean bListVisible = false;
    private boolean bAppToggleOn = false;
    private boolean bContactToggleOn = false;
    private boolean bAdapterEmpty = true;

//    @SuppressWarnings("TypeParameterUnusedInFormals")
//    private <T extends View> T findViewById(@IdRes int id) {
//        return mTBLauncherActivity.findViewById(id);
//    }

    public Context getContext() {
        return mTBLauncherActivity;
    }

    public void onCreateActivity(TBLauncherActivity tbLauncherActivity) {
        mTBLauncherActivity = tbLauncherActivity;

        mQuickList = mTBLauncherActivity.findViewById(R.id.quickList);
        populateList();
    }

    private void populateList() {
        mQuickList.removeAllViews();
        if (!isQuickListEnabled()) {
            mQuickList.setVisibility(View.GONE);
            return;
        }
        // apps filter
        {
            View filter = LayoutInflater.from(getContext()).inflate(R.layout.item_quick_list, mQuickList, false);
            Drawable drawable = ContextCompat.getDrawable(getContext(), R.drawable.ic_android);
            ((ImageView) filter.findViewById(android.R.id.icon)).setImageDrawable(drawable);
            ((TextView) filter.findViewById(android.R.id.text1)).setText("Applications");
            filter.setOnClickListener(v -> {
                //TBApplication.behaviour(v.getContext()).resultListVisible()
            });
            mQuickList.addView(filter);

            filter.setOnClickListener(this::toggleApps);
        }
        // contacts filter
        {
            View filter = LayoutInflater.from(getContext()).inflate(R.layout.item_quick_list, mQuickList, false);
            Drawable drawable = ContextCompat.getDrawable(getContext(), R.drawable.ic_contact);
            ((ImageView) filter.findViewById(android.R.id.icon)).setImageDrawable(drawable);
            ((TextView) filter.findViewById(android.R.id.text1)).setText("Contacts");
            mQuickList.addView(filter);

            filter.setOnClickListener(this::toggleContacts);
        }
    }

    private void toggleApps(View v) {
        TBApplication app = TBApplication.getApplication(v.getContext());
        if (bAdapterEmpty) {
            if (bAppToggleOn) {
                app.behaviour().clearAdapter();
                bAppToggleOn = false;
            } else {
                AppProvider appProvider = app.getDataHandler().getAppProvider();
                List<? extends EntryItem> list;
                list = appProvider != null ? appProvider.getPojos() : null;
                if (list != null)
                    app.behaviour().updateAdapter(list, false);
                bAppToggleOn = true;
            }
            // updateAdapter will change `bAdapterEmpty` and we change it back
            bAdapterEmpty = true;
        } else if (bAppToggleOn) {
            bAppToggleOn = false;
            app.behaviour().filterResults(null);
        } else {
            bAppToggleOn = true;
            bContactToggleOn = false;
            app.behaviour().filterResults(AppEntry.SCHEME);
        }
    }

    private void toggleContacts(View v) {
        TBApplication app = TBApplication.getApplication(v.getContext());
        if (bAdapterEmpty) {
            if (bContactToggleOn) {
                app.behaviour().clearAdapter();
                bContactToggleOn = false;
            } else {
                ContactsProvider contactsProvider = app.getDataHandler().getContactsProvider();
                List<? extends EntryItem> list;
                list = contactsProvider != null ? contactsProvider.getPojos() : null;
                if (list != null)
                    app.behaviour().updateAdapter(list, false);
                bContactToggleOn = true;
            }
            // updateAdapter will change `bAdapterEmpty` and we change it back
            bAdapterEmpty = true;
        } else if (bContactToggleOn) {
            bContactToggleOn = false;
            app.behaviour().filterResults(null);
        } else {
            bAppToggleOn = false;
            bContactToggleOn = true;
            app.behaviour().filterResults(ContactEntry.SCHEME);
        }
    }

    private boolean isQuickListEnabled() {
        return mIsEnabled;
    }

    public void showQuickList() {
        if (isQuickListEnabled()) {
            mQuickList.setVisibility(View.VISIBLE);
            mQuickList.animate()
                    .scaleY(1f)
                    .setListener(null)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        }
    }

    public void hideQuickList() {
        if (isQuickListEnabled()) {
            mQuickList.setVisibility(View.VISIBLE);
            mQuickList.animate()
                    .scaleY(0f)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mQuickList.setVisibility(View.GONE);
                        }
                    })
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        } else {
            mQuickList.setVisibility(View.GONE);
        }
    }

    public void onResume(SharedPreferences pref) {
        Resources resources = mQuickList.getResources();
        mIsEnabled = pref.getBoolean("quick-list-enabled", true);

        // size
        int percent = pref.getInt("quick-list-size", 0);

        // set layout height
        {
            int smallSize = resources.getDimensionPixelSize(R.dimen.bar_height);
            int largeSize = resources.getDimensionPixelSize(R.dimen.large_bar_height);
            ViewGroup.LayoutParams params = mQuickList.getLayoutParams();
            if (params instanceof LinearLayout.LayoutParams) {
                params.height = smallSize + (largeSize - smallSize) * percent / 100;
                mQuickList.setLayoutParams(params);
            } else {
                throw new IllegalStateException("mSearchBarContainer has the wrong layout params");
            }
        }

        int color = UIColors.getColor(pref, "quick-list-color");
        int alpha = UIColors.getAlpha(pref, "quick-list-alpha");

        // rounded drawable
        PaintDrawable drawable = new PaintDrawable();
        drawable.getPaint().setColor(UIColors.setAlpha(color, alpha));
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mQuickList.getLayoutParams();
        drawable.setCornerRadius(getContext().getResources().getDimension(R.dimen.bar_corner_radius));
        mQuickList.setBackground(drawable);
        int margin = (int) (params.height * .25f);
        params.setMargins(margin, 0, margin, margin);
    }

    public void adapterCleared() {
        bAppToggleOn = false;
        bContactToggleOn = false;

        bAdapterEmpty = true;
    }

    public void adapterUpdated() {
        bAppToggleOn = false;
        bContactToggleOn = false;

        bAdapterEmpty = false;
    }
}
