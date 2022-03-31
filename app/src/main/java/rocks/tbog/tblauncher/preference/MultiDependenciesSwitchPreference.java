package rocks.tbog.tblauncher.preference;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

public class MultiDependenciesSwitchPreference extends SwitchPreference {

    MultiDependencies multiDependencies;

    public MultiDependenciesSwitchPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initMultiDep(attrs);
    }

    public MultiDependenciesSwitchPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initMultiDep(attrs);
    }

    public MultiDependenciesSwitchPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initMultiDep(attrs);
    }

    public MultiDependenciesSwitchPreference(@NonNull Context context) {
        this(context, null);
    }

    private void initMultiDep(@Nullable AttributeSet attrs) {
        multiDependencies = new MultiDependencies(this, attrs) {
            @Override
            protected Preference findPreferenceInHierarchy(String key) {
                return MultiDependenciesSwitchPreference.this.findPreferenceInHierarchy(key);
            }
        };
    }

    @Override
    public void onAttached() {
        super.onAttached();
        multiDependencies.register();
    }

    @Override
    public void onDetached() {
        multiDependencies.unregister();
        super.onDetached();
    }

    @Override
    protected void onPrepareForRemoval() {
        multiDependencies.unregister();
        super.onPrepareForRemoval();
    }

    @Override
    public void onDependencyChanged(@NonNull Preference dependency, boolean disableDependent) {
        if(multiDependencies.hasDependencies())
            multiDependencies.onDependencyChanged(dependency, disableDependent);
        else super.onDependencyChanged(dependency, disableDependent);
    }
}
