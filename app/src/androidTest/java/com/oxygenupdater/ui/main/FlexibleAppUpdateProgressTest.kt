package com.oxygenupdater.ui.main

import androidx.collection.IntIntPair
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasProgressBarRangeInfo
import com.google.android.play.core.install.model.InstallStatus
import com.oxygenupdater.ComposeBaseTest
import com.oxygenupdater.get
import org.junit.Test
import kotlin.random.Random

class FlexibleAppUpdateProgressTest : ComposeBaseTest() {

    private var status by mutableStateOf(InstallStatus.DOWNLOADED)
    private var snackbarText: IntIntPair? = null

    @Test
    fun flexibleAppUpdateProgress() {
        var bytesDownloaded by mutableStateOf(1L)
        var totalBytesToDownload by mutableStateOf(2L)
        setContent {
            FlexibleAppUpdateProgress(
                status = status,
                bytesDownloaded = { bytesDownloaded },
                totalBytesToDownload = { totalBytesToDownload },
                snackbarMessageId = { snackbarText?.first },
                updateSnackbarText = { snackbarText = it },
            )
        }

        /** First we test for the initial value of status = [InstallStatus.DOWNLOADED] */
        rule[FlexibleAppUpdateProgress_IndicatorTestTag].assertDoesNotExist()
        snackbarText.check(AppUpdateDownloadedSnackbarData)

        /** Then for [InstallStatus.PENDING] */
        status = InstallStatus.PENDING
        rule[FlexibleAppUpdateProgress_IndicatorTestTag].assert(
            hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)
        )
        snackbarText.check(null) // should be dismissed

        /** Then for [InstallStatus.DOWNLOADING] */
        status = InstallStatus.DOWNLOADING
        validateForProgress(bytesDownloaded / totalBytesToDownload.toFloat())
        // Then for some other progress value
        bytesDownloaded = Random.nextInt(0, Int.MAX_VALUE).toLong()
        totalBytesToDownload = bytesDownloaded + Random.nextInt(0, Int.MAX_VALUE)
        validateForProgress(bytesDownloaded / totalBytesToDownload.toFloat())
        // Then for invalid values of `bytesDownloaded` and `totalBytesToDownload`
        bytesDownloaded = 2; totalBytesToDownload = 0
        validateForProgress(1f) // should not be a DB0 error and be clamped to 1

        // Then for other values
        validateForOtherInstallStatuses(InstallStatus.UNKNOWN)
        validateForOtherInstallStatuses(InstallStatus.INSTALLING)
        validateForOtherInstallStatuses(InstallStatus.INSTALLED)
        validateForOtherInstallStatuses(InstallStatus.FAILED)
        validateForOtherInstallStatuses(InstallStatus.CANCELED)
    }

    private fun validateForProgress(progress: Float) {
        rule[FlexibleAppUpdateProgress_IndicatorTestTag].run {
            assert(hasProgressBarRangeInfo(ProgressBarRangeInfo(progress, 0f..1f)))
        }
        snackbarText.check(null) // should not change
    }

    private fun validateForOtherInstallStatuses(@InstallStatus newStatus: Int) {
        status = newStatus
        advanceFrame()
        rule[FlexibleAppUpdateProgress_IndicatorTestTag].assertDoesNotExist()
        snackbarText.check(null)
    }

    private fun IntIntPair?.check(actual: IntIntPair?) = assert(this == actual) {
        "Snackbar text did not match. Expected: $this, actual: $actual."
    }
}
