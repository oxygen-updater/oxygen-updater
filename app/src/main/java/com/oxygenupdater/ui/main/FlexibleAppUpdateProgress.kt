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
    unregisterAppUpdateListener: () -> Unit,
) {
    if (status == InstallStatus.DOWNLOADED) {
        unregisterAppUpdateListener() // no need to observe progress now
        updateSnackbarText(AppUpdateDownloadedSnackbarData)
    }
    /**
     * Note that we're not dismissing [AppUpdateDownloadedSnackbarData] because it is meant
     * to be a persistent message that the user must interact with (the 'Reload' action to
     * apply the update). In any case, if we were to dismiss it, it should only be done in
     * this composable, and not [AppUpdateInfo], to allow this composable to handle the
     * complete flow of a flexible app update, i.e. from start to finish. Duplicating the
     * dismissal code in [AppUpdateInfo] would have the effect of immediately dismissing
     * the snackbar that was shown via this composable, which is of course not intended.
     */

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
