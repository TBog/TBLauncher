package rocks.tbog.tblauncher.drawable;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import rocks.tbog.tblauncher.utils.Utilities;

public class FourCodePointDrawable extends TextDrawable {
    private final State mState;

    public FourCodePointDrawable(int cp1, int cp2, int cp3, int cp4) {
        this(new State(cp1, cp2, cp3, cp4));
    }

    public FourCodePointDrawable(State state) {
        super();
        mState = state;
    }

    @NonNull
    public static FourCodePointDrawable fromText(CharSequence text, boolean soloFirstLine) {
        int idx = 0;
        int cp1 = Character.codePointAt(text, idx);
        idx = Utilities.getNextCodePointIndex(text, idx);
        int cp2 = 0;
        if (!soloFirstLine) {
            cp2 = Character.codePointAt(text, idx);
            idx = Utilities.getNextCodePointIndex(text, idx);
        }
        int cp3 = Character.codePointAt(text, idx);
        idx = Utilities.getNextCodePointIndex(text, idx);
        int cp4 = idx < text.length() ? Character.codePointAt(text, idx) : 0;
        return new FourCodePointDrawable(cp1, cp2, cp3, cp4);
    }

    @Nullable
    @Override
    public ConstantState getConstantState() {
        return mState;
    }

    @Override
    protected int getLineCount() {
        return 2;
    }

    @Override
    protected char[] getText(int line) {
        int i = line * 2;
        int cp1 = mState.mCodePoint[i];
        int cp2 = mState.mCodePoint[i + 1];
        if (cp2 == 0)
            return Character.toChars(cp1);
        int cc1 = Character.charCount(cp1);
        int cc2 = Character.charCount(cp2);
        char[] result = new char[cc1 + cc2];
        System.arraycopy(Character.toChars(cp1), 0, result, 0, cc1);
        System.arraycopy(Character.toChars(cp2), 0, result, cc1, cc2);
        return result;
    }

    protected static class State extends ConstantState {
        final int[] mCodePoint = new int[4];

        protected State(int cp1, int cp2, int cp3, int cp4) {
            mCodePoint[0] = cp1;
            mCodePoint[1] = cp2;
            mCodePoint[2] = cp3;
            mCodePoint[3] = cp4;
        }

        @NonNull
        @Override
        public Drawable newDrawable() {
            return new FourCodePointDrawable(this);
        }

        @Override
        public int getChangingConfigurations() {
            return 0;
        }
    }
}
