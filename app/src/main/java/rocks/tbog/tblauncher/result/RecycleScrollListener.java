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
import rocks.tbog.tblauncher.ui.KeyboardScrollHider;
import rocks.tbog.tblauncher.ui.RecyclerList;

public class RecycleScrollListener extends RecyclerView.OnScrollListener {
    private static final String TAG = "RScrL";
    private final KeyboardScrollHider.KeyboardHandler handler;
    private int mScrollAmountY = 0;
    private int mHideKeyboardThreshold = 0;
    private int mKeyboardHeight = -1;
    private int mListHeight = -1;
    private boolean mResizeFinished = true;
    private boolean mResizeInProgress = false;

    public RecycleScrollListener(KeyboardScrollHider.KeyboardHandler handler) {
        this.handler = handler;
    }

    @Override
    public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
        int lastItem = -1;
        if (recyclerView.getLayoutManager() instanceof RecycleListLayoutManager) {
            lastItem = ((RecycleListLayoutManager) recyclerView.getLayoutManager()).findLastVisibleItemPosition();
            RecyclerView.ViewHolder vh = recyclerView.findViewHolderForAdapterPosition(lastItem);
            int lastItemHeight = vh != null ? vh.itemView.getHeight() : 0;
            if (lastItemHeight == 0)
                lastItemHeight = recyclerView.getResources().getDimensionPixelSize(R.dimen.icon_size);
            mHideKeyboardThreshold = lastItemHeight * 3 / 5;
        }
        Log.d(TAG, "scroll state=" + newState + " last=" + lastItem);
        if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
            mResizeFinished = false;
            mResizeInProgress = false;
            mKeyboardHeight = getKeyboardHeight(recyclerView);
            mListHeight = recyclerView.getHeight();
            Log.d(TAG, "start drag; mKeyboardHeight=" + mKeyboardHeight + " mListHeight=" + mListHeight);
        } else {
            if (mResizeInProgress && !mResizeFinished) {
                mResizeInProgress = false;
                ValueAnimator anim = ValueAnimator.ofInt(recyclerView.getHeight(), ((View) recyclerView.getParent()).getHeight());
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
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (recyclerView instanceof RecyclerList)
                            ((RecyclerList) recyclerView).unblockTouchEvents();
                        setListLayoutHeight(recyclerView, ViewGroup.LayoutParams.MATCH_PARENT);
                        mResizeFinished = true;
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });
                anim.start();
            }
            mResizeFinished = true;
        }
    }

    @Override
    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        int state = recyclerView.getScrollState();
        if (state == RecyclerView.SCROLL_STATE_IDLE && dx == 0 && dy == 0) {
            // list got updated
            mScrollAmountY = 0;
//            mKeyboardHeight = getKeyboardHeight(recyclerView);
//            mListHeight = recyclerView.getHeight();
//            Log.d(TAG, "list updated; mKeyboardHeight=" + mKeyboardHeight + " mListHeight=" + mListHeight);
        }
        if (mResizeFinished || state != RecyclerView.SCROLL_STATE_DRAGGING)
            return;

        mScrollAmountY -= dy;
        Log.d(TAG, "state=" + state + " scrollY=" + mScrollAmountY);

        if (mScrollAmountY > mHideKeyboardThreshold) {
            int containerHeight = ((View) recyclerView.getParent()).getHeight();
            Log.d(TAG, "containerHeight=" + containerHeight);
            int listHeight = ViewGroup.LayoutParams.MATCH_PARENT;
            if ((mListHeight + mScrollAmountY) >= containerHeight) {
                // if list height is MATCH_PARENT then start the resize process (hide keyboard)
                if (!mResizeInProgress && recyclerView.getLayoutParams().height == ViewGroup.LayoutParams.MATCH_PARENT) {
                    mResizeInProgress = true;
                    handler.hideKeyboard();

                    ViewTreeObserver vto = recyclerView.getViewTreeObserver();
                    vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            vto.removeOnGlobalLayoutListener(this);
                            if (!mResizeFinished && recyclerView.getScrollState() == RecyclerView.SCROLL_STATE_DRAGGING) {
                                mResizeInProgress = true;
                                int height = Math.min(mListHeight + mScrollAmountY, ((View) recyclerView.getParent()).getHeight());
                                setListLayoutHeight(recyclerView, height);
                            }
                        }
                    });
                } else if (recyclerView.getLayoutParams().height != ViewGroup.LayoutParams.MATCH_PARENT) {
                    // list reached container size, stop resizing
                    mResizeFinished = true;
                    Log.d(TAG, "resize finished");
                    listHeight = ViewGroup.LayoutParams.MATCH_PARENT;
                }
            } else {
                listHeight = mListHeight + mScrollAmountY;
            }
            //if (mResizeInProgress)
            setListLayoutHeight(recyclerView, listHeight);
        }
    }

    private void setListLayoutHeight(ViewGroup list, int height) {
        final ViewGroup.LayoutParams params = list.getLayoutParams();
        if (params.height != height) {
            Log.i(TAG, "set layout height " + height);
            params.height = height;
            list.setLayoutParams(params);
            list.forceLayout();
        }
    }

    @NonNull
    private View getRootView(ViewGroup list) {
        // we need a root view that has `android:fitsSystemWindows="true"` to find the height of the keyboard
        ViewGroup rootView = (ViewGroup) list.getRootView();
        ViewGroup rootLayout = rootView.findViewById(R.id.root_layout);
        // child 0 is `R.id.notificationBackground`
        // child 1 is a full-screen ViewGroup that has `android:fitsSystemWindows="true"`
        return rootLayout.getChildAt(1);
    }

    private int getKeyboardHeight(ViewGroup list) {
        // we need a root view that has `android:fitsSystemWindows="true"` to find the height of the keyboard
        return getRootView(list).getPaddingBottom();
    }
}
