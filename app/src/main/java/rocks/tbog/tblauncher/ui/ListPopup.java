package rocks.tbog.tblauncher.ui;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Rect;
import android.os.Build;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import rocks.tbog.tblauncher.R;

public class ListPopup extends PopupWindow {
    private final View.OnClickListener mClickListener;
    private final View.OnLongClickListener mLongClickListener;
    private OnItemLongClickListener mItemLongClickListener;
    private OnItemClickListener mItemClickListener;
    private DataSetObserver mObserver;
    private ListAdapter mAdapter;
    private boolean dismissOnClick = true;
    private float dimAmount = .7f;

    public static ListPopup create(@NonNull Context context, ListAdapter adapter) {
        ContextThemeWrapper ctx = new ContextThemeWrapper(context, R.style.ListPopupTheme);
        ListPopup popup = new ListPopup(ctx);
        View root = popup.getContentView().getRootView();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            root.setClipToOutline(true);
        }
        root.setBackgroundResource(R.drawable.popup_background);
        popup.setAdapter(adapter);
        return popup;
    }

    private ListPopup(@NonNull Context context) {
        super(context, null, android.R.attr.popupMenuStyle);
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        ScrollView scrollView = new ScrollView(context);
        scrollView.addView(layout);
        setContentView(scrollView);
        setWidth(LinearLayout.LayoutParams.WRAP_CONTENT);
        setHeight(LinearLayout.LayoutParams.WRAP_CONTENT);
        mItemClickListener = null;
        mClickListener = v -> {
            if (dismissOnClick)
                dismiss();
            if (mItemClickListener != null) {
                LinearLayout layout1 = getLinearLayout();
                int position = layout1.indexOfChild(v);
                mItemClickListener.onItemClick(mAdapter, v, position);
            }
        };
        mLongClickListener = v -> {
            if (mItemLongClickListener == null)
                return false;
            LinearLayout layout12 = getLinearLayout();
            int position = layout12.indexOfChild(v);
            return mItemLongClickListener.onItemLongClick(mAdapter, v, position);
        };
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        mItemClickListener = onItemClickListener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener onItemLongClickListener) {
        mItemLongClickListener = onItemLongClickListener;
    }

    public ListAdapter getAdapter() {
        return mAdapter;
    }

    public void setDismissOnItemClick(boolean dismissOnClick) {
        this.dismissOnClick = dismissOnClick;
    }

    /**
     * Set background dim amount (0=no dim, 1=full dim)
     *
     * @param dimAmount a value of 0 or less to disable dimming
     */
    public void setDimAmount(float dimAmount) {
        this.dimAmount = dimAmount;
    }

    /**
     * Sets the adapter that provides the data and the views to represent the data
     * in this popup window.
     *
     * @param adapter The adapter to use to create this window's content.
     */
    public void setAdapter(ListAdapter adapter) {
        if (mObserver == null) {
            mObserver = new PopupDataSetObserver();
        } else if (mAdapter != null) {
            mAdapter.unregisterDataSetObserver(mObserver);
        }
        mAdapter = adapter;
        if (mAdapter != null) {
            adapter.registerDataSetObserver(mObserver);
        }
    }

    private LinearLayout getLinearLayout() {
        return (LinearLayout) ((ScrollView) getContentView()).getChildAt(0);
    }

    private void updateItems() {
        LinearLayout layout = getLinearLayout();
        layout.removeAllViews();
        int adapterCount = mAdapter.getCount();
        for (int i = 0; i < adapterCount; i += 1) {
            View view = mAdapter.getView(i, null, layout);
            layout.addView(view);
            if (mAdapter.isEnabled(i)) {
                view.setOnClickListener(mClickListener);
                if (mItemLongClickListener == null) {
                    view.setLongClickable(false);
                } else {
                    view.setOnLongClickListener(mLongClickListener);
                }
            }
        }
        layout.forceLayout();
    }

    private void beforeShow()
    {
        updateItems();

        // don't steal the focus, this will prevent the keyboard from changing
        setFocusable(false);
        // draw over stuff if needed
        setClippingEnabled(false);
    }

    public void showCenter(View viewForWindowToken) {
        beforeShow();
        setAnimationStyle(R.style.PopupAnimationBottom);
        super.showAtLocation(viewForWindowToken, Gravity.CENTER, 0, 0);
        applyDim();
    }

    public void show(View anchor) {
        show(anchor, .5f);
    }

    public void show(View anchor, float anchorOverlap) {
        beforeShow();

        final Rect displayFrame = new Rect();
        anchor.getWindowVisibleDisplayFrame(displayFrame);

        final int[] anchorPos = new int[2];
        anchor.getLocationOnScreen(anchorPos);

        final int distanceToBottom = displayFrame.bottom - (anchorPos[1] + anchor.getHeight());
        final int distanceToTop = anchorPos[1] - displayFrame.top;

        View rootView = getContentView().getRootView();
        rootView.forceLayout();
        rootView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

        setWidth(rootView.getMeasuredWidth());
        setHeight(LinearLayout.LayoutParams.WRAP_CONTENT);

        int xOffset = anchorPos[0] + anchor.getPaddingLeft();
        if (xOffset + rootView.getMeasuredWidth() > displayFrame.right)
            xOffset = displayFrame.right - rootView.getMeasuredWidth();

        int overlapAmount = (int) (anchor.getHeight() * anchorOverlap);
        int yOffset;
        if (distanceToBottom > rootView.getMeasuredHeight()) {
            // show below anchor
            yOffset = anchorPos[1] + overlapAmount;
            setAnimationStyle(R.style.PopupAnimationTop);
        } else if (distanceToTop > distanceToBottom) {
            // show above anchor
            yOffset = anchorPos[1] + overlapAmount - rootView.getMeasuredHeight();
            setAnimationStyle(R.style.PopupAnimationBottom);
            if (distanceToTop < rootView.getMeasuredHeight()) {
                // enable scroll
                setHeight(distanceToTop + overlapAmount);
                yOffset += rootView.getMeasuredHeight() - distanceToTop - overlapAmount;
            }
        } else {
            // show below anchor with scroll
            yOffset = anchorPos[1] + overlapAmount;
            setAnimationStyle(R.style.PopupAnimationTop);
            setHeight(distanceToBottom + overlapAmount);
        }

        super.showAtLocation(anchor, Gravity.START | Gravity.TOP, xOffset, yOffset);
        applyDim();
    }

    @Override
    public void showAtLocation(View parent, int gravity, int x, int y) {
        beforeShow();
        setAnimationStyle(R.style.PopupAnimationTop);
        super.showAtLocation(parent, gravity, x, y);
        applyDim();
    }

    /**
     * Must be called after calling show*
     */
    private void applyDim() {
        if (dimAmount > 0.f) {
            View container = getContentView().getRootView();
            WindowManager.LayoutParams p = (WindowManager.LayoutParams) container.getLayoutParams();
            // add flag
            p.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
            p.dimAmount = 0.7f;
            WindowManager wm = (WindowManager) container.getContext().getSystemService(Context.WINDOW_SERVICE);
            assert wm != null;
            wm.updateViewLayout(container, p);
        }

    }

    public interface OnItemClickListener {
        void onItemClick(ListAdapter adapter, View view, int position);
    }

    public interface OnItemLongClickListener {
        boolean onItemLongClick(ListAdapter adapter, View view, int position);
    }

    /**
     * Use `Item` for fast prototyping in an `ArrayAdapter<ListPopup.Item>`
     */
    public static class Item {
        @StringRes
        public final int stringId;
        final String string;

        public Item(Context context, @StringRes int stringId) {
            super();
            this.stringId = stringId;
            this.string = context.getResources()
                    .getString(stringId);
        }

        public Item(String string) {
            super();
            this.stringId = 0;
            this.string = string;
        }

        @NonNull
        @Override
        public String toString() {
            return this.string;
        }
    }

    protected class ScrollView extends android.widget.ScrollView {
        public ScrollView(Context context) {
            super(context);
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent event) {
            // act as a modal, if we click outside dismiss the popup
            final int x = (int) event.getX();
            final int y = (int) event.getY();
            if ((event.getAction() == MotionEvent.ACTION_DOWN)
                    && ((x < 0) || (x >= getWidth()) || (y < 0) || (y >= getHeight()))) {
                dismiss();
                return true;
            } else if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                dismiss();
                return true;
            }
            return super.dispatchTouchEvent(event);
        }
    }

    private class PopupDataSetObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            if (isShowing()) {
                // Resize the popup to fit new content
                updateItems();
                update();
            }
        }

        @Override
        public void onInvalidated() {
            dismiss();
        }
    }
}
