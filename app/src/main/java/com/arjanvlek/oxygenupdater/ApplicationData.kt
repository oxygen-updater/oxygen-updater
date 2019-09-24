package com.arjanvlek.oxygenupdater

import android.app.Activity
import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.arjanvlek.oxygenupdater.domain.SystemVersionProperties
import com.arjanvlek.oxygenupdater.internal.ThemeUtils
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logVerbose
import com.arjanvlek.oxygenupdater.internal.server.ServerConnector
import com.arjanvlek.oxygenupdater.settings.SettingsManager
import com.arjanvlek.oxygenupdater.views.AbstractFragment.Companion.ADS_TEST_DEVICES
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.core.CrashlyticsCore
import com.downloader.PRDownloader
import com.downloader.PRDownloaderConfig
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.analytics.FirebaseAnalytics
import io.fabric.sdk.android.Fabric
import java8.util.stream.StreamSupport
import kotlin.system.exitProcess

class ApplicationData : Application() {
    var mServerConnector: ServerConnector? = null
    var mSystemVersionProperties: SystemVersionProperties? = null

    override fun onCreate() {
        AppCompatDelegate.setDefaultNightMode(ThemeUtils.translateThemeToNightMode(this))

        super.onCreate()
        setupCrashReporting()
        setupDownloader()
    }

    fun getServerConnector(): ServerConnector {
        if (mServerConnector == null) {
            logVerbose(TAG, "Created ServerConnector for use within the application...")
            mServerConnector = ServerConnector(SettingsManager(this))
        }
        return mServerConnector as ServerConnector
    }

    fun getSystemVersionProperties(): SystemVersionProperties {
        // Store the system version properties in a cache, to prevent unnecessary calls to the native "getProp" command.
        if (mSystemVersionProperties == null) {
            logVerbose(TAG, "Creating new SystemVersionProperties instance...")
            mSystemVersionProperties = SystemVersionProperties()
        } else {
            logVerbose(TAG, "Using cached instance of SystemVersionProperties")
        }
        return mSystemVersionProperties as SystemVersionProperties
    }

    /**
     * Checks if the Google Play Services are installed on the device.
     *
     * @return Returns if the Google Play Services are installed.
     */
    fun checkPlayServices(activity: Activity, showErrorIfMissing: Boolean): Boolean {
        logVerbose(TAG, "Executing Google Play Services check...")
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)
        if (resultCode != ConnectionResult.SUCCESS && showErrorIfMissing) {
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                googleApiAvailability.getErrorDialog(activity, resultCode,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show()
            } else {
                exitProcess(0)
            }
            logVerbose(TAG, "Google Play Services are *NOT* available! Ads and notifications are not supported!")
            return false
        } else {
            val result = resultCode == ConnectionResult.SUCCESS
            if (result) {
                logVerbose(TAG, "Google Play Services are available.")
            } else {
                logVerbose(TAG, "Google Play Services are *NOT* available! Ads and notifications are not supported!")
            }
            return result
        }
    }


    private fun setupCrashReporting() {
        val settingsManager = SettingsManager(this)

        // Do not upload crash logs if we are on a debug build or if the user has turned off analytics in the Settings screen.
        val shareAnalytics = settingsManager.getPreference(SettingsManager.PROPERTY_SHARE_ANALYTICS_AND_LOGS, true)
        val disableCrashCollection = BuildConfig.DEBUG || !shareAnalytics

        // Do not share analytics data if the user has turned it off in the Settings screen
        FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(shareAnalytics)

        val crashlyticsCore = CrashlyticsCore.Builder()
                .disabled(disableCrashCollection)
                .build()

        val crashlytics = Crashlytics.Builder()
                .core(crashlyticsCore)
                .build()

        Fabric.with(this, crashlytics)
    }

    private fun setupDownloader() {
        val config = PRDownloaderConfig.newBuilder()
                .setDatabaseEnabled(true)
                .setUserAgent(APP_USER_AGENT)
                .setConnectTimeout(30000)
                .setReadTimeout(120000)
                .build()

        PRDownloader.initialize(applicationContext, config)
    }

    companion object {
        const val NO_OXYGEN_OS = "no_oxygen_os_ver_found"
        const val NUMBER_OF_INSTALL_GUIDE_PAGES = 5
        const val DEVICE_TOPIC_PREFIX = "device_"
        const val UPDATE_METHOD_TOPIC_PREFIX = "_update-method_"
        const val APP_USER_AGENT = "Oxygen_updater_" + BuildConfig.VERSION_NAME
        const val LOCALE_DUTCH = "Nederlands"
        const val UNABLE_TO_FIND_A_MORE_RECENT_BUILD = "unable to find a more recent build"
        const val NETWORK_CONNECTION_ERROR = "NETWORK_CONNECTION_ERROR"
        const val SERVER_MAINTENANCE_ERROR = "SERVER_MAINTENANCE_ERROR"
        const val APP_OUTDATED_ERROR = "APP_OUTDATED_ERROR"
        const val PUSH_NOTIFICATION_CHANNEL_ID = "com.arjanvlek.oxygenupdater.notifications"
        const val PROGRESS_NOTIFICATION_CHANNEL_ID = "com.arjanvlek.oxygenupdater.progress"
        private const val TAG = "ApplicationData"
        private const val PLAY_SERVICES_RESOLUTION_REQUEST = 9000

        fun buildAdRequest(): AdRequest {
            val adRequest = AdRequest.Builder()

            StreamSupport.stream(ADS_TEST_DEVICES).forEach { adRequest.addTestDevice(it) }
            return adRequest.build()
        }
    }
}
