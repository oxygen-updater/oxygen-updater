package com.arjanvlek.oxygenupdater.Support;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;

import com.arjanvlek.oxygenupdater.Model.OxygenOTAUpdate;
import com.arjanvlek.oxygenupdater.Model.DownloadProgressData;
import com.arjanvlek.oxygenupdater.R;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static android.app.DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR;
import static android.app.DownloadManager.COLUMN_REASON;
import static android.app.DownloadManager.COLUMN_STATUS;
import static android.app.DownloadManager.COLUMN_TOTAL_SIZE_BYTES;
import static android.app.DownloadManager.Request.VISIBILITY_VISIBLE;
import static android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED;
import static android.app.DownloadManager.STATUS_PAUSED;
import static android.app.DownloadManager.STATUS_PENDING;
import static android.app.DownloadManager.STATUS_RUNNING;
import static android.os.Environment.DIRECTORY_DOWNLOADS;
import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_DOWNLOAD_ID;
import static com.arjanvlek.oxygenupdater.Support.UpdateDownloader.DownloadSpeedUnits.BYTES;
import static com.arjanvlek.oxygenupdater.Support.UpdateDownloader.DownloadSpeedUnits.KILO_BYTES;
import static com.arjanvlek.oxygenupdater.Support.UpdateDownloader.DownloadSpeedUnits.MEGA_BYTES;
import static java.math.BigDecimal.ROUND_CEILING;

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
    private DownloadSpeedUnits previousSpeedUnits = BYTES;
    private double previousDownloadSpeed = NOT_SET;
    private long previousNumberOfSecondsRemaining = NOT_SET;

    public UpdateDownloader(Activity baseActivity) {
        this.baseActivity = baseActivity;
        this.downloadManager = (DownloadManager) baseActivity.getSystemService(Context.DOWNLOAD_SERVICE);
        this.settingsManager = new SettingsManager(baseActivity.getApplicationContext());
    }

    public UpdateDownloader setUpdateDownloadListener(UpdateDownloadListener listener) {
        this.listener = listener;

        if(!initialized) {
            listener.onDownloadManagerInit();
            initialized = true;
        }
        return this;
    }


    public void downloadUpdate(OxygenOTAUpdate oxygenOTAUpdate) {
        if(oxygenOTAUpdate != null) {
            if(!oxygenOTAUpdate.getDownloadUrl().contains("http")) {
                listener.onDownloadError(404);
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

                listener.onDownloadStarted(downloadID);
            }
        }

    }

    public void cancelDownload() {
        if(settingsManager.containsPreference(PROPERTY_DOWNLOAD_ID)) {
            downloadManager.remove((long) settingsManager.getPreference(PROPERTY_DOWNLOAD_ID));
            clearUp();

            listener.onDownloadCancelled();
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
                        listener.onDownloadPending();

                        recheckDownloadProgress(oxygenOTAUpdate, 1);
                        break;
                    case STATUS_PAUSED:
                        listener.onDownloadPaused(cursor.getInt(cursor.getColumnIndex(COLUMN_REASON)));

                        recheckDownloadProgress(oxygenOTAUpdate, 5);
                        break;
                    case STATUS_RUNNING:

                        int bytesDownloadedSoFar = cursor.getInt(cursor.getColumnIndex(COLUMN_BYTES_DOWNLOADED_SO_FAR));
                        int totalSizeBytes = cursor.getInt(cursor.getColumnIndex(COLUMN_TOTAL_SIZE_BYTES));

                        DownloadProgressData eta = calculateDownloadETA(bytesDownloadedSoFar, totalSizeBytes);

                        listener.onDownloadProgressUpdate(eta);

                        previousBytesDownloadedSoFar = cursor.getInt(cursor.getColumnIndex(COLUMN_BYTES_DOWNLOADED_SO_FAR));

                        recheckDownloadProgress(oxygenOTAUpdate, 1);

                        break;
                    case DownloadManager.STATUS_SUCCESSFUL:
                        clearUp();

                        listener.onDownloadComplete();

                        verifyDownload(oxygenOTAUpdate);
                        break;
                    case DownloadManager.STATUS_FAILED:
                        clearUp();

                        listener.onDownloadError(cursor.getInt(cursor.getColumnIndex(COLUMN_REASON)));
                        break;
                }
                cursor.close();
            }
        }
    }

    public void verifyDownload(OxygenOTAUpdate oxygenOTAUpdate) {
        new DownloadVerifier().execute(oxygenOTAUpdate);
    }

    public boolean makeDownloadDirectory() {
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
        previousSpeedUnits = BYTES;
        previousDownloadSpeed = NOT_SET;
        previousNumberOfSecondsRemaining = NOT_SET;
        settingsManager.deletePreference(PROPERTY_DOWNLOAD_ID);
    }

    private DownloadProgressData calculateDownloadETA(long bytesDownloadedSoFar, long totalSizeBytes) {
        double bytesDownloadedInSecond;
        boolean validMeasurement = false;

        double downloadSpeed = NOT_SET;
        long numberOfSecondsRemaining = NOT_SET;
        long averageBytesPerSecond = NOT_SET;
        long currentTimeStamp = System.currentTimeMillis();
        long bytesRemainingToDownload = totalSizeBytes - bytesDownloadedSoFar;

        DownloadSpeedUnits speedUnits = BYTES;
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
            if (averageBytesPerSecond >= 0 && averageBytesPerSecond < 1024) {
                downloadSpeed = averageBytesPerSecond;
                speedUnits = BYTES;

            } else if (averageBytesPerSecond >= 1024 && averageBytesPerSecond < 1048576) {
                downloadSpeed = new BigDecimal(averageBytesPerSecond).setScale(0, ROUND_CEILING).divide(new BigDecimal(1024), ROUND_CEILING).doubleValue();
                speedUnits = KILO_BYTES;
            } else if (averageBytesPerSecond >= 1048576) {
                downloadSpeed = new BigDecimal(averageBytesPerSecond).setScale(2, ROUND_CEILING).divide(new BigDecimal(1048576), ROUND_CEILING).doubleValue();
                speedUnits = MEGA_BYTES;
            }

            if(validMeasurement) {
                previousNumberOfSecondsRemaining = numberOfSecondsRemaining;
                previousTimeStamp = currentTimeStamp;
                previousDownloadSpeed = downloadSpeed;
                previousSpeedUnits = speedUnits;
            } else {
                downloadSpeed = previousDownloadSpeed;
                speedUnits = previousSpeedUnits;
                numberOfSecondsRemaining = previousNumberOfSecondsRemaining;
            }
        }

        previousBytesDownloadedSoFar = bytesDownloadedSoFar;

        int progress = 0;

        if(totalSizeBytes > 0.0) {
            progress = (int) ((bytesDownloadedSoFar * 100) / totalSizeBytes);
        }

        return new DownloadProgressData(downloadSpeed, speedUnits, numberOfSecondsRemaining, progress);
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

    private class DownloadVerifier extends AsyncTask<OxygenOTAUpdate, Integer, Boolean> {

        @Override
        protected void onPreExecute() {
            listener.onVerifyStarted();
        }

        @Override
        protected Boolean doInBackground(OxygenOTAUpdate... params) {
            String filename = params[0].getFilename();
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + File.separator + filename);
            return params[0] == null || params[0].getMD5Sum() == null || MD5.checkMD5(params[0].getMD5Sum(), file);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                listener.onVerifyComplete();
                clearUp();
            } else {
                listener.onVerifyError();
                clearUp();
            }
        }
    }

    public enum DownloadSpeedUnits {
        BYTES("B/s"), KILO_BYTES("KB/s"), MEGA_BYTES("MB/s");

        String stringValue;

        DownloadSpeedUnits(String stringValue) {
            this.stringValue = stringValue;
        }

        public String getStringValue() {
            return stringValue;
        }
    }
}
