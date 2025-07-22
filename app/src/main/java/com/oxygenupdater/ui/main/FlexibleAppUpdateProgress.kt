package com.oxygenupdater.ui.main

import androidx.annotation.VisibleForTesting
import androidx.collection.IntIntPair
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.util.fastCoerceAtLeast
import com.google.android.play.core.install.model.InstallStatus
import com.oxygenupdater.ui.common.modifierMaxWidth

@Composable
fun FlexibleAppUpdateProgress(
    @InstallStatus status: Int,
    bytesDownloaded: () -> Long, // deferred read
    totalBytesToDownload: () -> Long,
    snackbarMessageId: () -> Int?,
    updateSnackbarText: (IntIntPair?) -> Unit,
) {
    if (status == InstallStatus.DOWNLOADED) {
        updateSnackbarText(AppUpdateDownloadedSnackbarData)
    } else if (snackbarMessageId() == AppUpdateDownloadedSnackbarData.first) {
        // Dismiss only this snackbar
        updateSnackbarText(null)
    }

    if (status == InstallStatus.PENDING) LinearProgressIndicator(
        modifierMaxWidth.testTag(FlexibleAppUpdateProgress_IndicatorTestTag)
    ) else if (status == InstallStatus.DOWNLOADING) {
        val progress = bytesDownloaded().toFloat() / totalBytesToDownload().fastCoerceAtLeast(1)
        val animatedProgress by animateFloatAsState(
            targetValue = progress,
            animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
            label = "FlexibleUpdateProgressAnimation",
        )
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = modifierMaxWidth.testTag(FlexibleAppUpdateProgress_IndicatorTestTag)
        )
    }
}

private const val TAG = "FlexibleAppUpdateProgress"

@VisibleForTesting
const val FlexibleAppUpdateProgress_IndicatorTestTag = TAG + "_Indicator"
