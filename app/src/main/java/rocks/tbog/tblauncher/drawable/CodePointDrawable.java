package rocks.tbog.tblauncher.drawable;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CodePointDrawable extends TextDrawable {
    private final State mState;

    public CodePointDrawable(int codePoint) {
        this(new State(codePoint));
    }

    public CodePointDrawable(CharSequence text) {
        this(Character.codePointAt(text, 0));
    }

    public CodePointDrawable(State state) {
        super();
        mState = state;
    }

    @Nullable
    @Override
    public ConstantState getConstantState() {
        return mState;
    }

    @Override
    protected char[] getText(int line) {
        return Character.toChars(mState.mCodePoint);
    }

    protected static class State extends ConstantState {
        final int mCodePoint;

        protected State(int cp) {
            mCodePoint = cp;
        }

        @NonNull
        @Override
        public Drawable newDrawable() {
            return new CodePointDrawable(this);
        }

        @Override
        public int getChangingConfigurations() {
            return 0;
        }
    }
}
