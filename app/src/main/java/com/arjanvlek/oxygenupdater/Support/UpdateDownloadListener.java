package com.arjanvlek.oxygenupdater.Support;

import com.arjanvlek.oxygenupdater.Model.DownloadProgressData;

public interface UpdateDownloadListener {

    void onDownloadManagerInit(UpdateDownloader caller);

    void onDownloadStarted(long downloadID);
    void onDownloadPending();
    void onDownloadProgressUpdate(DownloadProgressData downloadProgressData);
    void onDownloadPaused(int statusCode);
    void onDownloadComplete();
    void onDownloadCancelled();
    void onDownloadError(UpdateDownloader caller, int statusCode);

    void onVerifyStarted();
    void onVerifyError(UpdateDownloader caller);
    void onVerifyComplete();

}
