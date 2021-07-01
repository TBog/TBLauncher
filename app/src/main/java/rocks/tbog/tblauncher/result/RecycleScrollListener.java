package rocks.tbog.tblauncher.result;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.ui.KeyboardHandler;
import rocks.tbog.tblauncher.ui.RecyclerList;
import rocks.tbog.tblauncher.ui.WindowInsetsHelper;
import rocks.tbog.tblauncher.utils.DebugInfo;

public class RecycleScrollListener extends RecyclerView.OnScrollListener {
    private static final String TAG = "RScrL";
    private final KeyboardHandler handler;
    private int mScrollAmountY = 0;
    private int mHideKeyboardThreshold = -1;
    private int mListHeight = -1;
    // list resize after keyboard hidden has finished
    private boolean mResizeFinished = false;
    // keyboard hide requested
    private boolean mResizeInProgress = false;
    // waiting for the keyboard to set window insets
    private boolean mWaitForInsets = false;

    public RecycleScrollListener(KeyboardHandler handler) {
        this.handler = handler;
    }

    @Override
    public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
        if (mHideKeyboardThreshold == -1) {
            if (recyclerView.getLayoutManager() instanceof RecycleListLayoutManager) {
                final int lastItem = ((RecycleListLayoutManager) recyclerView.getLayoutManager()).findLastVisibleItemPosition();
                RecyclerView.ViewHolder vh = recyclerView.findViewHolderForAdapterPosition(lastItem);
                int lastItemHeight = vh != null ? vh.itemView.getHeight() : 0;
                if (lastItemHeight == 0)
                    lastItemHeight = recyclerView.getResources().getDimensionPixelSize(R.dimen.icon_size);
                mHideKeyboardThreshold = lastItemHeight * 3 / 5;
            } else {
                int itemHeight = recyclerView.getResources().getDimensionPixelSize(R.dimen.icon_size);
                mHideKeyboardThreshold = itemHeight * 3 / 5;
            }
        }
        Log.d(TAG, "scroll state=" + scrollStateString(newState));
        if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
            mResizeFinished = false;
            mResizeInProgress = false;
            mWaitForInsets = false;
            mListHeight = recyclerView.getHeight();
            int keyboardHeight = WindowInsetsHelper.getKeyboardHeight(recyclerView);
            boolean keyboardVisible = WindowInsetsHelper.isKeyboardVisible(recyclerView);
            Log.i(TAG, "start drag; keyboardHeight=" + keyboardHeight + " keyboardVisible=" + keyboardVisible + " mListHeight=" + mListHeight);
            if (DebugInfo.keyboardScrollHiderTouch(recyclerView.getContext()))
                recyclerView.setBackgroundColor(0x80ffd700);

