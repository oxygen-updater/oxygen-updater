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
import static com.arjanvlek.oxygenupdater.download.DownloadHelper.DIRECTORY_ROOT;
import static com.arjanvlek.oxygenupdater.notifications.LocalNotifications.HAS_ERROR;
import static com.arjanvlek.oxygenupdater.notifications.LocalNotifications.HAS_NO_ERROR;
import static com.arjanvlek.oxygenupdater.notifications.LocalNotifications.NOT_ONGOING;
import static com.arjanvlek.oxygenupdater.notifications.LocalNotifications.ONGOING;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_DOWNLOADER_STATE;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_DOWNLOAD_ID;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_DOWNLOAD_PROGRESS;

/**
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

    public static final int NOT_SET = -1;

    // We need to have class-level status fields, because we need to be able to check this in a blocking / synchronous way.
    public static final AtomicBoolean isDownloading = new AtomicBoolean(false);
    public static final AtomicBoolean isVerifying = new AtomicBoolean(false);
    public static final AtomicBoolean isOperationPending = new AtomicBoolean(false);

    private UpdateData updateData;
    private AsyncTask verifier;

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
        Log.d(TAG, "onDestroy() was called, saving state & pausing current activity");

        SettingsManager settingsManager = new SettingsManager(getApplicationContext());

        if (isRunning()) {
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
                checkDownloadStatus(downloadId);
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

    private void downloadUpdate(UpdateData updateData) {
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

        // Download the update
        int downloadId = PRDownloader
                .download(updateData.getDownloadUrl(), Environment.getExternalStoragePublicDirectory(DIRECTORY_ROOT).getAbsolutePath(), updateData.getFilename())
                .setPriority(Priority.HIGH)
                .build()
                .setOnStartOrResumeListener(() -> sendBroadcastIntent(DownloadReceiver.TYPE_STARTED_RESUMED))
                .setOnPauseListener(() -> sendBroadcastIntent(DownloadReceiver.TYPE_PAUSED, (intent -> {
                    DownloadProgressData downloadProgressData = new DownloadProgressData(0, progressPercentage);
                    LocalNotifications.showDownloadPausedNotification(getApplicationContext(), updateData, downloadProgressData);
                    intent.putExtra(DownloadReceiver.PARAM_PROGRESS, downloadProgressData);
                    return intent;
                })))
                .setOnCancelListener(() -> sendBroadcastIntent(DownloadReceiver.TYPE_CANCELLED))
                .setOnProgressListener(progress -> {
                    long currentTimestamp = System.currentTimeMillis();

                    // This method gets called *very* often. We only want to send a broadcast twice per second and update the notification every 2 seconds
                    // Otherwise, the UI & Notification Renderer would be overflowed by our requests
                    if (previousProgressTimeStamp == NOT_SET || currentTimestamp - previousProgressTimeStamp > 1000) {
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
                        sendBroadcastIntent(DownloadReceiver.TYPE_DOWNLOAD_COMPLETED);
                        isDownloading.set(false);
                        verifyUpdate(updateData);
                    }

                    @Override
                    public void onError(Error error) {
                        // If the error is a connection error, we retry to resume downloading in 5 seconds (if there is a network connection then).
                        if (error.isConnectionError()) {
                            SettingsManager settingsManager = new SettingsManager(getApplicationContext());
                            int downloadId = settingsManager.getPreference(SettingsManager.PROPERTY_DOWNLOAD_ID, -1);
                            int progress = settingsManager.getPreference(SettingsManager.PROPERTY_DOWNLOAD_PROGRESS, -1);
                            DownloadProgressData progressData = new DownloadProgressData(NOT_SET, progress, true);
                            LocalNotifications.showDownloadPausedNotification(getApplicationContext(), updateData, progressData);

                            new Handler().postDelayed(() -> {
                              if (Utils.checkNetworkConnection(getApplicationContext())) {
                                  resumeDownload(downloadId, updateData);
                              } else {
                                onError(error);
                              }
                            }, 5000);

                            sendBroadcastIntent(DownloadReceiver.TYPE_PROGRESS_UPDATE, (i -> i.putExtra(DownloadReceiver.PARAM_PROGRESS, progressData)));
                        } else if (error.isServerError()) {
                            // Otherwise, we inform the user that the server has refused the download & that it must be restarted at a later stage.
                            LocalNotifications.showDownloadFailedNotification(getApplicationContext(), true, R.string.download_error_server, R.string.download_notification_error_server);

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

        isDownloading.set(true);

        // We persist the download ID so we can access it from anywhere and even after an app restart.
        SettingsManager settingsManager = new SettingsManager(getApplicationContext());
        settingsManager.savePreference(SettingsManager.PROPERTY_DOWNLOAD_ID, downloadId);
    }

    private void pauseDownload(int downloadId) {
        if (downloadId != -1) {
            isDownloading.set(false);
            Log.d(TAG, "Pausing using PRDownloader.pause()");
            PRDownloader.pause(downloadId);
        }
    }

    private void resumeDownload(int downloadId, UpdateData updateData) {
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

        }
    }

    private void cancelDownload(int downloadId) {
        if (downloadId != -1) {
            PRDownloader.cancel(downloadId);
            LocalNotifications.hideDownloadingNotification(getApplicationContext());
            clearUp();
        }
    }

    private void deleteDownloadedFile(UpdateData updateData) {
        File downloadedFile = new File(Environment.getExternalStoragePublicDirectory(DIRECTORY_ROOT).getAbsolutePath(), updateData.getFilename());
        if (!downloadedFile.delete()) {
            Logger.logWarning(TAG, "Could not delete downloaded file " + updateData.getFilename());
        }
    }

    private void checkDownloadStatus(int downloadId) {
        Status prDownloaderStatus = downloadId == -1 ? Status.UNKNOWN : PRDownloader.getStatus(downloadId);
        DownloadStatus downloadStatus = DownloadStatus.NOT_DOWNLOADING;
        DownloadProgressData progress = null;
        boolean hasNetwork = Utils.checkNetworkConnection(getApplicationContext());

        switch (prDownloaderStatus) {
            case QUEUED:
                downloadStatus = DownloadStatus.DOWNLOAD_QUEUED;
                progress = new DownloadProgressData(NOT_SET, 0);
                break;
            case UNKNOWN:
                int storedProgressPercentage = new SettingsManager(getApplicationContext()).getPreference(PROPERTY_DOWNLOAD_PROGRESS, 0);

                if (storedProgressPercentage == 0) {
                    break;
                }

                downloadStatus = DownloadStatus.DOWNLOAD_PAUSED;
                progress = new DownloadProgressData(NOT_SET,  storedProgressPercentage, !hasNetwork);
                break;
            case PAUSED:
                downloadStatus = DownloadStatus.DOWNLOAD_PAUSED;
                progress = new DownloadProgressData(NOT_SET, this.progressPercentage, !hasNetwork);
                break;

        }

        if (prDownloaderStatus == Status.COMPLETED) {
            if (isVerifying.get()) {
                downloadStatus = DownloadStatus.VERIFYING;
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
        verifier = new AsyncTask<UpdateData, Void, Boolean>() {
            @Override
            protected void onPreExecute() {
                isVerifying.set(true);

                LocalNotifications.showVerifyingNotification(getApplicationContext(), ONGOING, HAS_NO_ERROR);
                sendBroadcastIntent(DownloadReceiver.TYPE_VERIFY_STARTED);
            }

            @Override
            protected Boolean doInBackground(UpdateData... updateDatas) {
                File downloadedFile = new File(Environment.getExternalStoragePublicDirectory(DIRECTORY_ROOT).getAbsolutePath(), updateData.getFilename());

                return updateData.getMD5Sum() == null || MD5.checkMD5(updateData.getMD5Sum(), downloadedFile);
            }

            @Override
            protected void onPostExecute(Boolean valid) {
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
        }.execute();
    }

    private DownloadProgressData calculateDownloadETA(long bytesDownloadedSoFar, long totalSizeBytes) {
        double bytesDownloadedInSecond;
        boolean validMeasurement = false;

        long numberOfSecondsRemaining = NOT_SET;
        long averageBytesPerSecond = NOT_SET;
        long currentTimeStamp = System.currentTimeMillis();
        long bytesRemainingToDownload = totalSizeBytes - bytesDownloadedSoFar;

        if(previousBytesDownloadedSoFar != NOT_SET) {

            double numberOfElapsedSeconds = TimeUnit.MILLISECONDS.toSeconds(currentTimeStamp - previousTimeStamp);

            if(numberOfElapsedSeconds > 0.0) {
                bytesDownloadedInSecond = (bytesDownloadedSoFar - previousBytesDownloadedSoFar) / (numberOfElapsedSeconds);
            } else {
                bytesDownloadedInSecond = 0;
            }

            // DownloadManager.query doesn't always have new data. If no new data is available, return the previously stored data to keep the UI showing that.
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

        if(averageBytesPerSecond != NOT_SET) {

            if(validMeasurement) {
                previousNumberOfSecondsRemaining = numberOfSecondsRemaining;
                previousTimeStamp = currentTimeStamp;
            } else {
                numberOfSecondsRemaining = previousNumberOfSecondsRemaining;
            }
        }

        previousBytesDownloadedSoFar = bytesDownloadedSoFar;

        int progress = 0;

        if(totalSizeBytes > 0.0) {
            progress = (int) ((bytesDownloadedSoFar * 100) / totalSizeBytes);
        }

        return new DownloadProgressData(numberOfSecondsRemaining, progress);
    }

    private double calculateAverageBytesDownloadedInSecond(List<Double> measurements) {
        if(measurements == null || measurements.isEmpty()) {
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
}
