package rocks.tbog.tblauncher.preference;

import android.app.Application;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.AndroidViewModel;

import rocks.tbog.tblauncher.utils.Utilities;

public abstract class EditAddResetEditor extends AndroidViewModel {
    private FragmentManager mFragmentManager = null;
    protected FragmentManager getFragmentManager() {
        return mFragmentManager;
    }
    public EditAddResetEditor(@NonNull Application application) {
        super(application);
    }

    public void loadDefaults(@NonNull Context context)
    {
        Utilities.runAsync(()-> loadDefaultsInternal(context));
    }

    public void loadData(@NonNull Context context, @NonNull SharedPreferences prefs)
    {
        Utilities.runAsync(()-> loadDataInternal(context, prefs));
    }

    @WorkerThread
    public abstract void loadDefaultsInternal(@NonNull Context context);
    @WorkerThread
    public abstract void loadDataInternal(@NonNull Context context, @NonNull SharedPreferences prefs);
    public abstract void applyChanges(@NonNull Context context);
    public abstract void bindEditView(@NonNull View view);
    public abstract void bindAddView(@NonNull View view);

    public void onStartLifecycle(@NonNull Dialog dialog, @NonNull BasePreferenceDialog owner) {
        FragmentActivity activity = owner.getActivity();
        if (activity != null)
            mFragmentManager = activity.getSupportFragmentManager();
    }
}
