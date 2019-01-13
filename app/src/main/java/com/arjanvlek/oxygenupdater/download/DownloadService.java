package com.arjanvlek.oxygenupdater.download;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.arjanvlek.oxygenupdater.R;
import com.arjanvlek.oxygenupdater.internal.Utils;
import com.arjanvlek.oxygenupdater.internal.logger.Logger;
import com.arjanvlek.oxygenupdater.notifications.LocalNotifications;
import com.arjanvlek.oxygenupdater.settings.SettingsManager;
import com.arjanvlek.oxygenupdater.updateinformation.UpdateData;
import com.downloader.Error;
import com.downloader.OnDownloadListener;
import com.downloader.PRDownloader;
import com.downloader.PRDownloaderConfig;
import com.downloader.Priority;
import com.downloader.Status;
import com.downloader.internal.DownloadRequestQueue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import java8.util.function.Function;

import static com.arjanvlek.oxygenupdater.ApplicationData.APP_USER_AGENT;
import static com.arjanvlek.oxygenupdater.notifications.LocalNotifications.HAS_ERROR;
import static com.arjanvlek.oxygenupdater.notifications.LocalNotifications.HAS_NO_ERROR;
import static com.arjanvlek.oxygenupdater.notifications.LocalNotifications.NOT_ONGOING;
import static com.arjanvlek.oxygenupdater.notifications.LocalNotifications.ONGOING;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_DOWNLOADER_STATE;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_DOWNLOAD_ID;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_DOWNLOAD_PROGRESS;

/**
 * DownloadService handles the downloading and MD5 verification of OxygenOS updates.
 * It does so by using the PRDownloader library, which offers Pause and Resume support as well as automatic recovery after network loss.
 * The service also sends status updates to {@link DownloadReceiver}, which can pass the status of the download to the UI.
 *
 * This is a *very* complex service, because it is responsible for all state transitions that take
 * place when downloading or verifying an update. See {@link DownloadStatus} for all possible states.
 *
 * To make clear how this service can be used, see the following state table.
 * Its rows contain all possible transitions (functions or conditions) given a state S for an updateData UD
 * Its columns describe the state S' the service is in (for this UD) *after* having performed the transition.
 *
 * State S / S'       NOT_DOWNLOADING      DOWNLOAD_QUEUED     DOWNLOADING       DOWNLOAD_PAUSED    VERIFYING         DOWNLOAD_COMPLETED
 * NOT_DOWNLOADING    null / noDownloadUrl downloadUpdate()        (-)                 (-)             (-)                    (-)
 * DOWNLOAD_QUEUED    insufficientStorage        (-)             [internal]            (-)             (-)                    (-)
 * DOWNLOADING        cancelDownload(),          (-)               (-)           pauseDownload(),   downloadComplete          (-)
 *                    serverError                (-)               (-)           connectionError       (-)                    (-)
 * DOWNLOAD_PAUSED    cancelDownload()           (-)          resumeDownload()         (-)             (-)                    (-)
 * VERIFYING          verificationError          (-)               (-)                 (-)             (-)            verificationComplete
 * DOWNLOAD_COMPLETED deleteDownload()           (-)               (-)                 (-)             (-)                    (-)
 *
 * Background running of this service
 * When the app gets killed, the service gets killed as well, but when this happens and it was still running
 * (that is: in the state DOWNLOADING or VERIFYING), it will get auto-restarted via an ACTION_SERVICE_RESTART intent (through {@link DownloadServiceRestarter})
 * A restart then calls resumeDownload() when the state was DOWNLOADING or verifyUpdate() when the state was VERIFYING.
 * The state is saved in SettingsManager.PROPERTY_DOWNLOADER_STATE.
 *
 * Thread loop (in onHandleIntent())
 * To prevent the service from continuously restarting when downloading / verifying (as those have own threads, so the main thread is completed very quickly),
 * the service executes a while-true loop in which it listens every 50ms if a new command has been ordered or if the download has finished.
 * If a new command has been ordered, it exits the current loop (causing a restart), executes the new Intent (because Intents are sequential) and then executes the restart intent
 * The after-executed restart intent *should* do nothing, because the action usually cancelled or paused the download.
 *
 * Oxygen Updater, copyright 2019 Arjan Vlek. File created by arjan.vlek on 03/01/2019.
 */
public class DownloadService extends IntentService {

    public static final String TAG = "DownloadService";

