package rocks.tbog.tblauncher.preference;

import android.util.AttributeSet;
import android.util.Log;

import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public abstract class MultiDependencies {
    private static final String TAG = "MDep";
    private static final String NS = "http://tbog.rocks/res/pref";
    private static final Method PREFERENCE_METHOD_REGISTER_DEPENDENT;
    private static final Method PREFERENCE_METHOD_UNREGISTER_DEPENDENT;

    private final Preference host;
    private final Map<String, Boolean> dependencies = new HashMap<>();


    static {
        final Class<Preference> prefClass = Preference.class;
        Method registerMethod = null;
        Method unregisterMethod = null;
        try {
            registerMethod = prefClass.getDeclaredMethod("registerDependent", Preference.class);
            registerMethod.setAccessible(true);
            unregisterMethod = prefClass.getDeclaredMethod("unregisterDependent", Preference.class);
            unregisterMethod.setAccessible(true);
        } catch (Throwable t) {
            Log.w(TAG, "make methods from " + prefClass + " accessible", t);
        }
        PREFERENCE_METHOD_REGISTER_DEPENDENT = registerMethod;
        PREFERENCE_METHOD_UNREGISTER_DEPENDENT = unregisterMethod;
    }

    //We have to get access to the 'findPreferenceInHierarchy' function
    //from the extended preference, because this function is protected
    protected abstract Preference findPreferenceInHierarchy(String key);

    public MultiDependencies(Preference host, AttributeSet attrs) {

        this.host = host;

        final String dependencyString = getAttributeStringValue(attrs, NS, "dependencies", null);

        if (dependencyString != null) {
            String[] dependencies = dependencyString.split(",");
            for (String dependency : dependencies) {
                this.dependencies.put(dependency.trim(), false);
            }
        }
    }

    public void register() {
        if (hasDependencies())
            registerDependencies();
    }

    public void unregister() {
        unregisterDependencies();
    }

    public void onDependencyChanged(Preference dependency, boolean disableDependent) {
        setDependencyState(dependency.getKey(), !disableDependent);
        setHostState();
    }

    private void setDependencyState(String key, boolean enabled) {
        if (dependencies.containsKey(key))
            dependencies.put(key, enabled);
    }

    private static String getAttributeStringValue(AttributeSet attrs, String namespace, String name, String defaultValue) {
        String value = attrs.getAttributeValue(namespace, name);
        if (value == null)
            value = defaultValue;
        return value;
    }

    private void registerDependencies() {
        for (final Map.Entry<String, Boolean> entry : dependencies.entrySet()) {
            final Preference preference = findPreferenceInHierarchy(entry.getKey());

            if (preference != null) {
                try {
                    PREFERENCE_METHOD_REGISTER_DEPENDENT.invoke(preference, host);
                } catch (final Exception e) {
                    Log.e(TAG, "registerDependent on (" + host.getClass() + ") " + host);
                }

                boolean enabled = preference.isEnabled();
                if (preference instanceof CheckBoxPreference) {
                    enabled &= ((CheckBoxPreference) preference).isChecked();
                }

                setDependencyState(preference.getKey(), enabled);
            }
        }
        setHostState();
    }

    private void unregisterDependencies() {
        for (final Map.Entry<String, Boolean> entry : dependencies.entrySet()) {
            final Preference preference = findPreferenceInHierarchy(entry.getKey());

            if (preference != null) {
                try {
                    PREFERENCE_METHOD_UNREGISTER_DEPENDENT.invoke(preference, host);
                } catch (final Exception e) {
                    Log.e(TAG, "unregisterDependent on (" + host.getClass() + ") " + host);
                }

                boolean enabled = preference.isEnabled();
                if (preference instanceof CheckBoxPreference) {
                    enabled &= ((CheckBoxPreference) preference).isChecked();
                }

                setDependencyState(preference.getKey(), enabled);
            }
        }
    }

    private void setHostState() {
        boolean enabled = true;
        for (Map.Entry<String, Boolean> entry : dependencies.entrySet()) {
            if (!entry.getValue()) {
                enabled = false;
                break;
            }
        }
        host.setEnabled(enabled);
    }

    public boolean hasDependencies() {
        return dependencies.size() > 0;
    }

}