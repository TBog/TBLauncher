package rocks.tbog.tblauncher.ui;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.AbsListView;

import androidx.annotation.NonNull;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.utils.DebugInfo;

/**
 * Utility class for automatically hiding the keyboard when scrolling down a {@see ListView},
 * keeping the position of the finger on the list stable
 */
public class KeyboardScrollHider implements View.OnTouchListener {
    private static final String TAG = "KSH";

    private final KeyboardHandler handler;
    private final BlockableListView list;
    private final View listParent;
    private final BottomPullEffectView pullEffect;
    private final int resultItemHeight;
    private int listHeightInitial = 0;

    private float offsetYStart = 0;
    private float offsetYCurrent = 0;
    private int offsetYDiff = 0;

    //private MotionEvent lastMotionEvent;
    private float lastPosX = 0f;
    private int initialWindowPadding = 0;
    private boolean resizeDone = false;
    private boolean keyboardHidden = false;
    private boolean scrollBarEnabled = true;

    public KeyboardScrollHider(KeyboardHandler handler, BlockableListView list, BottomPullEffectView pullEffect) {
        this.handler = handler;
        this.list = list;
        this.listParent = (View) list.getParent();
        this.pullEffect = pullEffect;
        resultItemHeight = list.getResources().getDimensionPixelSize(R.dimen.icon_size) / 2;
    }

    /**
     * Start monitoring and intercepting touch events of the target list view and providing our
     * transformations
     */
    @SuppressLint("ClickableViewAccessibility")
    public void start() {
        this.list.setOnTouchListener(this);
    }

    /**
     *
     */
    @SuppressLint("ClickableViewAccessibility")
    @SuppressWarnings("unused")
    public void stop() {
        this.list.setOnTouchListener(null);
        this.handleResizeDone();
    }

    @NonNull
    private View getRootView() {
        // we need a root view that has `android:fitsSystemWindows="true"` to find the height of the keyboard
        ViewGroup rootView = (ViewGroup) this.list.getRootView();
        ViewGroup rootLayout = rootView.findViewById(R.id.root_layout);
        // child 0 is `R.id.notificationBackground`
        // child 1 is a full-screen ViewGroup that has `android:fitsSystemWindows="true"`
        return rootLayout.getChildAt(1);
    }

    private int getWindowPadding() {
        // we need a root view that has `android:fitsSystemWindows="true"` to find the height of the keyboard
        return getRootView().getPaddingBottom();
    }

    private int getWindowWidth() {
        return getRootView().getWidth();
    }

    private void setListLayoutHeight(int height) {
        final ViewGroup.LayoutParams params = this.list.getLayoutParams();
        if (params.height != height) {
            //Log.i(TAG, "height=" + height + " scroll=" + this.list.getScrollY());
            params.height = height;
            this.list.setLayoutParams(params);
            if (DebugInfo.keyboardScrollHiderTouch(list.getContext()))
                list.setBackgroundColor(0x80ffd700);
            this.list.forceLayout();
        }
    }

    private void handleResizeDone() {
        if (this.resizeDone) {
            return;
        }
        Log.i(TAG, "resize done.");

        this.list.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);

        // Give the list view the control over it's input back
        this.list.unblockTouchEvents();

        // Quickly fade out edge pull effect
        this.pullEffect.releasePull();

        // Make sure list uses the height of it's parent
        this.list.setVerticalScrollBarEnabled(this.scrollBarEnabled);
        this.setListLayoutHeight(ViewGroup.LayoutParams.MATCH_PARENT);

