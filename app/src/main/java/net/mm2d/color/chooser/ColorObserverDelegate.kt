package net.mm2d.color.chooser

import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.distinctUntilChanged

internal class ColorObserverDelegate<T>(
    private val target: T
) where T : View,
        T : Observer<Int> {
    private var colorLiveData: MutableLiveData<Int>? = null

    fun onAttachedToWindow() {
        val owner = target.findColorLiveDataOwner()
            ?: throw IllegalStateException("parent is not ColorLiveDataOwner")
        val liveData = owner.getColorLiveData()
        liveData.distinctUntilChanged()
            .observeForever(target)
        colorLiveData = liveData
    }

    fun onDetachedFromWindow() {
        colorLiveData?.removeObserver(target)
        colorLiveData = null
    }

    fun post(color: Int) {
        colorLiveData?.postValue(color)
    }
}
