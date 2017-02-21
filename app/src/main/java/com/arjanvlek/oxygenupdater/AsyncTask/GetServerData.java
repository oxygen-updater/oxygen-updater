package com.arjanvlek.oxygenupdater.AsyncTask;


import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.Html;

import com.arjanvlek.oxygenupdater.BuildConfig;
import com.arjanvlek.oxygenupdater.Model.Banner;
import com.arjanvlek.oxygenupdater.Model.OxygenOTAUpdate;
import com.arjanvlek.oxygenupdater.Model.ServerStatus;
import com.arjanvlek.oxygenupdater.Model.SystemVersionProperties;
import com.arjanvlek.oxygenupdater.R;
import com.arjanvlek.oxygenupdater.Server.ProcessedServerResult;
import com.arjanvlek.oxygenupdater.Server.ServerResult;
import com.arjanvlek.oxygenupdater.Support.Callback;
import com.arjanvlek.oxygenupdater.Support.NetworkConnectionManager;
import com.arjanvlek.oxygenupdater.Support.SettingsManager;
import com.arjanvlek.oxygenupdater.notifications.Dialogs;
import com.arjanvlek.oxygenupdater.views.AbstractFragment;

import java.util.ArrayList;
import java.util.List;

import static com.arjanvlek.oxygenupdater.Model.ServerStatus.Status.UNREACHABLE;
import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_DEVICE_ID;
import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_OFFLINE_FILE_NAME;
import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_OFFLINE_UPDATE_DESCRIPTION;
import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_OFFLINE_UPDATE_DOWNLOAD_SIZE;
import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_OFFLINE_UPDATE_INFORMATION_AVAILABLE;
import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_OFFLINE_UPDATE_NAME;
import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_SHOW_APP_UPDATE_MESSAGES;
import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_SHOW_NEWS_MESSAGES;
import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_UPDATE_METHOD_ID;

public class GetServerData extends AsyncTask<Void, Void, ServerResult> {

    private final Callback<ProcessedServerResult> callback;
    private final AbstractFragment fragment;
    private final SettingsManager settingsManager;
    private final boolean online;

    private static final String UNABLE_TO_FIND_A_MORE_RECENT_BUILD = "unable to find a more recent build";

    public GetServerData(AbstractFragment fragment, SettingsManager settingsManager, boolean online, Callback<ProcessedServerResult> callback) {
        this.callback = callback;
        this.fragment = fragment;
        this.settingsManager = settingsManager;
        this.online = online;
    }

    @Override
    protected ServerResult doInBackground(Void... callbacks) {
        ServerResult serverResult = new ServerResult();
        serverResult.setServerStatus(fragment.getApplicationContext().getServerConnector().getServerStatus());

        Long deviceId = settingsManager.getPreference(PROPERTY_DEVICE_ID);
        Long updateMethodId = settingsManager.getPreference(PROPERTY_UPDATE_METHOD_ID);

        serverResult.setServerMessages(fragment.getApplicationContext().getServerConnector().getServerMessages(deviceId, updateMethodId));


        SystemVersionProperties systemVersionProperties = fragment.getApplicationContext().getSystemVersionProperties();
        OxygenOTAUpdate oxygenOTAUpdate = fragment.getApplicationContext().getServerConnector().getOxygenOTAUpdate(deviceId, updateMethodId, systemVersionProperties.getOxygenOSOTAVersion());
        if (oxygenOTAUpdate != null && oxygenOTAUpdate.getInformation() != null && oxygenOTAUpdate.getInformation().equals(UNABLE_TO_FIND_A_MORE_RECENT_BUILD) && oxygenOTAUpdate.isUpdateInformationAvailable() && oxygenOTAUpdate.isSystemIsUpToDate()) {
            oxygenOTAUpdate = fragment.getApplicationContext().getServerConnector().getMostRecentOxygenOTAUpdate(deviceId, updateMethodId);
        } else if (!online) {
            if (settingsManager.checkIfCacheIsAvailable()) {
                oxygenOTAUpdate = new OxygenOTAUpdate();
                oxygenOTAUpdate.setVersionNumber(settingsManager.getPreference(PROPERTY_OFFLINE_UPDATE_NAME));
                oxygenOTAUpdate.setDownloadSize(settingsManager.getPreference(PROPERTY_OFFLINE_UPDATE_DOWNLOAD_SIZE));
                oxygenOTAUpdate.setDescription(settingsManager.getPreference(PROPERTY_OFFLINE_UPDATE_DESCRIPTION));
                oxygenOTAUpdate.setUpdateInformationAvailable(settingsManager.getPreference(PROPERTY_OFFLINE_UPDATE_INFORMATION_AVAILABLE));
                oxygenOTAUpdate.setFilename(settingsManager.getPreference(PROPERTY_OFFLINE_FILE_NAME));
            } else {
                Dialogs.showNoNetworkConnectionError(fragment.getParentFragment());
            }
        }
        serverResult.setUpdateData(oxygenOTAUpdate != null ? oxygenOTAUpdate : new OxygenOTAUpdate());
        return serverResult;
    }

    @Override
    protected void onPostExecute(final ServerResult serverResult) {
        List<Banner> inAppBars = new ArrayList<>();

        // Add the "No connection" bar depending on the network status of the device.
        if (!online) {
            inAppBars.add(new Banner() {
                @Override
                public String getBannerText(Context context) {
                    return context.getString(R.string.error_no_internet_connection);
                }

                @Override
                public int getColor(Context context) {
                    return ContextCompat.getColor(context, R.color.holo_red_light);
                }
            });
        }

        if (serverResult.getServerMessages() != null && settingsManager.getPreference(PROPERTY_SHOW_NEWS_MESSAGES, true)) {
            inAppBars.addAll(serverResult.getServerMessages());
        }

        if (serverResult.getServerStatus() == null) {
            ServerStatus serverStatus = new ServerStatus();
            serverStatus.setStatus(UNREACHABLE);
            serverStatus.setLatestAppVersion(BuildConfig.VERSION_NAME);
            serverResult.setServerStatus(serverStatus);
        }

        ServerStatus.Status status = serverResult.getServerStatus().getStatus();

        if (status.isUserRecoverableError()) {
            inAppBars.add(serverResult.getServerStatus());
        }

        if (status.isNonRecoverableError()) {
            switch (status) {
                case MAINTENANCE:
                    Dialogs.showServerMaintenanceError(fragment.getParentFragment());
                    break;
                case OUTDATED:
                    Dialogs.showAppOutdatedError(fragment.getParentFragment());
                    break;
            }
        }

        if (settingsManager.getPreference(PROPERTY_SHOW_APP_UPDATE_MESSAGES, true) && !serverResult.getServerStatus().checkIfAppIsUpToDate()) {
            inAppBars.add(new Banner() {

                @Override
                public CharSequence getBannerText(Context context) {
                    //noinspection deprecation Suggested fix requires API level 24, which is too new for this app, or an ugly if-else statement.
                    return Html.fromHtml(String.format(fragment.getString(R.string.new_app_version), serverResult.getServerStatus().getLatestAppVersion()));
                }

                @Override
                public int getColor(Context context) {
                    return ContextCompat.getColor(context, R.color.holo_green_light);
                }
            });
        }

        callback.onActionPerformed(new ProcessedServerResult(serverResult.getUpdateData(), online, inAppBars));
    }
}
