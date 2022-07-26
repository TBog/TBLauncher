package net.mm2d.color.chooser

import android.view.View
import androidx.lifecycle.MutableLiveData

internal interface ColorLiveDataOwner {
    fun getColorLiveData(): MutableLiveData<Int>
}

internal fun View.findColorLiveDataOwner(): ColorLiveDataOwner? {
    if (this is ColorLiveDataOwner) return this
    val parent = parent
    return if (parent !is View) null else parent.findColorLiveDataOwner()
}
