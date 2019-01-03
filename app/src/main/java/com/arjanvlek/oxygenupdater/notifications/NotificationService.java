package com.arjanvlek.oxygenupdater.notifications;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.os.PersistableBundle;

import com.arjanvlek.oxygenupdater.ApplicationData;
import com.arjanvlek.oxygenupdater.internal.Utils;
import com.arjanvlek.oxygenupdater.internal.logger.Logger;
import com.arjanvlek.oxygenupdater.settings.SettingsManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import java8.util.stream.StreamSupport;

import static com.arjanvlek.oxygenupdater.settings.SettingsManager.PROPERTY_NOTIFICATION_DELAY_IN_SECONDS;

public class NotificationService extends FirebaseMessagingService {


    private static final List<Integer> AVAILABLE_JOB_IDS = Arrays.asList(8326, 8327, 8328, 8329, 8330, 8331, 8332, 8333);
    public static final String TAG = "NotificationService";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        try {
            SettingsManager settingsManager = new SettingsManager(getApplicationContext());

            //  Receive the notification contents but build / show the actual notification with a small random delay to avoid overloading the server.
            Map<String, String> messageContents = remoteMessage.getData();
            int displayDelayInSeconds = Utils.randomBetween(1, settingsManager.getPreference(PROPERTY_NOTIFICATION_DELAY_IN_SECONDS, 1800));

            Logger.logDebug(TAG, "Displaying push notification in " + displayDelayInSeconds + " second(s)");

            PersistableBundle taskData = new PersistableBundle();

            JobScheduler scheduler = (JobScheduler) getApplication().getSystemService(Context.JOB_SCHEDULER_SERVICE);

            if (scheduler == null) {
                Logger.logError(TAG, "Job scheduler service is not available");
                return;
            }

            Integer jobId = StreamSupport.stream(AVAILABLE_JOB_IDS)
                    .filter(id -> StreamSupport.stream(scheduler.getAllPendingJobs()).noneMatch(ji -> ji.getId() == id))
                    .findAny()
                    .orElseThrow(() -> new RuntimeException("There are too many notifications scheduled. Cannot schedule a new notification!"));

            taskData.putString(DelayedPushNotificationDisplayer.KEY_NOTIFICATION_CONTENTS, new ObjectMapper().writeValueAsString(messageContents));

            JobInfo.Builder task = new JobInfo.Builder(jobId, new ComponentName(getApplication(), DelayedPushNotificationDisplayer.class))
                    .setRequiresDeviceIdle(false)
                    .setRequiresCharging(false)
                    .setMinimumLatency(displayDelayInSeconds * 1000)
                    .setExtras(taskData);

            if (Build.VERSION.SDK_INT >= 26) {
                task.setRequiresBatteryNotLow(false);
                task.setRequiresStorageNotLow(false);
            }

            scheduler.schedule(task.build());
        } catch (Exception e) {
            Logger.context = (ApplicationData) getApplication();
            Logger.logError(TAG, "Error dispatching push notification: " + e);
        }
    }
}
