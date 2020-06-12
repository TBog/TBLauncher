package rocks.tbog.tblauncher;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.List;

import rocks.tbog.tblauncher.dataprovider.Provider;
import rocks.tbog.tblauncher.entry.AppEntry;
import rocks.tbog.tblauncher.entry.ContactEntry;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.entry.ShortcutEntry;
import rocks.tbog.tblauncher.utils.UIColors;

public class QuickList {
    private TBLauncherActivity mTBLauncherActivity;
    private boolean mIsEnabled = true;
    private LinearLayout mQuickList;

    // bAdapterEmpty is true when no search results are displayed
    private boolean bAdapterEmpty = true;

    // is any filter activated?
    private boolean bFilterOn = false;

    // last filter scheme, used for better toggle behaviour
    private String mLastFilter = null;

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
            mQuickList.addView(filter);

            filter.setOnClickListener(v -> toggleFilter(v, TBApplication.getApplication(v.getContext()).getDataHandler().getAppProvider()));
            filter.setTag(R.id.tag_scheme, AppEntry.SCHEME);
        }
        // contacts filter
        {
            View filter = LayoutInflater.from(getContext()).inflate(R.layout.item_quick_list, mQuickList, false);
            Drawable drawable = ContextCompat.getDrawable(getContext(), R.drawable.ic_contact);
            ((ImageView) filter.findViewById(android.R.id.icon)).setImageDrawable(drawable);
            ((TextView) filter.findViewById(android.R.id.text1)).setText("Contacts");
            mQuickList.addView(filter);

            filter.setOnClickListener(v -> toggleFilter(v, TBApplication.getApplication(v.getContext()).getDataHandler().getContactsProvider()));
            filter.setTag(R.id.tag_scheme, ContactEntry.SCHEME);
        }
        // pinned shortcuts filter
        {
            View filter = LayoutInflater.from(getContext()).inflate(R.layout.item_quick_list, mQuickList, false);
            Drawable drawable = ContextCompat.getDrawable(getContext(), R.drawable.ic_send);
            ((ImageView) filter.findViewById(android.R.id.icon)).setImageDrawable(drawable);
            ((TextView) filter.findViewById(android.R.id.text1)).setText("Shortcuts");
            mQuickList.addView(filter);

            filter.setOnClickListener(v -> toggleFilter(v, TBApplication.getApplication(v.getContext()).getDataHandler().getShortcutsProvider()));
            filter.setTag(R.id.tag_scheme, ShortcutEntry.SCHEME);
        }
    }

    private void toggleFilter(View v, Provider<? extends EntryItem> provider) {
        Context ctx = v.getContext();
        TBApplication app = TBApplication.getApplication(ctx);

        // if there is no search we need to filter, just show all matching entries
        if (bAdapterEmpty) {
            if (bFilterOn && provider != null && mLastFilter == provider.getScheme()) {
                app.behaviour().clearAdapter();
                bFilterOn = false;
            } else {
                List<? extends EntryItem> list;
                list = provider != null ? provider.getPojos() : null;
                if (list != null) {
                    app.behaviour().updateAdapter(list, false);
                    mLastFilter = provider.getScheme();
                    bFilterOn = true;
                } else {
                    bFilterOn = false;
                }
            }
            // updateAdapter will change `bAdapterEmpty` and we change it back because we want
            // bAdapterEmpty to represent a search we need to filter
            bAdapterEmpty = true;
        } else if (bFilterOn && (provider == null || mLastFilter == provider.getScheme())) {
            animToggleOff();
            bFilterOn = false;
            app.behaviour().filterResults(null);
        } else if (provider != null) {
            animToggleOff();
            bFilterOn = true;
            mLastFilter = provider.getScheme();
            app.behaviour().filterResults(provider.getScheme());
        }

        // show what is currently toggled
        if (bFilterOn) {
            animToggleOn(v);
        }
    }

    private void animToggleOn(View v) {
        if (v.getTag(R.id.tag_anim) instanceof ValueAnimator) {
            ValueAnimator colorAnim = (ValueAnimator) v.getTag(R.id.tag_anim);
            colorAnim.start();
        } else {
            int colorTo = UIColors.getPrimaryColor(v.getContext());
            int colorFrom = v.getBackground() instanceof ColorDrawable ? ((ColorDrawable) v.getBackground()).getColor() : (colorTo & 0x00FFFFFF);
            ValueAnimator colorAnim = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
            colorAnim.addUpdateListener(animator -> v.setBackgroundColor((int) animator.getAnimatedValue()));
//            colorAnim.addListener(new AnimatorListenerAdapter() {
//                @Override
//                public void onAnimationEnd(Animator animation, boolean isReverse) {
//                    if (isReverse)
//                        mLastFilter = null;
//                }
//            });
            colorAnim.start();
            v.setTag(R.id.tag_anim, colorAnim);
        }
    }

    private void animToggleOff() {
        if (!bFilterOn)
            return;
        int n = mQuickList.getChildCount();
        for (int i = 0; i < n; i += 1) {
            View view = mQuickList.getChildAt(i);
            if (mLastFilter == null || mLastFilter == view.getTag(R.id.tag_scheme)) {
                if (view.getTag(R.id.tag_anim) instanceof ValueAnimator) {
                    ValueAnimator colorAnim = (ValueAnimator) view.getTag(R.id.tag_anim);
                    colorAnim.reverse();
                } else {
                    view.setBackgroundColor(UIColors.getPrimaryColor(mQuickList.getContext()) & 0x00FFFFFF);
                }
            }
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

    public void hideQuickList(boolean animate) {
        if (isQuickListEnabled()) {
            mLastFilter = null;
            animToggleOff();
            if (animate) {
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
                mQuickList.setScaleY(0f);
                mQuickList.setVisibility(View.GONE);
            }
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
        animToggleOff();
        bFilterOn = false;
        bAdapterEmpty = true;
    }

    public void adapterUpdated() {
        animToggleOff();
        bFilterOn = false;
        bAdapterEmpty = false;
    }
}
