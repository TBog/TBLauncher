/*
 * Copyright (c) 2020 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.color.chooser.util

import android.content.Context
import android.util.TypedValue
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.Dimension
import androidx.core.content.ContextCompat

@ColorInt
internal fun View.getColor(@ColorRes id: Int): Int =
    ContextCompat.getColor(context, id)

@Dimension
internal fun View.getDimension(@DimenRes id: Int): Float =
    resources.getDimension(id)

@Dimension
internal fun View.getPixels(@DimenRes id: Int): Int =
    resources.getDimensionPixelSize(id)

@Dimension
internal fun Int.toPixelsAsDp(context: Context): Int =
    TypedValue.complexToDimensionPixelSize(this, context.resources.displayMetrics)
