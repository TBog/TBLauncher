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
import android.widget.LinearLayout
import androidx.core.graphics.alpha
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.mm2d_cc_view_control.view.*
import net.mm2d.color.chooser.util.resolveColor
import net.mm2d.color.chooser.util.setAlpha
import net.mm2d.color.chooser.util.toOpacity
import java.util.*

/**
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
internal class ControlView
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), ColorObserver {
    private val colorChangeMediator by lazy {
        findColorChangeMediator()
    }
    private val normalTint =
        ColorStateList.valueOf(context.resolveColor(R.attr.colorAccent, Color.BLUE))
    private val errorTint =
        ColorStateList.valueOf(context.resolveColor(R.attr.colorError, Color.RED))
    private var changeHexTextByUser = true
    private var hasAlpha: Boolean = true
    private val rgbFilter = arrayOf(HexadecimalFilter(), LengthFilter(6))
    private val argbFilter = arrayOf(HexadecimalFilter(), LengthFilter(8))
    var color: Int = Color.BLACK
        private set

    init {
        orientation = VERTICAL
        inflate(context, R.layout.mm2d_cc_view_control, this)
        color_preview.setColor(color)
        seek_alpha.setValue(color.alpha)
        seek_alpha.onValueChanged = { value, fromUser ->
            text_alpha.text = value.toString()
            if (fromUser) {
                setAlpha(value)
            }
        }
        edit_hex.filters = argbFilter
        edit_hex.addTextChangedListener(object : TextWatcher {
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
                    color_preview.setColor(color)
                    seek_alpha.setValue(color.alpha)
                    colorChangeMediator?.onChangeColor(color.toOpacity())
                } catch (e: IllegalArgumentException) {
                    setError()
                }
            }
        })
    }

    fun setAlpha(alpha: Int) {
        seek_alpha.setValue(alpha)
        color = color.setAlpha(alpha)
        color_preview.setColor(color)
        setColorToHexText()
    }

    fun setWithAlpha(withAlpha: Boolean) {
        hasAlpha = withAlpha
        section_alpha.isVisible = withAlpha
        if (withAlpha) {
            edit_hex.filters = argbFilter
        } else {
            edit_hex.filters = rgbFilter
            setAlpha(0xff)
        }
    }

    private fun setError() {
        ViewCompat.setBackgroundTintList(edit_hex, errorTint)
    }

    private fun clearError() {
        ViewCompat.setBackgroundTintList(edit_hex, normalTint)
    }

    override fun onChanged(color: Int?) {
        if (color == null) return
        if (this.color.toOpacity() == color) return
        this.color = color.setAlpha(seek_alpha.value)
        color_preview.setColor(this.color)
        setColorToHexText()
        seek_alpha.setMaxColor(color)
    }

    @SuppressLint("SetTextI18n")
    private fun setColorToHexText() {
        changeHexTextByUser = false
        if (hasAlpha) {
            edit_hex.setText("%08X".format(color))
        } else {
            edit_hex.setText("%06X".format(color and 0xffffff))
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
                .toUpperCase(Locale.ENGLISH)
            if (source.toString() == converted) return null
            if (source !is Spanned) return converted
            return SpannableString(converted).also {
                TextUtils.copySpansFrom(source, 0, converted.length, null, it, 0)
            }
        }
    }
}
