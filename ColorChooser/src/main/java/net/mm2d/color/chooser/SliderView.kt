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
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.mm2d_cc_view_slider.view.*

/**
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
internal class SliderView
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), ColorObserver {
    private val colorChangeMediator by lazy {
        findColorChangeMediator()
    }

    init {
        orientation = VERTICAL
        inflate(context, R.layout.mm2d_cc_view_slider, this)
        seek_red.onValueChanged = { value, fromUser ->
            text_red.text = value.toString()
            updateBySeekBar(fromUser)
        }
        seek_green.onValueChanged = { value, fromUser ->
            text_green.text = value.toString()
            updateBySeekBar(fromUser)
        }
        seek_blue.onValueChanged = { value, fromUser ->
            text_blue.text = value.toString()
            updateBySeekBar(fromUser)
        }
    }

    override fun onChanged(color: Int?) {
        if (color == null) return
        seek_red.setValue(Color.red(color))
        seek_green.setValue(Color.green(color))
        seek_blue.setValue(Color.blue(color))
    }

    private fun updateBySeekBar(fromUser: Boolean) {
        if (!fromUser) return
        val color = Color.rgb(seek_red.value, seek_green.value, seek_blue.value)
        colorChangeMediator?.onChangeColor(color)
    }
}
