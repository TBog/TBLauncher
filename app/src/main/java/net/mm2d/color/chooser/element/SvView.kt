/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.color.chooser.element

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorInt
import rocks.tbog.tblauncher.R
import net.mm2d.color.chooser.util.ColorUtils
import net.mm2d.color.chooser.util.getColor
import net.mm2d.color.chooser.util.getDimension
import net.mm2d.color.chooser.util.getPixels
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal class SvView
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    @ColorInt
    private var color: Int = Color.BLACK
    private var maxColor: Int = Color.RED
    private var maskBitmap: Bitmap? = null
    private val paint = Paint().also { it.isAntiAlias = true }
    private val requestPadding = getPixels(R.dimen.mm2d_cc_panel_margin)
    private val requestWidth = getPixels(R.dimen.mm2d_cc_hsv_size) + requestPadding * 2
    private val requestHeight = getPixels(R.dimen.mm2d_cc_hsv_size) + requestPadding * 2
    private val sampleRadius = getDimension(R.dimen.mm2d_cc_sample_radius)
    private val sampleFrameRadius = sampleRadius + getDimension(R.dimen.mm2d_cc_sample_frame)
    private val sampleShadowRadius =
        sampleFrameRadius + getDimension(R.dimen.mm2d_cc_sample_shadow)
    private val maskRect = Rect(0, 0, TONE_SIZE, TONE_SIZE)
    private val targetRect = Rect()
    private var hue: Float = 0f
    private val colorSampleFrame = getColor(R.color.mm2d_cc_sample_frame)
    private val colorSampleShadow = getColor(R.color.mm2d_cc_sample_shadow)
    private val hsvCache = FloatArray(3)
    var saturation: Float = 0f
        private set
    var value: Float = 0f
        private set
    var onColorChanged: ((color: Int) -> Unit)? = null

    init {
        Thread {
            maskBitmap = createMaskBitmap()
            postInvalidate()
        }.start()
    }

    fun setColor(@ColorInt color: Int) {
        this.color = color
        ColorUtils.colorToHsv(color, hsvCache)
        updateHue(hsvCache[0])
        updateSv(hsvCache[1], hsvCache[2])
    }

    fun setHue(h: Float) {
        color = ColorUtils.hsvToColor(h, saturation, value)
        updateHue(h)
    }

    private fun updateHue(h: Float) {
        if (hue == h) {
            return
        }
        hue = h
        maxColor = ColorUtils.hsvToColor(hue, 1f, 1f)
        invalidate()
    }

    private fun updateSv(s: Float, v: Float, fromUser: Boolean = false) {
        if (saturation == s && value == v) {
            return
        }
        saturation = s
        value = v
        invalidate()
        if (fromUser) {
            onColorChanged?.invoke(color)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            parent.requestDisallowInterceptTouchEvent(true)
        }
        val s = ((event.x - targetRect.left) / targetRect.width()).coerceIn(0f, 1f)
        val v = ((targetRect.bottom - event.y) / targetRect.height()).coerceIn(0f, 1f)
        color = ColorUtils.hsvToColor(hue, s, v)
        updateSv(s, v, true)
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
        val mask = maskBitmap ?: return
        paint.color = maxColor
        canvas.drawRect(targetRect, paint)
        canvas.drawBitmap(mask, maskRect, targetRect, paint)
        val x = saturation * targetRect.width() + targetRect.left
        val y = (1f - value) * targetRect.height() + targetRect.top
        paint.color = colorSampleShadow
        canvas.drawCircle(x, y, sampleShadowRadius, paint)
        paint.color = colorSampleFrame
        canvas.drawCircle(x, y, sampleFrameRadius, paint)
        paint.color = color
        canvas.drawCircle(x, y, sampleRadius, paint)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val paddingHorizontal = paddingLeft + paddingRight
        val paddingVertical = paddingTop + paddingBottom
        val resizeWidth = MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.EXACTLY
        val resizeHeight = MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY

        if (!resizeWidth && !resizeHeight) {
            setMeasuredDimension(
                resolveSizeAndState(
                    max(requestWidth + paddingHorizontal, suggestedMinimumWidth),
                    widthMeasureSpec,
                    MeasureSpec.UNSPECIFIED
                ),
                resolveSizeAndState(
                    max(requestHeight + paddingVertical, suggestedMinimumHeight),
                    heightMeasureSpec,
                    MeasureSpec.UNSPECIFIED
                )
            )
            return
        }

        var widthSize = resolveAdjustedSize(requestWidth + paddingHorizontal, widthMeasureSpec)
        var heightSize = resolveAdjustedSize(requestHeight + paddingVertical, heightMeasureSpec)
        val actualAspect =
            (widthSize - paddingHorizontal).toFloat() / (heightSize - paddingVertical)
        if (abs(actualAspect - 1f) < 0.0000001) {
            setMeasuredDimension(widthSize, heightSize)
            return
        }
        if (resizeWidth) {
            val newWidth = heightSize - paddingVertical + paddingHorizontal
            if (!resizeHeight) {
                widthSize = resolveAdjustedSize(newWidth, widthMeasureSpec)
            }
            if (newWidth <= widthSize) {
                widthSize = newWidth
                setMeasuredDimension(widthSize, heightSize)
                return
            }
        }
        if (resizeHeight) {
            val newHeight = widthSize - paddingHorizontal + paddingVertical
            if (!resizeWidth) {
                heightSize = resolveAdjustedSize(newHeight, heightMeasureSpec)
            }
            if (newHeight <= heightSize) {
                heightSize = newHeight
            }
        }
        setMeasuredDimension(widthSize, heightSize)
    }

    private fun resolveAdjustedSize(desiredSize: Int, measureSpec: Int): Int {
        val specMode = MeasureSpec.getMode(measureSpec)
        val specSize = MeasureSpec.getSize(measureSpec)
        return when (specMode) {
            MeasureSpec.UNSPECIFIED -> desiredSize
            MeasureSpec.AT_MOST -> min(desiredSize, specSize)
            MeasureSpec.EXACTLY -> specSize
            else -> desiredSize
        }
    }

    companion object {
        private const val TONE_MAX = 255f
        private const val TONE_SIZE = 256

        private fun createMaskBitmap(): Bitmap {
            val pixels = IntArray(TONE_SIZE * TONE_SIZE)
            for (y in 0 until TONE_SIZE) {
                for (x in 0 until TONE_SIZE) {
                    pixels[x + y * TONE_SIZE] =
                        ColorUtils.svToMask(x / TONE_MAX, (TONE_MAX - y) / TONE_MAX)
                }
            }
            return Bitmap.createBitmap(pixels, TONE_SIZE, TONE_SIZE, Bitmap.Config.ARGB_8888)
        }
    }
}
