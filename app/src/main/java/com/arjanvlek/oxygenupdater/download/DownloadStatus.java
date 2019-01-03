package com.arjanvlek.oxygenupdater.download;

/**
 * Oxygen Updater, copyright 2019 Arjan Vlek. File created by arjan.vlek on 05/01/2019.
 */
public enum DownloadStatus {

    NOT_DOWNLOADING,
    DOWNLOAD_QUEUED,
    DOWNLOADING,
    DOWNLOAD_PAUSED,
    VERIFYING,
    DOWNLOAD_COMPLETED
}
