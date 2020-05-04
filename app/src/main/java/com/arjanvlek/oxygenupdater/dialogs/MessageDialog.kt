package com.arjanvlek.oxygenupdater.dialogs

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.os.Handler
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.internal.KotlinCallback
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import kotlinx.android.synthetic.main.bottom_sheet_message.*
import kotlin.system.exitProcess

/**
 * Wrapper around [BottomSheetDialog]
 */
class MessageDialog(
    private val activity: Activity,
    private val title: String? = null,
    private val message: CharSequence? = null,
    private val positiveButtonText: String? = null,
    private val negativeButtonText: String? = null,
    @DrawableRes private val positiveButtonIcon: Int? = null,
    private val cancellable: Boolean = false,
    private val dialogListener: KotlinCallback<Int>? = null
) : BottomSheetDialog(activity) {

    @SuppressLint("InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(LayoutInflater.from(activity).inflate(R.layout.bottom_sheet_message, null, false))
        setupViews()
    }

    private fun setupViews() {
        titleTextView.text = title
        messageTextView.text = message

        positiveButton.setup(positiveButtonText, View.OnClickListener {
            dismiss()
            dialogListener?.invoke(Dialog.BUTTON_POSITIVE)
        }, positiveButtonIcon)

        negativeButton.setup(negativeButtonText, View.OnClickListener {
            dismiss()
            dialogListener?.invoke(Dialog.BUTTON_NEGATIVE)
        })

        setupCancellableBehaviour()
    }

    private fun setupCancellableBehaviour() {
        setCancelable(cancellable)
        setCanceledOnTouchOutside(cancellable)

        if (!cancellable) {
            setOnKeyListener { _, keyCode, _ ->
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    dismiss()
                    exit()
                }
                true
            }

            setOnDismissListener { exit() }
        }
    }

    /**
     * This can be used to programmatically dismiss a dialog without triggering the dismiss listener,
     * which is set for non-cancellable dialogs
     */
    fun bypassListenerAndDismiss() {
        setOnKeyListener(null)
        setOnDismissListener(null)

        dismiss()

        // Reset cancellable behaviour, to
        setupCancellableBehaviour()
    }

    private fun MaterialButton.setup(string: String?, onClickListener: View.OnClickListener, @DrawableRes drawableResId: Int? = null) {
        string?.let {
            isVisible = true
            text = it
            setOnClickListener(onClickListener)
        }

        drawableResId?.let { icon = activity.getDrawable(it) }
    }

    private fun exit(activity: Activity? = this.activity) {
        if (activity != null) {
            activity.finishAffinity()
        } else {
            Handler().postDelayed({ exitProcess(0) }, 2000)
        }
    }
}
