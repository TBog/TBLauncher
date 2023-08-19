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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Observer
import rocks.tbog.tblauncher.databinding.Mm2dCcViewSliderBinding

internal class SliderView
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), Observer<Int> {
    private val delegate = ColorObserverDelegate(this)
    private val binding: Mm2dCcViewSliderBinding =
        Mm2dCcViewSliderBinding.inflate(LayoutInflater.from(context), this)

    init {
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

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        delegate.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        delegate.onDetachedFromWindow()
    }

    override fun onChanged(value: Int) {
        binding.seekRed.setValue(Color.red(value))
        binding.seekGreen.setValue(Color.green(value))
        binding.seekBlue.setValue(Color.blue(value))
    }

    private fun updateBySeekBar(fromUser: Boolean) {
        if (!fromUser) return
        val color = Color.rgb(
            binding.seekRed.value,
            binding.seekGreen.value,
            binding.seekBlue.value
        )
        delegate.post(color)
    }
}
