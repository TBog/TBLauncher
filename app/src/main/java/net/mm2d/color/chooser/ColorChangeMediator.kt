/*
 * Copyright (c) 2020 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.color.chooser

import android.view.View

interface ColorChangeMediator {
    fun onChangeColor(color: Int)
}

internal fun View.findColorChangeMediator(): ColorChangeMediator? {
    if (this is ColorChangeMediator) return this
    val parent = parent
    return if (parent !is View) null else parent.findColorChangeMediator()
}
