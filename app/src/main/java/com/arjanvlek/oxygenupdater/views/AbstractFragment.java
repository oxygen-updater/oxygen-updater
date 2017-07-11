package com.arjanvlek.oxygenupdater.views;

import android.support.v4.app.Fragment;

import com.arjanvlek.oxygenupdater.ApplicationData;
import com.arjanvlek.oxygenupdater.internal.server.ServerConnector;
import com.arjanvlek.oxygenupdater.internal.logger.Logger;
import com.arjanvlek.oxygenupdater.settings.SettingsManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public abstract class AbstractFragment extends Fragment {

    private ApplicationData applicationData;
    private SettingsManager settingsManager;

    //Test devices for ads.
    public static final List<String> ADS_TEST_DEVICES = Arrays.asList("BE7E0AF85E0332807B1EA3FE4236F93C", "0FD2DE005EB9DD19BD02FB2CD4D87902");

    public ApplicationData getApplicationData() {
        if (applicationData == null) {
            try {
                applicationData = (ApplicationData) getActivity().getApplication();
            } catch (Exception e) {
                Logger.logError("AbstractFragment", "FAILED to get application data: ", e);
                // Return empty application data which can still be used for SystemVersionProperties and to check for root access.
                applicationData = new ApplicationData();
            }
        }
        return applicationData;
    }

    public ServerConnector getServerConnector() {
        if(applicationData == null) {
            applicationData = (ApplicationData) getActivity().getApplication();
        }
        return applicationData.getServerConnector();
    }

    public SettingsManager getSettingsManager() {
        if(applicationData == null) {
            applicationData = (ApplicationData) getActivity().getApplication();
        }

        if(settingsManager == null) {
            settingsManager = new SettingsManager(applicationData);
        }

        return settingsManager;
    }
}
