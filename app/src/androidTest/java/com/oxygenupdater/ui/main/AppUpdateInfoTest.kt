package com.oxygenupdater.ui.main

import androidx.collection.IntIntPair
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.oxygenupdater.ComposeBaseTest
import org.junit.Test

class AppUpdateInfoTest : ComposeBaseTest() {

    @Test
    fun appUpdateInfo() {
        var status by mutableStateOf(InstallStatus.DOWNLOADED)
        var availability by mutableStateOf(UpdateAvailability.UPDATE_AVAILABLE)
        var snackbarText: IntIntPair? = null
        setContent(true) {
            AppUpdateInfo(
                status = status,
                availability = { availability },
                snackbarMessageId = { snackbarText?.first },
                updateSnackbarText = { snackbarText = it },
                resetAppUpdateIgnoreCount = {
                    trackCallback("resetAppUpdateIgnoreCount")
                },
                incrementAppUpdateIgnoreCount = {
                    trackCallback("incrementAppUpdateIgnoreCount")
                },
                unregisterAppUpdateListener = {
                    trackCallback("unregisterAppUpdateListener")
                },
                requestUpdate = {
                    trackCallback("requestUpdate")
                },
                requestImmediateUpdate = {
                    trackCallback("requestImmediateUpdate")
                },
            )
        }

        /** First we test for the initial value of status = [InstallStatus.DOWNLOADED] */
        snackbarText.check(AppUpdateDownloadedSnackbarData)
        ensureCallbackInvokedExactlyOnce("unregisterAppUpdateListener")

        /** Then for initial value of availability = [UpdateAvailability.UPDATE_AVAILABLE] */
        status = InstallStatus.DOWNLOADING
        advanceFrame()
        snackbarText.check(null) // should be dismissed
        ensureCallbackInvokedExactlyOnce("requestUpdate")

        // Then for other values
        availability = UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
        advanceFrame()
        snackbarText.check(null) // should not change
        ensureCallbackInvokedExactlyOnce("requestImmediateUpdate")

        availability = UpdateAvailability.UPDATE_NOT_AVAILABLE
        advanceFrame()
        snackbarText.check(null)
        ensureNoCallbacksWereInvoked()

        availability = UpdateAvailability.UNKNOWN
        advanceFrame()
        snackbarText.check(null)
        ensureNoCallbacksWereInvoked()
    }

    private fun IntIntPair?.check(actual: IntIntPair?) = assert(this == actual) {
        "Snackbar text did not match. Expected: $this, actual: $actual."
    }
}