    public static final String PARAM_ACTION = "ACTION";
    public static final String ACTION_DOWNLOAD_UPDATE = "ACTION_DOWNLOAD_UPDATE";
    public static final String ACTION_CANCEL_DOWNLOAD = "ACTION_CANCEL_DOWNLOAD";
    public static final String ACTION_PAUSE_DOWNLOAD = "ACTION_PAUSE_DOWNLOAD";
    public static final String ACTION_RESUME_DOWNLOAD = "ACTION_RESUME_DOWNLOAD";
    public static final String ACTION_GET_INITIAL_STATUS = "ACTION_GET_INITIAL_STATUS";
    public static final String ACTION_DELETE_DOWNLOADED_UPDATE = "ACTION_DELETE_DOWNLOADED_UPDATE";
    public static final String ACTION_SERVICE_RESTART = "ACTION_SERVICE_RESTART";

    public static final String PARAM_UPDATE_DATA = "UPDATE_DATA";
    public static final String PARAM_DOWNLOAD_ID = "DOWNLOAD_ID";

    public static final String INTENT_SERVICE_RESTART = "com.arjanvlek.oxygenupdater.intent.restartDownloadService";
    private static final String NOTIFICATION_CHANNEL_ID = "com.arjanvlek.oxygenupdater.download.DownloadService";

    public static final String DIRECTORY_ROOT = "";
    public static final int NOT_SET = -1;

    // We need to have class-level status fields, because we need to be able to check this in a blocking / synchronous way.
    public static final AtomicBoolean isDownloading = new AtomicBoolean(false);
    public static final AtomicBoolean isVerifying = new AtomicBoolean(false);
    public static final AtomicBoolean isOperationPending = new AtomicBoolean(false);

    private UpdateData updateData;
    private AsyncTask verifier;
    private static Runnable autoResumeOnConnectionErrorRunnable;

    // Progress calculation data
    private final List<Double> measurements = new ArrayList<>();
    private long previousProgressTimeStamp = NOT_SET;
    private int progressPercentage = 0;
    private long previousBytesDownloadedSoFar = NOT_SET;
    private long previousTimeStamp = NOT_SET;
    private long previousNumberOfSecondsRemaining = NOT_SET;

    /**
     * Creates an IntentService. Invoked by your subclass's constructor.
     */
    public DownloadService() {
        super(TAG);
    }

    /**
     * Performs an operation on this IntentService. The action must be one of the actions as declared in this class.
     * @param activity Calling activity
     * @param action Action to perform
     * @param updateData Update data on which the action must be performed
     */
    public static void performOperation(Activity activity, String action, UpdateData updateData) {
        if (activity == null) {
            return;
        }

        // We want to make sure the new operation can always be processed, even if the service is kept running in a thread loop.
        isOperationPending.set(true);

        SettingsManager settingsManager = new SettingsManager(activity);
        int downloadId = settingsManager.getPreference(PROPERTY_DOWNLOAD_ID, -1);

        Intent downloadIntent = new Intent(activity, DownloadService.class);
        downloadIntent.putExtra(DownloadService.PARAM_ACTION, action);
        downloadIntent.putExtra(DownloadService.PARAM_UPDATE_DATA, updateData);
        downloadIntent.putExtra(DownloadService.PARAM_DOWNLOAD_ID, downloadId);

        activity.startService(downloadIntent);
    }

