package com.oxygenupdater.ui.update

import androidx.compose.runtime.Immutable
import androidx.work.WorkInfo

/** Required because [workInfo] is unstable */
@Immutable
data class WorkInfoWithStatus(
    val workInfo: WorkInfo?,
    val downloadStatus: DownloadStatus,
)
