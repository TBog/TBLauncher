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
import android.view.LayoutInflater
import android.widget.LinearLayout
import rocks.tbog.tblauncher.databinding.Mm2dCcViewSliderBinding

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
    private var binding: Mm2dCcViewSliderBinding = Mm2dCcViewSliderBinding.inflate(LayoutInflater.from(context), this)

    init {
        orientation = VERTICAL
        binding.seekRed.onValueChanged = { value, fromUser ->
            binding.textRed.text = value.toString()
            updateBySeekBar(fromUser)
        }
        binding.seekGreen.onValueChanged = { value, fromUser ->
            binding.textGreen.text = value.toString()
            updateBySeekBar(fromUser)
        }
        binding.seekBlue.onValueChanged = { value, fromUser ->
            binding.textBlue.text = value.toString()
            updateBySeekBar(fromUser)
        }
    }

    override fun onChanged(color: Int?) {
        if (color == null) return
        binding.seekRed.setValue(Color.red(color))
        binding.seekGreen.setValue(Color.green(color))
        binding.seekBlue.setValue(Color.blue(color))
    }

    private fun updateBySeekBar(fromUser: Boolean) {
        if (!fromUser) return
        val color = Color.rgb(binding.seekRed.value, binding.seekGreen.value, binding.seekBlue.value)
        colorChangeMediator?.onChangeColor(color)
    }
}
