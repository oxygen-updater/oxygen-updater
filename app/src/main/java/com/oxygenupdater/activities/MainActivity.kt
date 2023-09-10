package com.oxygenupdater.activities

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.google.android.gms.ads.AdView
import com.google.firebase.analytics.FirebaseAnalytics
import com.oxygenupdater.BuildConfig
import com.oxygenupdater.OxygenUpdater
import com.oxygenupdater.R
import com.oxygenupdater.enums.PurchaseType
import com.oxygenupdater.extensions.openPlayStorePage
import com.oxygenupdater.extensions.showToast
import com.oxygenupdater.extensions.startNewsItemActivity
import com.oxygenupdater.extensions.startOnboardingActivity
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.models.Device
import com.oxygenupdater.models.DeviceOsSpec
import com.oxygenupdater.models.NewsItem
import com.oxygenupdater.models.SystemVersionProperties
import com.oxygenupdater.ui.RefreshAwareState
import com.oxygenupdater.ui.TopAppBar
import com.oxygenupdater.ui.about.AboutScreen
import com.oxygenupdater.ui.common.BannerAd
import com.oxygenupdater.ui.common.rememberCallback
import com.oxygenupdater.ui.common.rememberSaveableState
import com.oxygenupdater.ui.common.rememberTypedCallback
import com.oxygenupdater.ui.device.DeviceScreen
import com.oxygenupdater.ui.device.IncorrectDeviceDialog
import com.oxygenupdater.ui.device.UnsupportedDeviceOsSpecDialog
import com.oxygenupdater.ui.device.defaultDeviceName
import com.oxygenupdater.ui.main.AboutRoute
import com.oxygenupdater.ui.main.AppUpdateInfo
import com.oxygenupdater.ui.main.DeviceRoute
import com.oxygenupdater.ui.main.FlexibleAppUpdateProgress
import com.oxygenupdater.ui.main.MainMenu
import com.oxygenupdater.ui.main.MainSnackbar
import com.oxygenupdater.ui.main.NewsListRoute
import com.oxygenupdater.ui.main.NoConnectionSnackbarData
import com.oxygenupdater.ui.main.NotificationPermission
import com.oxygenupdater.ui.main.Screen
import com.oxygenupdater.ui.main.ServerStatusBanner
import com.oxygenupdater.ui.main.ServerStatusDialogs
import com.oxygenupdater.ui.main.SettingsRoute
import com.oxygenupdater.ui.main.UpdateRoute
import com.oxygenupdater.ui.news.NewsListScreen
import com.oxygenupdater.ui.news.NewsListViewModel
import com.oxygenupdater.ui.onboarding.NOT_SET_L
import com.oxygenupdater.ui.settings.SettingsScreen
import com.oxygenupdater.ui.settings.SettingsViewModel
import com.oxygenupdater.ui.settings.adFreeConfig
import com.oxygenupdater.ui.theme.AppTheme
import com.oxygenupdater.ui.update.KEY_DOWNLOAD_ERROR_MESSAGE
import com.oxygenupdater.ui.update.UpdateInformationViewModel
import com.oxygenupdater.ui.update.UpdateScreen
import com.oxygenupdater.utils.ContributorUtils
import com.oxygenupdater.utils.Logger.logBillingError
import com.oxygenupdater.utils.Logger.logDebug
import com.oxygenupdater.utils.Logger.logWarning
import com.oxygenupdater.utils.NotificationTopicSubscriber
import com.oxygenupdater.utils.Utils
import com.oxygenupdater.utils.Utils.checkPlayServices
import com.oxygenupdater.utils.hasRootAccess
import com.oxygenupdater.viewmodels.BillingViewModel
import com.oxygenupdater.viewmodels.MainViewModel
import kotlinx.coroutines.Job
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Timer
import kotlin.concurrent.schedule

class MainActivity : BaseActivity() {

    private val viewModel by viewModel<MainViewModel>()
    private val updateViewModel by viewModel<UpdateInformationViewModel>()
    private val newsListViewModel by viewModel<NewsListViewModel>()
    private val settingsViewModel by viewModel<SettingsViewModel>()
    private val billingViewModel by viewModel<BillingViewModel>()

    private val startPage: Int
    private val downloadErrorMessage: String?

