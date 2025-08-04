package com.oxygenupdater.ui.main

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.collection.IntIntPair
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import com.google.android.play.core.install.model.ActivityResult.RESULT_IN_APP_UPDATE_FAILED
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

@Composable
fun AppUpdateInfo(
    @InstallStatus status: Int,
    @UpdateAvailability availability: () -> Int, // deferred read
    snackbarMessageId: () -> Int?,
    updateSnackbarText: (IntIntPair?) -> Unit,
    resetAppUpdateIgnoreCount: () -> Unit,
    incrementAppUpdateIgnoreCount: () -> Unit,
    unregisterAppUpdateListener: () -> Unit,
    requestUpdate: RequestAppUpdateCallback,
    requestImmediateUpdate: RequestAppUpdateCallback,
) = if (status == InstallStatus.DOWNLOADED) {
    unregisterAppUpdateListener()
    updateSnackbarText(AppUpdateDownloadedSnackbarData)
} else {
    /**
     * Note that we're not dismissing [AppUpdateDownloadedSnackbarData] because it is meant
     * to be a persistent message that the user must interact with (the 'Reload' action to
     * apply the update). In any case, if we were to dismiss it, it shouldn't be done here;
     * rather, only in [FlexibleAppUpdateProgress], to allow that composable to handle the
     * complete flow of a flexible app update, i.e. from start to finish. Duplicating the
     * dismissal code here would have the effect of immediately dismissing the snackbar
     * that was shown via [FlexibleAppUpdateProgress], which is of course not intended.
     */

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
            resetAppUpdateIgnoreCount()
            if (snackbarMessageId() == AppUpdateFailedSnackbarData.first) {
                // Dismiss only this snackbar
                updateSnackbarText(null)
            }
        } else if (code == RESULT_CANCELED) {
            // Increment ignore count and show app update banner
            incrementAppUpdateIgnoreCount()
            updateSnackbarText(AppUpdateFailedSnackbarData)
        } else if (code == RESULT_IN_APP_UPDATE_FAILED) {
            // Show app update banner
            updateSnackbarText(AppUpdateFailedSnackbarData)
        }
    }

    when (availability()) {
        UpdateAvailability.UPDATE_AVAILABLE -> {
            // Must be called inside an effect to avoid IllegalStateException: Launcher has not been initialized
            val callback = rememberUpdatedState(requestUpdate)
            LaunchedEffect(launcher) { callback.value(launcher) }
        }

        UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
            // Must be called inside an effect to avoid IllegalStateException: Launcher has not been initialized
            val callback = rememberUpdatedState(requestImmediateUpdate)
            // If an IMMEDIATE update is in the stalled state, we should resume it
            LaunchedEffect(launcher) { callback.value(launcher) }
        }

        else -> {}
    }
}

typealias RequestAppUpdateCallback = (ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>) -> Unit
