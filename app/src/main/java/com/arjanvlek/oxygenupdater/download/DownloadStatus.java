package com.arjanvlek.oxygenupdater.download;

/**
 * Oxygen Updater, copyright 2019 Arjan Vlek. File created by arjan.vlek on 05/01/2019.
 */
public enum DownloadStatus {

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
     * The download has been paused due to the loss of network connectivity
     */
    DOWNLOAD_PAUSED_WAITING_FOR_CONNECTION,

    /**
     * The downloaded file is being verified (MD5 check)
     */
    VERIFYING,

    /**
     * The file has been successfully downloaded
     */
    DOWNLOAD_COMPLETED
}
