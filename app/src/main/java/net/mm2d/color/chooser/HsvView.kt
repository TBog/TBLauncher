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
import net.mm2d.color.chooser.util.ColorUtils
import rocks.tbog.tblauncher.databinding.Mm2dCcViewHsvBinding

internal class HsvView
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), ColorObserver {
    private val colorChangeMediator by lazy {
        findColorChangeMediator()
    }
    private var color: Int = Color.BLACK
    private val binding: Mm2dCcViewHsvBinding =
        Mm2dCcViewHsvBinding.inflate(LayoutInflater.from(context), this)

    init {
        binding.svView.onColorChanged = {
            color = it
            colorChangeMediator?.onChangeColor(color)
        }
        binding.hueView.onHueChanged = {
            color = ColorUtils.hsvToColor(it, binding.svView.saturation, binding.svView.value)
            binding.svView.setHue(it)
            colorChangeMediator?.onChangeColor(color)
        }
    }

    override fun onChanged(color: Int?) {
        if (color == null) return
        if (this.color == color) return
        this.color = color
        binding.svView.setColor(color)
        binding.hueView.setColor(color)
    }
}
