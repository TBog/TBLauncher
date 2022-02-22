/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.color.chooser

import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import net.mm2d.color.chooser.ViewPagerAdapter.ViewHolder

internal class ViewPagerAdapter(
    pageViews: List<View>
) : RecyclerView.Adapter<ViewHolder>() {
    private val pageViews = pageViews.toList().onEach {
        it.id = ViewCompat.generateViewId()
        it.layoutParams = RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(pageViews[viewType])

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = Unit
    override fun getItemViewType(position: Int): Int = position
    override fun getItemCount(): Int = pageViews.size
}