    /*
     * We want to keep this service running, even if the app gets killed.
     * Otherwise, the download stops and can no longer be resumed.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            startMyOwnForeground();
        }
        else {
            startForeground(1, new Notification());
        }
    }

    /*
     * We create a notification channel and notification to allow the app downloading in the background on Android versions >= O.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private void startMyOwnForeground() {
        String channelName = getString(R.string.download_service_name);
        NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        channel.setLightColor(Color.BLUE);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.createNotificationChannel(channel);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.drawable.ic_stat_settings)
                .setContentTitle(getString(R.string.download_in_background))
                .setPriority(NotificationManager.IMPORTANCE_NONE)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();

        startForeground(2, notification);
    }

    /*
     * Required to keep connection to downloader database.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    /*
     * We auto-restart this service if it gets killed while a download is still in progress.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        SettingsManager settingsManager = new SettingsManager(getApplicationContext());

        if (isRunning()) {
            Log.d(TAG, "onDestroy() was called whilst running, saving state & pausing current activity");
            settingsManager.savePreference(PROPERTY_DOWNLOADER_STATE, isDownloading.get() ? DownloadStatus.DOWNLOADING.toString() : isVerifying.get() ? DownloadStatus.VERIFYING.toString() : DownloadStatus.NOT_DOWNLOADING.toString());

            // We briefly pause the download - it can be (and will be) resumed in the newly spawned service.
            if (isDownloading.get()) {
                Log.d(TAG, "Pausing download using PRDownloader.pause()");
                PRDownloader.pause(settingsManager.getPreference(PROPERTY_DOWNLOAD_ID, -1));
            }

            // If onDestroy() happens when checking the MD5, we'll have to start checking it over again (this process is not resumable)
            if (isVerifying.get() && this.verifier != null) {
                Log.d(TAG, "Aborting in-progress MD5 verification");
                this.verifier.cancel(true);
            }

            isDownloading.set(false);
            isVerifying.set(false);

            if (autoResumeOnConnectionErrorRunnable != null) {
                new Handler().removeCallbacks(autoResumeOnConnectionErrorRunnable);
                autoResumeOnConnectionErrorRunnable = null;
            }

            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction(INTENT_SERVICE_RESTART);
            broadcastIntent.putExtra(DownloadService.PARAM_UPDATE_DATA, updateData);
            broadcastIntent.putExtra(DownloadService.PARAM_DOWNLOAD_ID, settingsManager.getPreference(PROPERTY_DOWNLOAD_ID, -1));
            broadcastIntent.setClass(this, DownloadServiceRestarter.class);
            this.sendBroadcast(broadcastIntent);
        } else {
            settingsManager.savePreference(PROPERTY_DOWNLOADER_STATE, DownloadStatus.NOT_DOWNLOADING.toString());
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) {
            return;
        }

        isOperationPending.set(false);
        Logger.init(getApplication());

        PRDownloaderConfig config = PRDownloaderConfig.newBuilder()
                .setDatabaseEnabled(true)
                .setUserAgent(APP_USER_AGENT)
                .setConnectTimeout(30_000)
                .setReadTimeout(120_000)
                .build();

        PRDownloader.initialize(getApplicationContext(), config);

        String action = intent.getStringExtra(PARAM_ACTION);
        int downloadId = intent.getIntExtra(PARAM_DOWNLOAD_ID, -1);
        updateData = intent.getParcelableExtra(PARAM_UPDATE_DATA);

        switch(action) {
            case ACTION_SERVICE_RESTART:
                // If restarting the service, we'll have to pick up the work we were doing.
                // This is done by reading the persisted state of the service.
                SettingsManager settingsManager = new SettingsManager(getApplicationContext());
                DownloadStatus serviceStatus = DownloadStatus.valueOf(settingsManager.getPreference(PROPERTY_DOWNLOADER_STATE, DownloadStatus.NOT_DOWNLOADING.toString()));

                if (serviceStatus == DownloadStatus.DOWNLOADING) {
                    Log.d(TAG, "Resuming temporarily-paused download");
                    resumeDownload(downloadId, updateData);
                    break;
                } else if (serviceStatus == DownloadStatus.VERIFYING) {
                    Log.d(TAG, "Restarted aborted MD5 verification");
                    verifyUpdate(updateData);
                    break;
                }
                break;
            case ACTION_DOWNLOAD_UPDATE:
                downloadUpdate(updateData);
                break;
            case ACTION_PAUSE_DOWNLOAD:
                pauseDownload(downloadId);
                break;
            case ACTION_RESUME_DOWNLOAD:
                resumeDownload(downloadId, updateData);
                break;
            case ACTION_CANCEL_DOWNLOAD:
                cancelDownload(downloadId);
                break;
            case ACTION_DELETE_DOWNLOADED_UPDATE:
                deleteDownloadedFile(updateData);
                break;
            case ACTION_GET_INITIAL_STATUS:
                checkDownloadStatus(downloadId, updateData);
                break;
        }

        // Keep this thread running to avoid the service continuously exiting / restarting.
        // Only if a new operation (Intent) has been called, release this thread and let the service live on in that new thread.
        while (true) {
            try {
                Thread.sleep(50);
                if (isOperationPending.get() || !isRunning()) {
                    break;
                }
            } catch (InterruptedException ignored) {

            }
        }
    }

    private synchronized void downloadUpdate(UpdateData updateData) {
        // Check if the update is downloadable
        if (updateData == null || updateData.getDownloadUrl() == null) {
            LocalNotifications.showDownloadFailedNotification(getApplicationContext(), false, R.string.download_error_internal, R.string.download_notification_error_internal);
            sendBroadcastIntent(DownloadReceiver.TYPE_DOWNLOAD_ERROR, (intent) -> {
                intent.putExtra(DownloadReceiver.PARAM_ERROR_IS_INTERNAL_ERROR, true);
                Logger.logError(TAG, "Update data is null or has no Download URL");
                return intent;
            });
            return;
        }

        if (!updateData.getDownloadUrl().contains("http")) {
            LocalNotifications.showDownloadFailedNotification(getApplicationContext(), false, R.string.download_error_internal, R.string.download_notification_error_internal);
            sendBroadcastIntent(DownloadReceiver.TYPE_DOWNLOAD_ERROR, (intent) -> {
                intent.putExtra(DownloadReceiver.PARAM_ERROR_IS_INTERNAL_ERROR, true);
                Logger.logError(TAG, "Update data has invalid Download URL (" + updateData.getDownloadUrl() + ")");
                return intent;
            });
            return;
        }

        // Check if there is enough free storage space before downloading
        long availableSizeInBytes = new StatFs(Environment.getExternalStoragePublicDirectory(DIRECTORY_ROOT).getPath()).getAvailableBytes();
        long requiredFreeBytes = updateData.getDownloadSize();

        // Download size in bytes is approximately as it is entered in Megabytes by the contributors.
        // Also, we don't want this app to fill up ALL storage of the phone.
        // This means we should require slightly more storage space available than strictly required (25 MB).
        long SAFE_MARGIN = 1048576 * 25;

        if ((availableSizeInBytes - SAFE_MARGIN) < requiredFreeBytes) {
            LocalNotifications.showDownloadFailedNotification(getApplicationContext(), false, R.string.download_error_storage, R.string.download_notification_error_storage_full);
            sendBroadcastIntent(DownloadReceiver.TYPE_DOWNLOAD_ERROR, (intent) -> {
                intent.putExtra(DownloadReceiver.PARAM_ERROR_IS_STORAGE_SPACE_ERROR, true);
                return intent;
            });
            return;
        }

        Log.d(TAG, "Downloading " + updateData.getFilename());

        // Download the update
        isDownloading.set(true);
        int downloadId = PRDownloader
                .download(updateData.getDownloadUrl(), Environment.getExternalStoragePublicDirectory(DIRECTORY_ROOT).getAbsolutePath(), updateData.getFilename())
                .setPriority(Priority.HIGH)
                .build()
                .setOnStartOrResumeListener(() -> sendBroadcastIntent(DownloadReceiver.TYPE_STARTED_RESUMED))
                .setOnPauseListener(() -> sendBroadcastIntent(DownloadReceiver.TYPE_PAUSED, (intent -> {
                    isDownloading.set(false);
                    DownloadProgressData downloadProgressData = new DownloadProgressData(0, progressPercentage);
                    LocalNotifications.showDownloadPausedNotification(getApplicationContext(), updateData, downloadProgressData);
                    intent.putExtra(DownloadReceiver.PARAM_PROGRESS, downloadProgressData);
                    return intent;
                })))
                .setOnCancelListener(() -> sendBroadcastIntent(DownloadReceiver.TYPE_CANCELLED))
                .setOnProgressListener(progress -> {
                    if (progress.currentBytes > progress.totalBytes) {
                        Logger.logError(TAG, "Download progress exceeded total file size, server returned incorrect data or app is in an invalid state!");
                        cancelDownload(new SettingsManager(getApplicationContext()).getPreference(PROPERTY_DOWNLOAD_ID, -1));
                        downloadUpdate(updateData);
                        return;
                    }
                    long currentTimestamp = System.currentTimeMillis();

                    // This method gets called *very* often. We only want to update the notification and send a broadcast once per second
                    // Otherwise, the UI & Notification Renderer would be overflowed by our requests
                    if (previousProgressTimeStamp == NOT_SET || currentTimestamp - previousProgressTimeStamp > 1000) {
                        //Log.d(TAG, "Received download progress update: " + progress.currentBytes + " / " + progress.totalBytes);
                        DownloadProgressData progressData = calculateDownloadETA(progress.currentBytes, progress.totalBytes);
                        new SettingsManager(getApplicationContext()).savePreference(PROPERTY_DOWNLOAD_PROGRESS, progressData.getProgress());

                        this.previousProgressTimeStamp = currentTimestamp;
                        this.progressPercentage = progressData.getProgress();

                        sendBroadcastIntent(DownloadReceiver.TYPE_PROGRESS_UPDATE, (i -> i.putExtra(DownloadReceiver.PARAM_PROGRESS, progressData)));
                        LocalNotifications.showDownloadingNotification(getApplicationContext(), updateData, progressData);
                    }
                })
                .start(new OnDownloadListener() {
                    @Override
                    public void onDownloadComplete() {
                        Log.d(TAG, "Downloading of " + updateData.getFilename() + " complete, verification will begin soon...");
                        sendBroadcastIntent(DownloadReceiver.TYPE_DOWNLOAD_COMPLETED);
                        // isVerifying must be set before isDownloading to ensure the condition 'isRunning()' remains true at all times.
                        isVerifying.set(true);
                        isDownloading.set(false);
                        verifyUpdate(updateData);
                    }

                    @Override
                    public void onError(Error error) {
                        // PRDownloader's Error class distinguishes errors as connection errors and server errors.
                        // A connection error is an error with the network connection of the user, such as bad / lost wifi or data connection.
                        // A server error is an error which is deliberately returned by the server, such as a 404 / 500 / 503 status code.

                        // If the error is a connection error, we retry to resume downloading in 5 seconds (if there is a network connection then).
                        if (error.isConnectionError()) {
                            Log.d(TAG, "Pausing download due to connection error");
                            SettingsManager settingsManager = new SettingsManager(getApplicationContext());
                            int downloadId = settingsManager.getPreference(SettingsManager.PROPERTY_DOWNLOAD_ID, -1);
                            int progress = settingsManager.getPreference(SettingsManager.PROPERTY_DOWNLOAD_PROGRESS, -1);
                            DownloadProgressData progressData = new DownloadProgressData(NOT_SET, progress, true);
                            LocalNotifications.showDownloadPausedNotification(getApplicationContext(), updateData, progressData);

                            Handler handler = new Handler();

                            if (autoResumeOnConnectionErrorRunnable == null) {
                                autoResumeOnConnectionErrorRunnable = new Runnable() {
                                    @Override
                                    public void run() {
                                        if (Utils.checkNetworkConnection(getApplicationContext())) {
                                            Log.d(TAG, "Network connectivity restored, resuming download...");
                                            resumeDownload(downloadId, updateData);
                                        } else {
                                            handler.postDelayed(this, 5000);
                                        }
                                    }
                                };

                                handler.postDelayed(autoResumeOnConnectionErrorRunnable,5000);
                            }

                            sendBroadcastIntent(DownloadReceiver.TYPE_PROGRESS_UPDATE, (i -> i.putExtra(DownloadReceiver.PARAM_PROGRESS, progressData)));
                        } else if (error.isServerError()) {
                            // Otherwise, we inform the user that the server has refused the download & that it must be restarted at a later stage.
                            LocalNotifications.showDownloadFailedNotification(getApplicationContext(), false, R.string.download_error_server, R.string.download_notification_error_server);

                            sendBroadcastIntent(DownloadReceiver.TYPE_DOWNLOAD_ERROR, (intent -> {
                                intent.putExtra(DownloadReceiver.PARAM_ERROR_IS_SERVER_ERROR, true);
                                return intent;
                            }));

                            clearUp();
                        } else {
                            // If not server and connection error, something has gone wrong internally. This should never happen!
                            LocalNotifications.showDownloadFailedNotification(getApplicationContext(), true, R.string.download_error_internal, R.string.download_notification_error_internal);

                            sendBroadcastIntent(DownloadReceiver.TYPE_DOWNLOAD_ERROR, (intent -> {
                                intent.putExtra(DownloadReceiver.PARAM_ERROR_IS_INTERNAL_ERROR, true);
                                return intent;
                            }));

                            clearUp();
                        }
                    }
                });

        // We persist the download ID so we can access it from anywhere and even after an app restart.
        SettingsManager settingsManager = new SettingsManager(getApplicationContext());
        settingsManager.savePreference(SettingsManager.PROPERTY_DOWNLOAD_ID, downloadId);
    }

    private synchronized void pauseDownload(int downloadId) {
        Log.d(TAG, "Pausing download #" + downloadId);

        if (downloadId != -1) {
            isDownloading.set(false);
            Log.d(TAG, "Pausing using PRDownloader.pause()");
            PRDownloader.pause(downloadId);
        } else {
            Logger.logWarning(TAG, "Not pausing download, invalid download ID provided.");
        }
    }

    private synchronized void resumeDownload(int downloadId, UpdateData updateData) {
        Log.d(TAG, "Resuming download #" + downloadId);

        if (downloadId != -1) {
            // If the download is still in the PRDownloader request queue, resume it.
            // If not, start the download again (PRdownloader will still resume it using its own SQLite database)
            if (DownloadRequestQueue.getInstance().getStatus(downloadId) != Status.UNKNOWN) {
                Log.d(TAG, "Resuming using PRDownloader.resume()");
                isDownloading.set(true);
                PRDownloader.resume(downloadId);
            } else {
                Log.d(TAG, "Resuming using PRDownloader.download()");
                downloadUpdate(updateData);
            }

        } else {
            Logger.logWarning(TAG, "Not resuming download, invalid download ID provided.");
        }
    }

    private synchronized void cancelDownload(int downloadId) {
        Log.d(TAG, "Cancelling download #" + downloadId);

        if (downloadId != -1) {
            isDownloading.set(false);
            PRDownloader.cancel(downloadId);
            LocalNotifications.hideDownloadingNotification(getApplicationContext());
            clearUp();
            Log.d(TAG, "Cancelled download #" + downloadId);
        } else {
            Logger.logWarning(TAG, "Not cancelling download, no valid ID was provided...");
        }
    }

    private synchronized void deleteDownloadedFile(UpdateData updateData) {
        Log.d(TAG, "Deleting downloaded update file " + updateData.getFilename());

        File downloadedFile = new File(Environment.getExternalStoragePublicDirectory(DIRECTORY_ROOT).getAbsolutePath(), updateData.getFilename());
        if (!downloadedFile.delete()) {
            Logger.logWarning(TAG, "Could not delete downloaded file " + updateData.getFilename());
        }
    }

    private synchronized void checkDownloadStatus(int downloadId, UpdateData updateData) {
        // If the downloader id got lost then it is either complete or never started.
        // This will be handled below in the UNKNOWN branch.
        Logger.logDebug(TAG, "Checking status for download #" + downloadId + " and updateData " + updateData.getVersionNumber());
        Status prDownloaderStatus = downloadId == -1 ? Status.UNKNOWN : PRDownloader.getStatus(downloadId);
        DownloadStatus downloadStatus = DownloadStatus.NOT_DOWNLOADING;
        DownloadProgressData progress = null;
        boolean hasNetwork = Utils.checkNetworkConnection(getApplicationContext());

        switch (prDownloaderStatus) {
            case QUEUED:
                Logger.logDebug(TAG, "Download #" + downloadId + " is queued");
                // If queued, there is no progress and the download will start soon.
                downloadStatus = DownloadStatus.DOWNLOAD_QUEUED;
                progress = new DownloadProgressData(NOT_SET, 0);
                break;
            case RUNNING:
                // If running, return the previously-stored progress percentage.
                int storedProgressPercentage = new SettingsManager(getApplicationContext()).getPreference(PROPERTY_DOWNLOAD_PROGRESS, 0);
                downloadStatus = DownloadStatus.DOWNLOADING;
                progress = new DownloadProgressData(NOT_SET, storedProgressPercentage);
                Logger.logDebug(TAG, "Download #" + downloadId + " is running @" + storedProgressPercentage);
                break;
            case UNKNOWN:
                // If unknown, we have to check if it is unknown in our settings and handle according to that.
                storedProgressPercentage = new SettingsManager(getApplicationContext()).getPreference(PROPERTY_DOWNLOAD_PROGRESS, 0);
                progress = new DownloadProgressData(NOT_SET,  storedProgressPercentage, !hasNetwork);

                if (storedProgressPercentage == 0) {
                    // If unknown by PR downloader and unknown in our settings, manually check if the file exists
                    // If the file exists, assume we've passed the download and verification.
                    // If the file does not exist, the update has likely never been downloaded before.
                    downloadStatus = checkDownloadCompletionByFile(updateData) ? DownloadStatus.DOWNLOAD_COMPLETED : DownloadStatus.NOT_DOWNLOADING;
                    Logger.logDebug(TAG, "Download #" + downloadId + " is " + (downloadStatus == DownloadStatus.DOWNLOAD_COMPLETED ? "completed" : "not started"));
                    break;
                }

                // If unknown by PR downloader but known by our settings, the download is paused but lost from the PR downloader queu.
                // In this case we can resume it using PRDownloader.download (instead of PRDOwnloader.resume).
                Logger.logDebug(TAG, "Download #" + downloadId + " is paused (hard) @" + storedProgressPercentage);
                downloadStatus = DownloadStatus.DOWNLOAD_PAUSED;
                break;
            case PAUSED:
                // If the download is paused, we return its current percentage and allow resuming of it.
                storedProgressPercentage = new SettingsManager(getApplicationContext()).getPreference(PROPERTY_DOWNLOAD_PROGRESS, 0);
                downloadStatus = DownloadStatus.DOWNLOAD_PAUSED;
                progress = new DownloadProgressData(NOT_SET, storedProgressPercentage, !hasNetwork);
                Logger.logDebug(TAG, "Download #" + downloadId + " is paused (soft) @" + storedProgressPercentage);
                break;

        }

        // If the download is completed, we have to check if the file is still being verified.
        // If so, return that verification is ongoing.
        // Otherwise, look at the file if it still exists and allow installing (exists) or redownloading (deleted by user).
        if (prDownloaderStatus == Status.COMPLETED) {
            if (isVerifying.get()) {
                downloadStatus = DownloadStatus.VERIFYING;
                Logger.logDebug(TAG, "Download #" + downloadId + " is verifying");
            } else {
                downloadStatus = checkDownloadCompletionByFile(updateData) ? DownloadStatus.DOWNLOAD_COMPLETED : DownloadStatus.NOT_DOWNLOADING;
                Logger.logDebug(TAG, "Download #" + downloadId + " is " + (downloadStatus == DownloadStatus.DOWNLOAD_COMPLETED ? "completed" : "not started"));
            }
        }

        // Variable used in lambda expression should be final or effectively final...
        final DownloadStatus result = downloadStatus;
        final DownloadProgressData progressResult = progress;

        sendBroadcastIntent(DownloadReceiver.TYPE_STATUS_REQUEST, (intent -> {
            intent.putExtra(DownloadReceiver.PARAM_STATUS, result);
            intent.putExtra(DownloadReceiver.PARAM_PROGRESS, progressResult);
            return intent;
        }));
    }

    @SuppressLint("StaticFieldLeak")
    private void verifyUpdate(final UpdateData updateData) {
        isVerifying.set(true);
        verifier = new AsyncTask<UpdateData, Void, Boolean>() {
            @Override
            protected void onPreExecute() {
                Log.d(TAG, "Preparing to verify downloaded update file " + updateData.getFilename());
                isVerifying.set(true);

                LocalNotifications.showVerifyingNotification(getApplicationContext(), ONGOING, HAS_NO_ERROR);
                sendBroadcastIntent(DownloadReceiver.TYPE_VERIFY_STARTED);
            }

            @Override
            protected Boolean doInBackground(UpdateData... updateDatas) {
                Log.d(TAG, "Verifying " + updateData.getFilename());
                File downloadedFile = new File(Environment.getExternalStoragePublicDirectory(DIRECTORY_ROOT).getAbsolutePath(), updateData.getFilename());

                return updateData.getMD5Sum() == null || MD5.checkMD5(updateData.getMD5Sum(), downloadedFile);
            }

            @Override
            protected void onPostExecute(Boolean valid) {
                Log.d(TAG, "Verification result for " + updateData.getFilename() + ": " + valid);

                if (valid) {
                    LocalNotifications.hideVerifyingNotification(getApplicationContext());
                    LocalNotifications.showDownloadCompleteNotification(getApplicationContext(), updateData);
                    sendBroadcastIntent(DownloadReceiver.TYPE_VERIFY_COMPLETE);
                } else {
                    deleteDownloadedFile(updateData);
                    LocalNotifications.showVerifyingNotification(getApplicationContext(), NOT_ONGOING, HAS_ERROR);
                    sendBroadcastIntent(DownloadReceiver.TYPE_VERIFY_FAILED);
                }

                clearUp();
            }

            @Override
            protected void onCancelled() {
                if (updateData != null) {
                    Log.d(TAG, "Cancelled verification of " + updateData.getFilename());
                }
            }
        }.execute();
    }

    private DownloadProgressData calculateDownloadETA(long bytesDownloadedSoFar, long totalSizeBytes) {
        double bytesDownloadedInSecond;
        boolean validMeasurement = false;

        long numberOfSecondsRemaining = NOT_SET;
        long averageBytesPerSecond = NOT_SET;
        long currentTimeStamp = System.currentTimeMillis();
        long bytesRemainingToDownload = totalSizeBytes - bytesDownloadedSoFar;

        if (previousBytesDownloadedSoFar != NOT_SET) {

            double numberOfElapsedSeconds = TimeUnit.MILLISECONDS.toSeconds(currentTimeStamp - previousTimeStamp);

            if(numberOfElapsedSeconds > 0.0) {
                bytesDownloadedInSecond = (bytesDownloadedSoFar - previousBytesDownloadedSoFar) / (numberOfElapsedSeconds);
            } else {
                bytesDownloadedInSecond = 0;
            }

            // Sometimes no new progress data is available.
            // If no new data is available, return the previously stored data to keep the UI showing that.
            validMeasurement = bytesDownloadedInSecond > 0 || numberOfElapsedSeconds > 5;

            if (validMeasurement) {
                // In case of no network, clear all measurements to allow displaying the now-unknown ETA...
                if (bytesDownloadedInSecond == 0) {
                    measurements.clear();
                }

                // Remove old measurements to keep the average calculation based on 5 measurements
                if (measurements.size() > 10) {
                    measurements.subList(0, 1).clear();
                }

                measurements.add(bytesDownloadedInSecond);
            }

            // Calculate number of seconds remaining based off average download spead.
            averageBytesPerSecond = (long) calculateAverageBytesDownloadedInSecond(measurements);
            if (averageBytesPerSecond > 0) {
                numberOfSecondsRemaining = bytesRemainingToDownload / averageBytesPerSecond;
            } else {
                numberOfSecondsRemaining = NOT_SET;
            }
        }

        if (averageBytesPerSecond != NOT_SET) {
            if (validMeasurement) {
                previousNumberOfSecondsRemaining = numberOfSecondsRemaining;
                previousTimeStamp = currentTimeStamp;
            } else {
                numberOfSecondsRemaining = previousNumberOfSecondsRemaining;
            }
        }

        previousBytesDownloadedSoFar = bytesDownloadedSoFar;

        int progress = 0;

        if (totalSizeBytes > 0.0) {
            progress = (int) ((bytesDownloadedSoFar * 100) / totalSizeBytes);
        }

        return new DownloadProgressData(numberOfSecondsRemaining, progress);
    }

    private double calculateAverageBytesDownloadedInSecond(List<Double> measurements) {
        if (measurements == null || measurements.isEmpty()) {
            return 0;
        } else {
            double totalBytesDownloadedInSecond = 0;

            for (Double measurementData : measurements) {
                totalBytesDownloadedInSecond += measurementData;
            }

            return totalBytesDownloadedInSecond / measurements.size();
        }
    }

    // Clears all static / instance variables. If not called when download stops,
    // leftovers of a previous download *will* cause issues when using this service for the next time
    private void clearUp() {
        measurements.clear();
        previousProgressTimeStamp = NOT_SET;
        progressPercentage = 0;
        previousTimeStamp = NOT_SET;
        previousBytesDownloadedSoFar = NOT_SET;
        previousNumberOfSecondsRemaining = NOT_SET;
        verifier = null;
        isDownloading.set(false);
        isVerifying.set(false);
        autoResumeOnConnectionErrorRunnable = null;
        SettingsManager settingsManager = new SettingsManager(getApplicationContext());
        settingsManager.deletePreference(PROPERTY_DOWNLOAD_PROGRESS);
        settingsManager.deletePreference(PROPERTY_DOWNLOAD_ID);
    }

    private void sendBroadcastIntent(String intentType) {
        sendBroadcastIntent(intentType, i -> i);
    }

    private void sendBroadcastIntent(String intentType, Function<Intent, Intent> intentParamsCustomizer) {
        Intent broadcastIntent = new Intent();

        broadcastIntent.setAction(DownloadReceiver.ACTION_DOWNLOAD_EVENT);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent.putExtra(DownloadReceiver.PARAM_TYPE, intentType);

        broadcastIntent = intentParamsCustomizer.apply(broadcastIntent);

        sendBroadcast(broadcastIntent);
    }

    private static boolean isRunning() {
        return isDownloading.get() || isVerifying.get();
    }

    private boolean checkDownloadCompletionByFile(UpdateData updateData) {
        if (updateData == null || updateData.getFilename() == null) {
            Logger.logWarning(TAG, "Cannot check for download completion by file - null update data or filename provided!");
            return false;
        }
        return new File(Environment.getExternalStoragePublicDirectory(DIRECTORY_ROOT), updateData.getFilename()).exists();
    }
}
