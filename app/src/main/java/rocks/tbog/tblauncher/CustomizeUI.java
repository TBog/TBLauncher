package rocks.tbog.tblauncher;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.provider.Settings;
import android.text.InputType;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import rocks.tbog.tblauncher.ui.RecyclerList;
import rocks.tbog.tblauncher.ui.SearchEditText;
import rocks.tbog.tblauncher.utils.EdgeGlowHelper;
import rocks.tbog.tblauncher.utils.PrefCache;
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

    /**
     * InputType that behaves as if the consuming IME is a standard-obeying
     * soft-keyboard
     * <p>
     * *Auto Complete* means "we're handling auto-completion ourselves". Then
     * we ignore whatever the IME thinks we should display.
     */
    private final static int INPUT_TYPE_STANDARD = InputType.TYPE_CLASS_TEXT
        | InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE
        | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
    /**
     * InputType that behaves as if the consuming IME is SwiftKey
     * <p>
     * *Visible Password* fields will break many non-Latin IMEs and may show
     * unexpected behaviour in numerous ways. (#454, #517)
     */
    private final static int INPUT_TYPE_WORKAROUND = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT;

    private final View.OnLayoutChangeListener updateResultFadeOut = new UpdateResultFadeOut();

    private static final class UpdateResultFadeOut implements View.OnLayoutChangeListener {
        @Override
        public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
            SharedPreferences pref = TBApplication.getApplication(v.getContext()).preferences();
            boolean fadeOut = PrefCache.getResultFadeOut(pref);
            if (!fadeOut) {
                v.removeOnLayoutChangeListener(this);
                return;
            }
            int oldHeight = oldBottom - oldTop;
            if (oldHeight != v.getHeight()) {
                int backgroundColor = UIColors.getResultListBackground(pref);
                if (!setResultListGradientFade(v, backgroundColor))
                    v.removeOnLayoutChangeListener(this);
            }
        }
    }

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
    }

    public void onStart() {
        setSearchBarPref();
        View resultLayout = findViewById(R.id.resultLayout);
        setResultListPref(resultLayout, true);
        resultLayout.addOnLayoutChangeListener(updateResultFadeOut);
        updateResultFadeOut.onLayoutChange(resultLayout,
            resultLayout.getLeft(), resultLayout.getTop(), resultLayout.getRight(), resultLayout.getBottom(),
            0, 0, 0, 0);
        adjustInputType(mSearchBar);
        setNotificationBarColor();
    }

    private void setNotificationBarColor() {
        int argb = UIColors.getColor(mPref, "notification-bar-argb");
        boolean gradient = mPref.getBoolean("notification-bar-gradient", true);

        if (gradient) {
            int size = UISizes.getStatusBarSize(getContext());
            ViewGroup.LayoutParams params = mNotificationBackground.getLayoutParams();
            if (params != null) {
                params.height = size;
                mNotificationBackground.setLayoutParams(params);
            }
            Utilities.setColorFilterMultiply(mNotificationBackground, argb);
            UIColors.setStatusBarColor(mTBLauncherActivity, 0x00000000);
        } else {
            mNotificationBackground.setVisibility(View.GONE);
            UIColors.setStatusBarColor(mTBLauncherActivity, argb);
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
        final Context ctx = getContext();
        final Resources resources = mSearchBarContainer.getResources();

        // size
        int barHeight = mPref.getInt("search-bar-height", 0);
        if (barHeight <= 1)
            barHeight = resources.getInteger(R.integer.default_search_bar_height);
        barHeight = UISizes.dp2px(ctx, barHeight);
        int textSize = mPref.getInt("search-bar-text-size", 0);
        if (textSize <= 1)
            textSize = resources.getInteger(R.integer.default_size_text);

        // layout height and margins
        {
            ViewGroup.LayoutParams params = mSearchBarContainer.getLayoutParams();
            if (params instanceof ViewGroup.MarginLayoutParams) {
                params.height = barHeight;
                int hMargin = UISizes.getSearchBarMarginHorizontal(ctx);
                int vMargin = UISizes.getSearchBarMarginVertical(ctx);
                ((ViewGroup.MarginLayoutParams)params).setMargins(hMargin, vMargin, hMargin, vMargin);
                mSearchBarContainer.setLayoutParams(params);
            } else {
                throw new IllegalStateException("mSearchBarContainer has the wrong layout params");
            }
        }

        // text size
        {
            mSearchBar.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSize);
        }

        final int searchBarRipple = UIColors.setAlpha(UIColors.getColor(mPref, "search-bar-ripple-color"), 0xFF);
        final int searchIconColor = UIColors.setAlpha(UIColors.getColor(mPref, "search-bar-icon-color"), 0xFF);
        final int argbBackground = UIColors.getColor(mPref, "search-bar-argb");

        // text color
        {
            int searchTextCursor = UIColors.getColor(mPref, "search-bar-cursor-argb");
            int searchTextHighlight = UIColors.setAlpha(searchTextCursor, 0x7F);
            int searchTextColor = UIColors.getSearchTextColor(ctx);
            int searchHintColor = UIColors.setAlpha(searchTextColor, 0xBB);

            mSearchBar.setTextColor(searchTextColor);
            mSearchBar.setHintTextColor(searchHintColor);
            // set color for selection background
            mSearchBar.setHighlightColor(searchTextHighlight);

            Utilities.setTextCursorColor(mSearchBar, searchTextCursor);
            Utilities.setTextSelectHandleColor(mSearchBar, searchBarRipple);
        }

        // icon color
        {
            Utilities.setColorFilterMultiply(mLauncherButton, searchIconColor);
            Utilities.setColorFilterMultiply(mMenuButton, searchIconColor);
            Utilities.setColorFilterMultiply(mClearButton, searchIconColor);
            mLauncherButton.setBackground(getSelectorDrawable(mLauncherButton, searchBarRipple, true));
            mMenuButton.setBackground(getSelectorDrawable(mMenuButton, searchBarRipple, true));
            mClearButton.setBackground(getSelectorDrawable(mClearButton, searchBarRipple, true));
        }

        // set background
        boolean isGradient = mPref.getBoolean("search-bar-gradient", true);
        int cornerRadius = UISizes.getSearchBarRadius(ctx);
        if (isGradient || cornerRadius > 0) {
            GradientDrawable drawable;
            if (isGradient) {
                final GradientDrawable.Orientation orientation;
                if (PrefCache.searchBarAtBottom(mPref))
                    orientation = GradientDrawable.Orientation.TOP_BOTTOM;
                else
                    orientation = GradientDrawable.Orientation.BOTTOM_TOP;
                int alpha = Color.alpha(argbBackground);
                int c1 = UIColors.setAlpha(argbBackground, 0);
                int c2 = UIColors.setAlpha(argbBackground, alpha * 3 / 4);
                int c3 = UIColors.setAlpha(argbBackground, alpha);
                drawable = new GradientDrawable(orientation, new int[]{c1, c2, c3});
            } else {
                drawable = new GradientDrawable();
                drawable.setColor(argbBackground);
            }
            drawable.setCornerRadius(cornerRadius);
            mSearchBarContainer.setBackground(drawable);
        } else {
            mSearchBarContainer.setBackground(new ColorDrawable(argbBackground));
        }
    }

    public static void setResultListPref(View resultLayout) {
        setResultListPref(resultLayout, false);
    }

    private static boolean setResultListGradientFade(@NonNull View resultLayout, int backgroundColor) {
        Drawable bg = resultLayout.getBackground();
        if (bg instanceof GradientDrawable) {
            GradientDrawable drawable = (GradientDrawable) bg;
            drawable.setGradientType(GradientDrawable.LINEAR_GRADIENT);
            drawable.setOrientation(GradientDrawable.Orientation.TOP_BOTTOM);

            int color = backgroundColor & 0x00ffffff;
            int alpha = Color.alpha(backgroundColor);
            int c1 = UIColors.setAlpha(color, 0);
            int c2 = UIColors.setAlpha(color, alpha * 3 / 4);
            int c3 = UIColors.setAlpha(color, alpha);
            // compute fade percentage of height
            float p = UISizes.getResultIconSize(resultLayout.getContext()) * .5f / resultLayout.getHeight();
            // if height is too small, fade only on 66% of the height (33% fade in and 33% fade out)
            p = Math.min(p, .33f);
            return Utilities.setGradientDrawableColors(drawable,
                new int[]{c1, c2, c3, c3, c2, c1},
                new float[]{0f, p * .5f, p, 1f - p, 1f - (p * .5f), 1f});
        }
        return false;
    }

    public static void setResultListPref(View resultLayout, boolean setMargin) {
        Context ctx = resultLayout.getContext();
        SharedPreferences pref = TBApplication.getApplication(ctx).preferences();

        if (setMargin) {
            ViewGroup.LayoutParams params = resultLayout.getLayoutParams();
            if (params instanceof ViewGroup.MarginLayoutParams) {
                int hMargin = UISizes.getResultListMarginHorizontal(ctx);
                int vMargin = UISizes.getResultListMarginVertical(ctx);
                ((ViewGroup.MarginLayoutParams) params).setMargins(hMargin, vMargin, hMargin, vMargin);
            }
        }

        boolean fadeOut = PrefCache.getResultFadeOut(pref);
        int backgroundColor = UIColors.getResultListBackground(pref);
        int cornerRadius = UISizes.getResultListRadius(ctx);
        if (cornerRadius > 0 || fadeOut) {
            final GradientDrawable drawable = new GradientDrawable();
            drawable.setCornerRadius(cornerRadius);
            resultLayout.setBackground(drawable);
            if (fadeOut)
                setResultListGradientFade(resultLayout, backgroundColor);
            else
                drawable.setColor(backgroundColor);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // clip list content to rounded corners
                resultLayout.setClipToOutline(true);
            }
        } else {
            resultLayout.setBackgroundColor(backgroundColor);
        }

        int overscrollColor = UIColors.getResultListRipple(ctx);
        overscrollColor = UIColors.setAlpha(overscrollColor, 0x7F);
        if (resultLayout instanceof AbsListView) {
            setListViewSelectorPref((AbsListView) resultLayout, true);
            setListViewScrollbarPref(resultLayout);
            EdgeGlowHelper.setEdgeGlowColor((AbsListView) resultLayout, overscrollColor);
            if (setMargin)
                setFadingEdge(resultLayout, fadeOut);
        } else {
            View list = resultLayout.findViewById(R.id.resultList);
            if (list instanceof AbsListView) {
                setListViewSelectorPref((AbsListView) list, false);
                setListViewScrollbarPref(list);
                EdgeGlowHelper.setEdgeGlowColor((AbsListView) list, overscrollColor);
            } else if (list instanceof RecyclerList) {
                setListViewScrollbarPref(list);
                EdgeGlowHelper.setEdgeGlowColor((RecyclerList) list, overscrollColor);
            }
            if (setMargin)
                setFadingEdge(list, fadeOut);
        }
    }

    private static void setFadingEdge(@Nullable View view, boolean enabled) {
        if (view == null)
            return;
        if (enabled)
            view.setFadingEdgeLength(UISizes.getResultIconSize(view.getContext()));
        view.setVerticalFadingEdgeEnabled(enabled);
    }

    private void adjustInputType(EditText searchEditText) {
        int currentInputType = searchEditText.getInputType();
        int requiredInputType;

        if (isSuggestionsEnabled()) {
            requiredInputType = InputType.TYPE_CLASS_TEXT;
        } else {
            if (isNonCompliantKeyboard()) {
                requiredInputType = INPUT_TYPE_WORKAROUND;
            } else {
                requiredInputType = INPUT_TYPE_STANDARD;
            }
        }

        if (currentInputType != requiredInputType) {
            searchEditText.setInputType(requiredInputType);
        }
    }

    public static void setListViewSelectorPref(AbsListView listView, boolean borderless) {
        int touchColor = UIColors.getResultListRipple(listView.getContext());
        Drawable selector = getSelectorDrawable(listView, touchColor, borderless);
        listView.setSelector(selector);
    }

    public static Drawable getSelectorDrawable(View view, int color, boolean borderless) {
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

    public static void setListViewScrollbarPref(View listView) {
        int color = UIColors.getResultListRipple(listView.getContext());
        setListViewScrollbarPref(listView, UIColors.setAlpha(color, 0x7F));
    }

    public static void setListViewScrollbarPref(View listView, int color) {
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{color, color});
        drawable.setCornerRadius(UISizes.dp2px(listView.getContext(), 3));
        drawable.setSize(UISizes.dp2px(listView.getContext(), 4), drawable.getIntrinsicHeight());

        Utilities.setVerticalScrollbarThumbDrawable(listView, drawable);
    }

    public Context getContext() {
        return mTBLauncherActivity;
    }

    @NonNull
    public static Drawable getPopupBackgroundDrawable(@NonNull Context ctx) {
        int border = UISizes.dp2px(ctx, 1);
        int radius = UISizes.getPopupCornerRadius(ctx);

        GradientDrawable gradient = new GradientDrawable();
        gradient.setCornerRadius(radius);
        gradient.setStroke(border, UIColors.getPopupBorderColor(ctx));
        gradient.setColor(UIColors.getPopupBackgroundColor(ctx));

        return gradient;
    }

    public static Drawable getDialogButtonBarBackgroundDrawable(@NonNull Resources.Theme theme) {
        TypedValue typedValue = new TypedValue();
        if (theme.resolveAttribute(android.R.attr.buttonBarStyle, typedValue, true)) {
            TypedArray a = theme.obtainStyledAttributes(typedValue.resourceId, new int[]{android.R.attr.background});
            Drawable background = a.getDrawable(0);
            a.recycle();
            return background;
        }

        return null;
    }

    /**
     * Should we force the keyboard not to display suggestions?
     * (swiftkey is broken, see https://github.com/Neamar/KISS/issues/44)
     * (same for flesky: https://github.com/Neamar/KISS/issues/1263)
     */
    private boolean isNonCompliantKeyboard() {
        String currentKeyboard = Settings.Secure.getString(mTBLauncherActivity.getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD).toLowerCase();
        return currentKeyboard.contains("swiftkey") || currentKeyboard.contains("flesky") || currentKeyboard.endsWith(".latinime");
    }

    /**
     * Should the keyboard autocomplete and suggest options
     */
    private boolean isSuggestionsEnabled() {
        return mPref.getBoolean("enable-suggestions-keyboard", false);
    }
}
