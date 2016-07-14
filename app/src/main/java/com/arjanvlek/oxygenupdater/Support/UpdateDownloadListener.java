package com.arjanvlek.oxygenupdater.Support;

import com.arjanvlek.oxygenupdater.Model.DownloadProgressData;

public interface UpdateDownloadListener {

    void onDownloadManagerInit();

    void onDownloadStarted(long downloadID);
    void onDownloadPending();
    void onDownloadProgressUpdate(DownloadProgressData downloadProgressData);
    void onDownloadPaused(int statusCode);
    void onDownloadComplete();
    void onDownloadCancelled();
    void onDownloadError(int statusCode);

    void onVerifyStarted();
    void onVerifyError();
    void onVerifyComplete();

}
