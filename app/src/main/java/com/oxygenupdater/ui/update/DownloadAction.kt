package com.oxygenupdater.ui.update

import androidx.compose.runtime.Immutable

@Immutable
@JvmInline
value class DownloadAction private constructor(val value: Int) {

    override fun toString() = "DownloadAction." + when (this) {
        Enqueue -> "Enqueue"
        Pause -> "Pause"
        Cancel -> "Cancel"
        Delete -> "Delete"
        else -> "Invalid"
    }

    companion object {
        val Enqueue = DownloadAction(0)
        val Pause = DownloadAction(1)
        val Cancel = DownloadAction(2)
        val Delete = DownloadAction(3)
    }
}
