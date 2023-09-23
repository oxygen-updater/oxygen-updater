package com.oxygenupdater.ui.update

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

@Immutable
@JvmInline
value class DownloadAction(val value: Int) {

    override fun toString() = "DownloadAction." + when (this) {
        Enqueue -> "Enqueue"
        Pause -> "Pause"
        Cancel -> "Cancel"
        Delete -> "Delete"
        else -> "Invalid"
    }

    companion object {
        @Stable
        val Enqueue = DownloadAction(0)

        @Stable
        val Pause = DownloadAction(1)

        @Stable
        val Cancel = DownloadAction(2)

        @Stable
        val Delete = DownloadAction(3)
    }
}
