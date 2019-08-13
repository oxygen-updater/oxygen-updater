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
import android.util.Pair;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.arjanvlek.oxygenupdater.R;
import com.arjanvlek.oxygenupdater.internal.OxygenUpdaterException;
import com.arjanvlek.oxygenupdater.internal.Utils;
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

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import java8.util.function.Function;
import java8.util.stream.Collectors;
import java8.util.stream.StreamSupport;

import static com.arjanvlek.oxygenupdater.ApplicationData.APP_USER_AGENT;
import static com.arjanvlek.oxygenupdater.internal.logger.Logger.logDebug;
import static com.arjanvlek.oxygenupdater.internal.logger.Logger.logError;
import static com.arjanvlek.oxygenupdater.internal.logger.Logger.logInfo;
import static com.arjanvlek.oxygenupdater.internal.logger.Logger.logWarning;
import static com.arjanvlek.oxygenupdater.notifications.LocalNotifications.HAS_ERROR;
import static com.arjanvlek.oxygenupdater.notifications.LocalNotifications.HAS_NO_ERROR;
import static com.arjanvlek.oxygenupdater.notifications.LocalNotifications.NOT_ONGOING;
import static com.arjanvlek.oxygenupdater.notifications.LocalNotifications.ONGOING;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_DOWNLOADER_STATE;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_DOWNLOADER_STATE_HISTORY;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_DOWNLOAD_ID;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_DOWNLOAD_PROGRESS;

