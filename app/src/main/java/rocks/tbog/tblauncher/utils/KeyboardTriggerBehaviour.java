package rocks.tbog.tblauncher.utils;

import android.app.Activity;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import rocks.tbog.tblauncher.TBApplication;

public class KeyboardTriggerBehaviour extends LiveData<KeyboardTriggerBehaviour.Status> {
    private static final String TAG = "KeyTB";

    public enum Status {OPEN, CLOSED}

    private final View contentView;
    private final ViewTreeObserver.OnGlobalLayoutListener globalLayoutListener;

    public KeyboardTriggerBehaviour(Activity activity) {
        contentView = activity.findViewById(android.R.id.content);
        globalLayoutListener = () -> {
            TBApplication.state().syncKeyboardVisibility(contentView);
            boolean closed = TBApplication.state().isKeyboardHidden();
            Status status = getValue();
            Log.d(TAG, "[listener] isKeyboardHidden=" + closed + " status=" + status);
            if (closed && status != Status.CLOSED)
                setValue(Status.CLOSED);
            else if (!closed && status != Status.OPEN)
                setValue(Status.OPEN);
        };
    }

    @Override
    public void observe(@NonNull LifecycleOwner owner, @NonNull Observer<? super Status> observer) {
        super.observe(owner, observer);
        observersUpdated();
    }

    @Override
    public void observeForever(@NonNull Observer<? super Status> observer) {
        super.observeForever(observer);
        observersUpdated();
    }

    @Override
    public void removeObserver(@NonNull Observer<? super Status> observer) {
        super.removeObserver(observer);
        observersUpdated();
    }

    @Override
    public void removeObservers(@NonNull LifecycleOwner owner) {
        super.removeObservers(owner);
        observersUpdated();
    }

    private void observersUpdated() {
        if (hasObservers()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ViewCompat.setOnApplyWindowInsetsListener(contentView, (v, insets) -> {
                    globalLayoutListener.onGlobalLayout();
                    return insets;
                });
            } else {
                contentView.getViewTreeObserver().addOnGlobalLayoutListener(globalLayoutListener);
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ViewCompat.setOnApplyWindowInsetsListener(contentView, null);
            } else {
                contentView.getViewTreeObserver().removeOnGlobalLayoutListener(globalLayoutListener);
            }
        }
    }
}
