package com.arjanvlek.oxygenupdater.Support;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;

import com.arjanvlek.oxygenupdater.Model.OxygenOTAUpdate;
import com.arjanvlek.oxygenupdater.Model.DownloadProgressData;
import com.arjanvlek.oxygenupdater.R;

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
import static android.os.Environment.DIRECTORY_DOWNLOADS;
import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_DOWNLOAD_ID;


public class UpdateDownloader {

    private final Activity baseActivity;
    private final DownloadManager downloadManager;
    private final SettingsManager settingsManager;

    private UpdateDownloadListener listener;
    private List<Double> measurements = new ArrayList<>();

    public final static int NOT_SET = -1;

    private boolean initialized;
    private long previousBytesDownloadedSoFar = NOT_SET;
    private long previousTimeStamp;
    private long previousNumberOfSecondsRemaining = NOT_SET;

    public UpdateDownloader(Activity baseActivity) {
        this.baseActivity = baseActivity;
        this.downloadManager = (DownloadManager) baseActivity.getSystemService(Context.DOWNLOAD_SERVICE);
        this.settingsManager = new SettingsManager(baseActivity.getApplicationContext());
    }

    public UpdateDownloader setUpdateDownloadListenerAndStartPolling(UpdateDownloadListener listener, OxygenOTAUpdate oxygenOTAUpdate) {
        this.listener = listener;

        if(!initialized && listener != null) {
            listener.onDownloadManagerInit(this);
            initialized = true;
        }

        checkDownloadProgress(oxygenOTAUpdate);

        return this;
    }


    public void downloadUpdate(OxygenOTAUpdate oxygenOTAUpdate) {
        if(oxygenOTAUpdate != null) {
            if(!oxygenOTAUpdate.getDownloadUrl().contains("http")) {
                showDownloadErrorNotification(baseActivity, oxygenOTAUpdate, 404);
                if(listener != null) listener.onDownloadError(this, 404);
            } else {
                Uri downloadUri = Uri.parse(oxygenOTAUpdate.getDownloadUrl());

                DownloadManager.Request request = new DownloadManager.Request(downloadUri)
                        .setDescription(baseActivity.getString(R.string.download_description))
                        .setTitle(oxygenOTAUpdate.getVersionNumber() != null && !oxygenOTAUpdate.getVersionNumber().equals("null") && !oxygenOTAUpdate.getVersionNumber().isEmpty() ? oxygenOTAUpdate.getVersionNumber() : baseActivity.getString(R.string.download_unknown_update_name))
                        .setDestinationInExternalPublicDir(DIRECTORY_DOWNLOADS, oxygenOTAUpdate.getFilename())
                        .setVisibleInDownloadsUi(false)
                        .setNotificationVisibility(VISIBILITY_VISIBLE);

                long downloadID = downloadManager.enqueue(request);

                previousBytesDownloadedSoFar = NOT_SET;
                settingsManager.savePreference(PROPERTY_DOWNLOAD_ID, downloadID);

                checkDownloadProgress(oxygenOTAUpdate);

                if(listener != null) listener.onDownloadStarted(downloadID);
            }
        }

    }

    public void cancelDownload() {
        if(settingsManager.containsPreference(PROPERTY_DOWNLOAD_ID)) {
            downloadManager.remove((long) settingsManager.getPreference(PROPERTY_DOWNLOAD_ID));
            clearUp();

            if(listener != null)listener.onDownloadCancelled();
        }
    }