    init {
        val intent = intent

        startPage = try {
            intent?.getIntExtra(INTENT_START_PAGE, PAGE_UPDATE) ?: PAGE_UPDATE
        } catch (ignored: IndexOutOfBoundsException) {
            PAGE_UPDATE
        }

        // Force-show download error dialog if started from an intent with error info
        // TODO(compose/update): download error dialog logic is simplified in UIC. Verify if the dialog meant
        //  to be shown after clicking the download failed notification (i.e. on activity start) is shown
        //  immediately in the UI itself, in both cases: app in foreground and background/killed.
        //  Otherwise, see KEY_DOWNLOAD_ERROR_MESSAGE in UIF.setupServerResponseObservers()
        downloadErrorMessage = try {
            intent?.getStringExtra(KEY_DOWNLOAD_ERROR_MESSAGE)
        } catch (ignored: IndexOutOfBoundsException) {
            null
        }
    }

    private val screens = arrayOf(
        Screen.Update,
        Screen.NewsList,
        Screen.Device,
        Screen.About,
        Screen.Settings,
    )

    private val navOptions = NavOptions.Builder()
        // Pop up to the start destination to avoid polluting back stack
        .setPopUpTo(screens.getOrNull(startPage)?.route ?: UpdateRoute, inclusive = false, saveState = true)
        // Avoid multiple copies of the same destination when reselecting
        .setLaunchSingleTop(true)
        // Restore state on reselect
        .setRestoreState(true)
        .build()

    @Volatile
    private var bannerAdView: AdView? = null

