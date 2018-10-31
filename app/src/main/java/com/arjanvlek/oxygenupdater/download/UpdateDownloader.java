package com.arjanvlek.oxygenupdater.download;

import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;

import com.arjanvlek.oxygenupdater.R;
import com.arjanvlek.oxygenupdater.internal.Utils;
import com.arjanvlek.oxygenupdater.internal.logger.Logger;
import com.arjanvlek.oxygenupdater.notifications.LocalNotifications;
import com.arjanvlek.oxygenupdater.settings.SettingsManager;
import com.arjanvlek.oxygenupdater.updateinformation.UpdateData;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static android.app.DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR;
import static android.app.DownloadManager.COLUMN_REASON;
import static android.app.DownloadManager.COLUMN_STATUS;
import static android.app.DownloadManager.COLUMN_TOTAL_SIZE_BYTES;
import static android.app.DownloadManager.ERROR_CANNOT_RESUME;
import static android.app.DownloadManager.ERROR_DEVICE_NOT_FOUND;
import static android.app.DownloadManager.ERROR_FILE_ERROR;
import static android.app.DownloadManager.ERROR_HTTP_DATA_ERROR;
import static android.app.DownloadManager.ERROR_INSUFFICIENT_SPACE;
import static android.app.DownloadManager.ERROR_TOO_MANY_REDIRECTS;
import static android.app.DownloadManager.ERROR_UNHANDLED_HTTP_CODE;
import static android.app.DownloadManager.Request.VISIBILITY_VISIBLE;
import static android.app.DownloadManager.STATUS_PAUSED;
import static android.app.DownloadManager.STATUS_PENDING;
import static android.app.DownloadManager.STATUS_RUNNING;
import static com.arjanvlek.oxygenupdater.notifications.LocalNotifications.HAS_ERROR;
import static com.arjanvlek.oxygenupdater.notifications.LocalNotifications.HAS_NO_ERROR;
import static com.arjanvlek.oxygenupdater.notifications.LocalNotifications.NOT_ONGOING;
import static com.arjanvlek.oxygenupdater.notifications.LocalNotifications.ONGOING;
import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_DOWNLOAD_ID;


public class UpdateDownloader {

    private final Context context;
    private final DownloadManager downloadManager;
    private final SettingsManager settingsManager;

    private UpdateDownloadListener listener;
    private List<Double> measurements = new ArrayList<>();

    final static int NOT_SET = -1;

    private boolean initialized;
    private boolean isVerifying;
    private long previousBytesDownloadedSoFar = NOT_SET;
    private long previousTimeStamp;
    private long previousNumberOfSecondsRemaining = NOT_SET;
    private static final String TAG = "UpdateDownloader";
    public static final String DIRECTORY_ROOT = "";

    public UpdateDownloader(Context context) {
        this.context = context;
        if(context != null) {
            this.downloadManager = (DownloadManager) Utils.getSystemService(context, Context.DOWNLOAD_SERVICE);
            this.settingsManager = new SettingsManager(context);
        } else {
            this.downloadManager = null;
            this.settingsManager = new SettingsManager(null);
        }
    }

    public UpdateDownloader setUpdateDownloadListenerAndStartPolling(UpdateDownloadListener listener, UpdateData updateData) {
        this.listener = listener;

        if(!initialized && listener != null) {
            listener.onDownloadManagerInit(this);
            initialized = true;
        }

        checkDownloadProgress(updateData);

        return this;
    }


