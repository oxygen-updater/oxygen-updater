package com.arjanvlek.oxygenupdater.notifications

import android.app.Activity
import android.content.Context
import android.content.DialogInterface

import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction

import com.arjanvlek.oxygenupdater.ActivityLauncher
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.download.DownloadService
import com.arjanvlek.oxygenupdater.internal.Worker
import com.arjanvlek.oxygenupdater.updateinformation.UpdateData

import java8.util.function.Consumer

import com.arjanvlek.oxygenupdater.internal.logger.Logger.logDebug
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logError

object Dialogs {

    /**
     * Shows an [MessageDialog] with the occurred download error.
     *
     * @param title   Title of the error message
     * @param message Contents of the error message
     */
    fun showDownloadError(fragment: Fragment, updateData: UpdateData, isResumable: Boolean, @StringRes title: Int, @StringRes message: Int) {
        showDownloadError(fragment, updateData, isResumable, fragment.getString(title),
                fragment.getString(message))
    }

    /**
     * Shows an [MessageDialog] with the occurred download error.
     *
     * @param title   Title of the error message
     * @param message Contents of the error message
     */
    fun showDownloadError(fragment: Fragment, updateData: UpdateData, isResumable: Boolean, title: String, message: String) {
        checkPreconditions(fragment, object : Worker {
            override fun start() {
                val errorDialog = MessageDialog()
                        .setTitle(title)
                        .setMessage(message)
                        .setPositiveButtonText(fragment.getString(R.string.download_error_close))
                        .setNegativeButtonText(if (isResumable)
                            fragment.getString(R.string.download_error_resume)
                        else
                            fragment
                                    .getString(R.string.download_error_retry))
                        .setClosable(true)
                        .setDialogListener(object : MessageDialog.DialogListener {
                            override fun onDialogPositiveButtonClick(dialogFragment: DialogInterface) {
                                LocalNotifications.hideDownloadCompleteNotification(fragment.activity!!)
                            }

                            override fun onDialogNegativeButtonClick(dialogFragment: DialogInterface) {
                                LocalNotifications.hideDownloadCompleteNotification(fragment.activity!!)

                                DownloadService.performOperation(fragment.activity, DownloadService.ACTION_CANCEL_DOWNLOAD, updateData)
                                DownloadService.performOperation(fragment.activity, if (isResumable) DownloadService.ACTION_RESUME_DOWNLOAD else DownloadService.ACTION_DOWNLOAD_UPDATE, updateData)
                            }
                        })

                try {
                    errorDialog.setTargetFragment(fragment, 0)
                    val transaction = fragment.activity!!.supportFragmentManager.beginTransaction()
                    transaction.add(errorDialog, "DownloadError")
                    transaction.commitAllowingStateLoss()
                } catch (e: IllegalStateException) {
                    if (e.message != null && e.message!!.contains("onSaveInstanceState")) {
                        logDebug("MessageDialog", "Ignored IllegalStateException when showing dialog because the app was already exited", e)
                    } else {
                        logError("MessageDialog", "Error when displaying dialog 'DownloadError'", e)
                    }
                }
            }

        })
    }

    fun showNoNetworkConnectionError(fragment: Fragment) {
        checkPreconditions(fragment, object : Worker {
            override fun start() {
                val errorDialog = MessageDialog()
                        .setTitle(fragment.getString(R.string.error_app_requires_network_connection))
                        .setMessage(fragment.getString(R.string.error_app_requires_network_connection_message))
                        .setNegativeButtonText(fragment.getString(R.string.download_error_close))
                        .setClosable(false)
                errorDialog.setTargetFragment(fragment, 0)
                errorDialog.show(fragment.fragmentManager!!, "NetworkError")
            }

        })
    }