    @Volatile
    private var currentRoute: String? = null

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun Content() {
        val showDeviceBadge = viewModel.deviceOsSpec.let {
            it != null && it != DeviceOsSpec.SUPPORTED_OXYGEN_OS
        } || viewModel.deviceMismatch.let { it != null && it.first }
        Screen.Device.badge = if (showDeviceBadge) "!" else null

        val allDevices by viewModel.deviceState.collectAsStateWithLifecycle()
        val enabledDevices = remember(allDevices) { allDevices.filter { it.enabled } }
        LaunchedEffect(enabledDevices) {
            // Resubscribe to notification topics, if needed.
            // We're doing it here, instead of [SplashActivity], because it requires the app to be setup first
            // (`deviceId`, `updateMethodId`, etc need to be saved in [SharedPreferences]).
            if (checkPlayServices(this@MainActivity, false)) {
                viewModel.resubscribeToNotificationTopicsIfNeeded(enabledDevices)
            }
        }

        val showDeviceWarningDialog = !PrefManager.getBoolean(PrefManager.PROPERTY_IGNORE_UNSUPPORTED_DEVICE_WARNINGS, false)
        val showIncorrectDeviceDialog = !PrefManager.getBoolean(PrefManager.PROPERTY_IGNORE_INCORRECT_DEVICE_WARNINGS, false)
        if (showDeviceWarningDialog) viewModel.deviceOsSpec?.let {
            UnsupportedDeviceOsSpecDialog(it)
        }

        if (showIncorrectDeviceDialog) viewModel.deviceMismatch?.let {
            IncorrectDeviceDialog(it)
        }

        // Referential equality because we're reusing static Pairs
        var snackbarText by rememberSaveableState<Pair<Int, Int>?>("snackbarText", null, true)
        AppUpdateInfo(
            viewModel.appUpdateInfo.collectAsStateWithLifecycle().value,
            { snackbarText?.first },
            { snackbarText = it },
            viewModel::unregisterAppUpdateListener,
            requestUpdate = viewModel::requestUpdate,
            requestImmediateUpdate = viewModel::requestImmediateAppUpdate,
        )

        // Display the "No connection" banner if required
        val isNetworkAvailable by OxygenUpdater.isNetworkAvailable.collectAsStateWithLifecycle()
        if (!isNetworkAvailable) snackbarText = NoConnectionSnackbarData
        else if (snackbarText?.first == NoConnectionSnackbarData.first) {
            // Dismiss only this snackbar
            snackbarText = null
        }

        Column {
            val startScreen = remember(startPage) { screens.getOrNull(startPage) ?: Screen.Update }
            var subtitleResId by rememberSaveableState("subtitleResId", if (startScreen.useVersionName) 0 else startScreen.labelResId)

            val navController = rememberNavController()
            val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
            val openAboutScreen = rememberCallback(navController) { navController.navigateWithDefaults(AboutRoute) }
            TopAppBar(scrollBehavior, openAboutScreen, subtitleResId) {
                val serverMessages by viewModel.serverMessages.collectAsStateWithLifecycle()
                MainMenu(serverMessages, subtitleResId == Screen.NewsList.labelResId, rememberCallback(newsListViewModel::markAllRead))
            }

            FlexibleAppUpdateProgress(viewModel.appUpdateStatus.collectAsStateWithLifecycle().value, {
                snackbarText?.first
            }) { snackbarText = it }

            val serverStatus by viewModel.serverStatus.collectAsStateWithLifecycle()
            ServerStatusDialogs(serverStatus.status, ::openPlayStorePage)
            ServerStatusBanner(serverStatus)

            // NavHost can't preload other composables, so in order to get NewsList's unread count early,
            // we're using the initial state here itself. State is refreshed only once the user visits that
            // screen, so it's easy on the server too (no unnecessarily eager requests).
            // Note: can't use `by` here because it doesn't propagate to [newsListScreen]
            val newsListState = newsListViewModel.state.collectAsStateWithLifecycle().value
            LaunchedEffect(Unit) { // run only on init
                val unreadCount = newsListViewModel.unreadCount.intValue
                Screen.NewsList.badge = if (unreadCount == 0) null else "$unreadCount"
            }

            NavHost(
                navController, startScreen.route,
                Modifier
                    .weight(1f)
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
            ) {
                updateScreen { subtitleResId = it }
                newsListScreen(newsListState)
                deviceScreen(allDevices)
                aboutScreen()
                settingsScreen(enabledDevices, updateMismatchStatus = {
                    viewModel.deviceMismatch = Utils.checkDeviceMismatch(allDevices)
                }, openAboutScreen)
            }

            // This must be defined on the same level as NavHost, otherwise it won't work
            BackHandler {
                navController.run {
                    if (shouldStopNavigateAwayFromSettings()) showSettingsWarning()
                    else if (!popBackStack()) finishAffinity() // nothing to back to => exit
                }
            }

            // Ads should be shown if user hasn't bought the ad-free unlock
            val showAds = !billingViewModel.hasPurchasedAdFree.collectAsStateWithLifecycle(
                PrefManager.getBoolean(PrefManager.PROPERTY_AD_FREE, false)
            ).value
            if (showAds) BannerAd(BuildConfig.AD_BANNER_MAIN_ID, view = rememberTypedCallback { bannerAdView = it })

            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                currentRoute = navBackStackEntry?.destination?.route

                screens.forEach { screen ->
                    val labelResId = screen.labelResId
                    key(labelResId) {
                        val route = screen.route
                        val label = stringResource(labelResId)
                        val selected = currentRoute == route
                        if (selected) {
                            // UpdateScreen manages its own subtitle, so avoid race by not setting at all
                            // 0 => set to app version
                            if (labelResId != Screen.Update.labelResId) subtitleResId = if (screen.useVersionName) 0 else labelResId
                        }
                        NavigationBarItem(selected, rememberCallback(navController, route) {
                            navController.navigateWithDefaults(route)
                        }, icon = {
                            val badge = screen.badge
                            if (badge == null) Icon(screen.icon, label) else BadgedBox({
                                Badge {
                                    Text(badge.take(3), Modifier.semantics {
                                        contentDescription = "$badge unread articles"
                                    })
                                }
                            }) { Icon(screen.icon, label) }
                        }, label = {
                            Text(label)
                        }, alwaysShowLabel = false)
                    }
                }
            }
        }

