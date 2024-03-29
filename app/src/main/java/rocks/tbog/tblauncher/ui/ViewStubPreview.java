package rocks.tbog.tblauncher.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewStub;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintHelper;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.lang.ref.WeakReference;

import rocks.tbog.tblauncher.R;

/**
 * Copy of {@link android.view.ViewStub} so that we can see something in the preview
 */
public final class ViewStubPreview extends View {
    private int mLayoutResource;
    private int mInflatedId;
    private WeakReference<View> mInflatedViewRef = null;
    private LayoutInflater mInflater = null;
    private OnInflateListener mInflateListener = null;

    public ViewStubPreview(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ViewStubPreview(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ViewStubPreview, defStyle, 0);
        mInflatedId = a.getResourceId(R.styleable.ViewStubPreview_inflatedId, NO_ID);
        mLayoutResource = a.getResourceId(R.styleable.ViewStubPreview_layout, 0);
        a.recycle();
        if (!isInEditMode()) {
            setVisibility(GONE);
            setWillNotDraw(true);
        }
    }

    /**
     * Returns the id taken by the inflated view. If the inflated id is
     * {@link View#NO_ID}, the inflated view keeps its original id.
     *
     * @return A positive integer used to identify the inflated view or
     * {@link #NO_ID} if the inflated view should keep its id.
     * @attr name android:inflatedId
     * @see #setInflatedId(int)
     */
    public int getInflatedId() {
        return mInflatedId;
    }

    /**
     * Defines the id taken by the inflated view. If the inflated id is
     * {@link View#NO_ID}, the inflated view keeps its original id.
     *
     * @param inflatedId A positive integer used to identify the inflated view or
     *                   {@link #NO_ID} if the inflated view should keep its id.
     * @attr name android:inflatedId
     * @see #getInflatedId()
     */
    public void setInflatedId(int inflatedId) {
        mInflatedId = inflatedId;
    }

    /**
     * Returns the layout resource that will be used by {@link #setVisibility(int)} or
     * {@link #inflate()} to replace this StubbedView
     * in its parent by another view.
     *
     * @return The layout resource identifier used to inflate the new View.
     * @attr name android:layout
     * @see #setLayoutResource(int)
     * @see #setVisibility(int)
     * @see #inflate()
     */
    public int getLayoutResource() {
        return mLayoutResource;
    }

    /**
     * Specifies the layout resource to inflate when this StubbedView becomes visible or invisible
     * or when {@link #inflate()} is invoked. The View created by inflating the layout resource is
     * used to replace this StubbedView in its parent.
     *
     * @param layoutResource A valid layout resource identifier (different from 0.)
     * @attr name android:layout
     * @see #getLayoutResource()
     * @see #setVisibility(int)
     * @see #inflate()
     */
    public void setLayoutResource(int layoutResource) {
        mLayoutResource = layoutResource;
    }

    /**
     * Set {@link LayoutInflater} to use in {@link #inflate()}, or {@code null}
     * to use the default.
     */
    public void setLayoutInflater(LayoutInflater inflater) {
        mInflater = inflater;
    }

    /**
     * Get current {@link LayoutInflater} used in {@link #inflate()}.
     */
    public LayoutInflater getLayoutInflater() {
        return mInflater;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (isInEditMode()) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        setMeasuredDimension(0, 0);
    }

    @Override
    public void draw(Canvas canvas) {
        if (isInEditMode())
            super.draw(canvas);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        // don't draw the stub
    }

    /**
     * When visibility is set to {@link #VISIBLE} or {@link #INVISIBLE},
     * {@link #inflate()} is invoked and this StubbedView is replaced in its parent
     * by the inflated layout resource. After that calls to this function are passed
     * through to the inflated view.
     *
     * @param visibility One of {@link #VISIBLE}, {@link #INVISIBLE}, or {@link #GONE}.
     * @see #inflate()
     */
    @Override
    public void setVisibility(int visibility) {
        if (mInflatedViewRef != null) {
            View view = mInflatedViewRef.get();
            if (view != null) {
                view.setVisibility(visibility);
            } else {
                throw new IllegalStateException("setVisibility called on un-referenced view");
            }
        } else {
            super.setVisibility(visibility);
            if (visibility == VISIBLE || visibility == INVISIBLE) {
                inflate();
            }
        }
    }

    /**
     * Inflates the layout resource identified by {@link #getLayoutResource()}
     * and replaces this StubbedView in its parent by the inflated layout resource.
     *
     * @return The inflated layout resource.
     */
    public View inflate() {
        final ViewParent viewParent = getParent();
        if (viewParent instanceof ViewGroup) {
            if (mLayoutResource != 0) {
                final ViewGroup parent = (ViewGroup) viewParent;
                final LayoutInflater factory;
                if (mInflater != null) {
                    factory = mInflater;
                } else {
                    factory = LayoutInflater.from(getContext());
                }
                final View view = factory.inflate(mLayoutResource, parent, false);
                if (mInflatedId != NO_ID) {
                    view.setId(mInflatedId);
                }
                final int index = parent.indexOfChild(this);
                parent.removeViewInLayout(this);
                final ViewGroup.LayoutParams layoutParams = getLayoutParams();
                if (layoutParams != null) {
                    parent.addView(view, index, layoutParams);
                } else {
                    parent.addView(view, index);
                }

                // update parent ConstraintLayout constraints
                if (parent instanceof ConstraintLayout)
                    updateConstraintsAfterStubInflate((ConstraintLayout) parent, getId(), view.getId());

                mInflatedViewRef = new WeakReference<>(view);
                if (mInflateListener != null) {
                    mInflateListener.onInflate(this, view);
                }
                return view;
            } else {
                throw new IllegalArgumentException("ViewStub must have a valid layoutResource");
            }
        } else {
            throw new IllegalStateException("ViewStub must have a non-null ViewGroup viewParent");
        }
    }

