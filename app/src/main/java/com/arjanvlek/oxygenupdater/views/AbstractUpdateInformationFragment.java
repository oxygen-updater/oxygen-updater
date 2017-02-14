package com.arjanvlek.oxygenupdater.views;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import com.arjanvlek.oxygenupdater.BuildConfig;
import com.arjanvlek.oxygenupdater.Model.OxygenOTAUpdate;
import com.arjanvlek.oxygenupdater.Model.ServerMessage;
import com.arjanvlek.oxygenupdater.Model.ServerStatus;

import com.arjanvlek.oxygenupdater.Model.SystemVersionProperties;
import com.arjanvlek.oxygenupdater.R;
import com.arjanvlek.oxygenupdater.Support.NetworkConnectionManager;
import com.arjanvlek.oxygenupdater.Support.SettingsManager;

import java.util.List;

import static com.arjanvlek.oxygenupdater.Support.SettingsManager.PROPERTY_DEVICE_ID;
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

    protected abstract void displayServerStatus(ServerStatus serverStatus);

    protected abstract void displayServerMessages(List<ServerMessage> serverMessages);

    protected abstract void displayUpdateInformation(OxygenOTAUpdate oxygenOTAUpdate, boolean online, boolean force);

    protected abstract void initDownloadManager();

    protected abstract void checkIfUpdateIsAlreadyDownloaded(OxygenOTAUpdate oxygenOTAUpdate);

    protected abstract OxygenOTAUpdate buildOfflineOxygenOTAUpdate();

    protected class GetServerStatus extends AsyncTask<Void, Void, ServerStatus> {

        @Override
        protected ServerStatus doInBackground(Void... arg0) {
            return getApplicationContext().getServerConnector().getServerStatus();
        }

        @Override
        protected void onPostExecute(ServerStatus serverStatus) {
            displayServerStatus(serverStatus);
        }
    }

    protected class GetServerMessages extends  AsyncTask<Void, Void, List<ServerMessage>> {

        @Override
        protected List<ServerMessage> doInBackground(Void... arg0) {
            return getApplicationContext().getServerConnector().getServerMessages((Long)settingsManager.getPreference(PROPERTY_DEVICE_ID), (Long)settingsManager.getPreference(PROPERTY_UPDATE_METHOD_ID));
        }

        @Override
        protected void onPostExecute(List<ServerMessage> serverMessages) {
            displayServerMessages(serverMessages);
        }
    }


    protected class GetUpdateInformation extends AsyncTask<Void, Void, OxygenOTAUpdate> {

        @Override
        protected OxygenOTAUpdate doInBackground(Void... arg0) {
            SystemVersionProperties systemVersionProperties = getApplicationContext().getSystemVersionProperties();
            OxygenOTAUpdate oxygenOTAUpdate = getApplicationContext().getServerConnector().getOxygenOTAUpdate((Long)settingsManager.getPreference(PROPERTY_DEVICE_ID), (Long)settingsManager.getPreference(PROPERTY_UPDATE_METHOD_ID), systemVersionProperties.getOxygenOSOTAVersion());
            if (oxygenOTAUpdate != null) {
                if(oxygenOTAUpdate.getInformation() != null && oxygenOTAUpdate.getInformation().equals(UNABLE_TO_FIND_A_MORE_RECENT_BUILD) && oxygenOTAUpdate.isUpdateInformationAvailable() && oxygenOTAUpdate.isSystemIsUpToDateCheck()) {
                    oxygenOTAUpdate = getApplicationContext().getServerConnector().getMostRecentOxygenOTAUpdate((Long)settingsManager.getPreference(PROPERTY_DEVICE_ID), (Long) settingsManager.getPreference(PROPERTY_UPDATE_METHOD_ID));
                }
                return oxygenOTAUpdate;

            } else {
                if (settingsManager.checkIfCacheIsAvailable()) {
                    return buildOfflineOxygenOTAUpdate();
                } else {
                    showNetworkError();
                    return null;
                }
            }
        }

        @Override
        protected void onPostExecute(OxygenOTAUpdate result) {
            super.onPostExecute(result);
            oxygenOTAUpdate = result;
            displayUpdateInformation(result, networkConnectionManager.checkNetworkConnection(), false);
            initDownloadManager();
            checkIfUpdateIsAlreadyDownloaded(oxygenOTAUpdate);
        }
    }

    protected void showNetworkError() {
        MessageDialog errorDialog = new MessageDialog()
                .setTitle(getString(R.string.error_app_requires_network_connection))
                .setMessage(getString(R.string.error_app_requires_network_connection_message))
                .setNegativeButtonText(getString(R.string.download_error_close))
                .setClosable(false);
        errorDialog.setTargetFragment(this, 0);
        errorDialog.show(getFragmentManager(), "NetworkError");
    }

    protected void showMaintenanceError() {
        MessageDialog serverMaintenanceErrorFragment = new MessageDialog()
                .setTitle(getString(R.string.error_maintenance))
                .setMessage(getString(R.string.error_maintenance_message))
                .setNegativeButtonText(getString(R.string.download_error_close))
                .setClosable(false);
        serverMaintenanceErrorFragment.setTargetFragment(this, 0);
        serverMaintenanceErrorFragment.show(getFragmentManager(), "MaintenanceError");
    }

    protected void showAppNotValidError() {
        MessageDialog appNotValidErrorFragment = new MessageDialog()
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
        appNotValidErrorFragment.setTargetFragment(this, 0);
        appNotValidErrorFragment.show(getFragmentManager(), "AppNotValidError");
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
