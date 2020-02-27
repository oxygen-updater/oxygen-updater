package com.arjanvlek.oxygenupdater.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.arjanvlek.oxygenupdater.models.UpdateData
import com.arjanvlek.oxygenupdater.services.DownloadService
import com.arjanvlek.oxygenupdater.services.DownloadService.Companion.PARAM_ACTION
import com.arjanvlek.oxygenupdater.services.DownloadService.Companion.PARAM_DOWNLOAD_ID
import com.arjanvlek.oxygenupdater.services.DownloadService.Companion.PARAM_UPDATE_DATA

/**
 * This Receiver creates a new Intent to restart [DownloadService] after it was killed whilst
 * in-progress. It receives a Download ID and UpdateData objects which it returns to the new
 * instance of DownloadService.
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 * @author Arjan Vlek (github.com/arjanvlek)
 */
class DownloadServiceRestarter : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val restartServiceIntent = Intent(context, DownloadService::class.java)
            .putExtra(PARAM_ACTION, DownloadService.ACTION_SERVICE_RESTART)
            .putExtra(PARAM_DOWNLOAD_ID, intent.getIntExtra(PARAM_DOWNLOAD_ID, -1))
            .putExtra(PARAM_UPDATE_DATA, intent.getParcelableExtra(PARAM_UPDATE_DATA) as UpdateData)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(restartServiceIntent)
        } else {
            context.startService(restartServiceIntent)
        }
    }
}
