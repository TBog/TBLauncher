/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.color.chooser.element

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.Style
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import rocks.tbog.tblauncher.R.drawable
import net.mm2d.color.chooser.util.ColorUtils
import kotlin.math.min

internal class PaletteCell @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val icon: Drawable = loadIcon(context)
    private var color: Int = Color.TRANSPARENT
    private val paint: Paint = Paint().also {
        it.style = Style.FILL_AND_STROKE
    }
    var checked: Boolean = false

    fun setColor(color: Int) {
        this.color = color
        paint.color = color
        isEnabled = color != Color.TRANSPARENT
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val size = min(min(width, height), icon.intrinsicWidth)
        icon.setBounds((w - size) / 2, (h - size) / 2, (w + size) / 2, (h + size) / 2)
    }

    override fun onDraw(canvas: Canvas) {
        if (color == Color.TRANSPARENT) return
        canvas.drawColor(color)
        if (checked) {
            DrawableCompat.setTint(icon, selectForeground(color))
            icon.draw(canvas)
        }
    }

    companion object {
        private var icon: Drawable? = null

        private fun loadIcon(context: Context): Drawable =
            icon ?: loadIconInner(context).also { icon = it }

        private fun loadIconInner(context: Context): Drawable =
            AppCompatResources.getDrawable(context, drawable.mm2d_cc_ic_check)!!.wrap()

        private fun Drawable.wrap(): Drawable = DrawableCompat.wrap(this)

        fun selectForeground(background: Int): Int =
            if (ColorUtils.shouldUseWhiteForeground(background)) Color.WHITE else Color.BLACK
    }
}
