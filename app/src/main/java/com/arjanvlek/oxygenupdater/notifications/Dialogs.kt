package com.arjanvlek.oxygenupdater.notifications

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.arjanvlek.oxygenupdater.ActivityLauncher
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.download.DownloadService
import com.arjanvlek.oxygenupdater.internal.KotlinCallback
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logDebug
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logError
import com.arjanvlek.oxygenupdater.models.UpdateData

object Dialogs {

    /**
     * Shows an [MessageDialog] with the occurred download error.
     *
     * @param title   Title of the error message
     * @param message Contents of the error message
     */
    fun showDownloadError(fragment: Fragment, updateData: UpdateData?, isResumable: Boolean, @StringRes title: Int, @StringRes message: Int) {
        showDownloadError(fragment, updateData, isResumable, fragment.getString(title), fragment.getString(message))
    }

    /**
     * Shows an [MessageDialog] with the occurred download error.
     *
     * @param title   Title of the error message
     * @param message Contents of the error message
     */
    @JvmStatic
    fun showDownloadError(fragment: Fragment, updateData: UpdateData?, isResumable: Boolean, title: String?, message: String?) {
        checkPreconditions(fragment) {
            val errorDialog = MessageDialog()
                .setTitle(title)
                .setMessage(message)
                .setPositiveButtonText(fragment.getString(R.string.download_error_close))
                .setNegativeButtonText(
                    if (isResumable) fragment.getString(R.string.download_error_resume) else fragment
                        .getString(R.string.download_error_retry)
                )
                .setClosable(true)
                .setDialogListener(object : MessageDialog.DialogListener {
                    override fun onDialogPositiveButtonClick(dialogFragment: DialogInterface?) {
                        LocalNotifications.hideDownloadCompleteNotification(fragment.activity)
                    }

                    override fun onDialogNegativeButtonClick(dialogFragment: DialogInterface?) {
                        LocalNotifications.hideDownloadCompleteNotification(fragment.activity)
                        DownloadService.performOperation(fragment.activity, DownloadService.ACTION_CANCEL_DOWNLOAD, updateData)
                        DownloadService.performOperation(
                            fragment.activity,
                            if (isResumable) DownloadService.ACTION_RESUME_DOWNLOAD else DownloadService.ACTION_DOWNLOAD_UPDATE,
                            updateData
                        )
                    }
                })
            try {
                errorDialog.setTargetFragment(fragment, 0)

                fragment.activity!!.supportFragmentManager.beginTransaction().apply {
                    add(errorDialog, "DownloadError")
                    commitAllowingStateLoss()
                }
            } catch (e: IllegalStateException) {
                if (e.message?.contains("onSaveInstanceState") == true) {
                    logDebug("MessageDialog", "Ignored IllegalStateException when showing dialog because the app was already exited", e)
                } else {
                    logError("MessageDialog", "Error when displaying dialog 'DownloadError'", e)
                }
            }
        }
    }

    @JvmStatic
    fun showNoNetworkConnectionError(fragment: Fragment) {
        checkPreconditions(fragment) {
            MessageDialog()
                .setTitle(fragment.getString(R.string.error_app_requires_network_connection))
                .setMessage(fragment.getString(R.string.error_app_requires_network_connection_message))
                .setNegativeButtonText(fragment.getString(R.string.download_error_close))
                .setClosable(false).apply {
                    setTargetFragment(fragment, 0)
                    show(fragment.parentFragmentManager, "NetworkError")
                }
        }
    }

    @JvmStatic
    fun showServerMaintenanceError(fragment: Fragment) {
        checkPreconditions(fragment) {
            MessageDialog()
                .setTitle(fragment.getString(R.string.error_maintenance))
                .setMessage(fragment.getString(R.string.error_maintenance_message))
                .setNegativeButtonText(fragment.getString(R.string.download_error_close))
                .setClosable(false).apply {
                    setTargetFragment(fragment, 0)
                    show(fragment.parentFragmentManager, "MaintenanceError")
                }
        }
    }

    @JvmStatic
    fun showAppOutdatedError(fragment: Fragment, activity: Activity?) {
        val activityLauncher = ActivityLauncher(activity!!)
        checkPreconditions(fragment) {
            MessageDialog()
                .setTitle(fragment.getString(R.string.error_app_outdated))
                .setMessage(fragment.getString(R.string.error_app_outdated_message))
                .setPositiveButtonText(fragment.getString(R.string.error_google_play_button_text))
                .setNegativeButtonText(fragment.getString(R.string.download_error_close))
                .setClosable(false)
                .setDialogListener(object : MessageDialog.DialogListener {
                    override fun onDialogPositiveButtonClick(dialogFragment: DialogInterface?) {
                        activityLauncher.launchPlayStorePage(activity)
                    }

                    override fun onDialogNegativeButtonClick(dialogFragment: DialogInterface?) {}
                }).apply {
                    setTargetFragment(fragment, 0)
                    show(fragment.parentFragmentManager, "AppOutdatedError")
                }
        }
    }

    @JvmStatic
    fun showUpdateAlreadyDownloadedMessage(updateData: UpdateData?, fragment: Fragment, actionPerformedCallback: KotlinCallback<Void?>) {
        checkPreconditions(fragment) {
            val dialog = MessageDialog()
                .setTitle(fragment.getString(R.string.delete_message_title))
                .setMessage(fragment.getString(R.string.delete_message_contents))
                .setClosable(true)
                .setPositiveButtonText(fragment.getString(R.string.install))
                .setNegativeButtonText(fragment.getString(R.string.delete_message_delete_button))
                .setDialogListener(object : MessageDialog.DialogListener {
                    override fun onDialogPositiveButtonClick(dialogFragment: DialogInterface?) {
                        if (fragment.activity == null || fragment.activity!!.application == null) {
                            return
                        }

                        ActivityLauncher(fragment.activity!!).UpdateInstallation(true, updateData)
                    }

                    override fun onDialogNegativeButtonClick(dialogFragment: DialogInterface?) {
                        actionPerformedCallback.invoke(null)
                    }
                })
            try {
                dialog.setTargetFragment(fragment, 0)
                fragment.activity!!.supportFragmentManager.beginTransaction().apply {
                    add(dialog, "DeleteDownload")
                    commitAllowingStateLoss()
                }
            } catch (e: IllegalStateException) {
                if (e.message?.contains("onSaveInstanceState") == true) {
                    logDebug("MessageDialog", "Ignored IllegalStateException when showing dialog because the app was already exited", e)
                } else {
                    logError("MessageDialog", "Error when displaying dialog 'DeleteDownload'", e)
                }
            }
        }
    }

    @JvmStatic
    fun showAdvancedModeExplanation(ctx: Context, fm: FragmentManager?) {
        if (fm == null) {
            return
        }

        MessageDialog()
            .setTitle(ctx.getString(R.string.settings_advanced_mode))
            .setMessage(ctx.getString(R.string.settings_advanced_mode_explanation))
            .setClosable(true)
            .setPositiveButtonText(ctx.getString(R.string.update_information_close)).apply {
                show(fm, "OU_AdvancedModeExplanation")
            }
    }

    private fun checkPreconditions(fragment: Fragment?, callback: () -> Unit) {
        if (fragment?.parentFragmentManager != null && fragment.isAdded && fragment.activity?.isFinishing == false) {
            callback.invoke()
        }
    }
}
