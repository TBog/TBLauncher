package rocks.tbog.tblauncher.ui;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import androidx.annotation.NonNull;

public class CodePointDrawable extends SquareDrawable {

    final int mCodePoint;

    public CodePointDrawable(int codePoint) {
        super();
        mPaint.setTextAlign(Paint.Align.LEFT);
        mPaint.setAntiAlias(true);
        mCodePoint = codePoint;
    }

    public CodePointDrawable(CharSequence text) {
        this(Character.codePointAt(text, 0));
    }

    @Override

    public void draw(@NonNull Canvas canvas) {
        char[] text = Character.toChars(mCodePoint);
        Rect r = getBounds();
        mPaint.setTextSize(r.height());

// may not center well letters without descent or ascent
//        mPaint.setTextAlign(Paint.Align.CENTER);
//        int xPos = getBounds().width() / 2;
//        int yPos = (int) ((getBounds().height() / 2) - ((mPaint.descent() + mPaint.ascent()) / 2));
//        canvas.drawText(text, 0, text.length, xPos, yPos, mPaint);


        // from https://stackoverflow.com/a/32081250
        //canvas.getClipBounds(r);
        int cHeight = r.height();
        int cWidth = r.width();
        mPaint.getTextBounds(text, 0, text.length, r);
        float x = cWidth / 2f - r.width() / 2f - r.left;
        float y = cHeight / 2f + r.height() / 2f - r.bottom;

        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(Color.WHITE);
        canvas.drawText(text, 0, text.length, x, y, mPaint);

        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(0.f); // 0 = hairline, always draws a single pixel independent of the canvas's matrix
        mPaint.setColor(Color.BLACK);
        canvas.drawText(text, 0, text.length, x, y, mPaint);
    }
}
