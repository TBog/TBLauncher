package rocks.tbog.tblauncher.result;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.ui.KeyboardScrollHider;

public class RecycleScrollListener extends RecyclerView.OnScrollListener {
    private static final String TAG = "RScrL";
    private final KeyboardScrollHider.KeyboardHandler handler;
    private int mScrollAmountY = 0;
    private int mHideKeyboardThreshold = 0;
    private int mKeyboardHeight = -1;
    private int mListHeight = -1;

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
        if (newState != RecyclerView.SCROLL_STATE_DRAGGING) {
            if (recyclerView.getLayoutParams().height != ViewGroup.LayoutParams.MATCH_PARENT) {
                ValueAnimator anim = ValueAnimator.ofInt(recyclerView.getHeight(), ((View) recyclerView.getParent()).getHeight());
                anim.setDuration(recyclerView.getResources().getInteger(android.R.integer.config_longAnimTime));
                anim.addUpdateListener(animation -> {
                    int h = (int) animation.getAnimatedValue();
                    setListLayoutHeight(recyclerView, h);
                });
                anim.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        setListLayoutHeight(recyclerView, ViewGroup.LayoutParams.MATCH_PARENT);
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
        }
    }

    @Override
    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        int state = recyclerView.getScrollState();
        if (state == 0 && dx == 0 && dy == 0) {
            // list got updated
            mScrollAmountY = 0;
            mKeyboardHeight = getKeyboardHeight(recyclerView);
            mListHeight = recyclerView.getHeight();
        }
        mScrollAmountY -= dy;
        Log.d(TAG, "state=" + state + " scrollY=" + mScrollAmountY);

        if (mScrollAmountY > mHideKeyboardThreshold) {
            int containerHeight = ((View) recyclerView.getParent()).getHeight();
            final int listHeight;
            if ((mListHeight + mScrollAmountY) < containerHeight)
                listHeight = mListHeight + mScrollAmountY;
            else
                listHeight = ViewGroup.LayoutParams.MATCH_PARENT;
            setListLayoutHeight(recyclerView, listHeight);
            if (recyclerView.getLayoutParams().height == ViewGroup.LayoutParams.MATCH_PARENT) {
                handler.hideKeyboard();
                recyclerView.post(() -> setListLayoutHeight(recyclerView, listHeight));
            }
        }
    }

    private void setListLayoutHeight(ViewGroup list, int height) {
        final ViewGroup.LayoutParams params = list.getLayoutParams();
        if (params.height != height) {
            //Log.i(TAG, "height=" + height + " scroll=" + this.list.getScrollY());
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
