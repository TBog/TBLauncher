package rocks.tbog.tblauncher.drawable;

import androidx.annotation.NonNull;

import rocks.tbog.tblauncher.utils.Utilities;

public class FourCodePointDrawable extends TextDrawable {

    final int[] mCodePoint = new int[4];

    public FourCodePointDrawable(int cp1, int cp2, int cp3, int cp4) {
        super();
        mCodePoint[0] = cp1;
        mCodePoint[1] = cp2;
        mCodePoint[2] = cp3;
        mCodePoint[3] = cp4;
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

    @Override
    protected int getLineCount() {
        return 2;
    }

    @Override
    protected char[] getText(int line) {
        int i = line * 2;
        int cp1 = mCodePoint[i];
        int cp2 = mCodePoint[i + 1];
        if (cp2 == 0)
            return Character.toChars(cp1);
        int cc1 = Character.charCount(cp1);
        int cc2 = Character.charCount(cp2);
        char[] result = new char[cc1 + cc2];
        System.arraycopy(Character.toChars(cp1), 0, result, 0, cc1);
        System.arraycopy(Character.toChars(cp2), 0, result, cc1, cc2);
        return result;
    }
}
