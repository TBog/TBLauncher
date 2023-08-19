/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.color.chooser

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.text.*
import android.text.InputFilter.LengthFilter
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.alpha
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import net.mm2d.color.chooser.util.resolveColor
import net.mm2d.color.chooser.util.setAlpha
import net.mm2d.color.chooser.util.toOpacity
import rocks.tbog.tblauncher.R
import rocks.tbog.tblauncher.databinding.Mm2dCcViewControlBinding
import java.util.*

internal class ControlView
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), Observer<Int> {
    private val delegate = ColorObserverDelegate(this)
    private val normalTint =
        ColorStateList.valueOf(context.resolveColor(R.attr.colorAccent, Color.BLUE))
    private val errorTint =
        ColorStateList.valueOf(context.resolveColor(R.attr.colorError, Color.RED))
    private var changeHexTextByUser = true
    private var hasAlpha: Boolean = true
    private val rgbFilter = arrayOf(HexadecimalFilter(), LengthFilter(6))
    private val argbFilter = arrayOf(HexadecimalFilter(), LengthFilter(8))
    private val binding: Mm2dCcViewControlBinding =
        Mm2dCcViewControlBinding.inflate(LayoutInflater.from(context), this)
    var color: Int = Color.BLACK
        private set

    init {
        binding.colorPreview.setColor(color)
        binding.seekAlpha.setValue(color.alpha)
        binding.seekAlpha.onValueChanged = { value, fromUser ->
            binding.textAlpha.text = value.toString()
            if (fromUser) {
                setAlpha(value)
            }
        }
        binding.editHex.filters = argbFilter
        binding.editHex.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = Unit
            override fun beforeTextChanged(s: CharSequence?, start: Int, c: Int, a: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!changeHexTextByUser) {
                    return
                }
                if (s.isNullOrEmpty()) {
                    setError()
                    return
                }
                try {
                    color = Color.parseColor("#$s")
                    clearError()
                    binding.colorPreview.setColor(color)
                    binding.seekAlpha.setValue(color.alpha)
                    delegate.post(color.toOpacity())
                } catch (e: IllegalArgumentException) {
                    setError()
                }
            }
        })
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        delegate.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        delegate.onDetachedFromWindow()
    }

    fun setAlpha(alpha: Int) {
        binding.seekAlpha.setValue(alpha)
        color = color.setAlpha(alpha)
        binding.colorPreview.setColor(color)
        setColorToHexText()
    }

    fun setWithAlpha(withAlpha: Boolean) {
        hasAlpha = withAlpha
        binding.seekAlpha.isVisible = withAlpha
        binding.textAlpha.isVisible = withAlpha
        if (withAlpha) {
            binding.editHex.filters = argbFilter
        } else {
            binding.editHex.filters = rgbFilter
            setAlpha(0xff)
        }
    }

    private fun setError() {
        ViewCompat.setBackgroundTintList(binding.editHex, errorTint)
    }

    private fun clearError() {
        ViewCompat.setBackgroundTintList(binding.editHex, normalTint)
    }

    override fun onChanged(value: Int) {
        if (this.color.toOpacity() == value) return
        this.color = value.setAlpha(binding.seekAlpha.value)
        binding.colorPreview.setColor(this.color)
        setColorToHexText()
        binding.seekAlpha.setMaxColor(value)
    }

    @SuppressLint("SetTextI18n")
    private fun setColorToHexText() {
        changeHexTextByUser = false
        if (hasAlpha) {
            binding.editHex.setText("%08X".format(color))
        } else {
            binding.editHex.setText("%06X".format(color and 0xffffff))
        }
        clearError()
        changeHexTextByUser = true
    }

    private class HexadecimalFilter : InputFilter {
        override fun filter(
            source: CharSequence?, start: Int, end: Int, dest: Spanned?, dstart: Int, dend: Int
        ): CharSequence? {
            val converted = source.toString()
                .replace("[^0-9a-fA-F]".toRegex(), "")
                .uppercase(Locale.ENGLISH)
            if (source.toString() == converted) return null
            if (source !is Spanned) return converted
            return SpannableString(converted).also {
                TextUtils.copySpansFrom(source, 0, converted.length, null, it, 0)
            }
        }
    }
}
