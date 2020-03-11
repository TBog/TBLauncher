package rocks.tbog.tblauncher.preference;

import android.content.Context;
import android.util.AttributeSet;

public class CustomDialogPreference extends androidx.preference.DialogPreference {

    private Object mValue = null;

    public CustomDialogPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public CustomDialogPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CustomDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomDialogPreference(Context context) {
        super(context);
    }

    public Object getValue() {
        return mValue;
    }

    public void setValue(Object value) {
        mValue = value;
    }

    public void persistValue() {
        Object value = getValue();
        if (value instanceof String)
            persistString((String) value);
        else if (value instanceof Integer)
            persistInt((Integer) value);
    }

    public boolean persistValueIfAllowed() {
        if (callChangeListener(getValue())) {
            persistValue();
            return true;
        }
        return false;
    }

    public boolean persistValueIfAllowed(Object value) {
        if (callChangeListener(value)) {
            setValue(value);
            persistValue();
            return true;
        }
        return false;
    }
}
