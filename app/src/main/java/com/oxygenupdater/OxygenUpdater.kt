package com.oxygenupdater

import android.annotation.SuppressLint
import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.os.Build
import android.provider.Settings
import androidx.core.content.getSystemService
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.oxygenupdater.database.SqliteMigrations
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.utils.Logger.logError
import com.oxygenupdater.utils.MD5
import com.oxygenupdater.utils.NotificationUtils
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import java.util.concurrent.atomic.AtomicBoolean

class OxygenUpdater : Application() {

    init {
        // Set settings before the main shell can be created
        Shell.enableVerboseLogging = BuildConfig.DEBUG
        // Accessing non-scoped storage (e.g. /data/data/) requires running in the global namespace
        Shell.setDefaultBuilder(Shell.Builder.create().setFlags(Shell.FLAG_MOUNT_MASTER))
    }

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

            _isNetworkAvailable.tryEmit(networkAvailability)
        }

        override fun onAvailable(network: Network) {
            _isNetworkAvailable.tryEmit(true)
        }
    }

    override fun onCreate() {
        setupKoin()
        super.onCreate()

        setupCrashReporting()
        setupNetworkCallback()

        // Support functions for Android 8.0 "Oreo" and up.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) NotificationUtils(this).run {
            deleteOldNotificationChannels()
            createNewNotificationGroupsAndChannels()
        }

        // Save app's version code to aid in future migrations (added in 5.4.0)
        PrefManager.putInt(PrefManager.KeyVersionCode, BuildConfig.VERSION_CODE)
        SqliteMigrations.deleteLocalBillingDatabase(this)
    }

    private fun setupKoin() = startKoin {
        // use AndroidLogger as Koin Logger - default Level.INFO
        androidLogger(Level.ERROR)
        // use the Android context given there
        androidContext(this@OxygenUpdater)
        // module list
        modules(allModules)
    }

    private fun setupNetworkCallback() = getSystemService<ConnectivityManager>()?.run {
        // Posting initial value is required, as [networkCallback]'s
        // methods get called only when network connectivity changes
        @Suppress("DEPRECATION")
        _isNetworkAvailable.tryEmit(activeNetworkInfo?.isConnectedOrConnecting == true)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                registerDefaultNetworkCallback(networkCallback)
            } else registerNetworkCallback(NetworkRequest.Builder().build(), networkCallback)
        } catch (e: Exception) {
            logError(TAG, "Couldn't setup network callback", e)
        }
    }

    private val mobileAdsInitDone = AtomicBoolean(false)
    fun setupMobileAds() {
        if (mobileAdsInitDone.get()) return else mobileAdsInitDone.set(true)

        val requestConfiguration = MobileAds.getRequestConfiguration().toBuilder()
        // If it's a debug build, add current device's ID to the list of test device IDs for ads
        if (BuildConfig.DEBUG) requestConfiguration.setTestDeviceIds(buildList(2) {
            add(AdRequest.DEVICE_ID_EMULATOR)
            try {
                @SuppressLint("HardwareIds")
                val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
                val deviceId = MD5.calculateMD5(androidId).uppercase()
                add(deviceId)
            } catch (_: Exception) {
                // no-op
            }
        })

        MobileAds.initialize(this)
        MobileAds.setRequestConfiguration(requestConfiguration.build())

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
        shouldShareLogs: Boolean = PrefManager.getBoolean(
            PrefManager.KeyShareAnalyticsAndLogs,
            true
        ),
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

        private val _isNetworkAvailable = MutableStateFlow(true)
        val isNetworkAvailable = _isNetworkAvailable.asStateFlow()

        // Test devices for ads.
        private val AdsTestDevices = mutableListOf(
            AdRequest.DEVICE_ID_EMULATOR
        )

        const val UnableToFindAMoreRecentBuild = "unable to find a more recent build"
        const val NetworkConnectionError = "NETWORK_CONNECTION_ERROR"
        const val ServerMaintenanceError = "SERVER_MAINTENANCE_ERROR"
        const val AppOutdatedError = "APP_OUTDATED_ERROR"

        fun buildAdRequest(): AdRequest = AdRequest.Builder().build()
    }
}
