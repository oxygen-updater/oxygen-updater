package com.oxygenupdater.compose.ui.update

import androidx.compose.runtime.Immutable

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 * @author [Arjan Vlek](https://github.com/arjanvlek)
 */
@Immutable
enum class DownloadStatus {

    /** No update is being downloaded and the update has not been downloaded yet */
    NOT_DOWNLOADING,

    /** The download is in the queue of the download executor, it will start soon */
    DOWNLOAD_QUEUED,

    /** The download is in progress */
    DOWNLOADING,

    /** The download has been paused by the user */
    DOWNLOAD_PAUSED,

    /** The file has been successfully downloaded */
    DOWNLOAD_COMPLETED,

    /** The file could not be downloaded */
    DOWNLOAD_FAILED,

    /** The downloaded file's MD5 checksum is being verified */
    VERIFYING,

    /** The downloaded file's MD5 checksum has been verified */
    VERIFICATION_COMPLETED,

    /** The downloaded file's MD5 checksum could not be verified */
    VERIFICATION_FAILED;

    val successful
        get() = this == DOWNLOAD_COMPLETED || this == VERIFICATION_COMPLETED

    val failed
        get() = this == DOWNLOAD_FAILED || this == VERIFICATION_FAILED

    val inProgress
        get() = this == DOWNLOAD_QUEUED || this == DOWNLOADING
}
