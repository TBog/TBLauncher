/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.color.chooser.element

import android.content.Context
import android.graphics.*
import android.graphics.Paint.Style
import android.util.AttributeSet
import android.view.View
import rocks.tbog.tblauncher.R
import net.mm2d.color.chooser.util.drawRectWithOffset
import net.mm2d.color.chooser.util.getColor
import net.mm2d.color.chooser.util.getDimension
import net.mm2d.color.chooser.util.getPixels
import kotlin.math.max

internal class PreviewView
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val paint = Paint().also {
        it.isAntiAlias = true
    }
    private val _width = getPixels(R.dimen.mm2d_cc_preview_width)
    private val _height = getPixels(R.dimen.mm2d_cc_preview_height)
    private val frameLineWidth = getDimension(R.dimen.mm2d_cc_sample_frame)
    private val shadowLineWidth = getDimension(R.dimen.mm2d_cc_sample_shadow)
    private val colorSampleFrame = getColor(R.color.mm2d_cc_sample_frame)
    private val colorSampleShadow = getColor(R.color.mm2d_cc_sample_shadow)
    private val checkerRect = Rect()
    private val targetRect = Rect()
    private val checkerSize = getPixels(R.dimen.mm2d_cc_checker_size)
    private val colorCheckerLight = getColor(R.color.mm2d_cc_checker_light)
    private val colorCheckerDark = getColor(R.color.mm2d_cc_checker_dark)
    private var checker: Bitmap? = null
    var color: Int = Color.BLACK
        private set

    override fun onDraw(canvas: Canvas) {
        paint.style = Style.STROKE
        paint.color = colorSampleShadow
        paint.strokeWidth = shadowLineWidth
        val shadow = frameLineWidth + shadowLineWidth / 2
        canvas.drawRectWithOffset(targetRect, shadow, paint)
        paint.color = colorSampleFrame
        paint.strokeWidth = frameLineWidth
        val frame = frameLineWidth / 2
        canvas.drawRectWithOffset(targetRect, frame, paint)
        val checker = checker ?: return
        canvas.drawBitmap(checker, checkerRect, targetRect, paint)
        paint.style = Style.FILL
        paint.color = color
        canvas.drawRect(targetRect, paint)
    }

    fun setColor(color: Int) {
        this.color = color
        invalidate()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val border = (frameLineWidth + shadowLineWidth).toInt()
        targetRect.set(
            paddingLeft + border,
            paddingTop + border,
            width - paddingRight - border,
            height - paddingBottom - border
        )
        checkerRect.set(0, 0, targetRect.width(), targetRect.height())
        checker = createChecker(
            checkerSize,
            checkerRect.width(),
            checkerRect.height(),
            colorCheckerLight,
            colorCheckerDark
        )
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            resolveSizeAndState(
                max(_width, suggestedMinimumWidth),
                widthMeasureSpec,
                MeasureSpec.UNSPECIFIED
            ),
            resolveSizeAndState(
                max(_height, suggestedMinimumHeight),
                heightMeasureSpec,
                MeasureSpec.UNSPECIFIED
            )
        )
    }

    companion object {
        private fun createChecker(
            step: Int,
            width: Int,
            height: Int,
            color1: Int,
            color2: Int
        ): Bitmap {
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    pixels[x + y * width] = if ((x / step + y / step) % 2 == 0) color1 else color2
                }
            }
            return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
        }
    }
}
