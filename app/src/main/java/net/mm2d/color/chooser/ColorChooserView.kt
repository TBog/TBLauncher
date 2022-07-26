/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.color.chooser

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.alpha
import androidx.core.view.doOnLayout
import androidx.lifecycle.MutableLiveData
import com.google.android.material.tabs.TabLayoutMediator
import rocks.tbog.tblauncher.databinding.Mm2dCcViewDialogBinding
import net.mm2d.color.chooser.util.toOpacity

internal class ColorChooserView
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), ColorLiveDataOwner {
    private val colorLiveData: MutableLiveData<Int> = MutableLiveData()
    private val binding: Mm2dCcViewDialogBinding =
        Mm2dCcViewDialogBinding.inflate(LayoutInflater.from(context), this)
    val color: Int
        get() = binding.controlView.color

    fun init(color: Int) {
        colorLiveData.value = color.toOpacity()
        binding.controlView.setAlpha(color.alpha)
        val pageTitles: List<String> = listOf("palette", "hsv", "rgb")
        val pageViews: List<View> = listOf(
            PaletteView(context), HsvView(context), SliderView(context),
        )
        binding.viewPager.adapter = ViewPagerAdapter(pageViews)
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = pageTitles[position]
        }.attach()
    }

    fun setCurrentItem(position: Int) {
        binding.viewPager.doOnLayout {
            binding.viewPager.post {
                binding.viewPager.setCurrentItem(position, false)
            }
        }
    }

    fun getCurrentItem(): Int = binding.viewPager.currentItem

    fun setWithAlpha(withAlpha: Boolean) {
        binding.controlView.setWithAlpha(withAlpha)
    }

    override fun getColorLiveData(): MutableLiveData<Int> = colorLiveData
}
