package rocks.tbog.tblauncher.preference;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.preference.PreferenceDialogFragmentCompat;

import rocks.tbog.tblauncher.utils.DialogHelper;

public abstract class BasePreferenceDialog extends PreferenceDialogFragmentCompat {
    private View mDialogView = null;
    private DialogLifecycleOwner mDialogLifecycleOwner = new DialogLifecycleOwner();

    public LifecycleOwner getDialogLifecycleOwner() {
        return mDialogLifecycleOwner;
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        mDialogView = view;
        mDialogLifecycleOwner.onCreate();
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        DialogHelper.setCustomTitle(builder, getPreference().getDialogTitle());
    }

    @Override
    public void onStart() {
        super.onStart();

        // hack to have the LinearLayout weight work
        ViewParent parent = mDialogView != null ? mDialogView.getParent() : null;
        while (parent instanceof ViewGroup) {
            ViewGroup layout = (ViewGroup) parent;
            ViewGroup.LayoutParams params = layout.getLayoutParams();
            if (params.height != ViewGroup.LayoutParams.MATCH_PARENT) {
                params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                params.height = ViewGroup.LayoutParams.MATCH_PARENT;
                layout.setLayoutParams(params);
            }
            if (layout.getId() == android.R.id.content)
                break;
            parent = parent.getParent();
        }

        DialogHelper.setButtonBarBackground(requireDialog());
        mDialogLifecycleOwner.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        mDialogLifecycleOwner.onStop();
    }

    @Override
    public void onDestroyView() {
        mDialogView = null;
        super.onDestroyView();
        mDialogLifecycleOwner.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        mDialogLifecycleOwner.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mDialogLifecycleOwner.onPause();
    }

    protected static class DialogLifecycleOwner implements LifecycleOwner {
        LifecycleRegistry lifecycleRegistry;

        public DialogLifecycleOwner() {
            lifecycleRegistry = new LifecycleRegistry(this);
        }

        public void onCreate() {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        }

        public void onStart() {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
        }

        public void onResume() {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
        }

        public void onPause() {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);
        }

        public void onStop() {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
        }

        public void onDestroy() {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);
        }

        @NonNull
        @Override
        public Lifecycle getLifecycle() {
            return lifecycleRegistry;
        }
    }
}
