package com.oxygenupdater.ui.update

import androidx.compose.runtime.Immutable

@Immutable
@JvmInline
value class DownloadStatus(val value: Int) {

    override fun toString() = "DownloadStatus." + when (this) {
        NotDownloading -> "NotDownloading"
        DownloadQueued -> "DownloadQueued"
        Downloading -> "Downloading"
        DownloadPaused -> "DownloadPaused"
        DownloadCompleted -> "DownloadCompleted"
        DownloadFailed -> "DownloadFailed"
        Verifying -> "Verifying"
        VerificationCompleted -> "VerificationCompleted"
        VerificationFailed -> "VerificationFailed"
        else -> "Invalid"
    }

    companion object {
        /** No update is being downloaded and the update has not been downloaded yet */
        val NotDownloading = DownloadStatus(0)

        /** The download is in the queue of the download executor, it will start soon */
        val DownloadQueued = DownloadStatus(1)

        /** The download is in progress */
        val Downloading = DownloadStatus(2)

        /** The download has been paused by the user */
        val DownloadPaused = DownloadStatus(3)

        /** The file has been successfully downloaded */
        val DownloadCompleted = DownloadStatus(4)

        /** The file could not be downloaded */
        val DownloadFailed = DownloadStatus(5)

        /** The downloaded file's MD5 checksum is being verified */
        val Verifying = DownloadStatus(6)

        /** The downloaded file's MD5 checksum has been verified */
        val VerificationCompleted = DownloadStatus(7)

        /** The downloaded file's MD5 checksum could not be verified */
        val VerificationFailed = DownloadStatus(8)
    }

    val successful
        get() = this == DownloadCompleted || this == VerificationCompleted

    val failed
        get() = this == DownloadFailed || this == VerificationFailed

    val inProgress
        get() = this == DownloadQueued || this == Downloading
}
