package rocks.tbog.tblauncher.ui;

public class CodePointDrawable extends TextDrawable {

    private final int mCodePoint;

    public CodePointDrawable(int codePoint) {
        super();
        mCodePoint = codePoint;
    }

    public CodePointDrawable(CharSequence text) {
        this(Character.codePointAt(text, 0));
    }

    @Override
    protected char[] getText(int line) {
        return Character.toChars(mCodePoint);
    }
}