    fun showServerMaintenanceError(fragment: Fragment) {
        checkPreconditions(fragment, object : Worker {
            override fun start() {
                val serverMaintenanceErrorFragment = MessageDialog()
                        .setTitle(fragment.getString(R.string.error_maintenance))
                        .setMessage(fragment.getString(R.string.error_maintenance_message))
                        .setNegativeButtonText(fragment.getString(R.string.download_error_close))
                        .setClosable(false)
                serverMaintenanceErrorFragment.setTargetFragment(fragment, 0)
                serverMaintenanceErrorFragment.show(fragment.fragmentManager!!, "MaintenanceError")
            }
        })
    }

    fun showAppOutdatedError(fragment: Fragment, activity: Activity) {
        val activityLauncher = ActivityLauncher(activity)

        checkPreconditions(fragment, object : Worker {
            override fun start() {
                MessageDialog()
                        .setTitle(fragment.getString(R.string.error_app_outdated))
                        .setMessage(fragment.getString(R.string.error_app_outdated_message))
                        .setPositiveButtonText(fragment.getString(R.string.error_google_play_button_text))
                        .setNegativeButtonText(fragment.getString(R.string.download_error_close))
                        .setClosable(false)
                        .setDialogListener(object : MessageDialog.DialogListener {
                            override fun onDialogPositiveButtonClick(dialogFragment: DialogInterface) {
                                activityLauncher.launchPlayStorePage(activity)
                            }

                            override fun onDialogNegativeButtonClick(dialogFragment: DialogInterface) {

                            }
                        })
                        .show(fragment.fragmentManager!!, "AppOutdatedError")
                fragment.setTargetFragment(fragment, 0)
            }
        })

    }

    fun showUpdateAlreadyDownloadedMessage(updateData: UpdateData, fragment: Fragment, actionPerformedCallback: Consumer<Void>) {

        checkPreconditions(fragment, object : Worker {
            override fun start() {
                val dialog = MessageDialog()
                        .setTitle(fragment.getString(R.string.delete_message_title))
                        .setMessage(fragment.getString(R.string.delete_message_contents))
                        .setClosable(true)
                        .setPositiveButtonText(fragment.getString(R.string.install))
                        .setNegativeButtonText(fragment.getString(R.string.delete_message_delete_button))
                        .setDialogListener(object : MessageDialog.DialogListener {
                            override fun onDialogPositiveButtonClick(dialogInterface: DialogInterface) {
                                if (fragment.activity == null || fragment.activity!!.application == null) {
                                    return
                                }

                                val activityLauncher = ActivityLauncher(fragment.activity!!)
                                activityLauncher.UpdateInstallation(true, updateData)
                            }

                            override fun onDialogNegativeButtonClick(dialogFragment: DialogInterface) {
                                actionPerformedCallback.accept(null)
                            }
                        })

                try {
                    dialog.setTargetFragment(fragment, 0)
                    val transaction = fragment.activity!!.supportFragmentManager.beginTransaction()
                    transaction.add(dialog, "DeleteDownload")
                    transaction.commitAllowingStateLoss()
                } catch (e: IllegalStateException) {
                    if (e.message != null && e.message!!.contains("onSaveInstanceState")) {
                        logDebug("MessageDialog", "Ignored IllegalStateException when showing dialog because the app was already exited", e)
                    } else {
                        logError("MessageDialog", "Error when displaying dialog 'DeleteDownload'", e)
                    }
                }
            }

        })
    }

    fun showAdvancedModeExplanation(ctx: Context, fm: FragmentManager?) {
        if (fm == null) {
            return
        }

        val dialog = MessageDialog()
                .setTitle(ctx.getString(R.string.settings_advanced_mode))
                .setMessage(ctx.getString(R.string.settings_advanced_mode_explanation))
                .setClosable(true)
                .setPositiveButtonText(ctx.getString(R.string.update_information_close))

        dialog.show(fm, "OU_AdvancedModeExplanation")
    }

    private fun checkPreconditions(fragment: Fragment?, callback: Worker) {
        if (fragment != null && fragment.fragmentManager != null && fragment.isAdded && fragment.activity != null && !fragment.activity!!.isFinishing) {
            callback.start()
        }
    }

}
