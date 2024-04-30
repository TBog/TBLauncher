package rocks.tbog.tblauncher.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceViewHolder;

import java.util.Collections;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.utils.PrefCache;
import rocks.tbog.tblauncher.utils.UIColors;
import rocks.tbog.tblauncher.utils.UISizes;

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

    public boolean persistValue() {
        Object value = getValue();
        if (value instanceof String)
            return persistString((String) value);
        else if (value instanceof Integer)
            return persistInt((Integer) value);
        else if (value instanceof Float)
            return persistFloat((Float) value);
        return false;
    }

    public boolean persistValueIfAllowed() {
        if (callChangeListener(getValue())) {
            return persistValue();
        }
        return false;
    }

    public boolean persistValueIfAllowed(Object value) {
        if (callChangeListener(value)) {
            setValue(value);
            return persistValue();
        }
        return false;
    }

    @Override
    protected void onSetInitialValue(@Nullable Object defaultValue) {
        if (getValue() == null)
            setValue(defaultValue);
        persistValue();
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        try {
            return a.getInteger(index, 0);
        } catch (UnsupportedOperationException e) {
            try {
                return a.getFloat(index, 0f);
            } catch (UnsupportedOperationException ignored) {
                return a.getString(index);
            }
        }
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        final String key = getKey();
        final var pref = getSharedPreferences();
        final var prefMap = pref != null ? pref.getAll() : Collections.emptyMap();
        Object value = prefMap.get(key);

        {
            View view = holder.findViewById(R.id.prefAmountPreview);
            if (view instanceof TextView) {
                String text = null;
                if (value instanceof Integer) {
                    Integer amount = ((Integer) value);
                    text = amount > 0 ? ("+" + amount) : amount.toString();
                    if (key.contains("-scale"))
                        text += "%";
                }
                ((TextView) view).setText(text);
                return;
            }
        }
        {
            View view = holder.findViewById(R.id.prefColorPreview);
            if (view instanceof ImageView) {
                int color = 0xFFffffff;
                if (value instanceof Integer)
                    color = (int) value | 0xFF000000;

                Context ctx = getContext();
                float radius = ctx.getResources().getDimension(R.dimen.color_preview_radius);
                int border = UISizes.dp2px(ctx, 1);
                Drawable drawable = UIColors.getPreviewDrawable(color, border, radius);
                ((ImageView) view).setImageDrawable(drawable);
                return;
            }
        }
        {
            View view = holder.findViewById(R.id.prefAlphaPreview);
            if (view instanceof TextView) {
                String text = null;
                if (value instanceof Integer)
                    text = ((Integer) value) * 100 / 255 + "%";
                ((TextView) view).setText(text);
                return;
            }
        }
        {
            View view = holder.findViewById(R.id.prefSizePreview);
            if (view instanceof TextView) {
                if (value instanceof Float) {
                    float size = (float) value;
                    ((TextView) view).setText(view.getResources().getString(R.string.size_float, size));
                } else {
                    int size = -1;
                    if ("result-search-cap".equals(key))
                        size = PrefCache.getResultSearcherCap(getContext());
                    else if (value instanceof Integer)
                        size = (int) value;
                    ((TextView) view).setText(view.getResources().getString(R.string.size, size));
                }
            }
        }
        {
            View view = holder.findViewById(R.id.prefShadowPreview);
            if (view instanceof TextView) {
                // used for shadow
                Object value2 = prefMap.get(key.replace("-dx", "-dy"));
                Object value3 = prefMap.get(key.replace("-dx", "-radius"));
                if (value instanceof Float && value2 instanceof Float && value3 instanceof Float) {
                    float v1 = (float) value;
                    float v2 = (float) value2;
                    float v3 = (float) value3;
                    ((TextView) view).setText(view.getResources().getString(R.string.shadow_preview, v1, v2, v3));
                } else {
                    ((TextView) view).setText("");
                }
            }
        }
        {
            View view = holder.findViewById(R.id.prefOffsetPreview);
            if (view instanceof TextView) {
                // used for shadow
                Object value2 = prefMap.get(key.replace("-dx", "-dy"));
                if (value instanceof Float && value2 instanceof Float) {
                    float v1 = (float) value;
                    float v2 = (float) value2;
                    ((TextView) view).setText(view.getResources().getString(R.string.offset_preview, v1, v2));
                } else {
                    ((TextView) view).setText("");
                }
            }
        }
    }
}
