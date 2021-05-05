/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.color.chooser

import androidx.core.view.forEach as kForEach
import kotlin.collections.forEach as kForEach
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.graphics.alpha
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import net.mm2d.color.chooser.util.toOpacity
import rocks.tbog.tblauncher.databinding.Mm2dCcViewDialogBinding

/**
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
internal class DialogView
@JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), ColorChangeMediator {
    private var binding: Mm2dCcViewDialogBinding = Mm2dCcViewDialogBinding.inflate(LayoutInflater.from(context), this)
    private val liveData: MutableLiveData<Int> = MutableLiveData()
    val color: Int
        get() = binding.controlView.color

    init {
        orientation = VERTICAL
    }

    fun init(color: Int, lifecycleOwner: LifecycleOwner) {
        onChangeColor(color.toOpacity())
        binding.controlView.setAlpha(color.alpha)
        val pages: List<Pair<String, View>> = listOf(
                "palette" to PaletteView(context),
                "hsv" to HsvPage(context),
                "rgb" to SliderPage(context)
        )
        binding.viewPager.adapter = ViewPagerAdapter(pages)
        binding.tabLayout.setupWithViewPager(binding.viewPager)
        pages.kForEach { observeRecursively(it.second, lifecycleOwner) }
        observeRecursively(binding.controlView, lifecycleOwner)
    }

    private fun observeRecursively(view: View, lifecycleOwner: LifecycleOwner) {
        if (view is ColorObserver) liveData.observe(lifecycleOwner, view)
        (view as? ViewGroup)?.kForEach { observeRecursively(it, lifecycleOwner) }
    }

    fun addObserver(obs: ColorObserver, lifecycleOwner: LifecycleOwner) {
        liveData.observe(lifecycleOwner, obs)
    }

    fun setWithAlpha(withAlpha: Boolean) {
        binding.controlView.setWithAlpha(withAlpha)
    }

    override fun onChangeColor(color: Int) {
        if (liveData.value == color) return
        liveData.value = color
    }
}
