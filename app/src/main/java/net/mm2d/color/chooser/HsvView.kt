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
import rocks.tbog.tblauncher.databinding.Mm2dCcViewHsvBinding
import net.mm2d.color.chooser.util.ColorUtils

internal class HsvView
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), Observer<Int> {
    private val delegate = ColorObserverDelegate(this)
    private var color: Int = Color.BLACK
    private val binding: Mm2dCcViewHsvBinding =
        Mm2dCcViewHsvBinding.inflate(LayoutInflater.from(context), this)

    init {
        binding.svView.onColorChanged = {
            color = it
            delegate.post(color)
        }
        binding.hueView.onHueChanged = {
            color = ColorUtils.hsvToColor(it, binding.svView.saturation, binding.svView.value)
            binding.svView.setHue(it)
            delegate.post(color)
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
        if (this.color == value) return
        this.color = value
        binding.svView.setColor(value)
        binding.hueView.setColor(value)
    }
}
