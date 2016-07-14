package com.arjanvlek.oxygenupdater;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

import com.arjanvlek.oxygenupdater.Support.NetworkConnectionManager;

import static com.arjanvlek.oxygenupdater.ApplicationContext.PACKAGE_REPLACED_KEY;

public class GcmPackageReplacedReceiver extends WakefulBroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        NetworkConnectionManager networkConnectionManager = new NetworkConnectionManager(context);
        if(networkConnectionManager.checkNetworkConnection()) {
            if (intent != null && intent.getAction().equals(Intent.ACTION_MY_PACKAGE_REPLACED)) {
                // invalidate the current GCM registration id, and re-register with GCM server using the GcmRegistrationIntentService
                Intent i = new Intent(context, GcmRegistrationIntentService.class);
                i.putExtra(PACKAGE_REPLACED_KEY, true);
                startWakefulService(context, i);
            } else {
                Intent i = new Intent(context, GcmRegistrationIntentService.class);
                i.putExtra(PACKAGE_REPLACED_KEY, true);
                startWakefulService(context, i);
            }
        }
    }
}