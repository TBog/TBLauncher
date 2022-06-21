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
import rocks.tbog.tblauncher.utils.DebugString;

public class RecycleScrollListener extends RecyclerView.OnScrollListener implements CustomRecycleLayoutManager.OverScrollListener {
    private static final String TAG = "RScrL";
    private final KeyboardHandler handler;
    private int mScrollAmountY = 0;
    private int mHideKeyboardThreshold = -1;
    private int mListHeight = -1;
    private final State mState = new State();

    private static class State {
        // list resize after keyboard hidden has finished
        private boolean resizeFinished = false;
        // keyboard hide requested
        private boolean resizeInProgress = false;
        // waiting for the keyboard to set window insets
        private boolean waitForInsets = false;
        private boolean resizeWithScroll = false;

        public boolean resizeFinished() {
            return resizeFinished;
        }

        public boolean resizeInProgress() {
            return resizeInProgress && !resizeFinished;
        }

        public boolean resizeWithScroll() {
            return resizeWithScroll && !resizeFinished;
        }

        @NonNull
        @Override
        public String toString() {
            return "[resizeWithScroll=" + resizeWithScroll + " waitForInsets=" + waitForInsets + " resizeInProgress=" + resizeInProgress + " resizeFinished=" + resizeFinished + "]";
        }

        public void reset() {
            resizeFinished = false;
            resizeInProgress = false;
            waitForInsets = false;
            resizeWithScroll = false;
        }

        public void setResizeAnimation() {
            resizeInProgress = false;
        }

        public void setResizeWithScroll() {
            resizeWithScroll = true;
        }

        public void setResizeFinished() {
            resizeFinished = true;
        }
    }

    public RecycleScrollListener(KeyboardHandler handler) {
        this.handler = handler;
    }

