package com.oxygenupdater.dialogs

import android.app.Activity
import android.content.DialogInterface.BUTTON_NEGATIVE
import android.content.DialogInterface.BUTTON_NEUTRAL
import android.content.DialogInterface.BUTTON_POSITIVE
import androidx.annotation.StringRes
import com.oxygenupdater.R
import com.oxygenupdater.extensions.openPlayStorePage
import com.oxygenupdater.internal.KotlinCallback
import com.oxygenupdater.utils.LocalNotifications

object Dialogs {

    /**
     * Shows a [MessageDialog] with the occurred download error.
     *
     * @param title   Title of the error message
     * @param message Contents of the error message
     */
    fun showDownloadError(
        activity: Activity?,
        isResumable: Boolean,
        @StringRes title: Int,
        @StringRes message: Int,
        callback: KotlinCallback<Boolean>? = null
    ) = checkPreconditions(activity) {
        showDownloadError(
            activity!!,
            isResumable,
            activity.getString(title),
            activity.getString(message),
            callback
        )
    }

    /**
     * Shows a [MessageDialog] with the occurred download error.
     *
     * @param title   Title of the error message
     * @param message Contents of the error message
     */
    fun showDownloadError(
        activity: Activity?,
        isResumable: Boolean,
        title: String?,
        message: CharSequence?,
        callback: KotlinCallback<Boolean>? = null
    ) = checkPreconditions(activity) {
        MessageDialog(
            activity!!,
            title = title,
            message = message,
            positiveButtonText = when {
                callback == null -> null
                isResumable -> activity.getString(R.string.download_error_resume)
                else -> activity.getString(R.string.download_error_retry)
            },
            negativeButtonText = activity.getString(R.string.download_error_close),
            positiveButtonIcon = when {
                callback == null -> null
                isResumable -> R.drawable.download
                else -> R.drawable.auto
            },
            cancellable = true
        ) {
            when (it) {
                BUTTON_POSITIVE -> {
                    LocalNotifications.hideDownloadCompleteNotification()
                    callback?.invoke(isResumable)
                }
                BUTTON_NEGATIVE -> LocalNotifications.hideDownloadCompleteNotification()
                BUTTON_NEUTRAL -> {
                    // no-op
                }
            }
        }.show()
    }

    /**
     * Shows a [MessageDialog] with the occurred article loading error.
     *
     * @param title   Title of the error message
     * @param message Contents of the error message
     */
    fun showArticleError(
        activity: Activity?,
        isRefreshable: Boolean,
        title: CharSequence?,
        message: CharSequence?,
        callback: () -> Unit
    ) = checkPreconditions(activity) {
        MessageDialog(
            activity!!,
            title = title?.toString(),
            message = message,
            positiveButtonText = if (isRefreshable) {
                activity.getString(R.string.download_error_retry)
            } else {
                null
            },
            negativeButtonText = activity.getString(R.string.download_error_close),
            positiveButtonIcon = if (isRefreshable) {
                R.drawable.auto
            } else {
                null
            },
            cancellable = true
        ) {
            when (it) {
                BUTTON_POSITIVE -> if (isRefreshable) {
                    callback.invoke()
                }
            }
        }.show()
    }

    fun showServerMaintenanceError(activity: Activity?) = checkPreconditions(activity) {
        MessageDialog(
            activity!!,
            title = activity.getString(R.string.error_maintenance),
            message = activity.getString(R.string.error_maintenance_message),
            negativeButtonText = activity.getString(R.string.download_error_close),
            cancellable = false
        ).show()
    }

    fun showAppOutdatedError(activity: Activity?) = checkPreconditions(activity) {
        MessageDialog(
            activity!!,
            title = activity.getString(R.string.error_app_outdated),
            message = activity.getString(R.string.error_app_outdated_message),
            positiveButtonText = activity.getString(R.string.error_google_play_button_text),
            negativeButtonText = activity.getString(R.string.download_error_close),
            cancellable = false
        ) {
            when (it) {
                BUTTON_POSITIVE -> activity.openPlayStorePage()
                BUTTON_NEGATIVE -> {
                    // no-op
                }
                BUTTON_NEUTRAL -> {
                    // no-op
                }
            }
        }.show()
    }

    fun showAdvancedModeExplanation(activity: Activity?, callback: KotlinCallback<Boolean>) = checkPreconditions(activity) {
        MessageDialog(
            activity!!,
            title = activity.getString(R.string.settings_advanced_mode),
            message = activity.getString(R.string.settings_advanced_mode_explanation),
            positiveButtonText = activity.getString(R.string.enable),
            negativeButtonText = activity.getString(android.R.string.cancel),
            positiveButtonIcon = R.drawable.done_outline,
            cancellable = true
        ) {
            when (it) {
                BUTTON_POSITIVE -> callback.invoke(true)
                BUTTON_NEGATIVE -> callback.invoke(false)
                BUTTON_NEUTRAL -> {
                    // no-op
                }
            }
        }.show()
    }

    private fun checkPreconditions(activity: Activity?, callback: () -> Unit) {
        if (activity?.isFinishing == false) {
            callback.invoke()
        }
    }
}
