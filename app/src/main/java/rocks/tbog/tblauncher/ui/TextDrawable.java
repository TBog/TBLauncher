package rocks.tbog.tblauncher.ui;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;

import androidx.annotation.NonNull;

public abstract class TextDrawable extends SquareDrawable {
    PointF[] cachedLinePos = null;
    float[] cachedLineSize = null;
    char[][] cachedText = null;

    public TextDrawable() {
        super();
        mPaint.setTextAlign(Paint.Align.LEFT);
        mPaint.setAntiAlias(true);
    }

    protected int getLineCount() {
        return 1;
    }

    protected abstract char[] getText(int line);

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        Rect rect = getCenterRect(bounds);
        precacheTextPosAndSize(rect);
    }

    protected void precacheTextPosAndSize(Rect rect) {
        final int lineCount = getLineCount();
        final float cHeight = rect.height();
        final float cWidth = rect.width();

        // cache text
        char[][] text = new char[lineCount][];
        for (int line = 0; line < lineCount; line += 1)
            text[line] = getText(line);

        cachedText = text;
        cachedLinePos = new PointF[lineCount];
        cachedLineSize = new float[lineCount];

        mPaint.setTextSize(cHeight);
        Rect[] lineRect = new Rect[lineCount];
        float heightSum = 0f;
        for (int line = 0; line < lineCount; line += 1) {
            lineRect[line] = new Rect();
            mPaint.getTextBounds(text[line], 0, text[line].length, lineRect[line]);
            heightSum += lineRect[line].height();
        }

        float[] expectedSize = new float[lineCount];

        // find size for each line to fill the height
        for (int line = 0; line < lineCount; line += 1) {
            expectedSize[line] = lineRect[line].height() / heightSum * cHeight;
            // use binary search to find a text size to fit the expectedSize
            float minTextSize = 0.f;
            float maxTextSize = expectedSize[line] * 2.f;
            while (minTextSize < maxTextSize) {
                mPaint.setTextSize((minTextSize + maxTextSize) * .5f);
                mPaint.getTextBounds(text[line], 0, text[line].length, lineRect[line]);
                if (lineRect[line].height() < expectedSize[line])
                    minTextSize = (int) mPaint.getTextSize() + 1;
                else
                    maxTextSize = (int) (mPaint.getTextSize() - .01f);
            }
            cachedLineSize[line] = maxTextSize;
        }

        // find size for each line to fill the width
        for (int line = 0; line < lineCount; line += 1) {
            // use binary search to find a text size to fit the width
            float minTextSize = 0.f;
            float maxTextSize = cachedLineSize[line];
            while (minTextSize < maxTextSize) {
                mPaint.setTextSize((minTextSize + maxTextSize) * .5f);
                mPaint.getTextBounds(text[line], 0, text[line].length, lineRect[line]);
                if (lineRect[line].width() < cWidth)
                    minTextSize = mPaint.getTextSize() + 1.f;
                else
                    maxTextSize = mPaint.getTextSize() - 1.f;
            }
            cachedLineSize[line] = maxTextSize;
        }

        // set line position
        float lineOffset = 0f;
        for (int line = 0; line < lineCount; line += 1) {
            // center text inspired from https://stackoverflow.com/a/32081250
            float x = cWidth * .5f - lineRect[line].width() * .5f - lineRect[line].left;
            float y = expectedSize[line] * .5f + lineRect[line].height() * .5f - lineRect[line].bottom;

            y += lineOffset;
            cachedLinePos[line] = new PointF(x, y);

            lineOffset += expectedSize[line];
        }
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        final int lineCount = getLineCount();
        for (int line = 0; line < lineCount; line += 1) {
            char[] text = cachedText[line];
            float x = cachedLinePos[line].x;
            float y = cachedLinePos[line].y;

            mPaint.setTextSize(cachedLineSize[line]);

            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(Color.WHITE);
            canvas.drawText(text, 0, text.length, x, y, mPaint);

            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(0.f); // 0 = hairline, always draws a single pixel independent of the canvas's matrix
            mPaint.setColor(Color.BLACK);
            canvas.drawText(text, 0, text.length, x, y, mPaint);
        }
    }
}
