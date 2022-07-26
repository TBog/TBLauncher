/*
 * Copyright (c) 2018 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.color.chooser

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import androidx.annotation.ColorInt
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import rocks.tbog.tblauncher.databinding.Mm2dCcColorChooserBinding

/**
 * Color chooser dialog
 */
object ColorChooserDialog {
    private const val KEY_REQUEST_KEY = "KEY_REQUEST_KEY"
    private const val KEY_INITIAL_COLOR = "KEY_INITIAL_COLOR"
    private const val KEY_WITH_ALPHA = "KEY_WITH_ALPHA"
    private const val KEY_INITIAL_TAB = "KEY_INITIAL_TAB"
    private const val RESULT_KEY_COLOR = "RESULT_KEY_COLOR"
    private const val RESULT_KEY_CANCEL = "RESULT_KEY_CANCEL"
    private const val TAG = "ColorChooserDialog"
    const val TAB_PALETTE: Int = 0
    const val TAB_HSV: Int = 1
    const val TAB_RGB: Int = 2

    /**
     * Listener receiving the result.
     *
     * Register using registerListener at the timing of onCreate of activity or onViewCreated of fragment.
     */
    @Deprecated("Use lambda expression")
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
    @Deprecated(
        "Use the lambda version",
        ReplaceWith(
            "registerListener(activity, requestKey, { listener.onColorSelected(it) }, {},)",
            "net.mm2d.color.chooser.ColorChooserDialog.registerListener"
        )
    )
    fun registerListener(
        activity: FragmentActivity,
        requestKey: String,
        listener: ColorChooserListener
    ) {
        registerListener(
            activity,
            requestKey,
            { listener.onColorSelected(it) },
            {},
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
    @Deprecated(
        "Use the lambda version",
        ReplaceWith(
            "registerListener(fragment, requestKey, { listener.onColorSelected(it) }, {},)",
            "net.mm2d.color.chooser.ColorChooserDialog.registerListener"
        )
    )
    fun registerListener(
        fragment: Fragment,
        requestKey: String,
        listener: ColorChooserListener
    ) {
        registerListener(
            fragment,
            requestKey,
            { listener.onColorSelected(it) },
            {},
        )
    }

    /**
     * Register result listener.
     *
     * Call at the timing of onCreate of activity.
     *
     * @param activity Caller fragment activity
     * @param requestKey Request Key, pass the same value to the `show`
     * @param onSelect Listener receiving the result
     * @param onCancel Listener receiving a cancel event
     */
    fun registerListener(
        activity: FragmentActivity,
        requestKey: String,
        onSelect: (color: Int) -> Unit,
        onCancel: (() -> Unit)? = null,
    ) {
        registerListener(
            activity.supportFragmentManager,
            requestKey,
            activity,
            onSelect,
            onCancel,
        )
    }

    /**
     * Register result listener.
     *
     * Call at the timing of onViewCreated of fragment.
     *
     * @param fragment Caller fragment
     * @param requestKey Request Key, pass the same value to the `show`
     * @param onSelect Listener receiving the result
     * @param onCancel Listener receiving a cancel event
     */
    fun registerListener(
        fragment: Fragment,
        requestKey: String,
        onSelect: (color: Int) -> Unit,
        onCancel: (() -> Unit)? = null,
    ) {
        registerListener(
            fragment.childFragmentManager,
            requestKey,
            fragment.viewLifecycleOwner,
            onSelect,
            onCancel,
        )
    }

    private fun registerListener(
        manager: FragmentManager,
        requestKey: String,
        lifecycleOwner: LifecycleOwner,
        onSelect: (color: Int) -> Unit,
        onCancel: (() -> Unit)?,
    ) {
        manager.setFragmentResultListener(requestKey, lifecycleOwner) { _, result ->
            if (result.getBoolean(RESULT_KEY_CANCEL)) {
                onCancel?.invoke()
            } else {
                onSelect.invoke(result.getInt(RESULT_KEY_COLOR))
            }
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

    internal class ColorChooserDialogImpl : DialogFragment() {
        private lateinit var colorChooserView: ColorChooserView

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val activity = requireActivity()
            colorChooserView =
                Mm2dCcColorChooserBinding.inflate(activity.layoutInflater).root

            if (savedInstanceState != null) {
                val tab = savedInstanceState.getInt(KEY_INITIAL_TAB, 0)
                colorChooserView.setCurrentItem(tab)
                val color = savedInstanceState.getInt(KEY_INITIAL_COLOR, 0)
                colorChooserView.init(color)
            } else {
                val arguments = requireArguments()
                val tab = arguments.getInt(KEY_INITIAL_TAB, 0)
                colorChooserView.setCurrentItem(tab)
                val color = arguments.getInt(KEY_INITIAL_COLOR, 0)
                colorChooserView.init(color)
            }
            colorChooserView.setWithAlpha(requireArguments().getBoolean(KEY_WITH_ALPHA))
            return AlertDialog.Builder(activity)
                .setView(colorChooserView)
                .setPositiveButton("OK") { _, _ ->
                    notifySelect()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.cancel()
                }
                .create()
        }

        override fun onSaveInstanceState(outState: Bundle) {
            super.onSaveInstanceState(outState)
            outState.putInt(KEY_INITIAL_TAB, colorChooserView.getCurrentItem())
            outState.putInt(KEY_INITIAL_COLOR, colorChooserView.color)
        }

        override fun onCancel(dialog: DialogInterface) {
            val key = requireArguments().getString(KEY_REQUEST_KEY) ?: return
            parentFragmentManager.setFragmentResult(
                key, bundleOf(RESULT_KEY_CANCEL to true)
            )
        }

        private fun notifySelect() {
            val key = requireArguments().getString(KEY_REQUEST_KEY) ?: return
            parentFragmentManager.setFragmentResult(
                key, bundleOf(
                    RESULT_KEY_CANCEL to false,
                    RESULT_KEY_COLOR to colorChooserView.color,
                )
            )
        }
    }
}