        this.list.post(() -> this.list.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_NORMAL));

        if (DebugInfo.keyboardScrollHiderTouch(list.getContext()))
            list.setBackgroundColor(0x00000000);
        this.resizeDone = true;
    }

    private void updateListViewHeight() {
        // Don't do anything if the window hasn't resized yet or if we're already done
        if (this.getWindowPadding() >= this.initialWindowPadding || this.resizeDone) {
            return;
        }

        // Resize in progress - prevent the view from responding to touch events directly
        this.list.blockTouchEvents();
        this.list.setVerticalScrollBarEnabled(false);

        int heightContainer = this.listParent.getHeight();
        int offsetYDiff = (int) (this.offsetYCurrent - this.offsetYStart);
        if (offsetYDiff < (this.offsetYDiff - resultItemHeight)) {
            double pullFeedback = Math.sqrt((double) (this.offsetYDiff - offsetYDiff) / resultItemHeight);
            offsetYDiff = this.offsetYDiff - (int) (resultItemHeight * pullFeedback);
        }

        // Determine new size of list view widget within its container
        int listLayoutHeight = ViewGroup.LayoutParams.MATCH_PARENT;
        if ((this.listHeightInitial + offsetYDiff) < heightContainer) {
            listLayoutHeight = this.listHeightInitial + offsetYDiff;
        }
        this.setListLayoutHeight(listLayoutHeight);
        if (offsetYDiff > this.offsetYDiff) {
            this.offsetYDiff = offsetYDiff;
        }

        if (this.getWindowPadding() < this.initialWindowPadding
                && listLayoutHeight == ViewGroup.LayoutParams.MATCH_PARENT) {
            // Window size has increased and view has reached it's new maximum size - we're done
            this.handleResizeDone();
            return;
        }

        // Display edge pulling effect while list view is detached from the bottom of its
        // container
        float distance = ((float) (heightContainer - listLayoutHeight)) / heightContainer;
        //float displacement = 1 - this.lastMotionEvent.getX() / getWindowWidth();
        float displacement = 1 - this.lastPosX / getWindowWidth();
        this.pullEffect.setPull(distance, displacement, false);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        this.scrollBarEnabled = this.list.isVerticalScrollBarEnabled();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                this.offsetYStart = event.getY();
                this.offsetYCurrent = event.getY();
                this.offsetYDiff = 0;

                this.lastPosX = event.getX();
                this.resizeDone = false;
                this.keyboardHidden = false;
                this.initialWindowPadding = this.getWindowPadding();

                // Lock list view height to its current value
                this.listHeightInitial = this.list.getHeight();
                this.setListLayoutHeight(this.listHeightInitial);
                if (DebugInfo.keyboardScrollHiderTouch(list.getContext()))
                    list.setBackgroundColor(0x80ff0000);
                break;

            case MotionEvent.ACTION_MOVE:
                this.offsetYCurrent = event.getY();
                if (offsetYStart > offsetYCurrent)
                    offsetYStart = offsetYCurrent;
                this.lastPosX = event.getX();

                this.updateListViewHeight();
                break;

            case MotionEvent.ACTION_UP:
                v.performClick();
            case MotionEvent.ACTION_CANCEL:
                this.lastPosX = 0f;

                if (!this.resizeDone) {
                    ValueAnimator animator = ValueAnimator.ofInt(
                            this.list.getHeight(),
                            this.listParent.getHeight()
                    );
                    animator.setDuration(250);
                    animator.setInterpolator(new AccelerateInterpolator());
                    animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animator) {
                            int height = (int) animator.getAnimatedValue();
                            KeyboardScrollHider.this.setListLayoutHeight(height);
                        }
                    });
                    animator.addListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            // Give the list view the control over it's input back
                            KeyboardScrollHider.this.list.unblockTouchEvents();

                            // Quickly fade out edge pull effect
                            KeyboardScrollHider.this.pullEffect.releasePull();
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            KeyboardScrollHider.this.handleResizeDone();
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {
                        }

                        @Override
                        public void onAnimationRepeat(Animator animation) {
                        }
                    });
                    animator.start();
                } else {
                    this.handleResizeDone();
                }

                break;
        }

        // Hide the keyboard if the user has scrolled down by about half a result item
        if (!this.keyboardHidden && (this.offsetYCurrent - this.offsetYStart) > resultItemHeight) {
            this.list.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
            this.handler.hideKeyboard();
            this.keyboardHidden = true;
            //this.list.finishGlows(); // unfortunately it's a private method
        }

        return false;
    }

    public void fixScroll() {
        this.list.post(() -> {
            resizeDone = false;
            handleResizeDone();
        });
    }

    public interface KeyboardHandler {
        void showKeyboard();

        void hideKeyboard();
    }
}
