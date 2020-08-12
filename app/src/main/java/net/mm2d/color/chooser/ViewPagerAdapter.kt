/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.color.chooser

import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter

/**
 * @author [大前良介 (OHMAE Ryosuke)](mailto:ryo@mm2d.net)
 */
internal class ViewPagerAdapter(
    viewList: List<Pair<String, View>>
) : PagerAdapter() {
    private val list = viewList.toList()
    override fun isViewFromObject(view: View, obj: Any): Boolean = view == obj
    override fun getCount(): Int = list.size
    override fun getPageTitle(position: Int): CharSequence? = list[position].first

    override fun instantiateItem(container: ViewGroup, position: Int): Any =
        list[position].second.also { container.addView(it) }

    override fun destroyItem(container: ViewGroup, position: Int, obj: Any) =
        container.removeView(obj as View)
}
