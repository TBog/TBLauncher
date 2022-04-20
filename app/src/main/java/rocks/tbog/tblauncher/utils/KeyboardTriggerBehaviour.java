package rocks.tbog.tblauncher.utils;

import android.app.Activity;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.core.view.ViewCompat;
import androidx.lifecycle.LiveData;

import rocks.tbog.tblauncher.TBApplication;

public class KeyboardTriggerBehaviour extends LiveData<KeyboardTriggerBehaviour.Status> {
    private static final String TAG = "KeyTB";

    private final View contentView;
    private final ViewTreeObserver.OnGlobalLayoutListener globalLayoutListener;

    public KeyboardTriggerBehaviour(Activity activity) {
        super(Status.CLOSED);
        contentView = activity.findViewById(android.R.id.content);
        globalLayoutListener = () -> {
            TBApplication.state().syncKeyboardVisibility(contentView);
            boolean closed = TBApplication.state().isKeyboardHidden();
            Status status = getValue();
            Log.d(TAG, "[listener] isKeyboardHidden=" + closed + " status=" + status);
            if (closed && status != Status.CLOSED)
                postValue(Status.CLOSED);
            else if (!closed && status != Status.OPEN)
                postValue(Status.OPEN);
        };
    }

    @Override
    protected void onActive() {
        super.onActive();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Log.d(TAG, "onActive - WindowInsetsListener");
            ViewCompat.setOnApplyWindowInsetsListener(contentView, (v, insets) -> {
                globalLayoutListener.onGlobalLayout();
                return insets;
            });
        } else {
            Log.d(TAG, "onActive - GlobalLayoutListener");
            contentView.getViewTreeObserver().addOnGlobalLayoutListener(globalLayoutListener);
        }
    }

    @Override
    protected void onInactive() {
        super.onInactive();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Log.d(TAG, "onInactive - WindowInsetsListener");
            ViewCompat.setOnApplyWindowInsetsListener(contentView, null);
        } else {
            Log.d(TAG, "onInactive - GlobalLayoutListener");
            contentView.getViewTreeObserver().removeOnGlobalLayoutListener(globalLayoutListener);
        }
    }

    public enum Status {OPEN, CLOSED}
}
