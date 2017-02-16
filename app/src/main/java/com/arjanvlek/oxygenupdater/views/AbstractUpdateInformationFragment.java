package com.arjanvlek.oxygenupdater.views;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.text.Html;

import com.arjanvlek.oxygenupdater.BuildConfig;
import com.arjanvlek.oxygenupdater.Model.Banner;
import com.arjanvlek.oxygenupdater.Model.OxygenOTAUpdate;
import com.arjanvlek.oxygenupdater.Model.ServerResult;
import com.arjanvlek.oxygenupdater.Model.ServerStatus;

import com.arjanvlek.oxygenupdater.Model.SystemVersionProperties;
import com.arjanvlek.oxygenupdater.R;
import com.arjanvlek.oxygenupdater.Support.Callback;
import com.arjanvlek.oxygenupdater.Support.NetworkConnectionManager;
import com.arjanvlek.oxygenupdater.Support.SettingsManager;

import java.util.ArrayList;
import java.util.List;

import static com.arjanvlek.oxygenupdater.Model.ServerStatus.Status.UNREACHABLE;
import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_DEVICE_ID;
import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_SHOW_APP_UPDATE_MESSAGES;
import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_SHOW_NEWS_MESSAGES;
import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_UPDATE_METHOD_ID;

public abstract class AbstractUpdateInformationFragment extends AbstractFragment {

    protected SettingsManager settingsManager;
    protected NetworkConnectionManager networkConnectionManager;
    protected OxygenOTAUpdate oxygenOTAUpdate;

