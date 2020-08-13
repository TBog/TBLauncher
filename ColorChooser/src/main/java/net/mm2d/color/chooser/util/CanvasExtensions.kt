/*
 * Copyright (c) 2020 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.color.chooser.util

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect

internal fun Canvas.drawRectWithOffset(rect: Rect, offset: Float, paint: Paint) =
    drawRect(
        rect.left - offset,
        rect.top - offset,
        rect.right + offset,
        rect.bottom + offset,
        paint
    )