    public void downloadUpdate(UpdateData updateData) {
        if (updateData != null && downloadManager != null) {
            if (!updateData.getDownloadUrl().contains("http")) {
                Logger.logError(TAG, "Invalid download URL: " + updateData.getDownloadUrl());
                showDownloadErrorNotification(context, updateData, 404);
                if(listener != null) listener.onDownloadError(this, 404);
            } else {
                Uri downloadUri = Uri.parse(updateData.getDownloadUrl());

                DownloadManager.Request request = new DownloadManager.Request(downloadUri)
                        .setDescription(context.getString(R.string.download_description))
                        .setTitle(updateData.getVersionNumber() != null && !updateData.getVersionNumber().equals("null") && !updateData.getVersionNumber().isEmpty() ? updateData.getVersionNumber() : context.getString(R.string.download_unknown_update_name))
                        .setDestinationInExternalPublicDir(DIRECTORY_ROOT, updateData.getFilename())
                        .setVisibleInDownloadsUi(false)
                        .setNotificationVisibility(VISIBILITY_VISIBLE);

                long downloadID = downloadManager.enqueue(request);

                previousBytesDownloadedSoFar = NOT_SET;
                settingsManager.savePreference(PROPERTY_DOWNLOAD_ID, downloadID);

                checkDownloadProgress(updateData);

                if(listener != null) listener.onDownloadStarted(downloadID);
            }
        } else {
            Logger.logError(TAG, "Update data is null while trying to download an update. This should not be possible!");
        }

    }

    public void cancelDownload(UpdateData updateData) {

        long downloadId = settingsManager.getPreference(PROPERTY_DOWNLOAD_ID, -1L);
        if (downloadId != -1L && downloadManager != null) {
            downloadManager.remove(downloadId);
        }

        clearUp();
        deleteDownload(updateData);

        if (listener != null) listener.onDownloadCancelled();
    }

    public boolean checkIfUpdateIsDownloaded(UpdateData updateData) {
        if (updateData == null || updateData.getId() == null) return false;
        File file = new File(Environment.getExternalStoragePublicDirectory(DIRECTORY_ROOT).getPath() + File.separator + updateData.getFilename());
        return (file.exists() && !settingsManager.containsPreference(PROPERTY_DOWNLOAD_ID));
    }

    public boolean checkIfAnUpdateIsBeingVerified() {
        return isVerifying;
    }

    public boolean deleteDownload(UpdateData updateData) {
        try {
            File file = new File(Environment.getExternalStoragePublicDirectory(DIRECTORY_ROOT).getPath() + File.separator + updateData.getFilename());
            return file.delete();
        } catch (Exception e) {
            Logger.logError(TAG, "Failed to delete downloaded update file: ", e);
            return false;
        }
    }

    private void checkDownloadProgress(UpdateData updateData) {

        if (settingsManager.containsPreference(PROPERTY_DOWNLOAD_ID) && downloadManager != null) {
            final long downloadId = settingsManager.getPreference(PROPERTY_DOWNLOAD_ID, -1L);

            if (downloadId == -1L) return;

            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(downloadId);

            Cursor cursor = downloadManager.query(query);
            if (cursor != null && cursor.moveToFirst()) {
                int status = cursor.getInt(cursor.getColumnIndex(COLUMN_STATUS));
                switch (status) {
                    case STATUS_PENDING:
                        if(listener != null) listener.onDownloadPending();

                        recheckDownloadProgress(updateData, 1);
                        break;
                    case STATUS_PAUSED:
                        if(listener != null) listener.onDownloadPaused(cursor.getInt(cursor.getColumnIndex(COLUMN_REASON)));

                        recheckDownloadProgress(updateData, 5);
                        break;
                    case STATUS_RUNNING:

                        int bytesDownloadedSoFar = cursor.getInt(cursor.getColumnIndex(COLUMN_BYTES_DOWNLOADED_SO_FAR));
                        int totalSizeBytes = cursor.getInt(cursor.getColumnIndex(COLUMN_TOTAL_SIZE_BYTES));

                        DownloadProgressData eta = calculateDownloadETA(bytesDownloadedSoFar, totalSizeBytes);

                        if(listener != null) listener.onDownloadProgressUpdate(eta);

                        previousBytesDownloadedSoFar = bytesDownloadedSoFar;

                        recheckDownloadProgress(updateData, 1);

                        break;
                    case DownloadManager.STATUS_SUCCESSFUL:
                        clearUp();

                        if(listener != null) listener.onDownloadComplete();

                        verifyDownload(context, updateData);
                        break;
                    case DownloadManager.STATUS_FAILED:
                        clearUp();

                        int statusCode = cursor.getInt(cursor.getColumnIndex(COLUMN_REASON));

                        showDownloadErrorNotification(context, updateData, statusCode);
                        if(listener != null) listener.onDownloadError(this, statusCode);
                        cancelDownload(updateData);
                        break;
                }
                cursor.close();
            } else {
                clearUp();
                deleteDownload(updateData);
                if (listener != null) listener.onDownloadCancelled();
            }
        }
    }

