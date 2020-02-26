package com.arjanvlek.oxygenupdater

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.provider.Settings
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.MutableLiveData
import com.arjanvlek.oxygenupdater.internal.server.ServerConnector
import com.arjanvlek.oxygenupdater.internal.settings.SettingsManager
import com.arjanvlek.oxygenupdater.models.SystemVersionProperties
import com.arjanvlek.oxygenupdater.utils.Logger.logVerbose
import com.arjanvlek.oxygenupdater.utils.MD5
import com.arjanvlek.oxygenupdater.utils.ThemeUtils
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.core.CrashlyticsCore
import com.downloader.PRDownloader
import com.downloader.PRDownloaderConfig
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.common.ConnectionResult.SUCCESS
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.analytics.FirebaseAnalytics
import io.fabric.sdk.android.Fabric
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.fragment.koin.fragmentFactory
import org.koin.core.context.startKoin
import java.util.*
import kotlin.system.exitProcess

class OxygenUpdater : Application() {

    val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onLost(network: Network) {
            isNetworkAvailable.postValue(false)
        }

        override fun onAvailable(network: Network) {
            isNetworkAvailable.postValue(true)
        }
    }

    var serverConnector: ServerConnector? = null
        get() {
            if (field == null) {
                logVerbose(TAG, "Created ServerConnector for use within the application...")
                field = ServerConnector(SettingsManager(this))
            }

            return field
        }

    // Store the system version properties in a cache, to prevent unnecessary calls to the native "getProp" command.
    var systemVersionProperties: SystemVersionProperties? = null
        get() {
            // Store the system version properties in a cache, to prevent unnecessary calls to the native "getProp" command.
            if (field == null) {
                logVerbose(TAG, "Creating new SystemVersionProperties instance...")
                field = SystemVersionProperties()
            } else {
                logVerbose(TAG, "Using cached instance of SystemVersionProperties")
            }

            return field
        }

    override fun onCreate() {
        AppCompatDelegate.setDefaultNightMode(ThemeUtils.translateThemeToNightMode(this))
        super.onCreate()

        setupCrashReporting()
        setupKoin()
        setupNetworkCallback()
        setupMobileAds()
        setupDownloader()

        // If it's a debug build, add current device's ID to the list of test device IDs for ads
        if (BuildConfig.DEBUG) {
            @SuppressLint("HardwareIds")
            val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
            val deviceId: String = MD5.calculateMD5(androidId).toUpperCase(Locale.getDefault())
            ADS_TEST_DEVICES.add(deviceId)
        }
    }

    private fun setupKoin() {
        startKoin {
            // use AndroidLogger as Koin Logger - default Level.INFO
            androidLogger()
            // use the Android context given there
            androidContext(this@OxygenUpdater)
            // setup a KoinFragmentFactory instance
            fragmentFactory()
            // module list
            modules(allModules)
        }
    }

    private fun setupNetworkCallback() {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        connectivityManager.registerNetworkCallback(
            NetworkRequest.Builder().build(),
            networkCallback
        )
    }

    private fun setupMobileAds() {
        MobileAds.initialize(this, getString(R.string.advertising_app_id))
    }

    /**
     * Checks if the Google Play Services are installed on the device.
     *
     * @return Returns if the Google Play Services are installed.
     */
    fun checkPlayServices(activity: Activity?, showErrorIfMissing: Boolean): Boolean {
        logVerbose(TAG, "Executing Google Play Services check...")

        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)

        return if (resultCode != SUCCESS && showErrorIfMissing) {
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                googleApiAvailability.getErrorDialog(activity, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST).show()
            } else {
                exitProcess(0)
            }

            logVerbose(TAG, "Google Play Services are *NOT* available! Ads and notifications are not supported!")
            false
        } else {
            val result = resultCode == SUCCESS

            if (result) {
                logVerbose(TAG, "Google Play Services are available.")
            } else {
                logVerbose(TAG, "Google Play Services are *NOT* available! Ads and notifications are not supported!")
            }

            result
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

    @Suppress("unused")
    companion object {
        private const val TAG = "OxygenUpdater"
        private const val PLAY_SERVICES_RESOLUTION_REQUEST = 9000

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

        val isNetworkAvailable = MutableLiveData<Boolean>()

        fun buildAdRequest(): AdRequest = AdRequest.Builder().apply {
            ADS_TEST_DEVICES.forEach { addTestDevice(it) }
        }.build()
    }
}
