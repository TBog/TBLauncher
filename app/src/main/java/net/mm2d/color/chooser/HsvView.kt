/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.color.chooser

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.mm2d_cc_view_hsv.view.*
import net.mm2d.color.chooser.util.ColorUtils
import rocks.tbog.tblauncher.R

/**
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
internal class HsvView
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), ColorObserver {
    private val colorChangeMediator by lazy {
        findColorChangeMediator()
    }
    private var color: Int = Color.BLACK

    init {
        inflate(context, R.layout.mm2d_cc_view_hsv, this)
        sv_view.onColorChanged = {
            color = it
            colorChangeMediator?.onChangeColor(color)
        }
        hue_view.onHueChanged = {
            color = ColorUtils.hsvToColor(it, sv_view.saturation, sv_view.value)
            sv_view.setHue(it)
            colorChangeMediator?.onChangeColor(color)
        }
    }

    override fun onChanged(color: Int?) {
        if (color == null) return
        if (this.color == color) return
        this.color = color
        sv_view.setColor(color)
        hue_view.setColor(color)
    }
}