            // if keyboard is already hidden, we are done
            if (!WindowInsetsHelper.isKeyboardVisible(recyclerView))
                handleResizeDone(recyclerView);
        } else if (newState == RecyclerView.SCROLL_STATE_IDLE) {
            if (mResizeInProgress && !mResizeFinished) {
                mResizeInProgress = false;
                final int fromValue = recyclerView.getHeight();
                final int toValue = ((View) recyclerView.getParent()).getHeight();
                if (fromValue == toValue) {
                    handleResizeDone(recyclerView);
                } else {
                    ValueAnimator anim = ValueAnimator.ofInt(fromValue, toValue);
                    anim.setInterpolator(new DecelerateInterpolator());
                    anim.setDuration(recyclerView.getResources().getInteger(android.R.integer.config_longAnimTime));
                    anim.addUpdateListener(animation -> {
                        int h = (int) animation.getAnimatedValue();
                        setListLayoutHeight(recyclerView, h);
                    });
                    anim.addListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            if (recyclerView instanceof RecyclerList)
                                ((RecyclerList) recyclerView).blockTouchEvents();
                            if (DebugInfo.keyboardScrollHiderTouch(recyclerView.getContext()))
                                recyclerView.setBackgroundColor(0x800000ff);

                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            handleResizeDone(recyclerView);
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {
                            // not happening
                        }

                        @Override
                        public void onAnimationRepeat(Animator animation) {
                            // not happening
                        }
                    });
                    anim.start();
                }
            }
        }
    }

    @NonNull
    private static String scrollStateString(int state) {
        switch (state) {
            case RecyclerView.SCROLL_STATE_IDLE:
                return "idle";
            case RecyclerView.SCROLL_STATE_DRAGGING:
                return "drag";
            case RecyclerView.SCROLL_STATE_SETTLING:
                return "sett";
            default:
                return "undef";
        }
    }

    @Override
    public void onScrolled(@NonNull RecyclerView list, int dx, int dy) {
        int state = list.getScrollState();
        if (state == RecyclerView.SCROLL_STATE_IDLE && dx == 0 && dy == 0) {
            // list got updated
            mScrollAmountY = 0;
            Log.i(TAG, "scroll reset");
        }
        if (mResizeFinished || state == RecyclerView.SCROLL_STATE_IDLE)
            return;

        mScrollAmountY -= dy;
        final boolean keyboardVisible = WindowInsetsHelper.isKeyboardVisible(list);
        Log.d(TAG, "state=" + scrollStateString(state) + " scrollY=" + mScrollAmountY + " KV=" + keyboardVisible + " waitForInsets=" + mWaitForInsets + " resizeInProgress=" + mResizeInProgress + " resizeFinished=" + mResizeFinished);

        // if scrolled enough to trigger the keyboard hiding
        if (mScrollAmountY > mHideKeyboardThreshold) {
            int containerHeight = ((View) list.getParent()).getHeight();
            Log.d(TAG, "containerHeight=" + containerHeight);

            if (!mWaitForInsets && mResizeInProgress) {
                // resize list to keep items under the dragging finger
                int height = Math.min(mListHeight + mScrollAmountY, containerHeight);
                if (height == containerHeight)
                    handleResizeDone(list);
                else
                    setListLayoutHeight(list, height);
            } else if ((mListHeight + mScrollAmountY) >= containerHeight) {
                hideKeyboardWhileDragging(list);
            }
        }
    }

    private void hideKeyboardWhileDragging(RecyclerView list) {
        if (mWaitForInsets) {
            int keyboardHeight = WindowInsetsHelper.getKeyboardHeight(list);
            Log.d(TAG, "onScrolled called while mWaitForInsets=true and keyboard height=" + keyboardHeight);
        } else {
            // start the resize process (hide keyboard)
            mWaitForInsets = true;
            mResizeInProgress = true;
            handler.hideKeyboard();

            ViewTreeObserver vto = list.getViewTreeObserver();
            vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    int keyboardHeight = WindowInsetsHelper.getKeyboardHeight(list);
                    boolean keyboardVisible = WindowInsetsHelper.isKeyboardVisible(list);
                    if (keyboardVisible) {
                        Log.d(TAG, "onPreDraw called with keyboard (height=" + keyboardHeight + " visible=" + keyboardVisible + ")");

                        // proceed with the current drawing pass, waiting for the keyboard to close
                        return true;
                    }
                    ViewTreeObserver vto = list.getViewTreeObserver();
                    vto.removeOnPreDrawListener(this);
                    if (!mWaitForInsets || mResizeFinished) {
                        Log.d(TAG, "onPreDraw called when mWaitForInsets=" + mWaitForInsets + " mResizeFinished=" + mResizeFinished);
                        return true;
                    }
                    if (list.getScrollState() == RecyclerView.SCROLL_STATE_IDLE) {
                        handleResizeDone(list);
                    } else {
                        mWaitForInsets = false;
                        int containerHeight = ((View) list.getParent()).getHeight();
                        int height = Math.min(mListHeight + mScrollAmountY, containerHeight);
                        Log.d(TAG, "onPreDraw height=min(" + (mListHeight + mScrollAmountY) + ", " + containerHeight + ")");
                        setListLayoutHeight(list, height);

                        if (DebugInfo.keyboardScrollHiderTouch(list.getContext()))
                            list.setBackgroundColor(0x8000ff00);

                        // request new pass
                        return false;
                    }
                    // proceed with the current drawing pass
                    return true;
                }
            });
        }
    }

    private void handleResizeDone(@NonNull RecyclerView list) {
        if (mResizeFinished) {
            return;
        }
        mScrollAmountY = 0;
        Log.i(TAG, "resize finished.");
        if (DebugInfo.keyboardScrollHiderTouch(list.getContext()))
            list.setBackgroundColor(0x00000000);

        // Give the list view the control over it's input back
        if (list instanceof RecyclerList)
            ((RecyclerList) list).unblockTouchEvents();

        setListLayoutHeight(list, ViewGroup.LayoutParams.MATCH_PARENT);

        mResizeFinished = true;
    }

    public static void setListLayoutHeight(ViewGroup list, int height) {
        final ViewGroup.LayoutParams params = list.getLayoutParams();
        if (params.height != height) {
            Log.d(TAG, "set layout height " + (height == ViewGroup.LayoutParams.MATCH_PARENT ? "MATCH_PARENT" : height));
            params.height = height;
            list.setLayoutParams(params);
            list.forceLayout();
        }
    }
}
