package com.arjanvlek.oxygenupdater.download

import android.annotation.SuppressLint
import android.app.Activity
import android.app.IntentService
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.AsyncTask
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.StatFs
import android.util.Pair

import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat

import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.internal.OxygenUpdaterException
import com.arjanvlek.oxygenupdater.internal.Utils
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logDebug
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logError
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logInfo
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logWarning
import com.arjanvlek.oxygenupdater.notifications.LocalNotifications
import com.arjanvlek.oxygenupdater.settings.SettingsManager
import com.arjanvlek.oxygenupdater.updateinformation.UpdateData

import com.downloader.Error
import com.downloader.OnDownloadListener
import com.downloader.PRDownloader
import com.downloader.PRDownloaderConfig
import com.downloader.Priority
import com.downloader.Status
import com.downloader.internal.DownloadRequestQueue

import org.joda.time.DateTimeZone
import org.joda.time.LocalDateTime
import org.joda.time.format.DateTimeFormat

import java8.util.function.Function
import java8.util.stream.Collectors
import java8.util.stream.StreamSupport

import java.io.File
import java.util.LinkedList
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.ArrayList

import com.arjanvlek.oxygenupdater.ApplicationData.Companion.APP_USER_AGENT
import com.arjanvlek.oxygenupdater.notifications.LocalNotifications.HAS_ERROR
import com.arjanvlek.oxygenupdater.notifications.LocalNotifications.HAS_NO_ERROR
import com.arjanvlek.oxygenupdater.notifications.LocalNotifications.NOT_ONGOING
import com.arjanvlek.oxygenupdater.notifications.LocalNotifications.ONGOING
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_DOWNLOADER_STATE
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_DOWNLOADER_STATE_HISTORY
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_DOWNLOAD_ID
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_DOWNLOAD_PROGRESS

/**
 * DownloadService handles the downloading and MD5 verification of OxygenOS updates.
 * It does so by using the PRDownloader library, which offers Pause and Resume support as well as automatic recovery after network loss.
 * The service also sends status updates to [DownloadReceiver], which can pass the status of the download to the UI.
 *
 *
 * This is a *very* complex service, because it is responsible for all state transitions that take
 * place when downloading or verifying an update. See [DownloadStatus] for all possible states.
 *
 *
 * To make clear how this service can be used, see the following state table.
 * Its rows contain all possible transitions (initiated by actions() or events) given a state S and an updateData UD
 * Its columns describe the state S' the service is in (for this UD) *after* having performed the transition.
 *
 *
 * State S \ S'                           NOT_DOWNLOADING      DOWNLOAD_QUEUED     DOWNLOADING       DOWNLOAD_PAUSED   DOWNLOAD_PAUSED_WAITING_FOR_CONNECTION   VERIFYING         DOWNLOAD_COMPLETED
 * NOT_DOWNLOADING                        null / noDownloadUrl downloadUpdate()        (-)                 (-)                         (-)                         (-)          updateAlreadyDownloaded
 * DOWNLOAD_QUEUED                        insufficientStorage        (-)          downloadStarted    serviceSuspended                  (-)                         (-)                    (-)
 * DOWNLOADING                            cancelDownload(),          (-)               (-)           pauseDownload(),             connectionError             downloadComplete            (-)
 * serverError                (-)               (-)           serviceSuspended                  (-)                         (-)                    (-)
 * DOWNLOAD_PAUSED                        cancelDownload()      resumeDownload()       (-)                 (-)                         (-)                         (-)                    (-)
 * DOWNLOAD_PAUSED_WAITING_FOR_CONNECTION cancelDownload()     connectionRestored      (-)                 (-)                         (-)                         (-)                    (-)
 * VERIFYING                              verificationError          (-)               (-)                 (-)                         (-)                         (-)            verificationComplete
 * DOWNLOAD_COMPLETED                     deleteDownload(),          (-)               (-)                 (-)                         (-)                         (-)                    (-)
 * updateManuallyDeleted
 *
 *
 * Background running of this service
 * When the app gets killed, the service gets killed as well, but when this happens and it was still running
 * (that is: in the state DOWNLOADING or VERIFYING), it will get auto-restarted via an ACTION_SERVICE_RESTART intent (through [DownloadServiceRestarter])
 * A restart then calls resumeDownload() when the state was DOWNLOADING or verifyUpdate() when the state was VERIFYING.
 * The state is saved in SettingsManager.PROPERTY_DOWNLOADER_STATE.
 *
 *
 * Thread loop (in onHandleIntent())
 * To prevent the service from continuously restarting when downloading / verifying (as those have own threads, so the main thread is completed very quickly),
 * the service executes a while-true loop in which it listens every 50ms if a new command has been ordered or if the download has finished.
 * If a new command has been ordered, it exits the current loop (causing a restart), executes the new Intent (because Intents are sequential) and then executes the restart intent
 * The after-executed restart intent *should* do nothing, because the action usually cancelled or paused the download.
 *
 *
 * Oxygen Updater, copyright 2019 Arjan Vlek. File created by arjan.vlek on 03/01/2019.
 */
/**
 * Creates an IntentService. Invoked by your subclass's constructor.
 */
class DownloadService : IntentService(TAG) {
    // Progress calculation data
    private val measurements = ArrayList<Double>()
    private var updateData: UpdateData? = null
    private var verifier: AsyncTask<*, *, *>? = null
    private var previousProgressTimeStamp = NOT_SET.toLong()
    private var progressPercentage = NO_PROGRESS
    private var previousBytesDownloadedSoFar = NOT_SET.toLong()
    private var previousTimeStamp = NOT_SET.toLong()
    private var previousNumberOfSecondsRemaining = NOT_SET.toLong()

