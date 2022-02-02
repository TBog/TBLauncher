/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.color.chooser

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import rocks.tbog.tblauncher.databinding.Mm2dCcColorChooserBinding
import rocks.tbog.tblauncher.ui.DialogFragment
import rocks.tbog.tblauncher.utils.UIColors

/**
 * Color chooser dialog
 */
object ColorChooserDialog {
    private const val KEY_REQUEST_KEY = "KEY_REQUEST_KEY"
    private const val KEY_INITIAL_COLOR = "KEY_INITIAL_COLOR"
    private const val KEY_WITH_ALPHA = "KEY_WITH_ALPHA"
    private const val KEY_INITIAL_TAB = "KEY_INITIAL_TAB"
    private const val RESULT_KEY_COLOR = "RESULT_KEY_COLOR"
    private const val TAG = "ColorChooserDialog"
    const val TAB_PALETTE: Int = 0
    const val TAB_HSV: Int = 1
    const val TAB_RGB: Int = 2

    /**
     * Listener receiving the result.
     *
     * Register using registerListener at the timing of onCreate of activity or onViewCreated of fragment.
     */
    fun interface ColorChooserListener {
        /**
         * Called when the color selection is confirmed.
         *
         * Not called if canceled.
         *
         * @param color selected color
         */
        fun onColorSelected(@ColorInt color: Int)
    }

    /**
     * Register result listener.
     *
     * Call at the timing of onCreate of activity.
     *
     * @param activity Caller fragment activity
     * @param requestKey Request Key, pass the same value to the `show`
     * @param listener Listener receiving the result
     */
    fun registerListener(
        activity: FragmentActivity,
        requestKey: String,
        listener: ColorChooserListener
    ) {
        registerListener(
            activity.supportFragmentManager,
            requestKey,
            activity,
            listener
        )
    }

    /**
     * Register result listener.
     *
     * Call at the timing of onViewCreated of fragment.
     *
     * @param fragment Caller fragment
     * @param requestKey Request Key, pass the same value to the `show`
     * @param listener Listener receiving the result
     */
    fun registerListener(
        fragment: Fragment,
        requestKey: String,
        listener: ColorChooserListener
    ) {
        registerListener(
            fragment.childFragmentManager,
            requestKey,
            fragment.viewLifecycleOwner,
            listener
        )
    }

    private fun registerListener(
        manager: FragmentManager,
        requestKey: String,
        lifecycleOwner: LifecycleOwner,
        listener: ColorChooserListener
    ) {
        manager.setFragmentResultListener(requestKey, lifecycleOwner) { _, result ->
            listener.onColorSelected(result.getInt(RESULT_KEY_COLOR))
        }
    }

    /**
     * Show dialog.
     *
     * @param activity FragmentActivity
     * @param requestKey Request Key used for registration with registerListener
     * @param initialColor initial color
     * @param withAlpha if true, alpha section is enabled
     * @param initialTab initial tab, TAB_PALETTE/TAB_HSV/TAB_RGB
     */
    fun show(
        activity: FragmentActivity,
        requestKey: String,
        @ColorInt initialColor: Int = Color.WHITE,
        withAlpha: Boolean = false,
        initialTab: Int = TAB_PALETTE
    ) {
        show(
            activity.supportFragmentManager,
            bundleOf(
                KEY_REQUEST_KEY to requestKey,
                KEY_INITIAL_COLOR to initialColor,
                KEY_WITH_ALPHA to withAlpha,
                KEY_INITIAL_TAB to initialTab,
            )
        )
    }

    /**
     * Show dialog.
     *
     * @param fragment Fragment
     * @param requestKey Request Key used for registration with registerListener
     * @param initialColor initial color
     * @param withAlpha if true, alpha section is enabled
     * @param initialTab initial tab, TAB_PALETTE/TAB_HSV/TAB_RGB
     */
    fun show(
        fragment: Fragment,
        requestKey: String,
        @ColorInt initialColor: Int = Color.WHITE,
        withAlpha: Boolean = false,
        initialTab: Int = TAB_PALETTE
    ) {
        show(
            fragment.childFragmentManager,
            bundleOf(
                KEY_REQUEST_KEY to requestKey,
                KEY_INITIAL_COLOR to initialColor,
                KEY_WITH_ALPHA to withAlpha,
                KEY_INITIAL_TAB to initialTab,
            )
        )
    }

    private fun show(manager: FragmentManager, arguments: Bundle) {
        if (manager.findFragmentByTag(TAG) != null) return
        if (manager.isStateSaved) return
        ColorChooserDialogImpl().also {
            it.arguments = arguments
        }.show(manager, TAG)
    }

    internal class ColorChooserDialogImpl : DialogFragment<Int>() {
        private var _colorChooserView: ColorChooserView? = null
        private val colorChooserView: ColorChooserView
            get() = _colorChooserView ?: throw IllegalStateException()

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            val dialogContext = requireDialog().context

            setupDefaultButtonOkCancel(dialogContext)
            setOnPositiveClickListener { _, _ -> notifySelect() }

            if (savedInstanceState != null) {
                val tab = savedInstanceState.getInt(KEY_INITIAL_TAB, TAB_RGB)
                val color = savedInstanceState.getInt(KEY_INITIAL_COLOR, UIColors.COLOR_DEFAULT)
                val arguments = requireArguments()
                arguments.putInt(KEY_INITIAL_TAB, tab)
                arguments.putInt(KEY_INITIAL_COLOR, color)
            }

            // make sure we use the dialog context
            val dialogInflater = inflater.cloneInContext(dialogContext)
            return super.onCreateView(dialogInflater, container, savedInstanceState)
        }

        override fun onDestroyView() {
            super.onDestroyView()
            _colorChooserView = null
        }

        override fun onSaveInstanceState(outState: Bundle) {
            super.onSaveInstanceState(outState)
            outState.putInt(KEY_INITIAL_TAB, colorChooserView.getCurrentItem())
            outState.putInt(KEY_INITIAL_COLOR, colorChooserView.color)
        }

        private fun notifySelect() {
            val key = requireArguments().getString(KEY_REQUEST_KEY) ?: return
            parentFragmentManager.setFragmentResult(
                key, bundleOf(RESULT_KEY_COLOR to colorChooserView.color)
            )
        }

        override fun layoutRes(): Int {
            // We'll override inflateLayoutRes so we don't need this
            return 0;
        }

        override fun inflateLayoutRes(inflater: LayoutInflater, container: ViewGroup?): View {
            _colorChooserView = Mm2dCcColorChooserBinding.inflate(inflater).root

            val arguments = requireArguments()
            colorChooserView.setCurrentItem(arguments.getInt(KEY_INITIAL_TAB, TAB_RGB))
            colorChooserView.init(arguments.getInt(KEY_INITIAL_COLOR, UIColors.COLOR_DEFAULT), this)
            colorChooserView.setWithAlpha(arguments.getBoolean(KEY_WITH_ALPHA))

            return colorChooserView
        }
    }
}
