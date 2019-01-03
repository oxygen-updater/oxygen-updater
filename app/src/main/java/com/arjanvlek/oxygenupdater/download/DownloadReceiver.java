package com.arjanvlek.oxygenupdater.download;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Oxygen Updater, copyright 2019 Arjan Vlek. File created by arjan.vlek on 03/01/2019.
 */
public class DownloadReceiver extends BroadcastReceiver {

    private final UpdateDownloadListener UIDownloadListener;

    public DownloadReceiver(UpdateDownloadListener UIDownloadListener) {
        if (UIDownloadListener == null) {
            throw new IllegalArgumentException("UIDownloadListener cannot be null");
        }

        this.UIDownloadListener = UIDownloadListener;
    }

    public static final String ACTION_DOWNLOAD_EVENT = "com.arjanvlek.oxygenupdater.intent.action.DOWNLOAD_EVENT";

    public static final String PARAM_TYPE = "TYPE";

    public static final String TYPE_STARTED_RESUMED = "STARTED_OR_RESUMED";
    public static final String TYPE_PAUSED = "PAUSED";
    public static final String TYPE_CANCELLED = "CANCELLED";
    public static final String TYPE_PROGRESS_UPDATE = "PROGRESS_UPDATE";
    public static final String TYPE_DOWNLOAD_COMPLETED = "COMPLETED";
    public static final String TYPE_DOWNLOAD_ERROR = "ERROR";
    public static final String TYPE_VERIFY_STARTED = "VERIFY_STARTED";
    public static final String TYPE_VERIFY_FAILED = "VERIFY_ERROR";
    public static final String TYPE_VERIFY_COMPLETE = "VERIFY_COMPLETE";
    public static final String TYPE_STATUS_REQUEST = "STATUS_REQUEST";

    public static final String PARAM_PROGRESS = "PROGRESS";
    public static final String PARAM_STATUS = "STATUS";
    public static final String PARAM_ERROR_IS_INTERNAL_ERROR = "IS_INTERNAL_ERROR";
    public static final String PARAM_ERROR_IS_STORAGE_SPACE_ERROR = "IS_STORAGE_SPACE_ERROR";
    public static final String PARAM_ERROR_IS_SERVER_ERROR = "IS_SERVER_ERROR";

    @Override
    public void onReceive(Context context, Intent intent) {
        String type = intent.getStringExtra(PARAM_TYPE);

        switch (type) {
            case TYPE_STARTED_RESUMED :
                UIDownloadListener.onDownloadStarted();
                break;
            case TYPE_PAUSED:
                DownloadProgressData progress = (DownloadProgressData) intent.getSerializableExtra(PARAM_PROGRESS);
                UIDownloadListener.onDownloadPaused(true, progress);
                break;
            case TYPE_CANCELLED:
                UIDownloadListener.onDownloadCancelled();
                break;
            case TYPE_PROGRESS_UPDATE:
                progress = (DownloadProgressData) intent.getSerializableExtra(PARAM_PROGRESS);
                UIDownloadListener.onDownloadProgressUpdate(progress);
                break;
            case TYPE_DOWNLOAD_ERROR:
                boolean internalError = intent.getBooleanExtra(PARAM_ERROR_IS_INTERNAL_ERROR, false);
                boolean storageError = intent.getBooleanExtra(PARAM_ERROR_IS_STORAGE_SPACE_ERROR, false);
                boolean serverError = intent.getBooleanExtra(PARAM_ERROR_IS_SERVER_ERROR, false);

                UIDownloadListener.onDownloadError(internalError, storageError, serverError);
                break;
            case TYPE_DOWNLOAD_COMPLETED:
                UIDownloadListener.onDownloadComplete();
                break;
            case TYPE_VERIFY_STARTED:
                UIDownloadListener.onVerifyStarted();
                break;
            case TYPE_VERIFY_FAILED:
                UIDownloadListener.onVerifyError();
                break;
            case TYPE_VERIFY_COMPLETE:
                UIDownloadListener.onVerifyComplete();
                break;
            case TYPE_STATUS_REQUEST:
                UIDownloadListener.onInitialStatusUpdate();

                // Some actions require additional information to be sent to the UI, such as download progress.
                // These are sent directly within the status request call.
                DownloadStatus status = (DownloadStatus) intent.getSerializableExtra(PARAM_STATUS);
                progress = (DownloadProgressData) intent.getSerializableExtra(PARAM_PROGRESS);
                switch(status) {
                    case DOWNLOAD_QUEUED:
                        UIDownloadListener.onDownloadPaused(false, progress);
                        break;
                    case VERIFYING:
                        UIDownloadListener.onVerifyStarted();
                        break;
                    case DOWNLOAD_PAUSED:
                        UIDownloadListener.onDownloadPaused(true, progress);
                        break;
                }
                break;
        }
    }
}
