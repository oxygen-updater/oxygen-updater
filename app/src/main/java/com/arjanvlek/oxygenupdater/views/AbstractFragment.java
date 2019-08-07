package com.arjanvlek.oxygenupdater.views;

import androidx.fragment.app.Fragment;

import com.arjanvlek.oxygenupdater.ApplicationData;
import com.arjanvlek.oxygenupdater.internal.server.ServerConnector;
import com.arjanvlek.oxygenupdater.settings.SettingsManager;

import java.util.Arrays;
import java.util.List;

import static com.arjanvlek.oxygenupdater.internal.logger.Logger.logError;


public abstract class AbstractFragment extends Fragment {

	//Test devices for ads.
	public static final List<String> ADS_TEST_DEVICES = Arrays.asList("BE7E0AF85E0332807B1EA3FE4236F93C", "0FD2DE005EB9DD19BD02FB2CD4D87902");
	private ApplicationData applicationData;
	private SettingsManager settingsManager;

	public ApplicationData getApplicationData() {
		if (applicationData == null) {
			try {
				applicationData = (ApplicationData) getActivity().getApplication();
			} catch (Exception e) {
				logError("AbstractFragment", "FAILED to get Application instance", e);
				// Return empty application data which can still be used for SystemVersionProperties and to check for root access.
				applicationData = new ApplicationData();
			}
		}
		return applicationData;
	}

	public ServerConnector getServerConnector() {
		if (applicationData == null && getActivity() != null) {
			applicationData = (ApplicationData) getActivity().getApplication();
		}
		return applicationData != null ? applicationData.getServerConnector() : new ServerConnector(new SettingsManager(null));
	}

	public SettingsManager getSettingsManager() {
		if (applicationData == null && getActivity() != null) {
			applicationData = (ApplicationData) getActivity().getApplication();
		}

		if (settingsManager == null) {
			settingsManager = new SettingsManager(applicationData);
		}

		return settingsManager;
	}
}
