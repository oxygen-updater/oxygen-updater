package com.arjanvlek.oxygenupdater.download;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Parcelable;

import com.arjanvlek.oxygenupdater.updateinformation.UpdateData;

/**
 * Oxygen Updater, copyright 2019 Arjan Vlek. File created by arjan.vlek on 05/01/2019.
 */
public class DownloadServiceRestarter extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent restartServiceIntent = new Intent(context, DownloadService.class);
        restartServiceIntent.putExtra(DownloadService.PARAM_ACTION, DownloadService.ACTION_SERVICE_RESTART);
        restartServiceIntent.putExtra(DownloadService.PARAM_DOWNLOAD_ID, intent.getIntExtra(DownloadService.PARAM_DOWNLOAD_ID, -1));
        restartServiceIntent.putExtra(DownloadService.PARAM_UPDATE_DATA, (UpdateData) intent.getParcelableExtra(DownloadService.PARAM_UPDATE_DATA));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(restartServiceIntent);
        } else {
            context.startService(restartServiceIntent);
        }
    }
}
