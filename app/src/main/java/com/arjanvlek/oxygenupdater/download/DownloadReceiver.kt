package com.arjanvlek.oxygenupdater.download

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * DownloadReceiver is capable of sending status updates from [DownloadService] to the UI
 *
 *
 * A UpdateDownloadListener is saved when registering the receiver, that contains the actual code of
 * the UI class.
 *
 *
 * Oxygen Updater, copyright 2019 Arjan Vlek. File created by arjan.vlek on 03/01/2019.
 */
class DownloadReceiver(private val UIDownloadListener: UpdateDownloadListener?) : BroadcastReceiver() {

    init {
        if (UIDownloadListener == null) {
            throw IllegalArgumentException("UIDownloadListener cannot be null")
        }
    }

    override fun onReceive(context: Context, intent: Intent) {

        val progress: DownloadProgressData

        when (intent.getStringExtra(PARAM_TYPE)) {
            TYPE_STARTED_RESUMED -> UIDownloadListener?.onDownloadStarted()
            TYPE_PAUSED -> {
                progress = intent.getSerializableExtra(PARAM_PROGRESS) as DownloadProgressData
                UIDownloadListener?.onDownloadPaused(false, progress)
            }
            TYPE_CANCELLED -> UIDownloadListener?.onDownloadCancelled()
            TYPE_PROGRESS_UPDATE -> {
                progress = intent.getSerializableExtra(PARAM_PROGRESS) as DownloadProgressData
                UIDownloadListener?.onDownloadProgressUpdate(progress)
            }
            TYPE_DOWNLOAD_ERROR -> {
                val internalError = intent.getBooleanExtra(PARAM_ERROR_IS_INTERNAL_ERROR, false)
                val storageError = intent.getBooleanExtra(PARAM_ERROR_IS_STORAGE_SPACE_ERROR, false)
                val serverError = intent.getBooleanExtra(PARAM_ERROR_IS_SERVER_ERROR, false)

                UIDownloadListener?.onDownloadError(internalError, storageError, serverError)
            }
            TYPE_DOWNLOAD_COMPLETED -> UIDownloadListener?.onDownloadComplete()
            TYPE_VERIFY_STARTED -> UIDownloadListener?.onVerifyStarted()
            TYPE_VERIFY_FAILED -> UIDownloadListener?.onVerifyError()
            TYPE_VERIFY_COMPLETE -> UIDownloadListener?.onVerifyComplete(true)
            TYPE_STATUS_REQUEST -> {
                UIDownloadListener?.onInitialStatusUpdate()

                // Some actions require additional information to be sent to the UI, such as download progress.
                // These are sent directly within the status request call.
                val status = intent.getSerializableExtra(PARAM_STATUS) as DownloadStatus
                progress = intent.getSerializableExtra(PARAM_PROGRESS) as DownloadProgressData
                when (status) {
                    DownloadStatus.NOT_DOWNLOADING -> UIDownloadListener?.onDownloadCancelled()
                    DownloadStatus.DOWNLOAD_QUEUED -> UIDownloadListener?.onDownloadPaused(true, progress)
                    DownloadStatus.DOWNLOADING -> UIDownloadListener?.onDownloadProgressUpdate(progress)
                    DownloadStatus.DOWNLOAD_PAUSED -> UIDownloadListener?.onDownloadPaused(false, progress)
                    DownloadStatus.DOWNLOAD_PAUSED_WAITING_FOR_CONNECTION -> UIDownloadListener?.onDownloadPaused(false, progress)
                    DownloadStatus.VERIFYING -> UIDownloadListener?.onVerifyStarted()
                    DownloadStatus.DOWNLOAD_COMPLETED -> UIDownloadListener?.onVerifyComplete(false)
                }
            }
        }
    }

    companion object {
        const val ACTION_DOWNLOAD_EVENT = "com.arjanvlek.oxygenupdater.intent.action.DOWNLOAD_EVENT"
        const val PARAM_TYPE = "TYPE"
        const val TYPE_STARTED_RESUMED = "STARTED_OR_RESUMED"
        const val TYPE_PAUSED = "PAUSED"
        const val TYPE_CANCELLED = "CANCELLED"
        const val TYPE_PROGRESS_UPDATE = "PROGRESS_UPDATE"
        const val TYPE_DOWNLOAD_COMPLETED = "COMPLETED"
        const val TYPE_DOWNLOAD_ERROR = "ERROR"
        const val TYPE_VERIFY_STARTED = "VERIFY_STARTED"
        const val TYPE_VERIFY_FAILED = "VERIFY_ERROR"
        const val TYPE_VERIFY_COMPLETE = "VERIFY_COMPLETE"
        const val TYPE_STATUS_REQUEST = "STATUS_REQUEST"
        const val PARAM_PROGRESS = "PROGRESS"
        const val PARAM_STATUS = "STATUS"
        const val PARAM_ERROR_IS_INTERNAL_ERROR = "IS_INTERNAL_ERROR"
        const val PARAM_ERROR_IS_STORAGE_SPACE_ERROR = "IS_STORAGE_SPACE_ERROR"
        const val PARAM_ERROR_IS_SERVER_ERROR = "IS_SERVER_ERROR"
    }
}