    @Override
    public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
        if (mHideKeyboardThreshold == -1) {
            if (recyclerView.getLayoutManager() instanceof CustomRecycleLayoutManager) {
                final int lastItem = ((CustomRecycleLayoutManager) recyclerView.getLayoutManager()).findLastVisibleItemPosition();
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
            mState.reset();
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
            if (recyclerView.getLayoutManager() instanceof CustomRecycleLayoutManager) {
                int lastVisible = ((CustomRecycleLayoutManager) recyclerView.getLayoutManager()).findLastVisibleItemPosition();
                int itemCount = recyclerView.getAdapter() != null ? recyclerView.getAdapter().getItemCount() : 0;
                if (lastVisible < (itemCount - 1)) {
                    final int range = recyclerView.computeVerticalScrollRange();
                    final int extent = recyclerView.computeVerticalScrollExtent();
                    final int offset = recyclerView.computeVerticalScrollOffset();
                    Log.d(TAG, "lastVisible=" + (lastVisible + 1) + "/" + itemCount + " range=" + range + " extent=" + extent + " offset=" + offset);
                    mScrollAmountY = range - extent - offset;
                } else {
                    mScrollAmountY = 0;
                }
            }
            Log.i(TAG, "scrollY=" + mScrollAmountY);
            if (mState.resizeInProgress()) {
                mState.setResizeAnimation();
                int layoutParamsHeight = recyclerView.getLayoutParams().height;
                final int fromValue = layoutParamsHeight < 0 ? recyclerView.getHeight() : layoutParamsHeight;
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
                            if (DebugInfo.keyboardScrollHiderTouch(recyclerView.getContext()))
                                recyclerView.setBackgroundColor(0x800000ff);
                            setListAutoScroll(recyclerView, true);
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            handleResizeDone(recyclerView);
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {
                            handleResizeDone(recyclerView);
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
    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        if (!(recyclerView instanceof RecyclerList))
            return;
        final RecyclerList list = (RecyclerList) recyclerView;
        int state = list.getScrollState();
        if (mState.resizeFinished() || state == RecyclerView.SCROLL_STATE_IDLE)
            return;

        mScrollAmountY -= dy;
        final boolean keyboardVisible = WindowInsetsHelper.isKeyboardVisible(list);
        Log.d(TAG, "state=" + scrollStateString(state) + " scrollY=" + mScrollAmountY + " KV=" + keyboardVisible + " state=" + mState);

        // if resize while scrolling is active ... resize
        if (mState.resizeWithScroll()) {
            int containerHeight = ((View) list.getParent()).getHeight();
            int newHeight = Math.min(mListHeight + mScrollAmountY, containerHeight);
            Log.d(TAG, "onScrolled: resizeWithScroll newHeight=" + newHeight + " containerHeight=" + containerHeight);
            if (newHeight == containerHeight) {
                handleResizeDone(list);
            } else if (newHeight > list.getHeight()) {
                setListLayoutHeight(list, newHeight);
            }
            return;
        }

        // if scrolled enough to trigger the keyboard hiding
        if (mScrollAmountY > mHideKeyboardThreshold) {
            int containerHeight = ((View) list.getParent()).getHeight();
            Log.d(TAG, "containerHeight=" + containerHeight);

            if (!mState.waitForInsets && mState.resizeInProgress()) {
                // resize list to keep items under the dragging finger
                int newHeight = Math.min(mListHeight + mScrollAmountY, containerHeight);
                if (newHeight == containerHeight) {
                    handleResizeDone(list);
                } else {
                    setListLayoutHeight(list, newHeight);
                    // activate resize while scrolling
                    mState.setResizeWithScroll();
                    if (DebugInfo.keyboardScrollHiderTouch(list.getContext()))
                        list.setBackgroundColor(0x80ff0000);
                }
            } else if ((mListHeight + mScrollAmountY) >= containerHeight) {
                setListLayoutHeight(list, mListHeight);
                hideKeyboardWhileDragging(list);
                // let the Scroll Listener resize the list without any additional scrolling
                setListAutoScroll(list, false);
            }
        }
    }

    @Override
    public void onOverScroll(RecyclerView list, int amount) {
        if (mState.resizeFinished() || !WindowInsetsHelper.isKeyboardVisible(list)) {
            Log.i(TAG, "overscroll show keyboard with state=" + mState);
            handler.showKeyboard();
        }
    }

    private void hideKeyboardWhileDragging(RecyclerView list) {
        if (mState.waitForInsets) {
            int keyboardHeight = WindowInsetsHelper.getKeyboardHeight(list);
            Log.d(TAG, "onScrolled called while mWaitForInsets=true and keyboard height=" + keyboardHeight);
        } else {
            // start the resize process (hide keyboard)
            mState.waitForInsets = true;
            mState.resizeInProgress = true;
            handler.hideKeyboard();

            ViewTreeObserver vto = ((View) list.getParent()).getViewTreeObserver();
            vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    int keyboardHeight = WindowInsetsHelper.getKeyboardHeight(list);
                    boolean keyboardVisible = WindowInsetsHelper.isKeyboardVisible(list);
                    if (keyboardVisible) {
                        Log.d(TAG, "onPreDraw called with keyboard visible and height=" + keyboardHeight);

                        // proceed with the current drawing pass, waiting for the keyboard to close
                        return true;
                    }
                    ViewTreeObserver vto = ((View) list.getParent()).getViewTreeObserver();
                    vto.removeOnPreDrawListener(this);
                    if (!mState.waitForInsets || mState.resizeFinished()) {
                        Log.d(TAG, "onPreDraw called when mWaitForInsets=" + mState.waitForInsets + " mResizeFinished=" + mState.resizeFinished);
                        return true;
                    }
                    if (list.getScrollState() == RecyclerView.SCROLL_STATE_IDLE) {
                        handleResizeDone(list);
                    } else {
                        mState.waitForInsets = false;
                        int containerHeight = ((View) list.getParent()).getHeight();
                        int newHeight = Math.min(mListHeight + mScrollAmountY, containerHeight);
                        Log.d(TAG, "onPreDraw height=min(" + (mListHeight + mScrollAmountY) + ", " + containerHeight + ")");
                        setListLayoutHeight(list, newHeight);

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

    private void setListAutoScroll(@NonNull RecyclerView list, boolean value) {
        if (list.getLayoutManager() instanceof CustomRecycleLayoutManager)
            ((CustomRecycleLayoutManager) list.getLayoutManager()).setAutoScrollBottom(value);
    }

    private void handleResizeDone(@NonNull RecyclerView list) {
        if (mState.resizeFinished()) {
            return;
        }
        mScrollAmountY = 0;
        Log.i(TAG, "resize finished.");
        if (DebugInfo.keyboardScrollHiderTouch(list.getContext()))
            list.setBackgroundColor(0x00000000);

        setListAutoScroll(list, true);

        setListLayoutHeight(list, ViewGroup.LayoutParams.MATCH_PARENT);

        mState.setResizeFinished();
    }

    public static void setListLayoutHeight(ViewGroup list, int height) {
        final ViewGroup.LayoutParams params = list.getLayoutParams();
        if (params.height != height) {
            Log.d(TAG, "change layout height from " + DebugString.layoutParamSize(params.height) + " to " + DebugString.layoutParamSize(height));
            params.height = height;
            list.setLayoutParams(params);
            list.forceLayout();
        }
    }
}
