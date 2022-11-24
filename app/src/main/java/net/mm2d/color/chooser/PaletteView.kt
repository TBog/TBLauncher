/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.color.chooser

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.content.res.TypedArray
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ArrayRes
import androidx.core.content.res.getColorOrThrow
import androidx.core.content.res.getResourceIdOrThrow
import androidx.core.content.res.use
import androidx.core.view.children
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.mm2d.color.chooser.element.PaletteCell
import net.mm2d.color.chooser.util.getPixels
import rocks.tbog.tblauncher.R
import java.lang.ref.SoftReference
import kotlin.collections.forEach as kForEach

internal class PaletteView
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr), Observer<Int> {
    private val delegate = ColorObserverDelegate(this)
    private val cellHeight = getPixels(R.dimen.mm2d_cc_palette_cell_height)
    private val cellAdapter = CellAdapter(context)
    private val linearLayoutManager = LinearLayoutManager(context)

    init {
        val padding = resources.getDimensionPixelSize(R.dimen.mm2d_cc_palette_padding)
        setPadding(0, padding, 0, padding)
        clipToPadding = false
        setHasFixedSize(true)
        overScrollMode = View.OVER_SCROLL_NEVER
        itemAnimator = null
        layoutManager = linearLayoutManager
        adapter = cellAdapter
        isVerticalFadingEdgeEnabled = true
        setFadingEdgeLength(padding)
        cellAdapter.onColorChanged = {
            delegate.post(it)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        delegate.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        delegate.onDetachedFromWindow()
    }

    override fun isPaddingOffsetRequired(): Boolean = true
    override fun getTopPaddingOffset(): Int = -paddingTop
    override fun getBottomPaddingOffset(): Int = paddingBottom

    override fun onChanged(color: Int?) {
        color ?: return
        cellAdapter.setColor(color)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (oldh == 0 && h > 0) {
            linearLayoutManager.scrollToPositionWithOffset(cellAdapter.index, (h - cellHeight) / 2)
        }
    }

    private class CellAdapter(context: Context) : Adapter<CellHolder>() {
        private val inflater: LayoutInflater = LayoutInflater.from(context)
        private val list: List<IntArray> = createPalette(context)
        private var color: Int = 0
        var onColorChanged: ((color: Int) -> Unit)? = null
        var index: Int = -1
            private set

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CellHolder =
            CellHolder(inflater.inflate(R.layout.mm2d_cc_item_palette, parent, false))
                .also { holder -> holder.onColorChanged = { onColorChanged?.invoke(it) } }

        override fun onBindViewHolder(holder: CellHolder, position: Int) =
            holder.apply(list[position], color)

        override fun getItemCount(): Int = list.size

        fun setColor(newColor: Int) {
            if (color == newColor) return
            color = newColor
            val newIndex = list.indexOfFirst { it.contains(newColor) }
            val lastIndex = index
            index = newIndex
            if (lastIndex >= 0) notifyItemChanged(lastIndex)
            if (newIndex >= 0) notifyItemChanged(newIndex)
        }
    }

    private class CellHolder(itemView: View) : ViewHolder(itemView) {
        private val viewList: List<PaletteCell> = (itemView as ViewGroup).children
            .map { it as PaletteCell }
            .toList()
        var onColorChanged: ((color: Int) -> Unit)? = null

        init {
            viewList.kForEach {
                it.setOnClickListener(::performOnColorChanged)
            }
        }

        private fun performOnColorChanged(view: View) {
            onColorChanged?.invoke(view.tag as? Int ?: return)
        }

        fun apply(colors: IntArray, selected: Int) {
            viewList.withIndex().kForEach { (i, view) ->
                val color = if (i < colors.size) colors[i] else Color.TRANSPARENT
                view.tag = color
                view.setColor(color)
                view.checked = color == selected
            }
        }
    }

    companion object {
        private var cache: SoftReference<List<IntArray>> = SoftReference<List<IntArray>>(null)

        @SuppressLint("Recycle")
        private fun <R> Resources.useTypedArray(@ArrayRes id: Int, block: TypedArray.() -> R): R =
            obtainTypedArray(id).use { it.block() }

        private fun createPalette(context: Context): List<IntArray> {
            cache.get()?.let { return it }
            val resources = context.resources
            return resources.useTypedArray(R.array.material_colors) {
                (0 until length()).map { resources.readColorArray(getResourceIdOrThrow(it)) }
            }.also {
                cache = SoftReference(it)
            }
        }

        private fun Resources.readColorArray(id: Int): IntArray =
            useTypedArray(id) { IntArray(length()) { getColorOrThrow(it) } }
    }
}
