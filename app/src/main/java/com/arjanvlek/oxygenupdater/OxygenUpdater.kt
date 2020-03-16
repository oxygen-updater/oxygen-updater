package com.arjanvlek.oxygenupdater

import android.annotation.SuppressLint
import android.app.Application
import android.net.ConnectivityManager
import android.net.NetworkRequest
import android.os.Build
import android.provider.Settings
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.getSystemService
import com.arjanvlek.oxygenupdater.internal.settings.SettingsManager
import com.arjanvlek.oxygenupdater.utils.MD5
import com.arjanvlek.oxygenupdater.utils.ThemeUtils
import com.arjanvlek.oxygenupdater.utils.networkCallback
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.core.CrashlyticsCore
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.firebase.analytics.FirebaseAnalytics
import io.fabric.sdk.android.Fabric
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import java.util.*

class OxygenUpdater : Application() {

    override fun onCreate() {
        setupKoin()
        AppCompatDelegate.setDefaultNightMode(ThemeUtils.translateThemeToNightMode(this))
        super.onCreate()

        setupCrashReporting()
        setupNetworkCallback()
        setupMobileAds()
    }

    private fun setupKoin() {
        startKoin {
            // use AndroidLogger as Koin Logger - default Level.INFO
            androidLogger()
            // use the Android context given there
            androidContext(this@OxygenUpdater)
            // module list
            modules(allModules)
        }
    }

    private fun setupNetworkCallback() {
        getSystemService<ConnectivityManager>()?.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                registerDefaultNetworkCallback(networkCallback)
            } else {
                registerNetworkCallback(
                    NetworkRequest.Builder().build(),
                    networkCallback
                )
            }
        }
    }

    private fun setupMobileAds() {
        // If it's a debug build, add current device's ID to the list of test device IDs for ads
        if (BuildConfig.DEBUG) {
            @SuppressLint("HardwareIds")
            val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
            val deviceId: String = MD5.calculateMD5(androidId).toUpperCase(Locale.getDefault())
            ADS_TEST_DEVICES.add(deviceId)
        }

        val requestConfiguration = RequestConfiguration.Builder()
            .setTestDeviceIds(ADS_TEST_DEVICES)
            .build()

        MobileAds.initialize(this, getString(R.string.advertising_app_id))
        MobileAds.setRequestConfiguration(requestConfiguration)
    }

    private fun setupCrashReporting() {
        val settingsManager by inject<SettingsManager>()

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

    @Suppress("unused")
    companion object {
        private const val TAG = "OxygenUpdater"

        // Test devices for ads.
        private val ADS_TEST_DEVICES = mutableListOf("B5EB6278CE611E4A14FCB2E2DDF48993", "AA361A327964F1B961D98E98D8BB9843")

        const val NO_OXYGEN_OS = "no_oxygen_os_ver_found"
        const val NUMBER_OF_INSTALL_GUIDE_PAGES = 5
        const val APP_USER_AGENT = "Oxygen_updater_" + BuildConfig.VERSION_NAME
        const val UNABLE_TO_FIND_A_MORE_RECENT_BUILD = "unable to find a more recent build"
        const val NETWORK_CONNECTION_ERROR = "NETWORK_CONNECTION_ERROR"
        const val SERVER_MAINTENANCE_ERROR = "SERVER_MAINTENANCE_ERROR"
        const val APP_OUTDATED_ERROR = "APP_OUTDATED_ERROR"
        const val PUSH_NOTIFICATION_CHANNEL_ID = "com.arjanvlek.oxygenupdater.internal.notifications"
        const val PROGRESS_NOTIFICATION_CHANNEL_ID = "com.arjanvlek.oxygenupdater.progress"

        fun buildAdRequest(): AdRequest = AdRequest.Builder().build()
    }
}