    public void checkDownloadProgress(OxygenOTAUpdate oxygenOTAUpdate) {

        if(settingsManager.containsPreference(PROPERTY_DOWNLOAD_ID)) {
            final Long downloadId = settingsManager.getPreference(PROPERTY_DOWNLOAD_ID);

            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(downloadId);

            Cursor cursor = downloadManager.query(query);
            if (cursor != null && cursor.moveToFirst()) {
                int status = cursor.getInt(cursor.getColumnIndex(COLUMN_STATUS));
                switch (status) {
                    case STATUS_PENDING:
                        if(listener != null) listener.onDownloadPending();

                        recheckDownloadProgress(oxygenOTAUpdate, 1);
                        break;
                    case STATUS_PAUSED:
                        if(listener != null) listener.onDownloadPaused(cursor.getInt(cursor.getColumnIndex(COLUMN_REASON)));

                        recheckDownloadProgress(oxygenOTAUpdate, 5);
                        break;
                    case STATUS_RUNNING:

                        int bytesDownloadedSoFar = cursor.getInt(cursor.getColumnIndex(COLUMN_BYTES_DOWNLOADED_SO_FAR));
                        int totalSizeBytes = cursor.getInt(cursor.getColumnIndex(COLUMN_TOTAL_SIZE_BYTES));

                        DownloadProgressData eta = calculateDownloadETA(bytesDownloadedSoFar, totalSizeBytes);

                        if(listener != null) listener.onDownloadProgressUpdate(eta);

                        previousBytesDownloadedSoFar = cursor.getInt(cursor.getColumnIndex(COLUMN_BYTES_DOWNLOADED_SO_FAR));

                        recheckDownloadProgress(oxygenOTAUpdate, 1);

                        break;
                    case DownloadManager.STATUS_SUCCESSFUL:
                        clearUp();

                        if(listener != null) listener.onDownloadComplete();

                        verifyDownload(oxygenOTAUpdate);
                        break;
                    case DownloadManager.STATUS_FAILED:
                        clearUp();

                        int statusCode = cursor.getInt(cursor.getColumnIndex(COLUMN_REASON));

                        showDownloadErrorNotification(baseActivity, oxygenOTAUpdate, statusCode);
                        if(listener != null) listener.onDownloadError(this, statusCode);
                        cancelDownload();
                        break;
                }
                cursor.close();
            }
        }
    }

    private void verifyDownload(OxygenOTAUpdate oxygenOTAUpdate) {
        new DownloadVerifier().execute(oxygenOTAUpdate);
    }

    private boolean makeDownloadDirectory() {
        File downloadDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        return downloadDirectory.mkdirs();
    }

    private void recheckDownloadProgress(final OxygenOTAUpdate oxygenOTAUpdate, int secondsDelay) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                checkDownloadProgress(oxygenOTAUpdate);
            }
        }, (secondsDelay * 1000));
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
                if (measurements.size() >= 5) {
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

    private class DownloadVerifier extends AsyncTask<OxygenOTAUpdate, Integer, Boolean> {

        private OxygenOTAUpdate oxygenOTAUpdate;

        @Override
        protected void onPreExecute() {
            if(listener != null) listener.onVerifyStarted();
        }

        @Override
        protected Boolean doInBackground(OxygenOTAUpdate... params) {
            this.oxygenOTAUpdate = params[0];
            String filename = oxygenOTAUpdate.getFilename();
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + File.separator + filename);
            return oxygenOTAUpdate == null || oxygenOTAUpdate.getMD5Sum() == null || MD5.checkMD5(oxygenOTAUpdate.getMD5Sum(), file);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                if(listener != null) listener.onVerifyComplete();
                clearUp();
            } else {
                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + File.separator + oxygenOTAUpdate.getFilename());
                try {
                    //noinspection ResultOfMethodCallIgnored
                    file.delete();
                } catch (Exception ignored) {

                }
                if(listener != null) listener.onVerifyError(instance());
                clearUp();
            }
        }
    }

    private void showDownloadErrorNotification(Activity activity, OxygenOTAUpdate oxygenOTAUpdate, int statusCode) {
        if (statusCode < 1000) {
            Notifications.showDownloadFailedNotification(activity, R.string.download_error_network, R.string.download_notification_error_network);
        } else {
            switch (statusCode) {
                case ERROR_UNHANDLED_HTTP_CODE:
                case ERROR_HTTP_DATA_ERROR:
                case ERROR_TOO_MANY_REDIRECTS:
                    Notifications.showDownloadFailedNotification(activity, R.string.download_error_network, R.string.download_notification_error_network);
                    break;
                case ERROR_FILE_ERROR:
                    makeDownloadDirectory();
                    Notifications.showDownloadFailedNotification(activity, R.string.download_error_directory, R.string.download_notification_error_storage_not_found);
                    break;
                case ERROR_INSUFFICIENT_SPACE:
                    Notifications.showDownloadFailedNotification(activity, R.string.download_error_storage, R.string.download_notification_error_storage_full);
                    break;
                case ERROR_DEVICE_NOT_FOUND:
                    Notifications.showDownloadFailedNotification(activity, R.string.download_error_sd_card, R.string.download_notification_error_sd_card_missing);
                    break;
                case ERROR_CANNOT_RESUME:
                    cancelDownload();
                    downloadUpdate(oxygenOTAUpdate);
                    break;
            }
        }
    }
}