    /**
     * Specifies the inflate listener to be notified after this ViewStub successfully
     * inflated its layout resource.
     *
     * @param inflateListener The OnInflateListener to notify of successful inflation.
     * @see android.view.ViewStub.OnInflateListener
     */
    public void setOnInflateListener(OnInflateListener inflateListener) {
        mInflateListener = inflateListener;
    }

    @Nullable
    public static View inflateStub(@Nullable View view) {
        return inflateStub(view, 0);
    }

    @Nullable
    public static View inflateStub(@Nullable View view, @LayoutRes int layoutRes) {
        if (view instanceof ViewStubPreview) {
            if (layoutRes != 0)
                ((ViewStubPreview) view).setLayoutResource(layoutRes);
            // ViewStubPreview already calls updateConstraintsAfterStubInflate
            return ((ViewStubPreview) view).inflate();
        }

        if (!(view instanceof ViewStub))
            return view;

        ViewStub stub = (ViewStub) view;
        int stubId = stub.getId();

        // get parent before the call to inflate
        ConstraintLayout constraintLayout = stub.getParent() instanceof ConstraintLayout ? (ConstraintLayout) stub.getParent() : null;

        if (layoutRes != 0)
            stub.setLayoutResource(layoutRes);
        View inflatedView = stub.inflate();
        int inflatedId = inflatedView.getId();

        updateConstraintsAfterStubInflate(constraintLayout, stubId, inflatedId);

        return inflatedView;
    }

    private static void updateConstraintsAfterStubInflate(@Nullable ConstraintLayout constraintLayout, int stubId, int inflatedId) {
        if (inflatedId == View.NO_ID)
            return;
        // change parent ConstraintLayout constraints
        if (constraintLayout != null && stubId != inflatedId) {
            int childCount = constraintLayout.getChildCount();
            for (int childIdx = 0; childIdx < childCount; childIdx += 1) {
                View child = constraintLayout.getChildAt(childIdx);
                if (child instanceof ConstraintHelper) {
                    // get a copy of the id list
                    int[] refIds = ((ConstraintHelper) child).getReferencedIds();
                    boolean changed = false;
                    // change constraint reference IDs
                    for (int idx = 0; idx < refIds.length; idx += 1) {
                        if (refIds[idx] == stubId) {
                            refIds[idx] = inflatedId;
                            changed = true;
                        }
                    }
                    if (changed)
                        ((ConstraintHelper) child).setReferencedIds(refIds);
                }
                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) child.getLayoutParams();
                if (changeConstraintLayoutParamsTarget(params, stubId, inflatedId))
                    child.setLayoutParams(params);
            }
        }
    }

    private static boolean changeConstraintLayoutParamsTarget(ConstraintLayout.LayoutParams params, int fromId, int toId) {
        boolean changed = false;
        if (params.leftToLeft == fromId) {
            params.leftToLeft = toId;
            changed = true;
        }
        if (params.leftToRight == fromId) {
            params.leftToRight = toId;
            changed = true;
        }
        if (params.rightToLeft == fromId) {
            params.rightToLeft = toId;
            changed = true;
        }
        if (params.rightToRight == fromId) {
            params.rightToRight = toId;
            changed = true;
        }
        if (params.topToTop == fromId) {
            params.topToTop = toId;
            changed = true;
        }
        if (params.topToBottom == fromId) {
            params.topToBottom = toId;
            changed = true;
        }
        if (params.bottomToTop == fromId) {
            params.bottomToTop = toId;
            changed = true;
        }
        if (params.bottomToBottom == fromId) {
            params.bottomToBottom = toId;
            changed = true;
        }
        if (params.baselineToBaseline == fromId) {
            params.baselineToBaseline = toId;
            changed = true;
        }
        if (params.baselineToTop == fromId) {
            params.baselineToTop = toId;
            changed = true;
        }
        if (params.circleConstraint == fromId) {
            params.circleConstraint = toId;
            changed = true;
        }
        if (params.startToEnd == fromId) {
            params.startToEnd = toId;
            changed = true;
        }
        if (params.startToStart == fromId) {
            params.startToStart = toId;
            changed = true;
        }
        if (params.endToStart == fromId) {
            params.endToStart = toId;
            changed = true;
        }
        if (params.endToEnd == fromId) {
            params.endToEnd = toId;
            changed = true;
        }
        return changed;
    }

    /**
     * Listener used to receive a notification after a ViewStub has successfully
     * inflated its layout resource.
     *
     * @see android.view.ViewStub#setOnInflateListener(android.view.ViewStub.OnInflateListener)
     */
    public interface OnInflateListener {
        /**
         * Invoked after a ViewStub successfully inflated its layout resource.
         * This method is invoked after the inflated view was added to the
         * hierarchy but before the layout pass.
         *
         * @param stub     The ViewStub that initiated the inflation.
         * @param inflated The inflated View.
         */
        void onInflate(ViewStubPreview stub, View inflated);
    }
}