package com.oxygenupdater.ui.main

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.IntentSender
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.install.model.ActivityResult.RESULT_IN_APP_UPDATE_FAILED
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.oxygenupdater.internal.settings.PrefManager

@Composable
fun AppUpdateInfo(
    info: AppUpdateInfo?,
    snackbarMessageId: () -> Int?, // deferred read
    updateSnackbarText: (Pair<Int, Int>?) -> Unit,
    unregisterAppUpdateListener: () -> Unit,
    requestUpdate: RequestAppUpdateCallback,
    requestImmediateUpdate: RequestAppUpdateCallback,
) {
    if (info == null) return

    val status = info.installStatus()
    if (status == InstallStatus.DOWNLOADED) {
        unregisterAppUpdateListener()
        updateSnackbarText(AppUpdateDownloadedSnackbarData)
    } else {
        if (snackbarMessageId() == AppUpdateDownloadedSnackbarData.first) {
            // Dismiss only this snackbar
            updateSnackbarText(null)
        }

        /**
         * Control comes back to the activity in the form of a result only for a `FLEXIBLE` update request,
         * since an `IMMEDIATE` update is entirely handled by Google Play, with the exception of resuming an installation.
         * Check [com.oxygenupdater.activities.MainActivity.onResume] for more info on how this is handled.
         */
        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) {
            val code = it.resultCode
            if (code == RESULT_OK) {
                // Reset ignore count
                PrefManager.putInt(PrefManager.PROPERTY_FLEXIBLE_APP_UPDATE_IGNORE_COUNT, 0)
                if (snackbarMessageId() == AppUpdateFailedSnackbarData.first) {
                    // Dismiss only this snackbar
                    updateSnackbarText(null)
                }
            } else if (code == RESULT_CANCELED) {
                // Increment ignore count and show app update banner
                PrefManager.incrementInt(PrefManager.PROPERTY_FLEXIBLE_APP_UPDATE_IGNORE_COUNT)
                updateSnackbarText(AppUpdateFailedSnackbarData)
            } else if (code == RESULT_IN_APP_UPDATE_FAILED) {
                // Show app update banner
                updateSnackbarText(AppUpdateFailedSnackbarData)
            }
        }

        try {
            val availability = info.updateAvailability()
            if (availability == UpdateAvailability.UPDATE_AVAILABLE) {
                requestUpdate(launcher, info)
            } else if (availability == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                // If an IMMEDIATE update is in the stalled state, we should resume it
                requestImmediateUpdate(launcher, info)
            }
        } catch (e: IntentSender.SendIntentException) {
            // no-op
        }
    }
}

typealias RequestAppUpdateCallback = (ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>, AppUpdateInfo) -> Unit
