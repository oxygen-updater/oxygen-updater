package com.arjanvlek.oxygenupdater.notifications

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.view.KeyEvent
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logDebug
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logError
import kotlin.system.exitProcess

/**
 * Usage: Title text, Message text, Positive button text, Negative button text.
 */
class MessageDialog : DialogFragment() {
    private var dialogListener: DialogListener? = null
    private var title: String? = null
    private var message: String? = null
    private var positiveButtonText: String? = null
    private var negativeButtonText: String? = null
    private var closable = false

    fun setTitle(title: String?): MessageDialog {
        this.title = title
        return this
    }

    fun setMessage(message: String?): MessageDialog {
        this.message = message
        return this
    }

    fun setDialogListener(listener: DialogListener?): MessageDialog {
        dialogListener = listener
        return this
    }

    fun setPositiveButtonText(positiveButtonText: String?): MessageDialog {
        this.positiveButtonText = positiveButtonText
        return this
    }

    fun setNegativeButtonText(negativeButtonText: String?): MessageDialog {
        this.negativeButtonText = negativeButtonText
        return this
    }

    fun setClosable(closable: Boolean): MessageDialog {
        this.closable = closable
        return this
    }

    override fun show(manager: FragmentManager, tag: String?) {
        try {
            super.show(manager, tag)
        } catch (e: IllegalStateException) {
            if (e.message?.contains("onSaveInstanceState") == true) {
                logDebug("MessageDialog", "Ignored IllegalStateException when showing dialog because the app was already exited", e)
            } else {
                logError("MessageDialog", "Error when displaying dialog '$tag'", e)
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity)
            .setTitle(title)
            .setMessage(message)

        if (negativeButtonText != null) {
            builder.setNegativeButton(negativeButtonText) { dialog: DialogInterface?, _: Int ->
                dialogListener?.onDialogNegativeButtonClick(dialog)
                dismiss()
            }
        }

        if (positiveButtonText != null) {
            builder.setPositiveButton(positiveButtonText) { dialog: DialogInterface?, _: Int ->
                dialogListener?.onDialogPositiveButtonClick(dialog)
                dismiss()
            }
        }

        if (!closable) {
            builder.setCancelable(false)
                .setOnKeyListener { _: DialogInterface?, keyCode: Int, _: KeyEvent? ->
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        exit()
                    }
                    true
                }
                .setOnDismissListener { exit() }
        }

        return builder.create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (!closable) {
            exit()
        }
    }

    private fun exit(activity: Activity? = getActivity()) {
        if (activity != null) {
            activity.finish()
            exit(activity.parent)
        } else {
            Handler().postDelayed({ exitProcess(0) }, 2000)
        }
    }

    interface DialogListener {
        fun onDialogPositiveButtonClick(dialogFragment: DialogInterface?)
        fun onDialogNegativeButtonClick(dialogFragment: DialogInterface?)
    }
}