    private void verifyDownload(Context context, UpdateData updateData) {
        new DownloadVerifier(context).execute(updateData);
    }

    private void recheckDownloadProgress(final UpdateData updateData, int secondsDelay) {
        new Handler().postDelayed(() -> checkDownloadProgress(updateData), (secondsDelay * 1000));
    }

    private void clearUp() {
        previousTimeStamp = NOT_SET;
        previousBytesDownloadedSoFar = NOT_SET;
        previousNumberOfSecondsRemaining = NOT_SET;
        settingsManager.deletePreference(PROPERTY_DOWNLOAD_ID);
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

    private UpdateDownloader instance() {
        return this;
    }

    private class DownloadVerifier extends AsyncTask<UpdateData, Integer, Boolean> {

        private final Context context;
        private UpdateData updateData;

        DownloadVerifier(Context context) {
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            if(listener != null) listener.onVerifyStarted();
            isVerifying = true;
            LocalNotifications.showVerifyingNotification(context, ONGOING, HAS_NO_ERROR);
        }

        @Override
        protected Boolean doInBackground(UpdateData... params) {
            this.updateData = params[0];

            if (updateData == null || updateData.getMD5Sum() == null) return true;

            File file = new File(Environment.getExternalStoragePublicDirectory(DIRECTORY_ROOT).getPath() + File.separator + updateData.getFilename());

            if (!file.exists()) return null;

            return MD5.checkMD5(updateData.getMD5Sum(), file);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            isVerifying = false;

            // When the file has been moved away outside the app don't show the install guide.
            if (result == null) {
                LocalNotifications.hideVerifyingNotification(context);
                clearUp();
                return;
            }

            if (result) {
                if(listener != null) listener.onVerifyComplete();
                LocalNotifications.hideVerifyingNotification(context);
                LocalNotifications.showDownloadCompleteNotification(context, updateData);

                clearUp();
            } else {
                deleteDownload(updateData);

                if(listener != null) listener.onVerifyError(instance());
                LocalNotifications.showVerifyingNotification(context, NOT_ONGOING, HAS_ERROR);

                clearUp();
            }
        }
    }

    public static String getFilePath(UpdateData updateData) {
        return Environment.getExternalStoragePublicDirectory(DIRECTORY_ROOT).getPath() + File.separator + updateData.getFilename();
    }

    private void showDownloadErrorNotification(Context context, UpdateData updateData, int statusCode) {
        if (statusCode < 1000) {
            LocalNotifications.showDownloadFailedNotification(context, R.string.download_error_network, R.string.download_notification_error_network);
        } else {
            switch (statusCode) {
                case ERROR_UNHANDLED_HTTP_CODE:
                case ERROR_HTTP_DATA_ERROR:
                case ERROR_TOO_MANY_REDIRECTS:
                    LocalNotifications.showDownloadFailedNotification(context, R.string.download_error_network, R.string.download_notification_error_network);
                    break;
                case ERROR_FILE_ERROR:
                    LocalNotifications.showDownloadFailedNotification(context, R.string.download_error_directory, R.string.download_notification_error_storage_not_found);
                    break;
                case ERROR_INSUFFICIENT_SPACE:
                    LocalNotifications.showDownloadFailedNotification(context, R.string.download_error_storage, R.string.download_notification_error_storage_full);
                    cancelDownload(updateData);
                    break;
                case ERROR_DEVICE_NOT_FOUND:
                    LocalNotifications.showDownloadFailedNotification(context, R.string.download_error_sd_card, R.string.download_notification_error_sd_card_missing);
                    break;
                case ERROR_CANNOT_RESUME:
                    Logger.logWarning("UpdateDownloader", "Download of update failed with status ERROR_CANNOT_RESUME");
                    cancelDownload(updateData);
                    downloadUpdate(updateData);
                    break;
            }
        }
    }
}
