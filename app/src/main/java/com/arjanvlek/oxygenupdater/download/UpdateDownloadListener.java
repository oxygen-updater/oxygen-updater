package com.arjanvlek.oxygenupdater.download;

public interface UpdateDownloadListener {

	void onInitialStatusUpdate();

	void onDownloadStarted();

	void onDownloadProgressUpdate(DownloadProgressData downloadProgressData);

	void onDownloadPaused(boolean queued, DownloadProgressData downloadProgressData);

	void onDownloadComplete();

	void onDownloadCancelled();

	void onDownloadError(boolean isInternalError, boolean isStorageSpaceError, boolean isServerError);

	void onVerifyStarted();

	void onVerifyError();

	void onVerifyComplete(boolean launchInstallation);

}
