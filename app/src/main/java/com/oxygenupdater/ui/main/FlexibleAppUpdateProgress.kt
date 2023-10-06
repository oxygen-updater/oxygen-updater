package com.oxygenupdater.ui.main

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.model.InstallStatus
import com.oxygenupdater.ui.common.modifierMaxWidth

@Composable
fun FlexibleAppUpdateProgress(
    state: InstallState?,
    snackbarMessageId: () -> Int?, // deferred read
    updateSnackbarText: (Pair<Int, Int>?) -> Unit,
) {
    val status = state?.installStatus() ?: return
    if (status == InstallStatus.DOWNLOADED) {
        updateSnackbarText(AppUpdateDownloadedSnackbarData)
    } else if (snackbarMessageId() == AppUpdateDownloadedSnackbarData.first) {
        // Dismiss only this snackbar
        updateSnackbarText(null)
    }

    if (status == InstallStatus.PENDING) LinearProgressIndicator(modifierMaxWidth)
    else if (status == InstallStatus.DOWNLOADING) {
        val bytesDownloaded = state.bytesDownloaded().toFloat()
        val totalBytesToDownload = state.totalBytesToDownload().coerceAtLeast(1)
        val progress = bytesDownloaded / totalBytesToDownload
        val animatedProgress by animateFloatAsState(
            progress, ProgressIndicatorDefaults.ProgressAnimationSpec,
            label = "FlexibleUpdateProgressAnimation"
        )
        LinearProgressIndicator(animatedProgress, modifierMaxWidth)
    }
}
