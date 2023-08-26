package com.oxygenupdater.compose.activities

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
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
import com.oxygenupdater.compose.icons.Announcement
import com.oxygenupdater.compose.icons.CustomIcons
import com.oxygenupdater.compose.ui.RefreshAwareState
import com.oxygenupdater.compose.ui.TopAppBar
import com.oxygenupdater.compose.ui.about.AboutScreen
import com.oxygenupdater.compose.ui.common.BannerAd
import com.oxygenupdater.compose.ui.common.rememberCallback
import com.oxygenupdater.compose.ui.common.rememberTypedCallback
import com.oxygenupdater.compose.ui.device.DeviceScreen
import com.oxygenupdater.compose.ui.device.IncorrectDeviceDialog
import com.oxygenupdater.compose.ui.device.UnsupportedDeviceOsSpecDialog
import com.oxygenupdater.compose.ui.device.defaultDeviceName
import com.oxygenupdater.compose.ui.dialogs.ContributorSheet
import com.oxygenupdater.compose.ui.dialogs.ModalBottomSheet
import com.oxygenupdater.compose.ui.dialogs.ServerMessagesSheet
import com.oxygenupdater.compose.ui.dialogs.SheetType
import com.oxygenupdater.compose.ui.dialogs.defaultModalBottomSheetState
import com.oxygenupdater.compose.ui.dialogs.rememberSheetType
import com.oxygenupdater.compose.ui.main.AboutRoute
import com.oxygenupdater.compose.ui.main.AppUpdateInfo
import com.oxygenupdater.compose.ui.main.DeviceRoute
import com.oxygenupdater.compose.ui.main.FlexibleAppUpdateProgress
import com.oxygenupdater.compose.ui.main.MainMenu
import com.oxygenupdater.compose.ui.main.MainSnackbar
import com.oxygenupdater.compose.ui.main.NewsListRoute
import com.oxygenupdater.compose.ui.main.NoConnectionSnackbarData
import com.oxygenupdater.compose.ui.main.Screen
import com.oxygenupdater.compose.ui.main.ServerStatusBanner
import com.oxygenupdater.compose.ui.main.ServerStatusDialogs
import com.oxygenupdater.compose.ui.main.SettingsRoute
import com.oxygenupdater.compose.ui.main.UpdateRoute
import com.oxygenupdater.compose.ui.news.NewsListScreen
import com.oxygenupdater.compose.ui.news.NewsListViewModel
import com.oxygenupdater.compose.ui.news.previousUnreadCount
import com.oxygenupdater.compose.ui.onboarding.NOT_SET_L
import com.oxygenupdater.compose.ui.settings.SettingsScreen
import com.oxygenupdater.compose.ui.settings.SettingsViewModel
import com.oxygenupdater.compose.ui.settings.adFreeConfig
import com.oxygenupdater.compose.ui.theme.AppTheme
import com.oxygenupdater.compose.ui.update.KEY_DOWNLOAD_ERROR_MESSAGE
import com.oxygenupdater.compose.ui.update.UpdateInformationViewModel
import com.oxygenupdater.compose.ui.update.UpdateScreen
import com.oxygenupdater.enums.PurchaseType
import com.oxygenupdater.extensions.openPlayStorePage
import com.oxygenupdater.extensions.startNewsItemActivity
import com.oxygenupdater.extensions.startOnboardingActivity
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.models.Device
import com.oxygenupdater.models.DeviceOsSpec
import com.oxygenupdater.models.NewsItem
import com.oxygenupdater.models.SystemVersionProperties
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

    private lateinit var navController: NavHostController

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun Content() {
        navController = rememberNavController()
        val sheetState = defaultModalBottomSheetState()

        val allDevices by viewModel.deviceState.collectAsStateWithLifecycle()
        LaunchedEffect(allDevices) {
            viewModel.deviceOsSpec = Utils.checkDeviceOsSpec(allDevices)
            viewModel.deviceMismatch = Utils.checkDeviceMismatch(this@MainActivity, allDevices)
        }

        val showDeviceBadge = viewModel.deviceOsSpec.let {
            it != null && it != DeviceOsSpec.SUPPORTED_OXYGEN_OS
        } || viewModel.deviceMismatch.let { it != null && it.first }
        Screen.Device.badge = if (showDeviceBadge) "!" else null

        val enabledDevices = remember(allDevices) { allDevices.filter { it.enabled } }
        LaunchedEffect(enabledDevices) {
            // Resubscribe to notification topics, if needed.
            // We're doing it here, instead of [SplashActivity], because it requires the app to be setup first
            // (`deviceId`, `updateMethodId`, etc need to be saved in [SharedPreferences]).
            if (checkPlayServices(this@MainActivity, false)) {
                viewModel.resubscribeToNotificationTopicsIfNeeded(enabledDevices)
            }
        }

        val startScreen = remember(startPage) {
            screens.getOrNull(startPage) ?: Screen.Update
        }

        val showDeviceWarningDialog = !PrefManager.getBoolean(PrefManager.PROPERTY_IGNORE_UNSUPPORTED_DEVICE_WARNINGS, false)
        val showIncorrectDeviceDialog = !PrefManager.getBoolean(PrefManager.PROPERTY_IGNORE_INCORRECT_DEVICE_WARNINGS, false)
        if (showDeviceWarningDialog) viewModel.deviceOsSpec?.let {
            UnsupportedDeviceOsSpecDialog(it)
        }

        if (showIncorrectDeviceDialog) viewModel.deviceMismatch?.let {
            IncorrectDeviceDialog(it)
        }

        val snackbarHostState = remember { SnackbarHostState() }
        MainSnackbar(
            snackbarHostState,
            viewModel.snackbarText,
            openPlayStorePage = ::openPlayStorePage,
            completeAppUpdate = viewModel::completeAppUpdate
        )

        AppUpdateInfo(
            viewModel.appUpdateInfo.collectAsStateWithLifecycle().value,
            viewModel.snackbarText,
            viewModel::unregisterAppUpdateListener,
            requestUpdate = { launcher, info ->
                viewModel.requestUpdate(launcher, info)
            },
            requestImmediateUpdate = { launcher, info ->
                viewModel.requestImmediateAppUpdate(launcher, info)
            },
        )

        // Display the "No connection" banner if required
        val isNetworkAvailable by OxygenUpdater.isNetworkAvailable.observeAsState(true)
        if (!isNetworkAvailable) viewModel.snackbarText.value = NoConnectionSnackbarData
        else if (viewModel.snackbarText.value?.first == NoConnectionSnackbarData.first) {
            // Dismiss only this snackbar
            viewModel.snackbarText.value = null
        }

        val serverMessages by viewModel.serverMessages.collectAsStateWithLifecycle()

        val initialSubtitle = startScreen.subtitle ?: stringResource(startScreen.labelResId)
        var subtitle by remember { mutableStateOf(initialSubtitle) }
        var showMarkAllRead by remember { mutableStateOf(false) }
        var sheetType by rememberSheetType()

        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
        Scaffold(Modifier.nestedScroll(scrollBehavior.nestedScrollConnection), topBar = {
            TopAppBar(scrollBehavior, {
                navController.navigateWithDefaults(AboutRoute)
            }, subtitle) {
                // Server-provided info & warning messages
                if (serverMessages.isNotEmpty()) IconButton({
                    sheetType = SheetType.ServerMessages
                }, Modifier.requiredWidth(40.dp)) {
                    Icon(CustomIcons.Announcement, stringResource(R.string.update_information_banner_server))
                }

                val showBecomeContributor = ContributorUtils.isAtLeastQAndPossiblyRooted
                // Don't show menu if there are no items in it
                // Box layout is required to make DropdownMenu position correctly (directly under icon)
                if (showMarkAllRead || showBecomeContributor) Box {
                    // Hide other menu items behind overflow icon
                    var showMenu by remember { mutableStateOf(false) }
                    IconButton({ showMenu = true }, Modifier.requiredWidth(40.dp)) {
                        Icon(Icons.Rounded.MoreVert, stringResource(androidx.compose.ui.R.string.dropdown_menu))
                    }

                    MainMenu(showMenu, {
                        showMenu = false
                    }, showMarkAllRead, {
                        newsListViewModel.markAllRead()
                    }, showBecomeContributor, openContributorSheet = {
                        sheetType = SheetType.Contributor
                    })
                }
            }
        }, bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                currentRoute = navBackStackEntry?.destination?.route
                showMarkAllRead = currentRoute == NewsListRoute

                screens.forEach { screen ->
                    val route = screen.route
                    val label = stringResource(screen.labelResId)
                    val selected = currentRoute == route
                    if (selected) subtitle = screen.subtitle ?: label
                    NavigationBarItem(selected, {
                        navController.navigateWithDefaults(route)
                    }, icon = {
                        val badge = screen.badge
                        if (badge == null) Icon(screen.icon, label) else BadgedBox({
                            Badge {
                                Text("$badge".take(3), Modifier.semantics {
                                    contentDescription = "$badge unread articles"
                                })
                            }
                        }) { Icon(screen.icon, label) }
                    }, label = {
                        Text(label)
                    }, alwaysShowLabel = false)
                }
            }
        }, snackbarHost = {
            SnackbarHost(snackbarHostState)
        }) { innerPadding ->
            // TODO(compose/perf): this causes children to recompose every single time on scroll (if scrollBehaviour is used)
            //  Consider implementing this whole layout without using Scaffold.
            Box(Modifier.padding(innerPadding)) {
                val hide = rememberCallback { sheetType = SheetType.None }

                LaunchedEffect(Unit) { // run only on init
                    // Offer contribution to users from app versions below v2.4.0 and v5.10.1
                    if (ContributorUtils.isAtLeastQAndPossiblyRooted && !PrefManager.contains(PrefManager.PROPERTY_CONTRIBUTE)) {
                        sheetType = SheetType.Contributor
                    }
                }

                if (sheetType != SheetType.None) ModalBottomSheet(hide, sheetState) {
                    when (sheetType) {
                        SheetType.Contributor -> ContributorSheet(hide, true)
                        SheetType.ServerMessages -> ServerMessagesSheet(serverMessages)
                        else -> {}
                    }
                }

                Column {
                    val serverStatus by viewModel.serverStatus.collectAsStateWithLifecycle()
                    ServerStatusDialogs(serverStatus.status) { openPlayStorePage() }
                    ServerStatusBanner(serverStatus)

                    // NavHost can't preload other composables, so in order to get NewsList's unread count early,
                    // we're using the initial state here itself. State is refreshed only once the user visits that
                    // screen, so it's easy on the server too (no unnecessarily eager requests).
                    // Note: can't use `by` here because it doesn't propagate to [newsListScreen]
                    val newsListState = newsListViewModel.state.collectAsStateWithLifecycle().value
                    LaunchedEffect(Unit) { // run only on init
                        @Suppress("DEPRECATION")
                        val unreadCount = newsListState.data.count { !it.read }
                        if (unreadCount != previousUnreadCount) {
                            Screen.NewsList.badge = if (unreadCount == 0) null else "$unreadCount"
                            previousUnreadCount = unreadCount
                        }
                    }

                    NavHost(navController, startScreen.route, Modifier.weight(1f)) {
                        updateScreen { subtitle = it }
                        newsListScreen(newsListState)
                        deviceScreen(allDevices)
                        aboutScreen()
                        settingsScreen(enabledDevices, updateMismatchStatus = {
                            viewModel.deviceMismatch = Utils.checkDeviceMismatch(this@MainActivity, allDevices)
                        })
                    }

                    // Ads should be shown if user hasn't bought the ad-free unlock
                    val showAds = !billingViewModel.hasPurchasedAdFree.collectAsStateWithLifecycle(
                        PrefManager.getBoolean(PrefManager.PROPERTY_AD_FREE, false)
                    ).value
                    if (showAds) BannerAd(BuildConfig.AD_BANNER_MAIN_ID) { bannerAdView = it }
                }

                // This must be defined on the same level as NavHost, otherwise it won't work
                // We can safely put it outside Column because it's an inline composable
                // TODO(compose): use PredictiveBackHandler (can only be tested on Android 14). Animate sheet close
                //  based on progress and delegate to NavHost's default implementation if it does nice things with predictive back.
                //  https://developer.android.com/guide/navigation/custom-back/predictive-back-gesture#opt-predictive
                //  Adjust all other BackHandlers if required
                BackHandler {
                    if (sheetState.isVisible) hide()
                    else navController.run {
                        if (shouldStopNavigateAwayFromSettings()) showSettingsWarning()
                        else if (!popBackStack()) finishAffinity() // nothing to back to => exit
                    }
                }

                // Gets placed below TopAppBar
                FlexibleAppUpdateProgress(
                    viewModel.appUpdateStatus.collectAsStateWithLifecycle().value,
                    viewModel.snackbarText,
                )
            }
        }
    }

    private fun NavController.navigateWithDefaults(
        route: String,
    ) = if (shouldStopNavigateAwayFromSettings()) showSettingsWarning() else navigate(route, navOptions)

    private fun NavGraphBuilder.updateScreen(setSubtitle: (String) -> Unit) = composable(UpdateRoute) {
        if (!PrefManager.checkIfSetupScreenHasBeenCompleted()) {
            startOnboardingActivity(startPage)
            return@composable finish()
        }

        LaunchedEffect(Unit) { // runs once every time this screen is visited
            settingsViewModel.updateCrashlyticsUserId()
        }

        val state = updateViewModel.state.collectAsStateWithLifecycle(null).value ?: return@composable
        val workInfoWithStatus by viewModel.workInfoWithStatus.collectAsStateWithLifecycle()

        UpdateScreen(state, workInfoWithStatus, downloadErrorMessage != null, rememberCallback {
            settingsViewModel.updateCrashlyticsUserId()
            viewModel.fetchServerStatus()
            updateViewModel.refresh()
        }, rememberTypedCallback(setSubtitle), enqueueDownload = rememberTypedCallback {
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
            },
            rememberCallback(newsListViewModel::markAllRead),
            rememberTypedCallback(newsListViewModel::toggleRead),
            rememberTypedCallback(::startNewsItemActivity)
        )
    }

    private fun NavGraphBuilder.deviceScreen(allDevices: List<Device>) = composable(DeviceRoute) {
        val defaultDeviceName = defaultDeviceName()
        DeviceScreen(remember(allDevices) {
            allDevices.find {
                it.productNames.contains(SystemVersionProperties.oxygenDeviceName)
            }?.name ?: defaultDeviceName
        }, viewModel.deviceOsSpec, viewModel.deviceMismatch)
    }

    private fun NavGraphBuilder.aboutScreen() = composable(AboutRoute) { AboutScreen() }

    private fun NavGraphBuilder.settingsScreen(
        cachedEnabledDevices: List<Device>,
        updateMismatchStatus: () -> Unit,
    ) = composable(SettingsRoute) {
        LaunchedEffect(cachedEnabledDevices) {
            // Passthrough from MainViewModel to avoid sending a request again
            settingsViewModel.fetchEnabledDevices(cachedEnabledDevices)
        }

        val state by settingsViewModel.state.collectAsStateWithLifecycle()

        // TODO(compose/settings): test all this thoroughly
        val adFreePrice by billingViewModel.adFreePrice.collectAsStateWithLifecycle(null)
        val adFreeState by billingViewModel.adFreeState.collectAsStateWithLifecycle(null)

        val markPending = rememberCallback {
            Toast.makeText(
                this@MainActivity,
                getString(R.string.purchase_error_pending_payment),
                Toast.LENGTH_LONG
            ).show()
        }

        val adFreeConfig = adFreeConfig(adFreeState, markPending, makePurchase = rememberTypedCallback {
            billingViewModel.makePurchase(this@MainActivity, it)
        })

        // Note: we use `this` instead of LocalLifecycleOwner because the latter can change, which results
        // in an IllegalArgumentException (can't reuse the same observer with different lifecycles)

        // no-op observe because the actual work is being done in BillingViewModel
        billingViewModel.purchaseStateChange.observe(this@MainActivity, remember { Observer<Purchase> {} })
        billingViewModel.pendingPurchase.observe(this@MainActivity, remember { Observer<Purchase?> { markPending() } })
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
                        Toast.makeText(
                            this@MainActivity,
                            getString(R.string.purchase_error_after_payment),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        })

        SettingsScreen(state, settingsViewModel.initialDeviceIndex, rememberTypedCallback {
            settingsViewModel.saveSelectedDevice(it)
            updateMismatchStatus()
        }, settingsViewModel.initialMethodIndex, rememberTypedCallback(state) {
            settingsViewModel.saveSelectedMethod(it)

            if (checkPlayServices(this@MainActivity, true)) {
                // Subscribe to notifications for the newly selected device and update method
                NotificationTopicSubscriber.resubscribeIfNeeded(state.enabledDevices, state.methodsForDevice)
            } else Toast.makeText(
                this@MainActivity,
                this@MainActivity.getString(R.string.notification_no_notification_support),
                Toast.LENGTH_LONG
            ).show()
        }, adFreePrice, adFreeConfig, openAboutScreen = rememberCallback {
            navController.navigateWithDefaults(AboutRoute)
        })
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

        if (!checkPlayServices(this, false)) Toast.makeText(
            this, getString(R.string.notification_no_notification_support), Toast.LENGTH_LONG
        ).show()

        setContent {
            AppTheme {
                EdgeToEdge()
                Content()
            }
        }

        lifecycle.addObserver(billingViewModel.lifecycleObserver)

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
            Toast.makeText(this, getString(R.string.settings_entered_incorrectly), Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, getString(R.string.settings_saving), Toast.LENGTH_LONG).show()
        }
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
