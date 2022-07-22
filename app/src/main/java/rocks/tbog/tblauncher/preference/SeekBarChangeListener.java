package rocks.tbog.tblauncher.preference;

import android.widget.SeekBar;
import android.widget.TextView;

import rocks.tbog.tblauncher.R;

public abstract class SeekBarChangeListener<T> implements SeekBar.OnSeekBarChangeListener {
    protected final int offset;
    protected final TextView textView;
    protected final ValueChanged<T> listener;

    interface ValueChanged<T> {
        void valueChanged(T newValue);
    }

    public SeekBarChangeListener(int offset, TextView textView, ValueChanged<T> listener) {
        this.offset = offset;
        this.textView = textView;
        this.listener = listener;
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // do nothing
    }

    public static class ProgressChangedInt extends SeekBarChangeListener<Integer> {

        public ProgressChangedInt(int offset, TextView textView, ValueChanged<Integer> listener) {
            super(offset, textView, listener);
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            final int newValue = progress + offset;
            textView.setText(textView.getResources().getString(R.string.value, newValue));
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            int progress = seekBar.getProgress();
            progress += offset;
            listener.valueChanged(progress);
        }
    }

    public static class ProgressChangedFloat extends SeekBarChangeListener<Float> {
        protected float incrementBy;

        public ProgressChangedFloat(int offset, float incrementBy, TextView textView, ValueChanged<Float> listener) {
            super(offset, textView, listener);
            this.incrementBy = incrementBy;
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            final float newValue = (progress + offset) * incrementBy;
            textView.setText(textView.getResources().getString(R.string.value_float, newValue));
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            float progress = seekBar.getProgress();
            progress = (progress + offset) * incrementBy;
            listener.valueChanged(progress);
        }
    }
}