/**
 * DownloadService handles the downloading and MD5 verification of OxygenOS updates.
 * It does so by using the PRDownloader library, which offers Pause and Resume support as well as automatic recovery after network loss.
 * The service also sends status updates to {@link DownloadReceiver}, which can pass the status of the download to the UI.
 * <p>
 * This is a *very* complex service, because it is responsible for all state transitions that take
 * place when downloading or verifying an update. See {@link DownloadStatus} for all possible states.
 * <p>
 * To make clear how this service can be used, see the following state table.
 * Its rows contain all possible transitions (initiated by actions() or events) given a state S and an updateData UD
 * Its columns describe the state S' the service is in (for this UD) *after* having performed the transition.
 * <p>
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
 * <p>
 * Background running of this service
 * When the app gets killed, the service gets killed as well, but when this happens and it was still running
 * (that is: in the state DOWNLOADING or VERIFYING), it will get auto-restarted via an ACTION_SERVICE_RESTART intent (through {@link DownloadServiceRestarter})
 * A restart then calls resumeDownload() when the state was DOWNLOADING or verifyUpdate() when the state was VERIFYING.
 * The state is saved in SettingsManager.PROPERTY_DOWNLOADER_STATE.
 * <p>
 * Thread loop (in onHandleIntent())
 * To prevent the service from continuously restarting when downloading / verifying (as those have own threads, so the main thread is completed very quickly),
 * the service executes a while-true loop in which it listens every 50ms if a new command has been ordered or if the download has finished.
 * If a new command has been ordered, it exits the current loop (causing a restart), executes the new Intent (because Intents are sequential) and then executes the restart intent
 * The after-executed restart intent *should* do nothing, because the action usually cancelled or paused the download.
 * <p>
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
	public static final String DIRECTORY_ROOT = "";
	public static final int NOT_SET = -1;
	public static final int NO_PROGRESS = 0;
	public static final AtomicBoolean isOperationPending = new AtomicBoolean(false);
	private static final String NOTIFICATION_CHANNEL_ID = "com.arjanvlek.oxygenupdater.download.DownloadService";
	/**
	 * Amount of milliseconds after which the connection gets marked as timed-out
	 */
	private static final int CONNECT_TIMEOUT = 30_000;
	/**
	 * Amount of milliseconds after which the download gets marked as timed-out *after* an initial
	 * connection has been established
	 */
	private static final int READ_TIMEOUT = 120_000;
	/**
	 * How often to check for new tasks or if we are finished (interrupt rate, determines how snappy
	 * the service responds to the UI)
	 */
	private static final int INTERRUPT_RATE = 50;
	/**
	 * Amount of free storage space to reserve when downloading an update (currently: 25 MB)
	 */
	private static final long SAFE_MARGIN = 1048576 * 25;
	/**
	 * How often to re-check for a network connection if no connection is currently available (in
	 * milliseconds)
	 */
	private static final int NO_CONNECTION_REFRESH_RATE = 5000;
	/**
	 * Date / time pattern for history of performed operations
	 */
	private static final String HISTORY_DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS";
	private static final List<Pair<LocalDateTime, String>> executedStateTransitions = new LinkedList<>();
	// We need to have class-level status fields, because we need to be able to check this in a blocking / synchronous way.
	private static DownloadStatus state = DownloadStatus.NOT_DOWNLOADING;
	private static Runnable autoResumeOnConnectionErrorRunnable;
	// Progress calculation data
	private final List<Double> measurements = new ArrayList<>();
	private UpdateData updateData;
	private AsyncTask verifier;
	private long previousProgressTimeStamp = NOT_SET;
	private int progressPercentage = NO_PROGRESS;
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
	 * Performs an operation on this IntentService. The action must be one of the actions as
	 * declared in this class.
	 *
	 * @param activity   Calling activity
	 * @param action     Action to perform
	 * @param updateData Update data on which the action must be performed
	 */
	public static void performOperation(Activity activity, String action, UpdateData updateData) {
		if (activity == null) {
			return;
		}

		// We want to make sure the new operation can always be processed, even if the service is kept running in a thread loop.
		isOperationPending.set(true);

		SettingsManager settingsManager = new SettingsManager(activity);
		int downloadId = settingsManager.getPreference(PROPERTY_DOWNLOAD_ID, NOT_SET);

		Intent downloadIntent = new Intent(activity, DownloadService.class);
		downloadIntent.putExtra(DownloadService.PARAM_ACTION, action);
		downloadIntent.putExtra(DownloadService.PARAM_UPDATE_DATA, updateData);
		downloadIntent.putExtra(DownloadService.PARAM_DOWNLOAD_ID, downloadId);

		try {
			activity.startService(downloadIntent);
		} catch (Exception e) {
			logError(TAG, withAppendedStateHistory("Failed to start DownloadService"), e);
			isOperationPending.set(false);
		}
	}

	public static boolean isRunning() {
		return state == DownloadStatus.DOWNLOADING
				|| state == DownloadStatus.DOWNLOAD_QUEUED
				|| state == DownloadStatus.VERIFYING
				|| state == DownloadStatus.DOWNLOAD_PAUSED_WAITING_FOR_CONNECTION;
	}

	private static String withAppendedStateHistory(String text) {
		return String.format(
				"%s\n\n%s\n%s",
				text,
				"History of actions performed by the downloader:",
				StreamSupport.stream(executedStateTransitions)
						.filter(est -> est.first != null && est.second != null)
						.map(est -> est.first.toString(HISTORY_DATETIME_PATTERN) + ": " + est.second)
						.collect(Collectors.joining("\n"))
		);
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
		} else {
			startForeground(1, new Notification());
		}
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
		settingsManager.savePreference(PROPERTY_DOWNLOADER_STATE, state.toString());

		if (isRunning()) {
			logDebug(TAG, "onDestroy() was called whilst running, saving state & pausing current activity");
			serializeExecutedStateTransitions();

			// We briefly pause the download - it can be (and will be) resumed in the newly spawned service.
			if (state == DownloadStatus.DOWNLOAD_QUEUED || state == DownloadStatus.DOWNLOADING) {
				logDebug(TAG, "Temporarily pausing download using PRDownloader.pause()");
				PRDownloader.pause(settingsManager.getPreference(PROPERTY_DOWNLOAD_ID, NOT_SET));
			}

			// If onDestroy() happens when checking the MD5, we'll have to start checking it over again (this process is not resumable)
			if (state == DownloadStatus.VERIFYING && verifier != null) {
				logDebug(TAG, "Aborting in-progress MD5 verification");
				verifier.cancel(true);
			}

			if (state == DownloadStatus.DOWNLOAD_PAUSED_WAITING_FOR_CONNECTION && autoResumeOnConnectionErrorRunnable != null) {
				logDebug(TAG, "Unregistering network connectivity listener");
				new Handler().removeCallbacks(autoResumeOnConnectionErrorRunnable);
				autoResumeOnConnectionErrorRunnable = null;
			}

			state = DownloadStatus.NOT_DOWNLOADING; // No transition, would override stored state in SharedPreferences!

			Intent broadcastIntent = new Intent();
			broadcastIntent.setAction(INTENT_SERVICE_RESTART);
			broadcastIntent.putExtra(DownloadService.PARAM_UPDATE_DATA, updateData);
			broadcastIntent.putExtra(DownloadService.PARAM_DOWNLOAD_ID, settingsManager.getPreference(PROPERTY_DOWNLOAD_ID, NOT_SET));
			broadcastIntent.setClass(this, DownloadServiceRestarter.class);
			sendBroadcast(broadcastIntent);
		}
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (intent == null) {
			return;
		}

		isOperationPending.set(false);

		PRDownloaderConfig config = PRDownloaderConfig.newBuilder()
				.setDatabaseEnabled(true)
				.setUserAgent(APP_USER_AGENT)
				.setConnectTimeout(CONNECT_TIMEOUT)
				.setReadTimeout(READ_TIMEOUT)
				.build();

		PRDownloader.initialize(getApplicationContext(), config);

		String action = intent.getStringExtra(PARAM_ACTION);
		int downloadId = intent.getIntExtra(PARAM_DOWNLOAD_ID, NOT_SET);
		updateData = intent.getParcelableExtra(PARAM_UPDATE_DATA);

		// Restore saved state
		SettingsManager settingsManager = new SettingsManager(getApplicationContext());
		state = DownloadStatus.valueOf(settingsManager.getPreference(PROPERTY_DOWNLOADER_STATE, DownloadStatus.NOT_DOWNLOADING.toString()));

		switch (action) {
			case ACTION_SERVICE_RESTART:
				// If restarting the service, we'll have to pick up the work we were doing.
				// This is done by reading the restored state of the service. Only the three running-states require an action.
				executedStateTransitions.clear();
				executedStateTransitions.addAll(deserializeExecutedStateTransitions());

				if (state == DownloadStatus.DOWNLOADING || state == DownloadStatus.DOWNLOAD_QUEUED) {
					logDebug(TAG, "Resuming temporarily-paused download");
					state = DownloadStatus.DOWNLOAD_QUEUED; // We are queued after onDestroy() was called. Not Downloading!
					resumeDownload(downloadId, updateData);
					break;
				} else if (state == DownloadStatus.DOWNLOAD_PAUSED_WAITING_FOR_CONNECTION) {
					logDebug(TAG, "Resumed re-checking for network connection");
					resumeDownloadOnReconnectingToNetwork(downloadId, updateData);
					break;
				} else if (state == DownloadStatus.VERIFYING) {
					logDebug(TAG, "Restarted aborted MD5 verification");
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
				Thread.sleep(INTERRUPT_RATE);
				if (isOperationPending.get() || !isRunning()) {
					break;
				}
			} catch (InterruptedException ignored) {

			}
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
				.setSmallIcon(R.drawable.settings)
				.setContentTitle(getString(R.string.download_in_background))
				.setPriority(NotificationManager.IMPORTANCE_NONE)
				.setCategory(Notification.CATEGORY_SERVICE)
				.build();

		startForeground(2, notification);
	}

	private synchronized void downloadUpdate(UpdateData updateData) {
		if (!isStateTransitionAllowed(DownloadStatus.DOWNLOAD_QUEUED)) {
			logWarning(TAG, new UpdateDownloadException("Not downloading update, is a download operation already in progress?"));
			return;
		}

		// Check if the update is downloadable
		Context context = getApplicationContext();
		if (updateData == null || updateData.getDownloadUrl() == null) {
			LocalNotifications.showDownloadFailedNotification(context, false, R.string.download_error_internal, R.string.download_notification_error_internal);
			sendBroadcastIntent(DownloadReceiver.TYPE_DOWNLOAD_ERROR, intent -> {
				intent.putExtra(DownloadReceiver.PARAM_ERROR_IS_INTERNAL_ERROR, true);
				logError(TAG, new UpdateDownloadException(withAppendedStateHistory("Update data is null or has no Download URL")));
				return intent;
			});
			return;
		}

		if (!updateData.getDownloadUrl().contains("http")) {
			LocalNotifications.showDownloadFailedNotification(context, false, R.string.download_error_internal, R.string.download_notification_error_internal);
			sendBroadcastIntent(DownloadReceiver.TYPE_DOWNLOAD_ERROR, intent -> {
				intent.putExtra(DownloadReceiver.PARAM_ERROR_IS_INTERNAL_ERROR, true);
				logError(TAG, new UpdateDownloadException("Update data has invalid Download URL (" + updateData.getDownloadUrl() + ")"));
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
		if ((availableSizeInBytes - SAFE_MARGIN) < requiredFreeBytes) {
			LocalNotifications.showDownloadFailedNotification(
					context,
					false,
					R.string.download_error_storage,
					R.string.download_notification_error_storage_full
			);
			sendBroadcastIntent(DownloadReceiver.TYPE_DOWNLOAD_ERROR, intent -> {
				intent.putExtra(DownloadReceiver.PARAM_ERROR_IS_STORAGE_SPACE_ERROR, true);
				return intent;
			});
			return;
		}

		logDebug(TAG, "Downloading " + updateData.getFilename());

		// Download the update
		performStateTransition(DownloadStatus.DOWNLOAD_QUEUED);
		int downloadId = PRDownloader
				.download(updateData.getDownloadUrl(), Environment.getExternalStoragePublicDirectory(DIRECTORY_ROOT).getAbsolutePath(), updateData.getFilename())
				.setPriority(Priority.HIGH)
				.build()
				.setOnStartOrResumeListener(() -> {
					performStateTransition(DownloadStatus.DOWNLOADING);
					sendBroadcastIntent(DownloadReceiver.TYPE_STARTED_RESUMED);
				})
				.setOnPauseListener(() -> {
					performStateTransition(DownloadStatus.DOWNLOAD_PAUSED);
					sendBroadcastIntent(DownloadReceiver.TYPE_PAUSED, (intent -> {
						DownloadProgressData downloadProgressData = new DownloadProgressData(NOT_SET, progressPercentage);
						LocalNotifications.showDownloadPausedNotification(context, updateData, downloadProgressData);
						intent.putExtra(DownloadReceiver.PARAM_PROGRESS, downloadProgressData);
						return intent;
					}));
				})
				.setOnCancelListener(() -> {
					performStateTransition(DownloadStatus.NOT_DOWNLOADING);
					sendBroadcastIntent(DownloadReceiver.TYPE_CANCELLED);
				})
				.setOnProgressListener(progress -> {
					if (progress.currentBytes > progress.totalBytes) {
						logError(TAG, new UpdateDownloadException(withAppendedStateHistory("Download progress exceeded total file size. Either the server returned incorrect data or the app is in an invalid state!")));
						cancelDownload(new SettingsManager(context).getPreference(PROPERTY_DOWNLOAD_ID, NOT_SET));
						downloadUpdate(updateData);
						return;
					}
					long currentTimestamp = System.currentTimeMillis();

					// This method gets called *very* often. We only want to update the notification and send a broadcast once per second
					// Otherwise, the UI & Notification Renderer would be overflowed by our requests
					if (previousProgressTimeStamp == NOT_SET || currentTimestamp - previousProgressTimeStamp > 1000) {
						// logDebug(TAG, "Received download progress update: " + progress.currentBytes + " / " + progress.totalBytes);
						DownloadProgressData progressData = calculateDownloadETA(progress.currentBytes, progress.totalBytes);
						new SettingsManager(context).savePreference(PROPERTY_DOWNLOAD_PROGRESS, progressData.getProgress());

						previousProgressTimeStamp = currentTimestamp;
						progressPercentage = progressData.getProgress();

						sendBroadcastIntent(DownloadReceiver.TYPE_PROGRESS_UPDATE, (i -> i.putExtra(DownloadReceiver.PARAM_PROGRESS, progressData)));
						LocalNotifications.showDownloadingNotification(context, updateData, progressData);
					}
				})
				.start(new OnDownloadListener() {
					@Override
					public void onDownloadComplete() {
						logDebug(TAG, "Downloading of " + updateData.getFilename() + " complete, verification will begin soon...");
						sendBroadcastIntent(DownloadReceiver.TYPE_DOWNLOAD_COMPLETED);
						// isVerifying must be set before isDownloading to ensure the condition 'isRunning()' remains true at all times.
						verifyUpdate(updateData);
					}

					@Override
					public void onError(Error error) {
						// PRDownloader's Error class distinguishes errors as connection errors and server errors.
						// A connection error is an error with the network connection of the user, such as bad / lost wifi or data connection.
						// A server error is an error which is deliberately returned by the server, such as a 404 / 500 / 503 status code.

						// If the error is a connection error, we retry to resume downloading in 5 seconds (if there is a network connection then).
						if (error.isConnectionError()) {
							logDebug(TAG, "Pausing download due to connection error");
							performStateTransition(DownloadStatus.DOWNLOAD_PAUSED_WAITING_FOR_CONNECTION);
							SettingsManager settingsManager = new SettingsManager(context);
							int downloadId = settingsManager.getPreference(SettingsManager.PROPERTY_DOWNLOAD_ID, NOT_SET);
							int progress = settingsManager.getPreference(SettingsManager.PROPERTY_DOWNLOAD_PROGRESS, NO_PROGRESS);

							resumeDownloadOnReconnectingToNetwork(downloadId, updateData);

							DownloadProgressData progressData = new DownloadProgressData(NOT_SET, progress, true);
							LocalNotifications.showDownloadPausedNotification(context, updateData, progressData);

							sendBroadcastIntent(DownloadReceiver.TYPE_PROGRESS_UPDATE, (i -> i.putExtra(DownloadReceiver.PARAM_PROGRESS, progressData)));
						} else if (error.isServerError()) {
							// Otherwise, we inform the user that the server has refused the download & that it must be restarted at a later stage.
							LocalNotifications.showDownloadFailedNotification(context, false, R.string.download_error_server, R.string.download_notification_error_server);

							sendBroadcastIntent(DownloadReceiver.TYPE_DOWNLOAD_ERROR, (intent -> {
								intent.putExtra(DownloadReceiver.PARAM_ERROR_IS_SERVER_ERROR, true);
								return intent;
							}));

							performStateTransition(DownloadStatus.NOT_DOWNLOADING);
							clearUp();
						} else {
							// If not server and connection error, something has gone wrong internally. This should never happen!
							LocalNotifications.showDownloadFailedNotification(context, true, R.string.download_error_internal, R.string.download_notification_error_internal);

							sendBroadcastIntent(DownloadReceiver.TYPE_DOWNLOAD_ERROR, (intent -> {
								intent.putExtra(DownloadReceiver.PARAM_ERROR_IS_INTERNAL_ERROR, true);
								return intent;
							}));

							performStateTransition(DownloadStatus.NOT_DOWNLOADING);
							clearUp();
						}
					}
				});

		// We persist the download ID so we can access it from anywhere and even after an app restart.
		SettingsManager settingsManager = new SettingsManager(context);
		settingsManager.savePreference(SettingsManager.PROPERTY_DOWNLOAD_ID, downloadId);
	}

	private synchronized void pauseDownload(int downloadId) {
		logDebug(TAG, "Pausing download #" + downloadId);

		if (downloadId != NOT_SET && isStateTransitionAllowed(DownloadStatus.DOWNLOAD_PAUSED)) {
			performStateTransition(DownloadStatus.DOWNLOAD_PAUSED);
			logDebug(TAG, "Pausing using PRDownloader.pause()");
			PRDownloader.pause(downloadId);
		} else {
			logWarning(TAG, new UpdateDownloadException("Not pausing download, invalid download ID provided or not pause-able."));
		}
	}

	private synchronized void resumeDownload(int downloadId, UpdateData updateData) {
		logDebug(TAG, "Resuming download #" + downloadId);

		if (!isStateTransitionAllowed(DownloadStatus.DOWNLOAD_QUEUED)) {
			logWarning(TAG, new UpdateDownloadException("Not resuming download, is a download operation already in progress?"));
			return;
		}

		if (downloadId != NOT_SET) {
			// If the download is still in the PRDownloader request queue, resume it.
			// If not, start the download again (PRdownloader will still resume it using its own SQLite database)
			if (DownloadRequestQueue.getInstance().getStatus(downloadId) != Status.UNKNOWN) {
				logDebug(TAG, "Resuming using PRDownloader.resume()");
				performStateTransition(DownloadStatus.DOWNLOAD_QUEUED);
				PRDownloader.resume(downloadId);
			} else {
				logDebug(TAG, "Resuming using PRDownloader.download()");
				downloadUpdate(updateData);
			}

		} else {
			logWarning(TAG, new UpdateDownloadException("Not resuming download, invalid download ID provided."));
		}
	}

	private synchronized void cancelDownload(int downloadId) {
		logDebug(TAG, "Cancelling download #" + downloadId);

		if (downloadId != NOT_SET) {
			performStateTransition(DownloadStatus.NOT_DOWNLOADING);
			PRDownloader.cancel(downloadId);
			LocalNotifications.hideDownloadingNotification(getApplicationContext());
			clearUp();
			logDebug(TAG, "Cancelled download #" + downloadId);
		} else {
			logWarning(TAG, new UpdateDownloadException("Not cancelling download, no valid ID was provided..."));
		}
	}

	private synchronized void deleteDownloadedFile(UpdateData updateData) {
		if (updateData == null || updateData.getFilename() == null) {
			logWarning(TAG, new UpdateDownloadException("Could not delete downloaded file, null update data or update data without file name was provided"));
			return;
		}

		logDebug(TAG, "Deleting downloaded update file " + updateData.getFilename());

		File downloadedFile = new File(Environment.getExternalStoragePublicDirectory(DIRECTORY_ROOT).getAbsolutePath(), updateData.getFilename());
		if (!downloadedFile.delete()) {
			logWarning(TAG, new UpdateDownloadException("Could not delete downloaded file " + updateData.getFilename()));
		}
		performStateTransition(DownloadStatus.NOT_DOWNLOADING);
	}

	private synchronized void checkDownloadStatus(int downloadId, UpdateData updateData) {
		logDebug(TAG, "Checking status for download #"
				+ downloadId
				+ (updateData != null ? " and updateData "
				+ updateData.getVersionNumber() : ""));

		DownloadStatus resultStatus;
		DownloadProgressData progress;
		boolean hasNetwork = Utils.checkNetworkConnection(getApplicationContext());

		switch (state) {
			case DOWNLOAD_QUEUED: {
				logDebug(TAG, "Download #" + downloadId + " is queued");
				// If queued, there is no progress and the download will start soon.
				resultStatus = DownloadStatus.DOWNLOAD_QUEUED;
				progress = new DownloadProgressData(NOT_SET, NO_PROGRESS);
				break;
			}
			case DOWNLOADING: {
				// If running, return the previously-stored progress percentage.
				int storedProgressPercentage = new SettingsManager(getApplicationContext()).getPreference(PROPERTY_DOWNLOAD_PROGRESS, NO_PROGRESS);
				resultStatus = DownloadStatus.DOWNLOADING;
				progress = new DownloadProgressData(NOT_SET, storedProgressPercentage);
				logDebug(TAG, "Download #" + downloadId + " is running @" + storedProgressPercentage);
				break;
			}
			case DOWNLOAD_PAUSED: {
				// If the download is paused, we return its current percentage and allow resuming of it.
				int storedProgressPercentage = new SettingsManager(getApplicationContext()).getPreference(PROPERTY_DOWNLOAD_PROGRESS, NO_PROGRESS);
				resultStatus = DownloadStatus.DOWNLOAD_PAUSED;
				progress = new DownloadProgressData(NOT_SET, storedProgressPercentage, !hasNetwork);
				logDebug(TAG, "Download #" + downloadId + " is paused @" + storedProgressPercentage);
				break;
			}
			case DOWNLOAD_PAUSED_WAITING_FOR_CONNECTION: {
				// If the download is paused, we return its current percentage and allow resuming of it.
				int storedProgressPercentage = new SettingsManager(getApplicationContext()).getPreference(PROPERTY_DOWNLOAD_PROGRESS, NO_PROGRESS);
				resultStatus = DownloadStatus.DOWNLOAD_PAUSED_WAITING_FOR_CONNECTION;
				progress = new DownloadProgressData(NOT_SET, storedProgressPercentage, true);
				if (autoResumeOnConnectionErrorRunnable == null) {
					resumeDownloadOnReconnectingToNetwork(downloadId, updateData);
				}
				logDebug(TAG, "Download #" + downloadId + " is waiting for a network connection @" + storedProgressPercentage);
				break;
			}
			case VERIFYING: {
				// If the download is being verified, it is always at 100% completion and does not have to wait for a connection.
				resultStatus = DownloadStatus.VERIFYING;
				progress = new DownloadProgressData(NOT_SET, 100, false);
				logDebug(TAG, "Download #" + downloadId + " is verifying");
				break;
			}
			case NOT_DOWNLOADING: {
				// If not downloading in the service, manually check if the file exists to check if it is complete or not
				// If the file exists, assume we've passed the download and verification.
				// If the file does not exist, the update has likely never been downloaded before.
				resultStatus = checkDownloadCompletionByFile(updateData) ? DownloadStatus.DOWNLOAD_COMPLETED : DownloadStatus.NOT_DOWNLOADING;
				performStateTransition(resultStatus);
				progress = new DownloadProgressData(NOT_SET, resultStatus == DownloadStatus.DOWNLOAD_COMPLETED ? 100 : 0, false);
				logDebug(TAG, "Download #" + downloadId + " is " + (resultStatus == DownloadStatus.DOWNLOAD_COMPLETED ? "completed" : "not started"));
				break;
			}
			case DOWNLOAD_COMPLETED: {
				// Even if the service is marked as COMPLETED, manually check if the file exists.
				// This because the user can have switched device / a new update can have been released since last time we checked.
				// If the file exists, the download is indeed complete.
				// If the file does not exist, another update has likely been downloaded before and this update was not downloaded.
				resultStatus = checkDownloadCompletionByFile(updateData) ? DownloadStatus.DOWNLOAD_COMPLETED : DownloadStatus.NOT_DOWNLOADING;
				performStateTransition(resultStatus);
				progress = new DownloadProgressData(NOT_SET, resultStatus == DownloadStatus.DOWNLOAD_COMPLETED ? 100 : 0, false);
				logDebug(TAG, "Download #" + downloadId + " is " + (resultStatus == DownloadStatus.DOWNLOAD_COMPLETED ? "completed" : "not started"));
				break;
			}
			default: {
				resultStatus = DownloadStatus.NOT_DOWNLOADING;
				progress = new DownloadProgressData(NOT_SET, NO_PROGRESS);
				break;
			}
		}

		sendBroadcastIntent(DownloadReceiver.TYPE_STATUS_REQUEST, (intent -> {
			intent.putExtra(DownloadReceiver.PARAM_STATUS, resultStatus);
			intent.putExtra(DownloadReceiver.PARAM_PROGRESS, progress);
			return intent;
		}));
	}

	@SuppressLint("StaticFieldLeak")
	private void verifyUpdate(UpdateData updateData) {
		if (!isStateTransitionAllowed(DownloadStatus.VERIFYING)) {
			logWarning(TAG, new UpdateVerificationException("Not verifying update, is an update verification already in progress?"));
			return;
		}

		performStateTransition(DownloadStatus.VERIFYING);
		verifier = new AsyncTask<UpdateData, Void, Boolean>() {
			@Override
			protected Boolean doInBackground(UpdateData... updateDatas) {
				logDebug(TAG, "Verifying " + updateData.getFilename());
				File downloadedFile = new File(Environment.getExternalStoragePublicDirectory(DIRECTORY_ROOT)
						.getAbsolutePath(), updateData.getFilename());

				return updateData.getMD5Sum() == null || MD5.checkMD5(updateData.getMD5Sum(), downloadedFile);
			}

			@Override
			protected void onPreExecute() {
				logDebug(TAG, "Preparing to verify downloaded update file " + updateData.getFilename());
				performStateTransition(DownloadStatus.VERIFYING);

				LocalNotifications.showVerifyingNotification(getApplicationContext(), ONGOING, HAS_NO_ERROR);
				sendBroadcastIntent(DownloadReceiver.TYPE_VERIFY_STARTED);
			}

			@Override
			protected void onPostExecute(Boolean valid) {
				logDebug(TAG, "Verification result for " + updateData.getFilename() + ": " + valid);

				if (valid) {
					LocalNotifications.hideVerifyingNotification(getApplicationContext());
					LocalNotifications.showDownloadCompleteNotification(getApplicationContext(), updateData);
					performStateTransition(DownloadStatus.DOWNLOAD_COMPLETED);
					sendBroadcastIntent(DownloadReceiver.TYPE_VERIFY_COMPLETE);
				} else {
					deleteDownloadedFile(updateData);
					LocalNotifications.showVerifyingNotification(getApplicationContext(), NOT_ONGOING, HAS_ERROR);
					performStateTransition(DownloadStatus.NOT_DOWNLOADING);
					sendBroadcastIntent(DownloadReceiver.TYPE_VERIFY_FAILED);
				}

				clearUp();
			}

			@Override
			protected void onCancelled() {
				if (updateData != null) {
					logDebug(TAG, "Cancelled verification of " + updateData.getFilename());
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

			if (numberOfElapsedSeconds > 0.0) {
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

			// Calculate number of seconds remaining based off average download speed.
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
		progressPercentage = NO_PROGRESS;
		previousTimeStamp = NOT_SET;
		previousBytesDownloadedSoFar = NOT_SET;
		previousNumberOfSecondsRemaining = NOT_SET;
		verifier = null;
		autoResumeOnConnectionErrorRunnable = null;
		SettingsManager settingsManager = new SettingsManager(getApplicationContext());
		settingsManager.deletePreference(PROPERTY_DOWNLOAD_PROGRESS);
		settingsManager.deletePreference(PROPERTY_DOWNLOAD_ID);
		settingsManager.deletePreference(PROPERTY_DOWNLOADER_STATE);
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

	private boolean checkDownloadCompletionByFile(UpdateData updateData) {
		if (updateData == null || updateData.getFilename() == null) {
			logInfo(TAG, "Cannot check for download completion by file - null update data or filename provided!");
			return false;
		}
		return new File(Environment.getExternalStoragePublicDirectory(DIRECTORY_ROOT), updateData.getFilename()).exists();
	}

	private void resumeDownloadOnReconnectingToNetwork(int downloadId, UpdateData updateData) {
		Handler handler = new Handler();

		if (autoResumeOnConnectionErrorRunnable == null) {
			autoResumeOnConnectionErrorRunnable = new Runnable() {
				@Override
				public void run() {
					if (Utils.checkNetworkConnection(getApplicationContext())) {
						logDebug(TAG, "Network connectivity restored, resuming download...");
						resumeDownload(downloadId, updateData);
					} else {
						handler.postDelayed(this, NO_CONNECTION_REFRESH_RATE);
					}
				}
			};

			handler.postDelayed(autoResumeOnConnectionErrorRunnable, NO_CONNECTION_REFRESH_RATE);
		}
	}

	/**
	 * Transitions the state of the service from the current state S to the new state S' (if
	 * allowed).
	 *
	 * @param newState Destination state (S').
	 */
	private void performStateTransition(DownloadStatus newState) {
		if (isStateTransitionAllowed(newState) && newState != state) {
			//Log.v(TAG, state + " -> " + newState);
			executedStateTransitions.add(Pair.create(LocalDateTime.now(DateTimeZone.forID("Europe/Amsterdam")), state + " -> " + newState));
			state = newState;
			new SettingsManager(getApplicationContext()).savePreference(PROPERTY_DOWNLOADER_STATE, state.toString());
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
	private boolean isStateTransitionAllowed(DownloadStatus newState) {
		// If the new state is the same as the current state, allow it
		if (newState == state) {
			return true;
		}

		// Currently, a transition to not downloading exists from every possible state.
		if (newState == DownloadStatus.NOT_DOWNLOADING) {
			return true;
		}

		switch (state) {
			case NOT_DOWNLOADING:
				// Start download in service or when update is already (manually) downloaded
				return newState == DownloadStatus.DOWNLOAD_QUEUED || newState == DownloadStatus.DOWNLOAD_COMPLETED;
			case DOWNLOAD_QUEUED:
				// Start download execute (by library)
				return newState == DownloadStatus.DOWNLOADING;
			case DOWNLOADING:
				// Pause download, wait for connection or download completed
				return newState == DownloadStatus.DOWNLOAD_PAUSED || newState == DownloadStatus.DOWNLOAD_PAUSED_WAITING_FOR_CONNECTION || newState == DownloadStatus.VERIFYING;
			case DOWNLOAD_PAUSED:
				// Resume download (either via PRDownloader.Resume or via downloadUpdate()
				return newState == DownloadStatus.DOWNLOAD_QUEUED;
			case DOWNLOAD_PAUSED_WAITING_FOR_CONNECTION:
				// Resume download
				return newState == DownloadStatus.DOWNLOAD_QUEUED;
			case VERIFYING:
				return newState == DownloadStatus.DOWNLOAD_COMPLETED;
			default:
				return false;
		}
	}

	// Convert state history to following format and save it to SharedPreferences:
	// 2019-01-01 00:00:00.000|DOWNLOADING -> PAUSED,2019-01-01 00:00:01.000|PAUSED -> DOWNLOAD_QUEUED
	private void serializeExecutedStateTransitions() {
		String serializedStateHistory = StreamSupport.stream(executedStateTransitions)
				.map(est -> String.format("%s|%s", est.first.toString(HISTORY_DATETIME_PATTERN), est.second))
				.collect(Collectors.joining(","));

		SettingsManager settingsManager = new SettingsManager(getApplicationContext());
		settingsManager.savePreference(PROPERTY_DOWNLOADER_STATE_HISTORY, serializedStateHistory);
	}

	// Convert saved history from SharedPreferences back to format storable in this class.
	private List<Pair<LocalDateTime, String>> deserializeExecutedStateTransitions() {
		SettingsManager settingsManager = new SettingsManager(getApplicationContext());
		String serializedStateHistory = settingsManager.getPreference(PROPERTY_DOWNLOADER_STATE_HISTORY, "");

		if (serializedStateHistory == null || serializedStateHistory.isEmpty()) {
			return new ArrayList<>();
		}

		//noinspection Convert2MethodRef -> e !- null cannot convert to Objects::nonNull, requires too high Android version...
		return StreamSupport.stream(Arrays.asList(serializedStateHistory.split(",")))
				.map(elem -> {
					String[] parts = elem.split("\\|");
					LocalDateTime timestamp;

					if (parts.length < 2) {
						logError(TAG, new OxygenUpdaterException("Cannot parse downloader state. Contents of line: " + elem + ", total contents: " + serializedStateHistory));
						return null;
					}

					try {
						timestamp = LocalDateTime.parse(parts[0], DateTimeFormat.forPattern(HISTORY_DATETIME_PATTERN));
					} catch (Exception e) {
						timestamp = LocalDateTime.now();
					}
					return Pair.create(timestamp, parts[1]);
				})
				.filter(e -> e != null)
				.collect(Collectors.toList());
	}
}