        // Gets placed over TopAppBar
        MainSnackbar(
            snackbarText,
            openPlayStorePage = rememberCallback(::openPlayStorePage),
            completeAppUpdate = rememberCallback(viewModel::completeAppUpdate)
        )
    }

    private fun NavController.navigateWithDefaults(
        route: String,
    ) = if (shouldStopNavigateAwayFromSettings()) showSettingsWarning() else navigate(route, navOptions)

    private fun NavGraphBuilder.updateScreen(setSubtitleResId: (Int) -> Unit) = composable(UpdateRoute) {
        if (!PrefManager.checkIfSetupScreenHasBeenCompleted()) {
            startOnboardingActivity(startPage)
            return@composable finish()
        }

        LaunchedEffect(Unit) { // runs once every time this screen is visited
            settingsViewModel.updateCrashlyticsUserId()
            updateViewModel.refreshIfNeeded()
        }

        val state = updateViewModel.state.collectAsStateWithLifecycle(null).value ?: return@composable
        val workInfoWithStatus by viewModel.workInfoWithStatus.collectAsStateWithLifecycle()

        UpdateScreen(state, workInfoWithStatus, downloadErrorMessage != null, rememberCallback {
            settingsViewModel.updateCrashlyticsUserId()
            viewModel.fetchServerStatus()
            updateViewModel.refresh()
        }, rememberTypedCallback(setSubtitleResId), enqueueDownload = rememberTypedCallback {
            viewModel.setupDownloadWorkRequest(it)
            viewModel.enqueueDownloadWork()
        }, pauseDownload = rememberCallback(viewModel::pauseDownloadWork), cancelDownload = rememberTypedCallback {
            viewModel.cancelDownloadWork(this@MainActivity, it)
        }, deleteDownload = rememberTypedCallback {
            viewModel.deleteDownloadedFile(this@MainActivity, it)
        }, logDownloadError = rememberTypedCallback(viewModel::logDownloadError))
    }

    private fun NavGraphBuilder.newsListScreen(state: RefreshAwareState<List<NewsItem>>) = composable(NewsListRoute) {
        LaunchedEffect(Unit) {
            // Avoid refreshing every time this screen is visited by guessing
            // if it's the first load (`refreshing` is true only initially)
            if (state.refreshing) newsListViewModel.refresh()
        }

        NewsListScreen(
            state, rememberCallback {
                viewModel.fetchServerStatus()
                newsListViewModel.refresh()
            }, newsListViewModel.unreadCount,
            rememberCallback(newsListViewModel::markAllRead),
            rememberTypedCallback(newsListViewModel::toggleRead),
            rememberTypedCallback(::startNewsItemActivity)
        )
    }

    private fun NavGraphBuilder.deviceScreen(allDevices: List<Device>) = composable(DeviceRoute) {
        DeviceScreen(remember(allDevices) {
            allDevices.find {
                it.productNames.contains(SystemVersionProperties.oxygenDeviceName)
            }?.name ?: defaultDeviceName()
        }, viewModel.deviceOsSpec, viewModel.deviceMismatch)
    }

    private fun NavGraphBuilder.aboutScreen() = composable(AboutRoute) { AboutScreen() }

    private fun NavGraphBuilder.settingsScreen(
        cachedEnabledDevices: List<Device>,
        updateMismatchStatus: () -> Unit,
        openAboutScreen: () -> Unit,
    ) = composable(SettingsRoute) {
        LaunchedEffect(cachedEnabledDevices) {
            // Passthrough from MainViewModel to avoid sending a request again
            settingsViewModel.fetchEnabledDevices(cachedEnabledDevices)
        }

        val adFreePrice by billingViewModel.adFreePrice.collectAsStateWithLifecycle(null)
        val adFreeState by billingViewModel.adFreeState.collectAsStateWithLifecycle(null)

        val adFreeConfig = adFreeConfig(adFreeState, rememberTypedCallback {
            billingViewModel.makePurchase(this@MainActivity, it)
        }, rememberCallback {
            showToast(R.string.purchase_error_pending_payment)
        })

        // Note: we use `this` instead of LocalLifecycleOwner because the latter can change, which results
        // in an IllegalArgumentException (can't reuse the same observer with different lifecycles)
        billingViewModel.newPurchase.observe(this@MainActivity, remember {
            Observer<Pair<Int, Purchase?>> {
                val (responseCode, purchase) = it
                when (responseCode) {
                    BillingClient.BillingResponseCode.OK -> if (purchase != null) validateAdFreePurchase(
                        purchase, adFreePrice, PurchaseType.AD_FREE
                    )

                    BillingClient.BillingResponseCode.USER_CANCELED -> logDebug(TAG, "Purchase of ad-free version was cancelled by the user")

                    else -> {
                        logBillingError(TAG, "Purchase of the ad-free version failed due to an unknown error during the purchase flow: $responseCode")
                        showToast(R.string.purchase_error_after_payment)
                    }
                }
            }
        })

        val state by settingsViewModel.state.collectAsStateWithLifecycle()
        SettingsScreen(state, settingsViewModel.initialDeviceIndex, rememberTypedCallback {
            settingsViewModel.saveSelectedDevice(it)
            updateMismatchStatus()
        }, settingsViewModel.initialMethodIndex, rememberTypedCallback(state) {
            settingsViewModel.saveSelectedMethod(it)

            if (checkPlayServices(this@MainActivity, true)) {
                // Subscribe to notifications for the newly selected device and update method
                NotificationTopicSubscriber.resubscribeIfNeeded(state.enabledDevices, state.methodsForDevice)
            } else showToast(R.string.notification_no_notification_support)
        }, adFreePrice, adFreeConfig, openAboutScreen)
    }

    private val validatePurchaseTimer = Timer()

    /**
     * Validate the in app purchase on the app's server
     *
     * @param purchase Purchase which must be validated
     * @param amount Localized price
     * @param purchaseType the purchase type
     */
    private fun validateAdFreePurchase(
        purchase: Purchase,
        amount: String?,
        purchaseType: PurchaseType,
    ): Job = billingViewModel.verifyPurchase(
        purchase,
        amount,
        purchaseType
    ) {
        validatePurchaseTimer.cancel() // ensure only the latest task goes through
        if (isFinishing) return@verifyPurchase

        when {
            // If server can't be reached, keep trying until it can
            it == null -> validatePurchaseTimer.schedule(2000) {
                validateAdFreePurchase(purchase, amount, purchaseType)
            }

            !it.success -> logBillingError(
                TAG,
                "[validateAdFreePurchase] couldn't purchase ad-free: (${it.errorMessage})"
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) = super.onCreate(savedInstanceState).also {
        val analytics by inject<FirebaseAnalytics>()
        analytics.setUserProperty("device_name", SystemVersionProperties.oxygenDeviceName)

        if (!checkPlayServices(this, false)) showToast(R.string.notification_no_notification_support)

        lifecycle.addObserver(billingViewModel.lifecycleObserver)

        setContent {
            AppTheme {
                EdgeToEdge()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) NotificationPermission()

                // We're using Surface to avoid Scaffold's recomposition-on-scroll issue (when using scrollBehaviour and consuming innerPadding)
                Surface { Content() }
            }
        }

        // TODO(root): move this to the proper place
        hasRootAccess {
            if (!it) return@hasRootAccess ContributorUtils.stopDbCheckingProcess(this)

            ContributorUtils.startDbCheckingProcess(this)
        }
    }

    override fun onResume() = super.onResume().also {
        bannerAdView?.resume()
        viewModel.checkForStalledAppUpdate()
    }

    override fun onPause() = super.onPause().also {
        bannerAdView?.pause()
    }

    override fun onDestroy() = super.onDestroy().also {
        bannerAdView?.destroy()
        viewModel.maybePruneWork()
        viewModel.unregisterAppUpdateListener()
    }

    /** Checks if we should stop the user from navigating away from [Screen.Settings], if required prefs aren't saved */
    private fun NavController.shouldStopNavigateAwayFromSettings() = currentDestination?.route == SettingsRoute && !PrefManager.checkIfSetupScreenHasBeenCompleted()

    private fun showSettingsWarning() {
        val deviceId = PrefManager.getLong(PrefManager.PROPERTY_DEVICE_ID, NOT_SET_L)
        val updateMethodId = PrefManager.getLong(PrefManager.PROPERTY_UPDATE_METHOD_ID, NOT_SET_L)

        if (deviceId == NOT_SET_L || updateMethodId == NOT_SET_L) {
            logWarning(TAG, "Required preferences not valid: $deviceId, $updateMethodId")
            showToast(R.string.settings_entered_incorrectly)
        } else showToast(R.string.settings_saving)
    }

    companion object {
        private const val TAG = "MainActivity"

        const val PAGE_UPDATE = 0
        const val PAGE_NEWS = 1
        const val PAGE_DEVICE = 2
        const val PAGE_ABOUT = 3
        const val PAGE_SETTINGS = 4
        const val INTENT_START_PAGE = "start_page"
    }
}
