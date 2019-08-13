package com.arjanvlek.oxygenupdater;

import android.app.Activity;
import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import com.arjanvlek.oxygenupdater.domain.SystemVersionProperties;
import com.arjanvlek.oxygenupdater.internal.ThemeUtils;
import com.arjanvlek.oxygenupdater.internal.server.ServerConnector;
import com.arjanvlek.oxygenupdater.settings.SettingsManager;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;
import com.downloader.PRDownloader;
import com.downloader.PRDownloaderConfig;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.analytics.FirebaseAnalytics;

import io.fabric.sdk.android.Fabric;
import java8.util.stream.StreamSupport;

import static com.arjanvlek.oxygenupdater.internal.logger.Logger.logVerbose;
import static com.arjanvlek.oxygenupdater.views.AbstractFragment.ADS_TEST_DEVICES;

public class ApplicationData extends Application {

	public static final String NO_OXYGEN_OS = "no_oxygen_os_ver_found";
	public static final int NUMBER_OF_INSTALL_GUIDE_PAGES = 5;
	public static final String DEVICE_TOPIC_PREFIX = "device_";
	public static final String UPDATE_METHOD_TOPIC_PREFIX = "_update-method_";
	public static final String APP_USER_AGENT = "Oxygen_updater_" + BuildConfig.VERSION_NAME;
	public static final String LOCALE_DUTCH = "Nederlands";
	public static final String UNABLE_TO_FIND_A_MORE_RECENT_BUILD = "unable to find a more recent build";
	public static final String NETWORK_CONNECTION_ERROR = "NETWORK_CONNECTION_ERROR";
	public static final String SERVER_MAINTENANCE_ERROR = "SERVER_MAINTENANCE_ERROR";
	public static final String APP_OUTDATED_ERROR = "APP_OUTDATED_ERROR";
	public static final String PUSH_NOTIFICATION_CHANNEL_ID = "com.arjanvlek.oxygenupdater.notifications";
	public static final String PROGRESS_NOTIFICATION_CHANNEL_ID = "com.arjanvlek.oxygenupdater.progress";
	private static final String TAG = "ApplicationData";
	private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
	private ServerConnector serverConnector;
	private SystemVersionProperties systemVersionProperties;

	public static AdRequest buildAdRequest() {
		AdRequest.Builder adRequest = new AdRequest.Builder();

		StreamSupport.stream(ADS_TEST_DEVICES).forEach(adRequest::addTestDevice);
		return adRequest.build();
	}

	@Override
	public void onCreate() {
		AppCompatDelegate.setDefaultNightMode(ThemeUtils.translateThemeToNightMode(this));

		super.onCreate();
		setupCrashReporting();
		setupDownloader();
	}

	public ServerConnector getServerConnector() {
		if (serverConnector == null) {
			logVerbose(TAG, "Created ServerConnector for use within the application...");
			serverConnector = new ServerConnector(new SettingsManager(this));
		}
		return serverConnector;
	}

	public SystemVersionProperties getSystemVersionProperties() {
		// Store the system version properties in a cache, to prevent unnecessary calls to the native "getProp" command.
		if (systemVersionProperties == null) {
			logVerbose(TAG, "Creating new SystemVersionProperties instance...");
			systemVersionProperties = new SystemVersionProperties();
		} else {
			logVerbose(TAG, "Using cached instance of SystemVersionProperties");
		}
		return systemVersionProperties;
	}

	/**
	 * Checks if the Google Play Services are installed on the device.
	 *
	 * @return Returns if the Google Play Services are installed.
	 */
	public boolean checkPlayServices(Activity activity, boolean showErrorIfMissing) {
		logVerbose(TAG, "Executing Google Play Services check...");
		GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
		int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this);
		if (resultCode != ConnectionResult.SUCCESS && showErrorIfMissing) {
			if (googleApiAvailability.isUserResolvableError(resultCode)) {
				googleApiAvailability.getErrorDialog(activity, resultCode,
						PLAY_SERVICES_RESOLUTION_REQUEST).show();
			} else {
				System.exit(0);
			}
			logVerbose(TAG, "Google Play Services are *NOT* available! Ads and notifications are not supported!");
			return false;
		} else {
			boolean result = resultCode == ConnectionResult.SUCCESS;
			if (result) {
				logVerbose(TAG, "Google Play Services are available.");
			} else {
				logVerbose(TAG, "Google Play Services are *NOT* available! Ads and notifications are not supported!");
			}
			return result;
		}
	}


	private void setupCrashReporting() {
		SettingsManager settingsManager = new SettingsManager(this);

		// Do not upload crash logs if we are on a debug build or if the user has turned off analytics in the Settings screen.
		boolean shareAnalytics = settingsManager.getPreference(SettingsManager.PROPERTY_SHARE_ANALYTICS_AND_LOGS, true);
		boolean disableCrashCollection = BuildConfig.DEBUG || !shareAnalytics;

		// Do not share analytics data if the user has turned it off in the Settings screen
		FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(shareAnalytics);

		CrashlyticsCore crashlyticsCore = new CrashlyticsCore.Builder()
				.disabled(disableCrashCollection)
				.build();

		Crashlytics crashlytics = new Crashlytics.Builder()
				.core(crashlyticsCore)
				.build();

		Fabric.with(this, crashlytics);
	}

	private void setupDownloader() {
		PRDownloaderConfig config = PRDownloaderConfig.newBuilder()
				.setDatabaseEnabled(true)
				.setUserAgent(APP_USER_AGENT)
				.setConnectTimeout(30_000)
				.setReadTimeout(120_000)
				.build();

		PRDownloader.initialize(getApplicationContext(), config);
	}
}
