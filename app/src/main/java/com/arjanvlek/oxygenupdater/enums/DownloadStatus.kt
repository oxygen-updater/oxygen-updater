package com.arjanvlek.oxygenupdater.enums

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 * @author [Arjan Vlek](https://github.com/arjanvlek)
 */
enum class DownloadStatus {
    /**
     * No update is being downloaded and the update has not been downloaded yet.
     */
    NOT_DOWNLOADING,

    /**
     * The download is in the queue of the download executor, it will start soon.
     */
    DOWNLOAD_QUEUED,

    /**
     * The download is in progress
     */
    DOWNLOADING,

    /**
     * The download has been paused by the user
     */
    DOWNLOAD_PAUSED,

    /**
     * The file has been successfully downloaded
     */
    DOWNLOAD_COMPLETED,

    /**
     * The file could not be downloaded
     */
    DOWNLOAD_FAILED,

    /**
     * The downloaded file's MD5 checksum is being verified
     */
    VERIFYING,

    /**
     * The downloaded file's MD5 checksum has been verified
     */
    VERIFICATION_COMPLETED,

    /**
     * The downloaded file's MD5 checksum could not be verified
     */
    VERIFICATION_FAILED;

    fun shouldPruneWork() = this == DOWNLOAD_COMPLETED || this == DOWNLOAD_FAILED
            || this == VERIFICATION_COMPLETED || this == VERIFICATION_FAILED
}
