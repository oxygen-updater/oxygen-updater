package com.oxygenupdater

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.os.Build
import android.provider.Settings
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.getSystemService
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.jakewharton.threetenabp.AndroidThreeTen
import com.oxygenupdater.extensions.attachWithLocale
import com.oxygenupdater.internal.settings.SettingsManager
import com.oxygenupdater.utils.MD5
import com.oxygenupdater.utils.ThemeUtils
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import java.util.*

class OxygenUpdater : Application() {

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {

        /**
         * This callback is used in both [ConnectivityManager.registerDefaultNetworkCallback] and [ConnectivityManager.registerNetworkCallback].
         *
         * The former can be used only in API 24 and above, while the latter is recommended for API 21+.
         * The former is more robust, as it presents an accurate network availability status for all connections,
         * while the latter only works for the [NetworkRequest] that's passed into the function.
         *
         * This has the undesired effect of marking the network connection as "lost" after a period of time.
         * To combat this on older API levels, we're using the deprecated API to confirm the network connectivity status.
         */
        override fun onLost(network: Network) {
            @Suppress("DEPRECATION")
            val networkAvailability = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                getSystemService<ConnectivityManager>()
                    ?.activeNetworkInfo
                    ?.isConnectedOrConnecting == true
            } else {
                false
            }

            _isNetworkAvailable.postValue(networkAvailability)
        }

        override fun onAvailable(network: Network) {
            _isNetworkAvailable.postValue(true)
        }
    }

    override fun attachBaseContext(
        base: Context
    ) = super.attachBaseContext(base.attachWithLocale())

    override fun onCreate() {
        setupKoin()
        AppCompatDelegate.setDefaultNightMode(ThemeUtils.translateThemeToNightMode(this))
        super.onCreate()
        AndroidThreeTen.init(this)

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
            // Posting initial value is required, as [networkCallback]'s
            // methods get called only when network connectivity changes
            @Suppress("DEPRECATION")
            _isNetworkAvailable.postValue(activeNetworkInfo?.isConnectedOrConnecting == true)

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
            val deviceId = MD5.calculateMD5(androidId).toUpperCase(Locale.getDefault())
            ADS_TEST_DEVICES.add(deviceId)
        }

        val requestConfiguration = RequestConfiguration.Builder()
            .setTestDeviceIds(ADS_TEST_DEVICES)
            .build()

        MobileAds.initialize(this) {}
        MobileAds.setRequestConfiguration(requestConfiguration)

        // By default video ads run at device volume, which could be annoying
        // to some users. We're reducing ad volume to be 10% of device volume.
        // Note that this doesn't always guarantee that ads will run at a
        // reduced volume. This is either a longstanding SDK bug or due to
        // an undocumented behaviour.
        MobileAds.setAppVolume(0.1f)
    }

    /**
     * Syncs analytics and crashlytics collection to user's preference.
     *
     * @param shouldShareLogs user's preference for log sharing. Note that if
     * it's set to false, it does not take effect until the next app launch.
     *
     * @see [FirebaseAnalytics.setAnalyticsCollectionEnabled]
     * @see [FirebaseCrashlytics.setCrashlyticsCollectionEnabled]
     */
    fun setupCrashReporting(
        shouldShareLogs: Boolean = SettingsManager.getPreference(
            SettingsManager.PROPERTY_SHARE_ANALYTICS_AND_LOGS,
            true
        )
    ) {
        val analytics by inject<FirebaseAnalytics>()
        val crashlytics by inject<FirebaseCrashlytics>()

        // Sync analytics collection to user's preference
        analytics.setAnalyticsCollectionEnabled(shouldShareLogs)
        // Sync crashlytics collection to user's preference, but only if we're on a release build
        crashlytics.setCrashlyticsCollectionEnabled(shouldShareLogs && !BuildConfig.DEBUG)
    }

    @Suppress("unused")
    companion object {
        private const val TAG = "OxygenUpdater"

        @Suppress("ObjectPropertyName")
        private val _isNetworkAvailable = MutableLiveData<Boolean>()
        val isNetworkAvailable: LiveData<Boolean>
            get() = _isNetworkAvailable

        // Test devices for ads.
        private val ADS_TEST_DEVICES = mutableListOf(
            AdRequest.DEVICE_ID_EMULATOR
        )

        const val NO_OXYGEN_OS = "no_oxygen_os_ver_found"
        const val NUMBER_OF_INSTALL_GUIDE_PAGES = 5
        const val APP_USER_AGENT = "Oxygen_updater_" + BuildConfig.VERSION_NAME
        const val UNABLE_TO_FIND_A_MORE_RECENT_BUILD = "unable to find a more recent build"
        const val NETWORK_CONNECTION_ERROR = "NETWORK_CONNECTION_ERROR"
        const val SERVER_MAINTENANCE_ERROR = "SERVER_MAINTENANCE_ERROR"
        const val APP_OUTDATED_ERROR = "APP_OUTDATED_ERROR"

        fun buildAdRequest(): AdRequest = AdRequest.Builder().build()
    }
}
