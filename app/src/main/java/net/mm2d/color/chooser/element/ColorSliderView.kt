/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.color.chooser.element

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.Paint.Style
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.withStyledAttributes
import rocks.tbog.tblauncher.R
import net.mm2d.color.chooser.util.*

internal class ColorSliderView
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val paint = Paint().also {
        it.isAntiAlias = true
    }
    private val requestPadding = getPixels(R.dimen.mm2d_cc_panel_margin)
    private val requestWidth = getPixels(R.dimen.mm2d_cc_slider_width) + requestPadding * 2
    private val requestHeight = getPixels(R.dimen.mm2d_cc_slider_height) + requestPadding * 2
    private val sampleRadius = getDimension(R.dimen.mm2d_cc_sample_radius)
    private val sampleFrameRadius = sampleRadius + getDimension(R.dimen.mm2d_cc_sample_frame)
    private val sampleShadowRadius =
        sampleFrameRadius + getDimension(R.dimen.mm2d_cc_sample_shadow)
    private val frameLineWidth = getDimension(R.dimen.mm2d_cc_sample_frame)
    private val shadowLineWidth = getDimension(R.dimen.mm2d_cc_sample_shadow)
    private val gradationRect = Rect(0, 0, RANGE, 1)
    private val targetRect = Rect()
    private val colorSampleFrame = getColor(R.color.mm2d_cc_sample_frame)
    private val colorSampleShadow = getColor(R.color.mm2d_cc_sample_shadow)
    private var checker: Bitmap? = null
    private var floatValue: Float = 0f
    private var maxColor: Int = Color.WHITE
    private var gradation: Bitmap
    private var baseColor: Int = Color.BLACK
    private var alphaMode: Boolean = true
    var onValueChanged: ((value: Int, fromUser: Boolean) -> Unit)? = null
    val value: Int
        get() = (floatValue * MAX).toInt()

    init {
        context.withStyledAttributes(attrs, R.styleable.ColorSliderView) {
            maxColor = getColor(R.styleable.ColorSliderView_maxColor, Color.WHITE)
            baseColor = getColor(R.styleable.ColorSliderView_baseColor, Color.BLACK)
            alphaMode = getBoolean(R.styleable.ColorSliderView_alphaMode, true)
        }
        gradation = createGradation(maxColor)
        updateChecker()
    }

    fun setMaxColor(maxColor: Int) {
        this.maxColor = maxColor.toOpacity()
        gradation = createGradation(this.maxColor)
        invalidate()
    }

    fun setValue(value: Int) {
        floatValue = (value / MAX.toFloat()).coerceIn(0f, 1f)
        onValueChanged?.invoke(value, false)
        invalidate()
    }

    private fun updateChecker() {
        checker = if (alphaMode) {
            createChecker(
                getPixels(R.dimen.mm2d_cc_checker_size),
                getPixels(R.dimen.mm2d_cc_slider_height),
                getColor(R.color.mm2d_cc_checker_light),
                getColor(R.color.mm2d_cc_checker_dark)
            )
        } else {
            null
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            parent.requestDisallowInterceptTouchEvent(true)
        }
        floatValue = ((event.x - targetRect.left) / targetRect.width().toFloat()).coerceIn(0f, 1f)
        onValueChanged?.invoke(value, true)
        invalidate()
        return true
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        targetRect.set(
            paddingLeft + requestPadding,
            paddingTop + requestPadding,
            width - paddingRight - requestPadding,
            height - paddingBottom - requestPadding
        )
    }

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
        paint.style = Style.FILL
        if (alphaMode) {
            val checker = checker ?: return
            canvas.save()
            canvas.clipRect(targetRect)
            val top = targetRect.top.toFloat()
            for (left in targetRect.left until targetRect.right step checker.width) {
                canvas.drawBitmap(checker, left.toFloat(), top, paint)
            }
            canvas.restore()
        } else {
            paint.color = baseColor
            canvas.drawRect(targetRect, paint)
        }
        canvas.drawBitmap(gradation, gradationRect, targetRect, paint)
        val x = floatValue * targetRect.width() + targetRect.left
        val y = targetRect.centerY().toFloat()
        paint.color = colorSampleShadow
        canvas.drawCircle(x, y, sampleShadowRadius, paint)
        paint.color = colorSampleFrame
        canvas.drawCircle(x, y, sampleFrameRadius, paint)
        paint.color = baseColor
        canvas.drawCircle(x, y, sampleRadius, paint)
        paint.color = maxColor.setAlpha(value)
        canvas.drawCircle(x, y, sampleRadius, paint)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            getDefaultSize(
                maxOf(requestWidth + paddingLeft + paddingRight, suggestedMinimumWidth),
                widthMeasureSpec
            ),
            resolveSizeAndState(
                maxOf(requestHeight + paddingTop + paddingBottom, suggestedMinimumHeight),
                heightMeasureSpec,
                MeasureSpec.UNSPECIFIED
            )
        )
    }

    companion object {
        private const val MAX = 255
        private const val RANGE = 256

        private fun createGradation(color: Int): Bitmap {
            val pixels = IntArray(RANGE) { color.setAlpha(it) }
            return Bitmap.createBitmap(pixels, RANGE, 1, Bitmap.Config.ARGB_8888)
        }

        private fun createChecker(step: Int, height: Int, color1: Int, color2: Int): Bitmap {
            val width = step * 4
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