    /*
	 * We want to keep this service running, even if the app gets killed.
	 * Otherwise, the download stops and can no longer be resumed.
	 */
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            startMyOwnForeground()
        } else {
            startForeground(1, Notification())
        }
    }

    /*
	 * Required to keep connection to downloader database.
	 */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    /*
	 * We auto-restart this service if it gets killed while a download is still in progress.
	 */
    override fun onDestroy() {
        super.onDestroy()
        val settingsManager = SettingsManager(applicationContext)
        settingsManager.savePreference(PROPERTY_DOWNLOADER_STATE, state.toString())

        if (isRunning) {
            logDebug(TAG, "onDestroy() was called whilst running, saving state & pausing current activity")
            serializeExecutedStateTransitions()

            // We briefly pause the download - it can be (and will be) resumed in the newly spawned service.
            if (state == DownloadStatus.DOWNLOAD_QUEUED || state == DownloadStatus.DOWNLOADING) {
                logDebug(TAG, "Temporarily pausing download using PRDownloader.pause()")
                PRDownloader.pause(settingsManager.getPreference(PROPERTY_DOWNLOAD_ID, NOT_SET))
            }

            // If onDestroy() happens when checking the MD5, we'll have to start checking it over again (this process is not resumable)
            if (state == DownloadStatus.VERIFYING && verifier != null) {
                logDebug(TAG, "Aborting in-progress MD5 verification")
                verifier!!.cancel(true)
            }

            if (state == DownloadStatus.DOWNLOAD_PAUSED_WAITING_FOR_CONNECTION && autoResumeOnConnectionErrorRunnable != null) {
                logDebug(TAG, "Unregistering network connectivity listener")
                Handler().removeCallbacks(autoResumeOnConnectionErrorRunnable!!)
                autoResumeOnConnectionErrorRunnable = null
            }

            state = DownloadStatus.NOT_DOWNLOADING // No transition, would override stored state in SharedPreferences!

            val broadcastIntent = Intent()
            broadcastIntent.action = INTENT_SERVICE_RESTART
            broadcastIntent.putExtra(PARAM_UPDATE_DATA, updateData)
            broadcastIntent.putExtra(PARAM_DOWNLOAD_ID, settingsManager.getPreference(PROPERTY_DOWNLOAD_ID, NOT_SET))
            broadcastIntent.setClass(this, DownloadServiceRestarter::class.java)
            sendBroadcast(broadcastIntent)
        }
    }

    override fun onHandleIntent(intent: Intent?) {
        if (intent == null) {
            return
        }

        isOperationPending.set(false)

        val config = PRDownloaderConfig.newBuilder()
                .setDatabaseEnabled(true)
                .setUserAgent(APP_USER_AGENT)
                .setConnectTimeout(CONNECT_TIMEOUT)
                .setReadTimeout(READ_TIMEOUT)
                .build()

        PRDownloader.initialize(applicationContext, config)

        val action = intent.getStringExtra(PARAM_ACTION)
        val downloadId = intent.getIntExtra(PARAM_DOWNLOAD_ID, NOT_SET)
        updateData = intent.getParcelableExtra(PARAM_UPDATE_DATA)

        // Restore saved state
        val settingsManager = SettingsManager(applicationContext)
        state = DownloadStatus.valueOf(settingsManager.getPreference(PROPERTY_DOWNLOADER_STATE, DownloadStatus.NOT_DOWNLOADING.toString()))

        when (action) {
            ACTION_SERVICE_RESTART -> {
                // If restarting the service, we'll have to pick up the work we were doing.
                // This is done by reading the restored state of the service. Only the three running-states require an action.
                executedStateTransitions.clear()
                executedStateTransitions.addAll(deserializeExecutedStateTransitions())

                if (state == DownloadStatus.DOWNLOADING || state == DownloadStatus.DOWNLOAD_QUEUED) {
                    logDebug(TAG, "Resuming temporarily-paused download")
                    state = DownloadStatus.DOWNLOAD_QUEUED // We are queued after onDestroy() was called. Not Downloading!
                    resumeDownload(downloadId, updateData)
                } else if (state == DownloadStatus.DOWNLOAD_PAUSED_WAITING_FOR_CONNECTION) {
                    logDebug(TAG, "Resumed re-checking for network connection")
                    resumeDownloadOnReconnectingToNetwork(downloadId, updateData)
                } else if (state == DownloadStatus.VERIFYING) {
                    logDebug(TAG, "Restarted aborted MD5 verification")
                    verifyUpdate(updateData)
                }
            }
            ACTION_DOWNLOAD_UPDATE -> downloadUpdate(updateData)
            ACTION_PAUSE_DOWNLOAD -> pauseDownload(downloadId)
            ACTION_RESUME_DOWNLOAD -> resumeDownload(downloadId, updateData)
            ACTION_CANCEL_DOWNLOAD -> cancelDownload(downloadId)
            ACTION_DELETE_DOWNLOADED_UPDATE -> deleteDownloadedFile(updateData)
            ACTION_GET_INITIAL_STATUS -> checkDownloadStatus(downloadId, updateData)
        }

        // Keep this thread running to avoid the service continuously exiting / restarting.
        // Only if a new operation (Intent) has been called, release this thread and let the service live on in that new thread.
        while (true) {
            try {
                Thread.sleep(INTERRUPT_RATE.toLong())
                if (isOperationPending.get() || !isRunning) {
                    break
                }
            } catch (ignored: InterruptedException) {

            }

        }
    }

    /*
	 * We create a notification channel and notification to allow the app downloading in the
	 * background on Android versions >= O.
	 */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun startMyOwnForeground() {
        val channelName = getString(R.string.download_service_name)
        val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName,
                NotificationManager.IMPORTANCE_NONE)
        channel.lightColor = Color.BLUE
        channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)

        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        val notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.drawable.settings)
                .setContentTitle(getString(R.string.download_in_background))
                .setPriority(NotificationManager.IMPORTANCE_NONE)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build()

        startForeground(2, notification)
    }

    @Synchronized
    private fun downloadUpdate(updateData: UpdateData?) {
        if (!isStateTransitionAllowed(DownloadStatus.DOWNLOAD_QUEUED)) {
            logWarning(TAG, UpdateDownloadException("Not downloading update, is a download operation already in progress?"))
            return
        }

        // Check if the update is downloadable
        val context = applicationContext
        if (updateData?.downloadUrl == null) {
            LocalNotifications.showDownloadFailedNotification(context, false,
                    R.string.download_error_internal, R.string.download_notification_error_internal)
            sendBroadcastIntent(DownloadReceiver.TYPE_DOWNLOAD_ERROR, Function { intent ->
                intent.putExtra(DownloadReceiver.PARAM_ERROR_IS_INTERNAL_ERROR, true)
                logError(TAG, UpdateDownloadException(withAppendedStateHistory("Update data is null or has no Download URL")))
                intent
            })
            return
        }

        if (!updateData.downloadUrl!!.contains("http")) {
            LocalNotifications.showDownloadFailedNotification(context, false, R.string.download_error_internal, R.string.download_notification_error_internal)
            sendBroadcastIntent(DownloadReceiver.TYPE_DOWNLOAD_ERROR, Function { intent ->
                intent.putExtra(DownloadReceiver.PARAM_ERROR_IS_INTERNAL_ERROR, true)
                logError(TAG, UpdateDownloadException("Update data has invalid Download URL (" + updateData.downloadUrl + ")"))
                intent
            })
            return
        }

        // Check if there is enough free storage space before downloading
        val availableSizeInBytes = StatFs(Environment.getExternalStoragePublicDirectory(DIRECTORY_ROOT).path).availableBytes
        val requiredFreeBytes = updateData.downloadSize

        // Download size in bytes is approximately as it is entered in Megabytes by the contributors.
        // Also, we don't want this app to fill up ALL storage of the phone.
        // This means we should require slightly more storage space available than strictly required (25 MB).
        if (availableSizeInBytes - SAFE_MARGIN < requiredFreeBytes) {
            LocalNotifications.showDownloadFailedNotification(
                    context,
                    false,
                    R.string.download_error_storage,
                    R.string.download_notification_error_storage_full
            )
            sendBroadcastIntent(DownloadReceiver.TYPE_DOWNLOAD_ERROR, Function { intent ->
                intent.putExtra(DownloadReceiver.PARAM_ERROR_IS_STORAGE_SPACE_ERROR, true)
                intent
            })
            return
        }

        logDebug(TAG, "Downloading " + updateData.filename!!)

        // Download the update
        performStateTransition(DownloadStatus.DOWNLOAD_QUEUED)
        val downloadId = PRDownloader
                .download(updateData.downloadUrl, Environment
                        .getExternalStoragePublicDirectory(DIRECTORY_ROOT).absolutePath,
                        updateData.filename)
                .setPriority(Priority.HIGH)
                .build()
                .setOnStartOrResumeListener {
                    performStateTransition(DownloadStatus.DOWNLOADING)
                    sendBroadcastIntent(DownloadReceiver.TYPE_STARTED_RESUMED)
                }
                .setOnPauseListener {
                    performStateTransition(DownloadStatus.DOWNLOAD_PAUSED)
                    sendBroadcastIntent(DownloadReceiver.TYPE_PAUSED, Function { intent ->
                        val downloadProgressData = DownloadProgressData(NOT_SET.toLong(), progressPercentage)
                        LocalNotifications.showDownloadPausedNotification(context, updateData, downloadProgressData)
                        intent.putExtra(DownloadReceiver.PARAM_PROGRESS, downloadProgressData)
                        return@Function intent
                    })
                }
                .setOnCancelListener {
                    performStateTransition(DownloadStatus.NOT_DOWNLOADING)
                    sendBroadcastIntent(DownloadReceiver.TYPE_CANCELLED)
                }
                .setOnProgressListener { progress ->
                    if (progress.currentBytes > progress.totalBytes) {
                        logError(TAG, UpdateDownloadException(withAppendedStateHistory("Download progress exceeded total file size. Either the server returned incorrect data or the app is in an invalid state!")))
                        cancelDownload(SettingsManager(context).getPreference(PROPERTY_DOWNLOAD_ID, NOT_SET))
                        downloadUpdate(updateData)
                        PRDownloader
                                .download(updateData.downloadUrl, Environment.getExternalStoragePublicDirectory(DIRECTORY_ROOT).absolutePath, updateData.filename)
                                .setPriority(Priority.HIGH)
                                .build()
                                .setOnStartOrResumeListener {
                                    performStateTransition(DownloadStatus.DOWNLOADING)
                                    sendBroadcastIntent(DownloadReceiver.TYPE_STARTED_RESUMED)
                                }
                                .setOnPauseListener {
                                    performStateTransition(DownloadStatus.DOWNLOAD_PAUSED)
                                    sendBroadcastIntent(DownloadReceiver.TYPE_PAUSED, Function { intent ->
                                        val downloadProgressData =
                                                DownloadProgressData(NOT_SET.toLong(), progressPercentage)
                                        LocalNotifications.showDownloadPausedNotification(context,
                                                updateData, downloadProgressData)
                                        intent.putExtra(DownloadReceiver.PARAM_PROGRESS,
                                                downloadProgressData)
                                        intent
                                    })
                                }
                                .setOnCancelListener {
                                    performStateTransition(DownloadStatus.NOT_DOWNLOADING)
                                    sendBroadcastIntent(DownloadReceiver.TYPE_CANCELLED)
                                }
                                .setOnPauseListener {
                                    if (progress.currentBytes > progress.totalBytes) {
                                        logError(TAG, UpdateDownloadException(withAppendedStateHistory("Download progress exceeded total file size. Either the server returned incorrect data or the app is in an invalid state!")))
                                        cancelDownload(SettingsManager(context).getPreference(PROPERTY_DOWNLOAD_ID, NOT_SET))
                                        downloadUpdate(updateData)
                                        return@setOnPauseListener
                                    }
                                    val currentTimestamp = System.currentTimeMillis()

                                    // This method gets called *very* often. We only want to update
                                    // the notification and send a broadcast once per second
                                    // Otherwise, the UI & Notification Renderer would be overflowed by our requests
                                    if ((previousProgressTimeStamp == NOT_SET.toLong()) or
                                            (currentTimestamp - previousProgressTimeStamp > 1000)) {
                                        // logDebug(TAG, "Received download progress update: " + progress.currentBytes + " / " + progress.totalBytes)
                                        val progressData = calculateDownloadETA(progress.currentBytes, progress.totalBytes)
                                        SettingsManager(context).savePreference(PROPERTY_DOWNLOAD_PROGRESS, progressData.progress)

                                        previousProgressTimeStamp = currentTimestamp
                                        progressPercentage = progressData.progress

                                        sendBroadcastIntent(DownloadReceiver.TYPE_PROGRESS_UPDATE, Function { intent ->
                                            intent.putExtra(DownloadReceiver.PARAM_PROGRESS, progressData)
                                        })
                                        LocalNotifications.showDownloadingNotification(context, updateData, progressData)
                                    }
                                }
                    }
                    val currentTimestamp = System.currentTimeMillis()

                    // This method gets called *very* often. We only want to update the notification and send a broadcast once per second
                    // Otherwise, the UI & Notification Renderer would be overflowed by our requests
                    if (previousProgressTimeStamp == NOT_SET.toLong() || currentTimestamp - previousProgressTimeStamp > 1000) {
                        // logDebug(TAG, "Received download progress update: " + progress.currentBytes + " / " + progress.totalBytes);
                        val progressData = calculateDownloadETA(progress.currentBytes, progress.totalBytes)
                        SettingsManager(context).savePreference(PROPERTY_DOWNLOAD_PROGRESS, progressData.progress)

                        previousProgressTimeStamp = currentTimestamp
                        progressPercentage = progressData.progress

                        sendBroadcastIntent(DownloadReceiver.TYPE_PROGRESS_UPDATE,
                                Function { i -> i.putExtra(DownloadReceiver.PARAM_PROGRESS, progressData) })
                        LocalNotifications.showDownloadingNotification(context, updateData, progressData)
                    }
                }
                .start(object : OnDownloadListener {
                    override fun onDownloadComplete() {
                        logDebug(TAG, "Downloading of " + updateData.filename + " complete, verification will begin soon...")
                        sendBroadcastIntent(DownloadReceiver.TYPE_DOWNLOAD_COMPLETED)
                        // isVerifying must be set before isDownloading to ensure the condition 'isRunning()' remains true at all times.
                        verifyUpdate(updateData)
                    }

                    override fun onError(error: Error) {
                        // PRDownloader's Error class distinguishes errors as connection errors and server errors.
                        // A connection error is an error with the network connection of the user, such as bad / lost wifi or data connection.
                        // A server error is an error which is deliberately returned by the server, such as a 404 / 500 / 503 status code.

                        // If the error is a connection error, we retry to resume downloading in 5 seconds (if there is a network connection then).
                        when {
                            error.isConnectionError -> {
                                logDebug(TAG, "Pausing download due to connection error")
                                performStateTransition(DownloadStatus.DOWNLOAD_PAUSED_WAITING_FOR_CONNECTION)
                                val settingsManager = SettingsManager(context)
                                val downloadId = settingsManager.getPreference(PROPERTY_DOWNLOAD_ID, NOT_SET)
                                val progress = settingsManager.getPreference(PROPERTY_DOWNLOAD_PROGRESS, NO_PROGRESS)

                                resumeDownloadOnReconnectingToNetwork(downloadId, updateData)

                                val progressData = DownloadProgressData(NOT_SET.toLong(), progress, true)
                                LocalNotifications.showDownloadPausedNotification(context, updateData, progressData)

                                sendBroadcastIntent(DownloadReceiver.TYPE_PROGRESS_UPDATE,
                                        Function { i -> i.putExtra(DownloadReceiver.PARAM_PROGRESS, progressData) })
                            }
                            error.isServerError -> {
                                // Otherwise, we inform the user that the server has refused the download & that it must be restarted at a later stage.
                                LocalNotifications.showDownloadFailedNotification(context, false, R.string.download_error_server, R.string.download_notification_error_server)

                                sendBroadcastIntent(DownloadReceiver.TYPE_DOWNLOAD_ERROR, Function { intent ->
                                    intent.putExtra(DownloadReceiver.PARAM_ERROR_IS_SERVER_ERROR, true)
                                    intent
                                })

                                performStateTransition(DownloadStatus.NOT_DOWNLOADING)
                                clearUp()
                            }
                            else -> {
                                // If not server and connection error, something has gone wrong internally. This should never happen!
                                LocalNotifications.showDownloadFailedNotification(context, true, R.string.download_error_internal, R.string.download_notification_error_internal)

                                sendBroadcastIntent(DownloadReceiver.TYPE_DOWNLOAD_ERROR, Function { intent ->
                                    intent.putExtra(DownloadReceiver.PARAM_ERROR_IS_INTERNAL_ERROR, true)
                                    intent
                                })

                                performStateTransition(DownloadStatus.NOT_DOWNLOADING)
                                clearUp()
                            }
                        }
                    }
                })

        // We persist the download ID so we can access it from anywhere and even after an app restart.
        val settingsManager = SettingsManager(context)
        settingsManager.savePreference(PROPERTY_DOWNLOAD_ID, downloadId)
    }

    @Synchronized
    private fun pauseDownload(downloadId: Int) {
        logDebug(TAG, "Pausing download #$downloadId")

        if (downloadId != NOT_SET && isStateTransitionAllowed(DownloadStatus.DOWNLOAD_PAUSED)) {
            performStateTransition(DownloadStatus.DOWNLOAD_PAUSED)
            logDebug(TAG, "Pausing using PRDownloader.pause()")
            PRDownloader.pause(downloadId)
        } else {
            logWarning(TAG, UpdateDownloadException("Not pausing download, invalid download ID provided or not pause-able."))
        }
    }

    @Synchronized
    private fun resumeDownload(downloadId: Int, updateData: UpdateData?) {
        logDebug(TAG, "Resuming download #$downloadId")

        if (!isStateTransitionAllowed(DownloadStatus.DOWNLOAD_QUEUED)) {
            logWarning(TAG, UpdateDownloadException("Not resuming download, is a download operation already in progress?"))
            return
        }

        if (downloadId != NOT_SET) {
            // If the download is still in the PRDownloader request queue, resume it.
            // If not, start the download again (PRdownloader will still resume it using its own SQLite database)
            if (DownloadRequestQueue.getInstance().getStatus(downloadId) != Status.UNKNOWN) {
                logDebug(TAG, "Resuming using PRDownloader.resume()")
                performStateTransition(DownloadStatus.DOWNLOAD_QUEUED)
                PRDownloader.resume(downloadId)
            } else {
                logDebug(TAG, "Resuming using PRDownloader.download()")
                downloadUpdate(updateData)
            }

        } else {
            logWarning(TAG, UpdateDownloadException("Not resuming download, invalid download ID provided."))
        }
    }

    @Synchronized
    private fun cancelDownload(downloadId: Int) {
        logDebug(TAG, "Cancelling download #$downloadId")

        if (downloadId != NOT_SET) {
            performStateTransition(DownloadStatus.NOT_DOWNLOADING)
            PRDownloader.cancel(downloadId)
            LocalNotifications.hideDownloadingNotification(applicationContext)
            clearUp()
            logDebug(TAG, "Cancelled download #$downloadId")
        } else {
            logWarning(TAG, UpdateDownloadException("Not cancelling download, no valid ID was provided..."))
        }
    }

    @Synchronized
    private fun deleteDownloadedFile(updateData: UpdateData?) {
        if (updateData?.filename == null) {
            logWarning(TAG, UpdateDownloadException("Could not delete downloaded file, null update data or update data without file name was provided"))
            return
        }

        logDebug(TAG, "Deleting downloaded update file " + updateData.filename!!)

        val downloadedFile = File(Environment.getExternalStoragePublicDirectory(DIRECTORY_ROOT).absolutePath, updateData.filename!!)
        if (!downloadedFile.delete()) {
            logWarning(TAG, UpdateDownloadException("Could not delete downloaded file " + updateData.filename!!))
        }
        performStateTransition(DownloadStatus.NOT_DOWNLOADING)
    }

    @Synchronized
    private fun checkDownloadStatus(downloadId: Int, updateData: UpdateData?) {
        logDebug(TAG, "Checking status for download #"
                + downloadId
                + if (updateData != null)
            " and updateData " + updateData.versionNumber!!
        else
            "")

        val resultStatus: DownloadStatus
        val progress: DownloadProgressData
        val hasNetwork = Utils.checkNetworkConnection(applicationContext)

        when (state) {
            DownloadStatus.DOWNLOAD_QUEUED -> {
                logDebug(TAG, "Download #$downloadId is queued")
                // If queued, there is no progress and the download will start soon.
                resultStatus = DownloadStatus.DOWNLOAD_QUEUED
                progress = DownloadProgressData(NOT_SET.toLong(), NO_PROGRESS)
            }
            DownloadStatus.DOWNLOADING -> {
                // If running, return the previously-stored progress percentage.
                val storedProgressPercentage = SettingsManager(applicationContext).getPreference(PROPERTY_DOWNLOAD_PROGRESS, NO_PROGRESS)
                resultStatus = DownloadStatus.DOWNLOADING
                progress = DownloadProgressData(NOT_SET.toLong(), storedProgressPercentage)
                logDebug(TAG, "Download #$downloadId is running @$storedProgressPercentage")
            }
            DownloadStatus.DOWNLOAD_PAUSED -> {
                // If the download is paused, we return its current percentage and allow resuming of it.
                val storedProgressPercentage = SettingsManager(applicationContext).getPreference(PROPERTY_DOWNLOAD_PROGRESS, NO_PROGRESS)
                resultStatus = DownloadStatus.DOWNLOAD_PAUSED
                progress = DownloadProgressData(NOT_SET.toLong(), storedProgressPercentage, !hasNetwork)
                logDebug(TAG, "Download #$downloadId is paused @$storedProgressPercentage")
            }
            DownloadStatus.DOWNLOAD_PAUSED_WAITING_FOR_CONNECTION -> {
                // If the download is paused, we return its current percentage and allow resuming of it.
                val storedProgressPercentage = SettingsManager(applicationContext).getPreference(PROPERTY_DOWNLOAD_PROGRESS, NO_PROGRESS)
                resultStatus = DownloadStatus.DOWNLOAD_PAUSED_WAITING_FOR_CONNECTION
                progress = DownloadProgressData(NOT_SET.toLong(), storedProgressPercentage, true)
                if (autoResumeOnConnectionErrorRunnable == null) {
                    resumeDownloadOnReconnectingToNetwork(downloadId, updateData)
                }
                logDebug(TAG, "Download #$downloadId is waiting for a network connection @$storedProgressPercentage")
            }
            DownloadStatus.VERIFYING -> {
                // If the download is being verified, it is always at 100% completion and does not have to wait for a connection.
                resultStatus = DownloadStatus.VERIFYING
                progress = DownloadProgressData(NOT_SET.toLong(), 100, false)
                logDebug(TAG, "Download #$downloadId is verifying")
            }
            DownloadStatus.NOT_DOWNLOADING -> {
                // If not downloading in the service, manually check if the file exists to check if it is complete or not
                // If the file exists, assume we've passed the download and verification.
                // If the file does not exist, the update has likely never been downloaded before.
                resultStatus = if (checkDownloadCompletionByFile(updateData)) DownloadStatus.DOWNLOAD_COMPLETED else DownloadStatus.NOT_DOWNLOADING
                performStateTransition(resultStatus)
                progress = DownloadProgressData(NOT_SET.toLong(), if (resultStatus == DownloadStatus.DOWNLOAD_COMPLETED) 100 else 0, false)
                logDebug(TAG, "Download #" + downloadId + " is " + if (resultStatus == DownloadStatus.DOWNLOAD_COMPLETED) "completed" else "not started")
            }
            DownloadStatus.DOWNLOAD_COMPLETED -> {
                // Even if the service is marked as COMPLETED, manually check if the file exists.
                // This because the user can have switched device / a new update can have been released since last time we checked.
                // If the file exists, the download is indeed complete.
                // If the file does not exist, another update has likely been downloaded before and this update was not downloaded.
                resultStatus = if (checkDownloadCompletionByFile(updateData)) DownloadStatus.DOWNLOAD_COMPLETED else DownloadStatus.NOT_DOWNLOADING
                performStateTransition(resultStatus)
                progress = DownloadProgressData(NOT_SET.toLong(), if (resultStatus == DownloadStatus.DOWNLOAD_COMPLETED) 100 else 0, false)
                logDebug(TAG, "Download #" + downloadId + " is " + if (resultStatus == DownloadStatus.DOWNLOAD_COMPLETED) "completed" else "not started")
            }
            else -> {
                resultStatus = DownloadStatus.NOT_DOWNLOADING
                progress = DownloadProgressData(NOT_SET.toLong(), NO_PROGRESS)
            }
        }

        sendBroadcastIntent(DownloadReceiver.TYPE_STATUS_REQUEST, Function { intent ->
            intent.putExtra(DownloadReceiver.PARAM_STATUS, resultStatus)
            intent.putExtra(DownloadReceiver.PARAM_PROGRESS, progress)
            intent
        })
    }

    @SuppressLint("StaticFieldLeak")
    private fun verifyUpdate(updateData: UpdateData?) {
        if (!isStateTransitionAllowed(DownloadStatus.VERIFYING)) {
            logWarning(TAG, UpdateVerificationException("Not verifying update, is an update verification already in progress?"))
            return
        }

        performStateTransition(DownloadStatus.VERIFYING)
        verifier = object : AsyncTask<UpdateData, Void, Boolean>() {
            override fun doInBackground(vararg updateDatas: UpdateData): Boolean? {
                logDebug(TAG, "Verifying " + updateData!!.filename!!)
                val downloadedFile = File(Environment.getExternalStoragePublicDirectory(DIRECTORY_ROOT)
                        .absolutePath, updateData.filename!!)

                return updateData.mD5Sum == null || MD5.checkMD5(updateData.mD5Sum!!, downloadedFile)
            }

            override fun onPreExecute() {
                logDebug(TAG, "Preparing to verify downloaded update file " + updateData!!.filename!!)
                performStateTransition(DownloadStatus.VERIFYING)

                LocalNotifications.showVerifyingNotification(applicationContext, ONGOING, HAS_NO_ERROR)
                sendBroadcastIntent(DownloadReceiver.TYPE_VERIFY_STARTED)
            }

            override fun onPostExecute(valid: Boolean?) {
                logDebug(TAG, "Verification result for " + updateData!!.filename + ": " + valid)

                if (valid!!) {
                    LocalNotifications.hideVerifyingNotification(applicationContext)
                    LocalNotifications.showDownloadCompleteNotification(applicationContext, updateData)
                    performStateTransition(DownloadStatus.DOWNLOAD_COMPLETED)
                    sendBroadcastIntent(DownloadReceiver.TYPE_VERIFY_COMPLETE)
                } else {
                    deleteDownloadedFile(updateData)
                    LocalNotifications.showVerifyingNotification(applicationContext, NOT_ONGOING, HAS_ERROR)
                    performStateTransition(DownloadStatus.NOT_DOWNLOADING)
                    sendBroadcastIntent(DownloadReceiver.TYPE_VERIFY_FAILED)
                }

                clearUp()
            }

            override fun onCancelled() {
                if (updateData != null) {
                    logDebug(TAG, "Cancelled verification of " + updateData.filename!!)
                }
            }
        }.execute()
    }

    private fun calculateDownloadETA(bytesDownloadedSoFar: Long, totalSizeBytes: Long): DownloadProgressData {
        val bytesDownloadedInSecond: Double
        var validMeasurement = false

        var numberOfSecondsRemaining = NOT_SET.toLong()
        var averageBytesPerSecond = NOT_SET.toLong()
        val currentTimeStamp = System.currentTimeMillis()
        val bytesRemainingToDownload = totalSizeBytes - bytesDownloadedSoFar

        if (previousBytesDownloadedSoFar != NOT_SET.toLong()) {

            val numberOfElapsedSeconds = TimeUnit.MILLISECONDS.toSeconds(currentTimeStamp -
                    previousTimeStamp).toDouble()

            bytesDownloadedInSecond = if (numberOfElapsedSeconds > 0.0) {
                (bytesDownloadedSoFar - previousBytesDownloadedSoFar) / numberOfElapsedSeconds
            } else {
                0.0
            }

            // Sometimes no new progress data is available.
            // If no new data is available, return the previously stored data to keep the UI showing that.
            validMeasurement = bytesDownloadedInSecond > 0 || numberOfElapsedSeconds > 5

            if (validMeasurement) {
                // In case of no network, clear all measurements to allow displaying the now-unknown ETA...
                if (bytesDownloadedInSecond == 0.0) {
                    measurements.clear()
                }

                // Remove old measurements to keep the average calculation based on 5 measurements
                if (measurements.size > 10) {
                    measurements.subList(0, 1).clear()
                }

                measurements.add(bytesDownloadedInSecond)
            }

            // Calculate number of seconds remaining based off average download speed.
            averageBytesPerSecond = calculateAverageBytesDownloadedInSecond(measurements).toLong()
            numberOfSecondsRemaining = if (averageBytesPerSecond > 0) {
                bytesRemainingToDownload / averageBytesPerSecond
            } else {
                NOT_SET.toLong()
            }
        }

        if (averageBytesPerSecond != NOT_SET.toLong()) {
            if (validMeasurement) {
                previousNumberOfSecondsRemaining = numberOfSecondsRemaining
                previousTimeStamp = currentTimeStamp
            } else {
                numberOfSecondsRemaining = previousNumberOfSecondsRemaining
            }
        }

        previousBytesDownloadedSoFar = bytesDownloadedSoFar

        var progress = 0

        if (totalSizeBytes > 0.0) {
            progress = (bytesDownloadedSoFar * 100 / totalSizeBytes).toInt()
        }

        return DownloadProgressData(numberOfSecondsRemaining, progress)
    }

    private fun calculateAverageBytesDownloadedInSecond(measurements: List<Double>?): Double {
        return if (measurements == null || measurements.isEmpty()) {
            0.0
        } else {
            var totalBytesDownloadedInSecond = 0.0

            for (measurementData in measurements) {
                totalBytesDownloadedInSecond += measurementData
            }

            totalBytesDownloadedInSecond / measurements.size
        }
    }

    // Clears all static / instance variables. If not called when download stops,
    // leftovers of a previous download *will* cause issues when using this service for the next time
    private fun clearUp() {
        measurements.clear()
        previousProgressTimeStamp = NOT_SET.toLong()
        progressPercentage = NO_PROGRESS
        previousTimeStamp = NOT_SET.toLong()
        previousBytesDownloadedSoFar = NOT_SET.toLong()
        previousNumberOfSecondsRemaining = NOT_SET.toLong()
        verifier = null
        autoResumeOnConnectionErrorRunnable = null
        val settingsManager = SettingsManager(applicationContext)
        settingsManager.deletePreference(PROPERTY_DOWNLOAD_PROGRESS)
        settingsManager.deletePreference(PROPERTY_DOWNLOAD_ID)
        settingsManager.deletePreference(PROPERTY_DOWNLOADER_STATE)
    }

    private fun sendBroadcastIntent(intentType: String, intentParamsCustomizer: Function<Intent, Intent> = Function { i -> i }) {
        var broadcastIntent = Intent()

        broadcastIntent.action = DownloadReceiver.ACTION_DOWNLOAD_EVENT
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT)
        broadcastIntent.putExtra(DownloadReceiver.PARAM_TYPE, intentType)

        broadcastIntent = intentParamsCustomizer.apply(broadcastIntent)

        sendBroadcast(broadcastIntent)
    }

    private fun checkDownloadCompletionByFile(updateData: UpdateData?): Boolean {
        if (updateData?.filename == null) {
            logInfo(TAG, "Cannot check for download completion by file - null update data or filename provided!")
            return false
        }
        return File(Environment.getExternalStoragePublicDirectory(DIRECTORY_ROOT),
                updateData.filename!!).exists()
    }

    private fun resumeDownloadOnReconnectingToNetwork(downloadId: Int, updateData: UpdateData?) {
        val handler = Handler()

        if (autoResumeOnConnectionErrorRunnable == null) {
            autoResumeOnConnectionErrorRunnable = object : Runnable {
                override fun run() {
                    if (Utils.checkNetworkConnection(applicationContext)) {
                        logDebug(TAG, "Network connectivity restored, resuming download...")
                        resumeDownload(downloadId, updateData)
                    } else {
                        handler.postDelayed(this, NO_CONNECTION_REFRESH_RATE.toLong())
                    }
                }
            }

            handler.postDelayed(autoResumeOnConnectionErrorRunnable!!, NO_CONNECTION_REFRESH_RATE.toLong())
        }
    }

    /**
     * Transitions the state of the service from the current state S to the new state S' (if
     * allowed).
     *
     * @param newState Destination state (S').
     */
    private fun performStateTransition(newState: DownloadStatus) {
        if (isStateTransitionAllowed(newState) && newState != state) {
            //Log.v(TAG, state + " -> " + newState);
            executedStateTransitions.add(Pair.create(LocalDateTime.now(DateTimeZone.forID("Europe/Amsterdam")), "$state -> $newState"))
            state = newState
            SettingsManager(applicationContext).savePreference(PROPERTY_DOWNLOADER_STATE, state.toString())
        }
    }

    /**
     * Checks if a State Transition ST is allowed from the current state S to a given state S' This
     * is done using the table described in the top of this class.
     *
     * @param newState S' Destination state
     *
     * @return whether or not a state transition is allowed.
     */
    private fun isStateTransitionAllowed(newState: DownloadStatus): Boolean {
        // If the new state is the same as the current state, allow it
        if (newState == state) {
            return true
        }

        // Currently, a transition to not downloading exists from every possible state.
        if (newState == DownloadStatus.NOT_DOWNLOADING) {
            return true
        }

        when (state) {
            DownloadStatus.NOT_DOWNLOADING ->
                // Start download in service or when update is already (manually) downloaded
                return newState == DownloadStatus.DOWNLOAD_QUEUED || newState == DownloadStatus.DOWNLOAD_COMPLETED
            DownloadStatus.DOWNLOAD_QUEUED ->
                // Start download execute (by library)
                return newState == DownloadStatus.DOWNLOADING
            DownloadStatus.DOWNLOADING ->
                // Pause download, wait for connection or download completed
                return newState == DownloadStatus.DOWNLOAD_PAUSED || newState == DownloadStatus.DOWNLOAD_PAUSED_WAITING_FOR_CONNECTION || newState == DownloadStatus.VERIFYING
            DownloadStatus.DOWNLOAD_PAUSED ->
                // Resume download (either via PRDownloader.Resume or via downloadUpdate()
                return newState == DownloadStatus.DOWNLOAD_QUEUED
            DownloadStatus.DOWNLOAD_PAUSED_WAITING_FOR_CONNECTION ->
                // Resume download
                return newState == DownloadStatus.DOWNLOAD_QUEUED
            DownloadStatus.VERIFYING -> return newState == DownloadStatus.DOWNLOAD_COMPLETED
            else -> return false
        }
    }

    // Convert state history to following format and save it to SharedPreferences:
    // 2019-01-01 00:00:00.000|DOWNLOADING -> PAUSED,2019-01-01 00:00:01.000|PAUSED -> DOWNLOAD_QUEUED
    private fun serializeExecutedStateTransitions() {
        val serializedStateHistory = StreamSupport.stream(executedStateTransitions)
                .map { est -> String.format("%s|%s", est.first.toString(HISTORY_DATETIME_PATTERN), est.second) }
                .collect(Collectors.joining(","))

        val settingsManager = SettingsManager(applicationContext)
        settingsManager.savePreference(PROPERTY_DOWNLOADER_STATE_HISTORY, serializedStateHistory)
    }

    // Convert saved history from SharedPreferences back to format storable in this class.
    private fun deserializeExecutedStateTransitions(): List<Pair<LocalDateTime, String>> {
        val settingsManager = SettingsManager(applicationContext)
        val serializedStateHistory = settingsManager.getPreference(PROPERTY_DOWNLOADER_STATE_HISTORY, "")

        return if (serializedStateHistory == null || serializedStateHistory.isEmpty()) {
            ArrayList()
        } else StreamSupport.stream(listOf(*serializedStateHistory.split(",".toRegex())
                .dropLastWhile { it.isEmpty() }.toTypedArray()))
                .map { elem ->
                    val parts = elem.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    val timestamp: LocalDateTime

                    if (parts.size < 2) {
                        logError(TAG, OxygenUpdaterException("Cannot parse downloader state. Contents of line: $elem, total contents: $serializedStateHistory"))
                        return@map null
                    }

                    timestamp = try {
                        LocalDateTime.parse(parts[0], DateTimeFormat.forPattern(HISTORY_DATETIME_PATTERN))
                    } catch (e: Exception) {
                        LocalDateTime.now()
                    }

                    Pair.create(timestamp, parts[1])
                }
                .filter { e -> e != null }
                .collect(Collectors.toList()) as List<Pair<LocalDateTime, String>>
    }

    companion object {
        val TAG = "DownloadService"

        val PARAM_ACTION = "ACTION"
        val ACTION_DOWNLOAD_UPDATE = "ACTION_DOWNLOAD_UPDATE"
        val ACTION_CANCEL_DOWNLOAD = "ACTION_CANCEL_DOWNLOAD"
        val ACTION_PAUSE_DOWNLOAD = "ACTION_PAUSE_DOWNLOAD"
        val ACTION_RESUME_DOWNLOAD = "ACTION_RESUME_DOWNLOAD"
        val ACTION_GET_INITIAL_STATUS = "ACTION_GET_INITIAL_STATUS"
        val ACTION_DELETE_DOWNLOADED_UPDATE = "ACTION_DELETE_DOWNLOADED_UPDATE"
        val ACTION_SERVICE_RESTART = "ACTION_SERVICE_RESTART"

        val PARAM_UPDATE_DATA = "UPDATE_DATA"
        val PARAM_DOWNLOAD_ID = "DOWNLOAD_ID"

        val INTENT_SERVICE_RESTART = "com.arjanvlek.oxygenupdater.intent.restartDownloadService"
        val DIRECTORY_ROOT = ""
        val NOT_SET = -1
        val NO_PROGRESS = 0
        val isOperationPending = AtomicBoolean(false)
        private val NOTIFICATION_CHANNEL_ID = "com.arjanvlek.oxygenupdater.download.DownloadService"
        /**
         * Amount of milliseconds after which the connection gets marked as timed-out
         */
        private val CONNECT_TIMEOUT = 30000
        /**
         * Amount of milliseconds after which the download gets marked as timed-out *after* an initial
         * connection has been established
         */
        private val READ_TIMEOUT = 120000
        /**
         * How often to check for new tasks or if we are finished (interrupt rate, determines how snappy
         * the service responds to the UI)
         */
        private val INTERRUPT_RATE = 50
        /**
         * Amount of free storage space to reserve when downloading an update (currently: 25 MB)
         */
        private val SAFE_MARGIN = (1048576 * 25).toLong()
        /**
         * How often to re-check for a network connection if no connection is currently available (in
         * milliseconds)
         */
        private val NO_CONNECTION_REFRESH_RATE = 5000
        /**
         * Date / time pattern for history of performed operations
         */
        private val HISTORY_DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS"
        private val executedStateTransitions = LinkedList<Pair<LocalDateTime, String>>()
        // We need to have class-level status fields, because we need to be able to check this in a blocking / synchronous way.
        private var state = DownloadStatus.NOT_DOWNLOADING
        private var autoResumeOnConnectionErrorRunnable: Runnable? = null

        /**
         * Performs an operation on this IntentService. The action must be one of the actions as
         * declared in this class.
         *
         * @param activity   Calling activity
         * @param action     Action to perform
         * @param updateData Update data on which the action must be performed
         */
        fun performOperation(activity: Activity?, action: String, updateData: UpdateData) {
            if (activity == null) {
                return
            }

            // We want to make sure the new operation can always be processed, even if the service is kept running in a thread loop.
            isOperationPending.set(true)

            val settingsManager = SettingsManager(activity)
            val downloadId = settingsManager.getPreference(PROPERTY_DOWNLOAD_ID, NOT_SET)

            val downloadIntent = Intent(activity, DownloadService::class.java)
            downloadIntent.putExtra(PARAM_ACTION, action)
            downloadIntent.putExtra(PARAM_UPDATE_DATA, updateData)
            downloadIntent.putExtra(PARAM_DOWNLOAD_ID, downloadId)

            try {
                activity.startService(downloadIntent)
            } catch (e: Exception) {
                logError(TAG, withAppendedStateHistory("Failed to start DownloadService"), e)
                isOperationPending.set(false)
            }

        }

        val isRunning: Boolean
            get() = (state == DownloadStatus.DOWNLOADING
                    || state == DownloadStatus.DOWNLOAD_QUEUED
                    || state == DownloadStatus.VERIFYING
                    || state == DownloadStatus.DOWNLOAD_PAUSED_WAITING_FOR_CONNECTION)

        private fun withAppendedStateHistory(text: String): String {
            return String.format(
                    "%s\n\n%s\n%s",
                    text,
                    "History of actions performed by the downloader:",
                    StreamSupport.stream(executedStateTransitions)
                            .filter { est ->
                                (est.first != null) and (est.second != null)
                            }
                            .map { est ->
                                est.first.toString(HISTORY_DATETIME_PATTERN) + ": " + est.second
                            }
                            .collect(Collectors.joining("\n"))
            )
        }
    }
}
