package rocks.tbog.tblauncher.drawable;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import rocks.tbog.tblauncher.utils.Utilities;

public class TwoCodePointDrawable extends TextDrawable {
    private final State mState;
    private boolean bVertical = false;

    public TwoCodePointDrawable(int cp1, int cp2) {
        this(new State(cp1, cp2));
    }

    public TwoCodePointDrawable(State state) {
        super();
        mState = state;
    }

    @NonNull
    public static TwoCodePointDrawable fromText(CharSequence text, boolean vertical) {
        int cp1 = Character.codePointAt(text, 0);
        int cp2 = Character.codePointAt(text, Utilities.getNextCodePointIndex(text, 0));
        TwoCodePointDrawable drawable = new TwoCodePointDrawable(cp1, cp2);
        drawable.setVertical(vertical);
        return drawable;
    }

    public void setVertical(boolean vertical) {
        bVertical = vertical;
    }

    @Nullable
    @Override
    public ConstantState getConstantState() {
        return mState;
    }

    @Override
    protected int getLineCount() {
        return bVertical ? 2 : 1;
    }

    @Override
    protected char[] getText(int line) {
        final int cp1 = mState.mCodePoint1;
        final int cp2 = mState.mCodePoint2;
        if (bVertical)
            return line == 0 ? Character.toChars(cp1) : Character.toChars(cp2);
        int c1 = Character.charCount(cp1);
        int c2 = Character.charCount(cp2);
        char[] result = new char[c1 + c2];
        System.arraycopy(Character.toChars(cp1), 0, result, 0, c1);
        System.arraycopy(Character.toChars(cp2), 0, result, c1, c2);
        return result;
    }

    protected static class State extends ConstantState {
        final int mCodePoint1;
        final int mCodePoint2;

        protected State(int cp1, int cp2) {
            mCodePoint1 = cp1;
            mCodePoint2 = cp2;
        }

        @NonNull
        @Override
        public Drawable newDrawable() {
            return new TwoCodePointDrawable(this);
        }

        @Override
        public int getChangingConfigurations() {
            return 0;
        }
    }
}