    public static final String UNABLE_TO_FIND_A_MORE_RECENT_BUILD = "unable to find a more recent build";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settingsManager = new SettingsManager(getActivity().getApplicationContext());
        networkConnectionManager = new NetworkConnectionManager(getActivity().getApplicationContext());
    }

    protected class GetServerData extends AsyncTask<Callback, Void, ServerResult> {

        @Override
        protected ServerResult doInBackground(Callback... callbacks) {
            ServerResult serverResult = new ServerResult();
            serverResult.setCallback(callbacks[0]);
            serverResult.setServerStatus(getApplicationContext().getServerConnector().getServerStatus());

            Long deviceId = settingsManager.getPreference(PROPERTY_DEVICE_ID);
            Long updateMethodId = settingsManager.getPreference(PROPERTY_UPDATE_METHOD_ID);

            serverResult.setServerMessages(getApplicationContext().getServerConnector().getServerMessages(deviceId, updateMethodId));


            SystemVersionProperties systemVersionProperties = getApplicationContext().getSystemVersionProperties();
            OxygenOTAUpdate oxygenOTAUpdate = getApplicationContext().getServerConnector().getOxygenOTAUpdate(deviceId, updateMethodId, systemVersionProperties.getOxygenOSOTAVersion());
            if (oxygenOTAUpdate != null) {
                if(oxygenOTAUpdate.getInformation() != null && oxygenOTAUpdate.getInformation().equals(UNABLE_TO_FIND_A_MORE_RECENT_BUILD) && oxygenOTAUpdate.isUpdateInformationAvailable() && oxygenOTAUpdate.isSystemIsUpToDateCheck()) {
                    oxygenOTAUpdate = getApplicationContext().getServerConnector().getMostRecentOxygenOTAUpdate(deviceId, updateMethodId);
                }

            } else {
                if (settingsManager.checkIfCacheIsAvailable()) {
                    oxygenOTAUpdate = buildOfflineOxygenOTAUpdate();
                } else {
                    showNoNetworkConnectionError();
                    oxygenOTAUpdate = null;
                }
            }
            serverResult.setUpdateData(oxygenOTAUpdate);
            return serverResult;
        }

        @Override
        protected void onPostExecute(final ServerResult serverResult) {
            List<Banner> inAppBars = new ArrayList<>();

            // Add the "No connection" bar depending on the network status of the device.
            boolean online = networkConnectionManager.checkNetworkConnection();
            if(!online) {
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

            if(serverResult.getServerMessages() != null && settingsManager.getPreference(PROPERTY_SHOW_NEWS_MESSAGES, true)) {
                inAppBars.addAll(serverResult.getServerMessages());
            }

            if(serverResult.getServerStatus() == null) {
                ServerStatus serverStatus = new ServerStatus();
                serverStatus.setStatus(UNREACHABLE);
                serverStatus.setLatestAppVersion(BuildConfig.VERSION_NAME);
                serverResult.setServerStatus(serverStatus);
            }

            ServerStatus.Status status = serverResult.getServerStatus().getStatus();

            if(status.isUserRecoverableError()) {
                inAppBars.add(serverResult.getServerStatus());
            }

            if(status.isNonRecoverableError()) {
                switch (status) {
                    case MAINTENANCE:
                        showServerMaintenanceError();
                        break;
                    case OUTDATED:
                        showAppOutdatedError();
                        break;
                    default:
                }
            }

            if(settingsManager.getPreference(PROPERTY_SHOW_APP_UPDATE_MESSAGES, true) && !checkIfAppIsUpToDate(serverResult.getServerStatus().getLatestAppVersion())) {
                inAppBars.add(new Banner() {

                    @Override
                    public CharSequence getBannerText(Context context) {
                        //noinspection deprecation Suggested fix requires API level 24, which is too new for this app.
                        return Html.fromHtml(String.format(getString(R.string.new_app_version), serverResult.getServerStatus().getLatestAppVersion()));
                    }

                    @Override
                    public int getColor(Context context) {
                        return ContextCompat.getColor(context, R.color.holo_green_light);
                    }
                });
            }

            serverResult.getCallback().onActionPerformed(new ServerResultCallbackData(serverResult.getUpdateData(), online, inAppBars));
        }
    }

    protected class ServerResultCallbackData {
        private final OxygenOTAUpdate oxygenOTAUpdate;
        private final boolean online;
        private final List<Banner> inAppBars;

        ServerResultCallbackData(OxygenOTAUpdate oxygenOTAUpdate, boolean online, List<Banner> inAppBars) {
            this.oxygenOTAUpdate = oxygenOTAUpdate;
            this.online = online;
            this.inAppBars = inAppBars;
        }

        List<Banner> getInAppBars() {
            return inAppBars;
        }

        boolean isOnline() {
            return online;
        }

        OxygenOTAUpdate getOxygenOTAUpdate() {
            return oxygenOTAUpdate;
        }
    }


    protected abstract OxygenOTAUpdate buildOfflineOxygenOTAUpdate();

    protected void showNoNetworkConnectionError() {
        MessageDialog errorDialog = new MessageDialog()
                .setTitle(getString(R.string.error_app_requires_network_connection))
                .setMessage(getString(R.string.error_app_requires_network_connection_message))
                .setNegativeButtonText(getString(R.string.download_error_close))
                .setClosable(false);
        errorDialog.setTargetFragment(this, 0);
        errorDialog.show(getFragmentManager(), "NetworkError");
    }

    protected void showServerMaintenanceError() {
        MessageDialog serverMaintenanceErrorFragment = new MessageDialog()
                .setTitle(getString(R.string.error_maintenance))
                .setMessage(getString(R.string.error_maintenance_message))
                .setNegativeButtonText(getString(R.string.download_error_close))
                .setClosable(false);
        serverMaintenanceErrorFragment.setTargetFragment(this, 0);
        serverMaintenanceErrorFragment.show(getFragmentManager(), "MaintenanceError");
    }

    protected void showAppOutdatedError() {
        MessageDialog appOutdatedErrorFragment = new MessageDialog()
                .setTitle(getString(R.string.error_app_outdated))
                .setMessage(getString(R.string.error_app_outdated_message))
                .setPositiveButtonText(getString(R.string.error_google_play_button_text))
                .setNegativeButtonText(getString(R.string.download_error_close))
                .setClosable(false)
                .setDialogListener(new MessageDialog.DialogListener() {
                    @Override
                    public void onDialogPositiveButtonClick(DialogFragment dialogFragment) {
                        try {
                            final String appPackageName = BuildConfig.APPLICATION_ID;
                            try {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                            } catch (ActivityNotFoundException e) {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                            }
                        } catch (Exception ignored) {

                        }
                    }

                    @Override
                    public void onDialogNegativeButtonClick(DialogFragment dialogFragment) {

                    }
        });
        appOutdatedErrorFragment.setTargetFragment(this, 0);
        appOutdatedErrorFragment.show(getFragmentManager(), "AppOutdatedError");
    }

    protected boolean checkIfAppIsUpToDate(String appVersionFromResult) {
        String appVersion = BuildConfig.VERSION_NAME;
        appVersion = appVersion.replace(".", "");
        appVersionFromResult = appVersionFromResult.replace(".", "");
        try {
            int appVersionNumeric = Integer.parseInt(appVersion);
            int appVersionFromResultNumeric = Integer.parseInt(appVersionFromResult);
            return appVersionFromResultNumeric <= appVersionNumeric;
        } catch(Exception e) {
            return true;
        }
    }
}
