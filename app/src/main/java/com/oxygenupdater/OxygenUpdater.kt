package com.oxygenupdater

import android.app.Application
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import androidx.core.content.getSystemService
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.network.cachecontrol.CacheControlCacheStrategy
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.util.DebugLogger
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.oxygenupdater.database.SqliteMigrations
import com.oxygenupdater.extensions.get
import com.oxygenupdater.extensions.set
import com.oxygenupdater.internal.settings.KeyShareAnalyticsAndLogs
import com.oxygenupdater.internal.settings.KeyVersionCode
import com.oxygenupdater.models.SystemVersionProperties
import com.oxygenupdater.utils.NotifUtils
import com.oxygenupdater.utils.logError
import com.topjohnwu.superuser.Shell
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltAndroidApp
class OxygenUpdater : Application(), Configuration.Provider, SingletonImageLoader.Factory {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    /**
     * Must be a getter (i.e. `get()` instead of a normal `val`) to avoid:
     * ```UninitializedPropertyAccessException: lateinit property workerFactory has not been initialized```
     */
    override val workManagerConfiguration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.DEBUG else Log.ERROR)
            .build()

    @Inject
    lateinit var analytics: FirebaseAnalytics

    @Inject
    lateinit var crashlytics: FirebaseCrashlytics

    @Inject
    lateinit var sharedPreferences: SharedPreferences

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
        super.onCreate()

        setupCrashReporting()
        setupNetworkCallback()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) NotifUtils.refreshNotificationChannels(this)

        // Save app's version code to aid in future migrations (added in 5.4.0)
        sharedPreferences[KeyVersionCode] = BuildConfig.VERSION_CODE
        SqliteMigrations.deleteLocalBillingDatabase(this)
    }

    /**
     * Coil 3 does not respect `Cache-Control` headers by default, so we
     * enable it here.
     *
     * Requires `coreLibraryDesugaring` for API <25 support.
     */
    @OptIn(ExperimentalCoilApi::class)
    override fun newImageLoader(context: PlatformContext) = ImageLoader.Builder(context)
        .logger(if (BuildConfig.DEBUG) DebugLogger() else null).components {
            add(OkHttpNetworkFetcherFactory(cacheStrategy = { CacheControlCacheStrategy() }))
        }.build()

    /**
     * Syncs analytics and crashlytics collection to user's preference.
     *
     * @see [FirebaseAnalytics.setAnalyticsCollectionEnabled]
     * @see [FirebaseCrashlytics.setCrashlyticsCollectionEnabled]
     */
    fun setupCrashReporting() {
        analytics.setUserProperty("device_name", SystemVersionProperties.deviceProductName)

        val shouldShareLogs = sharedPreferences[KeyShareAnalyticsAndLogs, true]
        // Sync analytics collection to user's preference
        analytics.setAnalyticsCollectionEnabled(shouldShareLogs)
        // Sync crashlytics collection to user's preference, but only if we're on a release build
        crashlytics.isCrashlyticsCollectionEnabled = shouldShareLogs && !BuildConfig.DEBUG
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
        } catch (e: SecurityException) {
            crashlytics.logError(TAG, "Couldn't setup network callback", e)
        }
    }

    @Suppress("unused")
    companion object {
        private const val TAG = "OxygenUpdater"

        private val _isNetworkAvailable = MutableStateFlow(true)
        val isNetworkAvailable = _isNetworkAvailable.asStateFlow()

        const val UnableToFindAMoreRecentBuild = "unable to find a more recent build"
        const val NetworkConnectionError = "NETWORK_CONNECTION_ERROR"
        const val ServerMaintenanceError = "SERVER_MAINTENANCE_ERROR"
        const val AppOutdatedError = "APP_OUTDATED_ERROR"
    }
}
