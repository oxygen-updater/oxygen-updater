package com.arjanvlek.oxygenupdater.viewmodels

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.os.storage.StorageManager.ACTION_MANAGE_STORAGE
import android.os.storage.StorageManager.EXTRA_REQUESTED_BYTES
import android.os.storage.StorageManager.EXTRA_UUID
import androidx.core.content.getSystemService
import androidx.core.os.bundleOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkRequest.MIN_BACKOFF_MILLIS
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.dialogs.Dialogs
import com.arjanvlek.oxygenupdater.enums.DownloadStatus
import com.arjanvlek.oxygenupdater.exceptions.UpdateDownloadException
import com.arjanvlek.oxygenupdater.extensions.toWorkData
import com.arjanvlek.oxygenupdater.fragments.UpdateInformationFragment
import com.arjanvlek.oxygenupdater.fragments.UpdateInformationFragment.Companion.SAFE_MARGIN
import com.arjanvlek.oxygenupdater.internal.KotlinCallback
import com.arjanvlek.oxygenupdater.internal.settings.SettingsManager
import com.arjanvlek.oxygenupdater.models.Device
import com.arjanvlek.oxygenupdater.models.DeviceRequestFilter
import com.arjanvlek.oxygenupdater.models.NewsItem
import com.arjanvlek.oxygenupdater.models.ServerMessage
import com.arjanvlek.oxygenupdater.models.ServerStatus
import com.arjanvlek.oxygenupdater.models.UpdateData
import com.arjanvlek.oxygenupdater.repositories.ServerRepository
import com.arjanvlek.oxygenupdater.utils.LocalNotifications.showDownloadFailedNotification
import com.arjanvlek.oxygenupdater.utils.Logger.logDebug
import com.arjanvlek.oxygenupdater.utils.Logger.logInfo
import com.arjanvlek.oxygenupdater.utils.Logger.logWarning
import com.arjanvlek.oxygenupdater.utils.NotificationTopicSubscriber
import com.arjanvlek.oxygenupdater.workers.DIRECTORY_ROOT
import com.arjanvlek.oxygenupdater.workers.DownloadWorker
import com.arjanvlek.oxygenupdater.workers.Md5VerificationWorker
import com.arjanvlek.oxygenupdater.workers.WORK_UNIQUE_DOWNLOAD_NAME
import com.arjanvlek.oxygenupdater.workers.WORK_UNIQUE_MD5_VERIFICATION_NAME
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Shared between [com.arjanvlek.oxygenupdater.activities.MainActivity] and its three child fragments
 * (as part of [androidx.viewpager2.widget.ViewPager2]):
 * 1. [com.arjanvlek.oxygenupdater.fragments.NewsFragment]
 * 2. [com.arjanvlek.oxygenupdater.fragments.UpdateInformationFragment]
 * 3. [com.arjanvlek.oxygenupdater.fragments.DeviceInformationFragment]
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
class MainViewModel(
    private val serverRepository: ServerRepository
) : ViewModel() {

    private val _allDevices = MutableLiveData<List<Device>>()
    val allDevices: LiveData<List<Device>>
        get() = _allDevices

    private val _updateData = MutableLiveData<UpdateData>()
    private val _newsList = MutableLiveData<List<NewsItem>>()
    private val _serverStatus = MutableLiveData<ServerStatus>()
    private val _serverMessages = MutableLiveData<List<ServerMessage>>()

    private val _downloadStatusLiveData = MutableLiveData<Pair<DownloadStatus, WorkInfo?>>()
    private var _downloadStatus = DownloadStatus.NOT_DOWNLOADING
    val downloadStatus: LiveData<Pair<DownloadStatus, WorkInfo?>>
        get() = _downloadStatusLiveData

    val downloadWorkInfo
        get() = workManager.getWorkInfosForUniqueWorkLiveData(WORK_UNIQUE_DOWNLOAD_NAME)

    val verificationWorkInfo
        get() = workManager.getWorkInfosForUniqueWorkLiveData(WORK_UNIQUE_MD5_VERIFICATION_NAME)

    private val workManager by inject(WorkManager::class.java)
    private val settingsManager by inject(SettingsManager::class.java)

    private lateinit var downloadWorkRequest: OneTimeWorkRequest
    private lateinit var verificationWorkRequest: OneTimeWorkRequest

    fun fetchAllDevices(): LiveData<List<Device>> = viewModelScope.launch(Dispatchers.IO) {
        _allDevices.postValue(serverRepository.fetchDevices(DeviceRequestFilter.ALL, false))
    }.let { _allDevices }

    fun fetchUpdateData(
        online: Boolean,
        deviceId: Long,
        updateMethodId: Long,
        incrementalSystemVersion: String,
        errorCallback: KotlinCallback<String?>
    ): LiveData<UpdateData> = viewModelScope.launch(Dispatchers.IO) {
        _updateData.postValue(serverRepository.fetchUpdateData(online, deviceId, updateMethodId, incrementalSystemVersion, errorCallback))
    }.let { _updateData }

    fun fetchNews(
        context: Context,
        deviceId: Long,
        updateMethodId: Long
    ): LiveData<List<NewsItem>> = viewModelScope.launch(Dispatchers.IO) {
        _newsList.postValue(serverRepository.fetchNews(context, deviceId, updateMethodId))
    }.let { _newsList }

    fun fetchServerStatus(online: Boolean): LiveData<ServerStatus> = viewModelScope.launch(Dispatchers.IO) {
        _serverStatus.postValue(serverRepository.fetchServerStatus(online))
    }.let { _serverStatus }

    fun fetchServerMessages(
        serverStatus: ServerStatus,
        errorCallback: KotlinCallback<String?>
    ): LiveData<List<ServerMessage>> = viewModelScope.launch(Dispatchers.IO) {
        _serverMessages.postValue(serverRepository.fetchServerMessages(serverStatus, errorCallback))
    }.let { _serverMessages }

    fun subscribeToNotificationTopics(
        enabledDeviceList: List<Device>
    ) = viewModelScope.launch(Dispatchers.IO) {
        NotificationTopicSubscriber.subscribe(
            enabledDeviceList,
            serverRepository.fetchAllMethods()
        )
    }

    fun updateDownloadStatus(downloadStatus: DownloadStatus) {
        _downloadStatus = downloadStatus
        _downloadStatusLiveData.postValue(Pair(downloadStatus, null))
    }

    fun updateDownloadStatus(workInfo: WorkInfo, isVerificationWorkState: Boolean = false) {
        val workState = workInfo.state

        _downloadStatus = when (workState) {
            WorkInfo.State.ENQUEUED -> if (isVerificationWorkState) {
                DownloadStatus.VERIFYING
            } else {
                DownloadStatus.DOWNLOAD_QUEUED
            }
            WorkInfo.State.RUNNING -> if (isVerificationWorkState) {
                DownloadStatus.VERIFYING
            } else {
                DownloadStatus.DOWNLOADING
            }
            WorkInfo.State.SUCCEEDED -> if (isVerificationWorkState) {
                DownloadStatus.VERIFICATION_COMPLETED
            } else {
                DownloadStatus.DOWNLOAD_COMPLETED
            }
            WorkInfo.State.FAILED -> if (isVerificationWorkState) {
                DownloadStatus.VERIFICATION_FAILED
            } else {
                DownloadStatus.DOWNLOAD_FAILED
            }
            WorkInfo.State.BLOCKED -> if (isVerificationWorkState) {
                DownloadStatus.VERIFYING
            } else {
                DownloadStatus.DOWNLOAD_QUEUED
            }
            WorkInfo.State.CANCELLED -> if (isVerificationWorkState) {
                // Verification can never be cancelled
                DownloadStatus.VERIFICATION_FAILED
            } else {
                // Downloads are paused by cancelling by work
                // (we're tracking `bytesDone` and sending a `Range` header for resume operations),
                // so we assume all cancel operation are pause operations instead.
                // The only place from where the user can cancel the download is the action button
                // Hence, we've called `initDownloadLayout(NOT_DOWNLOADING)` in [UpdateInformationFragment] right after cancelling the work.
                DownloadStatus.DOWNLOAD_PAUSED
            }
        }

        // Post value only if it's changed, or if status is DOWNLOADING (because we need to update views for progress)
        if (_downloadStatus != _downloadStatusLiveData.value?.first || _downloadStatus == DownloadStatus.DOWNLOADING) {
            _downloadStatusLiveData.postValue(Pair(_downloadStatus, workInfo))
        }
    }

    fun checkDownloadCompletionByFile(updateData: UpdateData?): Boolean {
        if (updateData?.filename == null) {
            logInfo(UpdateInformationFragment.TAG, "Cannot check for download completion by file - null update data or filename provided!")
            return false
        }

        return File(Environment.getExternalStoragePublicDirectory(DIRECTORY_ROOT), updateData.filename!!).exists()
    }

    fun deleteDownloadedFile(context: Context, updateData: UpdateData?) {
        if (updateData?.filename == null) {
            logWarning(
                UpdateInformationFragment.TAG,
                UpdateDownloadException("Could not delete downloaded file, null update data or update data without file name was provided")
            )
            return
        }

        logDebug(UpdateInformationFragment.TAG, "Deleting any associated tracker preferences for downloaded file")
        settingsManager.deletePreference(SettingsManager.PROPERTY_DOWNLOAD_BYTES_DONE)

        logDebug(UpdateInformationFragment.TAG, "Deleting downloaded update file " + updateData.filename)

        val tempFile = File(context.getExternalFilesDir(null), updateData.filename!!)
        val zipFile = File(Environment.getExternalStoragePublicDirectory(DIRECTORY_ROOT).absolutePath, updateData.filename!!)

        if (tempFile.exists() && !tempFile.delete()) {
            logWarning(UpdateInformationFragment.TAG, UpdateDownloadException("Could not delete temporary file ${updateData.filename}"))
        }

        if (zipFile.exists() && !zipFile.delete()) {
            logWarning(UpdateInformationFragment.TAG, UpdateDownloadException("Could not delete downloaded file ${updateData.filename}"))
        }

        updateDownloadStatus(DownloadStatus.NOT_DOWNLOADING)
    }

    fun setupWorkRequests(updateData: UpdateData) {
        downloadWorkRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(updateData.toWorkData())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()

        verificationWorkRequest = OneTimeWorkRequestBuilder<Md5VerificationWorker>()
            .setInputData(updateData.toWorkData())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
            .build()
    }

    fun enqueueDownloadWork(activity: Activity, updateData: UpdateData) {
        val requiredFreeBytes = updateData.downloadSize
        val externalFilesDir = activity.getExternalFilesDir(null)!!

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val storageManager = activity.getSystemService<StorageManager>()!!
            val appSpecificExternalDirUuid = storageManager.getUuidForPath(externalFilesDir)

            // Get maximum bytes that can be allocated by the system to the app
            // This value is usually larger than [File.usableSpace],
            // because the system considers cached files that can be deleted
            val allocatableBytes = storageManager.getAllocatableBytes(appSpecificExternalDirUuid)
            if (allocatableBytes + SAFE_MARGIN >= requiredFreeBytes) {
                // Allocate bytes. The system will delete cached files if necessary, to fulfil this request
                storageManager.allocateBytes(appSpecificExternalDirUuid, requiredFreeBytes)

                // Since the required space has been freed up, we can enqueue the download work
                workManager.enqueueUniqueWork(
                    WORK_UNIQUE_DOWNLOAD_NAME,
                    ExistingWorkPolicy.REPLACE,
                    downloadWorkRequest
                )
            } else {
                val intent = Intent()
                    .setAction(ACTION_MANAGE_STORAGE)
                    .putExtras(
                        bundleOf(
                            EXTRA_UUID to appSpecificExternalDirUuid,
                            EXTRA_REQUESTED_BYTES to requiredFreeBytes - allocatableBytes - SAFE_MARGIN
                        )
                    )

                // Display prompt to user, requesting that they choose files to remove
                activity.startActivityForResult(
                    intent,
                    UpdateInformationFragment.MANAGE_STORAGE_REQUEST_CODE
                )
            }
        } else {
            // Check if there is enough free storage space before downloading
            val usableBytes = externalFilesDir.usableSpace

            if (usableBytes + SAFE_MARGIN >= requiredFreeBytes) {
                // Since we have enough space available, we can enqueue the download work
                workManager.enqueueUniqueWork(
                    WORK_UNIQUE_DOWNLOAD_NAME,
                    ExistingWorkPolicy.REPLACE,
                    downloadWorkRequest
                )
            } else {
                // Don't have enough space to complete the download. Display a notification and an error dialog to the user
                showDownloadFailedNotification(
                    activity,
                    false,
                    R.string.download_error_storage,
                    R.string.download_notification_error_storage_full
                )

                Dialogs.showDownloadError(
                    activity,
                    false,
                    R.string.download_error,
                    R.string.download_error_storage
                )
            }
        }
    }

    fun enqueueVerificationWork() {
        workManager.enqueueUniqueWork(
            WORK_UNIQUE_MD5_VERIFICATION_NAME,
            ExistingWorkPolicy.REPLACE,
            verificationWorkRequest
        )
    }

    fun cancelDownloadWork(context: Context, updateData: UpdateData?) {
        workManager.cancelUniqueWork(WORK_UNIQUE_DOWNLOAD_NAME)
        deleteDownloadedFile(context, updateData)
    }

    fun pauseDownloadWork() {
        workManager.cancelUniqueWork(WORK_UNIQUE_DOWNLOAD_NAME)
    }

    /**
     * This method is called in [UpdateInformationFragment.onDestroy], to prune finished work.
     *
     * This is done to avoid getting callbacks to respective work observers when the fragment is created,
     * as it results in unnecessary calls to [updateDownloadStatus]
     */
    fun maybePruneWork() {
        // Make it non-nullable (!!) because it's initialized with a value
        if (_downloadStatus.shouldPruneWork()) {
            workManager.pruneWork()
        }
    }
}
