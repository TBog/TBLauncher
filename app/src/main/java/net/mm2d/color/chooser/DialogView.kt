/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.color.chooser

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.graphics.alpha
import androidx.core.view.forEach
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import kotlinx.android.synthetic.main.mm2d_cc_view_dialog.view.*
import net.mm2d.color.chooser.util.toOpacity
import rocks.tbog.tblauncher.R

/**
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
internal class DialogView
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), ColorChangeMediator {
    private val liveData: MutableLiveData<Int> = MutableLiveData()
    val color: Int
        get() = control_view.color

    init {
        orientation = VERTICAL
        inflate(context, R.layout.mm2d_cc_view_dialog, this)
    }

    fun init(color: Int, lifecycleOwner: LifecycleOwner) {
        onChangeColor(color.toOpacity())
        control_view.setAlpha(color.alpha)
        val pages: List<Pair<String, View>> = listOf(
            "palette" to PaletteView(context),
            "hsv" to HsvPage(context),
            "rgb" to SliderPage(context)
        )
        view_pager.adapter = ViewPagerAdapter(pages)
        tab_layout.setupWithViewPager(view_pager)
        pages.forEach { observeRecursively(it.second, lifecycleOwner) }
        observeRecursively(control_view, lifecycleOwner)
    }

    private fun observeRecursively(view: View, lifecycleOwner: LifecycleOwner) {
        if (view is ColorObserver) liveData.observe(lifecycleOwner, view)
        (view as? ViewGroup)?.forEach { observeRecursively(it, lifecycleOwner) }
    }

    fun addObserver(obs: ColorObserver, lifecycleOwner: LifecycleOwner) {
        liveData.observe(lifecycleOwner, obs)
    }

    fun setWithAlpha(withAlpha: Boolean) {
        control_view.setWithAlpha(withAlpha)
    }

    override fun onChangeColor(color: Int) {
        if (liveData.value == color) return
        liveData.value = color
    }
}
