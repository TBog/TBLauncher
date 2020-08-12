/*
 * Copyright (c) 2019 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.color.chooser.util

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.StyleRes
import androidx.core.content.res.use

@ColorInt
internal fun Context.resolveColor(
    @AttrRes attr: Int,
    @ColorInt defaultColor: Int
): Int = resolveColor(0, attr, defaultColor)

@SuppressLint("Recycle")
@ColorInt
internal fun Context.resolveColor(
    @StyleRes style: Int,
    @AttrRes attr: Int,
    @ColorInt defaultColor: Int
): Int = obtainStyledAttributes(style, intArrayOf(attr))
    .use { it.getColor(0, defaultColor) }
