package com.oxygenupdater.compose.ui.main

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.model.InstallStatus

@Composable
fun FlexibleAppUpdateProgress(
    state: InstallState?,
    snackbarText: MutableState<Pair<Int, Int>?>,
) {
    val status = state?.installStatus() ?: return
    if (status == InstallStatus.DOWNLOADED) {
        snackbarText.value = AppUpdateDownloadedSnackbarData
    } else if (snackbarText.value?.first == AppUpdateDownloadedSnackbarData.first) {
        // Dismiss only this snackbar
        snackbarText.value = null
    }

    if (status == InstallStatus.PENDING) LinearProgressIndicator(Modifier.fillMaxWidth())
    else if (status == InstallStatus.DOWNLOADING) {
        val bytesDownloaded = state.bytesDownloaded().toFloat()
        val totalBytesToDownload = state.totalBytesToDownload().coerceAtLeast(1)
        val progress = bytesDownloaded / totalBytesToDownload
        val animatedProgress by animateFloatAsState(
            progress, ProgressIndicatorDefaults.ProgressAnimationSpec,
            label = "FlexibleUpdateProgressAnimation"
        )
        LinearProgressIndicator(animatedProgress, Modifier.fillMaxWidth())
    }
}
