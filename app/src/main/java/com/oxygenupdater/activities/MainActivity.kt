package com.oxygenupdater.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.collection.IntIntPair
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import com.google.android.gms.ads.AdView
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentInformation.PrivacyOptionsRequirementStatus
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.google.firebase.analytics.FirebaseAnalytics
import com.oxygenupdater.BuildConfig
import com.oxygenupdater.OxygenUpdater
import com.oxygenupdater.R
import com.oxygenupdater.enums.PurchaseType
import com.oxygenupdater.extensions.openPlayStorePage
import com.oxygenupdater.extensions.showToast
import com.oxygenupdater.extensions.startNewsItemActivity
import com.oxygenupdater.icons.CustomIcons
import com.oxygenupdater.icons.Image
import com.oxygenupdater.icons.LogoNotification
import com.oxygenupdater.internal.NotSet
import com.oxygenupdater.internal.NotSetL
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.models.Device
import com.oxygenupdater.models.DeviceOsSpec
import com.oxygenupdater.models.NewsItem
import com.oxygenupdater.models.SystemVersionProperties
import com.oxygenupdater.ui.CollapsingAppBar
import com.oxygenupdater.ui.RefreshAwareState
import com.oxygenupdater.ui.TopAppBar
import com.oxygenupdater.ui.about.AboutScreen
import com.oxygenupdater.ui.common.BannerAd
import com.oxygenupdater.ui.common.adLoadListener
import com.oxygenupdater.ui.common.rememberSaveableState
import com.oxygenupdater.ui.device.DeviceScreen
import com.oxygenupdater.ui.device.IncorrectDeviceDialog
import com.oxygenupdater.ui.device.UnsupportedDeviceOsSpecDialog
import com.oxygenupdater.ui.device.defaultDeviceName
import com.oxygenupdater.ui.main.AboutRoute
import com.oxygenupdater.ui.main.AppUpdateInfo
import com.oxygenupdater.ui.main.DeviceRoute
import com.oxygenupdater.ui.main.FlexibleAppUpdateProgress
import com.oxygenupdater.ui.main.MainMenu
import com.oxygenupdater.ui.main.MainNavigationBar
import com.oxygenupdater.ui.main.MainNavigationRail
import com.oxygenupdater.ui.main.MainScreens
import com.oxygenupdater.ui.main.MainSnackbar
import com.oxygenupdater.ui.main.NavType
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
import com.oxygenupdater.ui.onboarding.OnboardingScreen
import com.oxygenupdater.ui.settings.SettingsScreen
import com.oxygenupdater.ui.settings.SettingsViewModel
import com.oxygenupdater.ui.settings.adFreeConfig
import com.oxygenupdater.ui.theme.AppTheme
import com.oxygenupdater.ui.theme.backgroundVariant
import com.oxygenupdater.ui.update.KeyDownloadErrorMessage
import com.oxygenupdater.ui.update.UpdateInformationViewModel
import com.oxygenupdater.ui.update.UpdateScreen
import com.oxygenupdater.ui.update.WorkProgress
import com.oxygenupdater.utils.ContributorUtils
import com.oxygenupdater.utils.Logger.logBillingError
import com.oxygenupdater.utils.Logger.logDebug
import com.oxygenupdater.utils.Logger.logUmpConsentFormError
import com.oxygenupdater.utils.Logger.logWarning
import com.oxygenupdater.utils.NotificationTopicSubscriber
import com.oxygenupdater.utils.Utils
import com.oxygenupdater.utils.Utils.checkPlayServices
import com.oxygenupdater.utils.hasRootAccess
import com.oxygenupdater.viewmodels.BillingViewModel
import com.oxygenupdater.viewmodels.MainViewModel
import com.oxygenupdater.workers.WorkDataDownloadBytesDone
import com.oxygenupdater.workers.WorkDataDownloadEta
import com.oxygenupdater.workers.WorkDataDownloadFailureType
import com.oxygenupdater.workers.WorkDataDownloadProgress
import com.oxygenupdater.workers.WorkDataDownloadTotalBytes
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

    private var startPage = PageUpdate
    private var downloadErrorMessage: String? = null

    private val navOptions = NavOptions.Builder()
        // Pop up to the start destination to avoid polluting back stack
        .setPopUpTo(MainScreens.getOrNull(startPage)?.route ?: UpdateRoute, inclusive = false, saveState = true)
        // Avoid multiple copies of the same destination when reselecting
        .setLaunchSingleTop(true)
        // Restore state on reselect
        .setRestoreState(true)
        .build()

    @Volatile
    private var bannerAdView: AdView? = null

    @Volatile
    private var currentRoute: String? = null

    /**
     * Passthrough from [MainViewModel] to avoid sending a request again, but only after server request for
     * all devices completes ([MainViewModel] sends it on init, which would be `null` initially).
     *
     * This additional check is required only during onboarding to avoid unnecessary requests being sent.
     * We don't want to send a server request for enabled devices simultaneously, because that can be derived
     * from all devices anyway. Additionally, unnecessary changes in device lists will cause a repeated request
     * for update methods to also be sent.
     */
    @Suppress("NOTHING_TO_INLINE")
    @Composable
    private inline fun PassthroughEnabledDevicesToSettingsViewModel(enabledDevices: List<Device>?) {
        if (enabledDevices == null) return
        LaunchedEffect(enabledDevices) {
            settingsViewModel.fetchEnabledDevices(enabledDevices)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun OnboardingContent(windowSize: WindowSizeClass) = Column {
        val allDevices by viewModel.deviceState.collectAsStateWithLifecycle()
        val enabledDevices = remember(allDevices) { allDevices?.filter { it.enabled } }
        PassthroughEnabledDevicesToSettingsViewModel(enabledDevices)

        val deviceName = settingsViewModel.deviceName ?: remember(enabledDevices) {
            enabledDevices?.find {
                it.productNames.contains(SystemVersionProperties.oxygenDeviceName)
            }?.name
        }

        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

        // Use normal app bar if height isn't enough (e.g. landscape phones)
        if (windowSize.heightSizeClass == WindowHeightSizeClass.Compact) TopAppBar(
            scrollBehavior = scrollBehavior,
            navIconClicked = {},
            subtitleResId = R.string.onboarding,
            showIcon = false,
        ) else CollapsingAppBar(
            scrollBehavior = scrollBehavior,
            image = { modifier ->
                val context = LocalContext.current
                AsyncImage(
                    model = deviceName?.let {
                        val density = LocalDensity.current
                        remember(it, maxWidth) {
                            ImageRequest.Builder(context)
                                .data(Device.constructImageUrl(it))
                                .size(density.run { Size(maxWidth.roundToPx(), 256.dp.roundToPx()) })
                                .build()
                        }
                    },
                    contentDescription = stringResource(R.string.device_information_image_description),
                    placeholder = rememberVectorPainter(CustomIcons.Image),
                    error = rememberVectorPainter(CustomIcons.LogoNotification),
                    contentScale = ContentScale.Crop,
                    colorFilter = if (deviceName == null) ColorFilter.tint(MaterialTheme.colorScheme.primary) else null,
                    modifier = modifier
                )
            },
            title = stringResource(R.string.app_name),
            subtitle = stringResource(R.string.onboarding),
        )

        val state by settingsViewModel.state.collectAsStateWithLifecycle()
        OnboardingScreen(
            windowWidthSize = windowSize.widthSizeClass,
            scrollBehavior = scrollBehavior,
            lists = state,
            initialDeviceIndex = settingsViewModel.initialDeviceIndex,
            deviceChanged = settingsViewModel::saveSelectedDevice,
            initialMethodIndex = settingsViewModel.initialMethodIndex,
            methodChanged = settingsViewModel::saveSelectedMethod,
            startApp = { contribute ->
                if (checkPlayServices(this@MainActivity, false)) {
                    // Subscribe to notifications for the newly selected device and update method
                    settingsViewModel.subscribeToNotificationTopics(enabledDevices)
                } else showToast(R.string.notification_no_notification_support)

                if (PrefManager.checkIfSetupScreenIsFilledIn()) {
                    PrefManager.putBoolean(PrefManager.KeySetupDone, true)
                    (application as? OxygenUpdater)?.setupCrashReporting(
                        PrefManager.getBoolean(PrefManager.KeyShareAnalyticsAndLogs, true)
                    )

                    // If user enables OTA contribution, check if device is rooted and ask for root permission
                    if (ContributorUtils.isAtLeastQAndPossiblyRooted && contribute) {
                        showToast(R.string.contribute_allow_storage)
                        hasRootAccess {
                            PrefManager.putBoolean(PrefManager.KeyContribute, true)
                            viewModel.shouldShowOnboarding = false
                        }
                    } else {
                        // Skip shell creation and thus don't show root permission prompt
                        PrefManager.putBoolean(PrefManager.KeyContribute, false)
                        viewModel.shouldShowOnboarding = false
                    }
                } else {
                    val deviceId = PrefManager.getLong(PrefManager.KeyDeviceId, NotSetL)
                    val updateMethodId = PrefManager.getLong(PrefManager.KeyUpdateMethodId, NotSetL)
                    logWarning(TAG, "Required preferences not valid: $deviceId, $updateMethodId")
                    showToast(R.string.settings_entered_incorrectly)
                    viewModel.shouldShowOnboarding = true
                }
            },
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun Content(windowSize: WindowSizeClass) {
        LaunchedEffect(Unit) { // run only on init
            viewModel.fetchServerMessages()
        }

        val showDeviceBadge = viewModel.deviceOsSpec.let {
            it != null && it != DeviceOsSpec.SupportedOxygenOs
        } || viewModel.deviceMismatch.let { it != null && it.first }
        Screen.Device.badge = if (showDeviceBadge) "!" else null

        val allDevices by viewModel.deviceState.collectAsStateWithLifecycle()
        val enabledDevices = remember(allDevices) { allDevices?.filter { it.enabled } }
        LaunchedEffect(enabledDevices) {
            // Resubscribe to notification topics, if needed.
            // We're doing it here, instead of [SplashActivity], because it requires the app to be setup first
            // (`deviceId`, `updateMethodId`, etc need to be saved in [SharedPreferences]).
            if (checkPlayServices(this@MainActivity, false)) {
                viewModel.resubscribeToNotificationTopicsIfNeeded(enabledDevices)
            }
        }

        viewModel.deviceOsSpec?.let {
            var show by remember {
                mutableStateOf(!PrefManager.getBoolean(PrefManager.KeyIgnoreUnsupportedDeviceWarnings, false))
            }
            UnsupportedDeviceOsSpecDialog(show, { show = false }, it)
        }

        val showIncorrectDeviceDialog = !PrefManager.getBoolean(PrefManager.KeyIgnoreIncorrectDeviceWarnings, false)
        if (showIncorrectDeviceDialog) viewModel.deviceMismatch?.let {
            IncorrectDeviceDialog(it)
        }

        // Referential equality because we're reusing static Pairs
        var snackbarText by rememberSaveableState<IntIntPair?>("snackbarText", null, true)
        AppUpdateInfo(
            info = viewModel.appUpdateInfo.collectAsStateWithLifecycle().value,
            snackbarMessageId = { snackbarText?.first },
            updateSnackbarText = { snackbarText = it },
            unregisterAppUpdateListener = viewModel::unregisterAppUpdateListener,
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

        Row {
            val startScreen = remember(startPage) { MainScreens.getOrNull(startPage) ?: Screen.Update }
            var subtitleResId by rememberSaveableState("subtitleResId", if (startScreen.useVersionName) 0 else startScreen.labelResId)
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            currentRoute = navBackStackEntry?.destination?.route

            val openAboutScreen = { navController.navigateWithDefaults(AboutRoute) }

            val windowWidthSize = windowSize.widthSizeClass
            val navType = NavType.from(windowWidthSize)

            AnimatedVisibility(navType == NavType.SideRail) {
                MainNavigationRail(
                    currentRoute = currentRoute,
                    navigateTo = { navController.navigateWithDefaults(it) },
                    openAboutScreen = openAboutScreen,
                    setSubtitleResId = { subtitleResId = it },
                )
            }

            AnimatedVisibility(navType == NavType.SideRail) {
                VerticalDivider(color = MaterialTheme.colorScheme.backgroundVariant)
            }

            Column {
                val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
                TopAppBar(
                    scrollBehavior = scrollBehavior,
                    navIconClicked = openAboutScreen,
                    subtitleResId = subtitleResId,
                    showIcon = navType == NavType.BottomBar,
                ) {
                    val serverMessages by viewModel.serverMessages.collectAsStateWithLifecycle()
                    MainMenu(
                        serverMessages = serverMessages,
                        showMarkAllRead = subtitleResId == Screen.NewsList.labelResId,
                        markAllRead = newsListViewModel::markAllRead,
                    )
                }

                FlexibleAppUpdateProgress(
                    state = viewModel.appUpdateStatus.collectAsStateWithLifecycle().value,
                    snackbarMessageId = { snackbarText?.first },
                    updateSnackbarText = { snackbarText = it },
                )

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
                    navController = navController,
                    startDestination = startScreen.route,
                    modifier = Modifier
                        .weight(1f)
                        .nestedScroll(scrollBehavior.nestedScrollConnection)
                ) {
                    updateScreen(
                        navType = navType,
                        windowWidthSize = windowWidthSize,
                        setSubtitleResId = { subtitleResId = it },
                    )

                    newsListScreen(
                        navType = navType,
                        windowSize = windowSize,
                        state = newsListState,
                    )

                    deviceScreen(
                        navType = navType,
                        windowWidthSize = windowWidthSize,
                        allDevices = allDevices,
                    )

                    aboutScreen(
                        navType = navType,
                        windowWidthSize = windowWidthSize,
                    )

                    settingsScreen(
                        navType = navType,
                        cachedEnabledDevices = enabledDevices,
                        updateMismatchStatus = { viewModel.deviceMismatch = Utils.checkDeviceMismatch(allDevices) },
                        openAboutScreen = openAboutScreen,
                    )
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
                    PrefManager.getBoolean(PrefManager.KeyAdFree, false)
                ).value
                if (showAds) {
                    var adLoaded by rememberSaveableState("adLoaded", false)
                    BannerAd(
                        adUnitId = BuildConfig.AD_BANNER_MAIN_ID,
                        adListener = adLoadListener { adLoaded = it },
                        viewUpdated = { bannerAdView = it },
                        // We draw the activity edge-to-edge, so nav bar padding should be applied only if ad loaded
                        modifier = if (navType != NavType.BottomBar && adLoaded) Modifier.navigationBarsPadding() else Modifier
                    )
                }

                AnimatedVisibility(navType == NavType.BottomBar) {
                    MainNavigationBar(
                        currentRoute = currentRoute,
                        navigateTo = { navController.navigateWithDefaults(it) },
                        setSubtitleResId = { subtitleResId = it },
                    )
                }
            }
        }

        // Gets placed over TopAppBar
        MainSnackbar(
            snackbarText = snackbarText,
            openPlayStorePage = ::openPlayStorePage,
            completeAppUpdate = viewModel::completeAppUpdate,
        )
    }

    private fun NavController.navigateWithDefaults(
        route: String,
    ) = if (shouldStopNavigateAwayFromSettings()) showSettingsWarning() else navigate(route, navOptions)

    private fun NavGraphBuilder.updateScreen(
        navType: NavType,
        windowWidthSize: WindowWidthSizeClass,
        setSubtitleResId: (Int) -> Unit,
    ) = composable(UpdateRoute) {
        if (!PrefManager.checkIfSetupScreenHasBeenCompleted()) {
            viewModel.shouldShowOnboarding = true
            return@composable
        } else viewModel.shouldShowOnboarding = false

        DisposableEffect(Unit) {
            // Run this every time this screen is visited
            settingsViewModel.updateCrashlyticsUserId()
            updateViewModel.refreshIfNeeded()

            // Prune finished work when leaving this composable, e.g. when
            // switching to another screen or opening a new activity
            onDispose(viewModel::maybePruneWork)
        }

        val state = updateViewModel.state.collectAsStateWithLifecycle(null).value ?: return@composable
        val (workInfo, downloadStatus) = viewModel.workInfoWithStatus.collectAsStateWithLifecycle().value

        val outputData = workInfo?.outputData
        val progress = workInfo?.progress
        val failureType = outputData?.getInt(WorkDataDownloadFailureType, NotSet)
        UpdateScreen(
            navType = navType,
            windowWidthSize = windowWidthSize,
            state = state,
            refresh = {
                settingsViewModel.updateCrashlyticsUserId()
                viewModel.fetchServerStatus()
                updateViewModel.refresh()
            },
            _downloadStatus = downloadStatus,
            failureType = failureType,
            workProgress = if (progress == null) null else remember(progress) {
                WorkProgress(
                    bytesDone = progress.getLong(WorkDataDownloadBytesDone, NotSetL),
                    totalBytes = progress.getLong(WorkDataDownloadTotalBytes, NotSetL),
                    currentProgress = progress.getInt(WorkDataDownloadProgress, NotSet),
                    downloadEta = progress.getString(WorkDataDownloadEta),
                )
            },
            forceDownloadErrorDialog = downloadErrorMessage != null,
            setSubtitleResId = setSubtitleResId,
            enqueueDownload = {
                viewModel.setupDownloadWorkRequest(it)
                viewModel.enqueueDownloadWork()
            },
            pauseDownload = viewModel::pauseDownloadWork,
            cancelDownload = { viewModel.cancelDownloadWork(this@MainActivity, it) },
            deleteDownload = { viewModel.deleteDownloadedFile(this@MainActivity, it) },
            logDownloadError = { if (outputData != null) viewModel.logDownloadError(outputData) },
        )
    }

    private fun NavGraphBuilder.newsListScreen(
        navType: NavType,
        windowSize: WindowSizeClass,
        state: RefreshAwareState<List<NewsItem>>,
    ) = composable(NewsListRoute) {
        LaunchedEffect(Unit) {
            // Avoid refreshing every time this screen is visited by guessing
            // if it's the first load (`refreshing` is true only initially)
            if (state.refreshing) newsListViewModel.refresh()
        }

        NewsListScreen(
            navType = navType,
            windowSize = windowSize,
            state = state,
            refresh = {
                viewModel.fetchServerStatus()
                newsListViewModel.refresh()
            },
            unreadCountState = newsListViewModel.unreadCount,
            markAllRead = newsListViewModel::markAllRead,
            toggleRead = newsListViewModel::toggleRead,
            openItem = ::startNewsItemActivity,
        )
    }

    private fun NavGraphBuilder.deviceScreen(
        navType: NavType,
        windowWidthSize: WindowWidthSizeClass,
        allDevices: List<Device>?,
    ) = composable(DeviceRoute) {
        DeviceScreen(
            navType = navType,
            windowWidthSize = windowWidthSize,
            deviceName = remember(allDevices) {
                allDevices?.find {
                    it.productNames.contains(SystemVersionProperties.oxygenDeviceName)
                }?.name ?: defaultDeviceName()
            },
            deviceOsSpec = viewModel.deviceOsSpec,
            deviceMismatchStatus = viewModel.deviceMismatch,
        )
    }

    private fun NavGraphBuilder.aboutScreen(
        navType: NavType,
        windowWidthSize: WindowWidthSizeClass,
    ) = composable(AboutRoute) {
        AboutScreen(
            navType = navType,
            windowWidthSize = windowWidthSize,
        )
    }

    private fun NavGraphBuilder.settingsScreen(
        navType: NavType,
        cachedEnabledDevices: List<Device>?,
        updateMismatchStatus: () -> Unit,
        openAboutScreen: () -> Unit,
    ) = composable(SettingsRoute) {
        PassthroughEnabledDevicesToSettingsViewModel(cachedEnabledDevices)

        val adFreePrice by billingViewModel.adFreePrice.collectAsStateWithLifecycle(null)
        val adFreeState by billingViewModel.adFreeState.collectAsStateWithLifecycle(null)

        val adFreeConfig = adFreeConfig(
            state = adFreeState,
            makePurchase = { billingViewModel.makePurchase(this@MainActivity, it) },
            markPending = { showToast(R.string.purchase_error_pending_payment) },
        )

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
        SettingsScreen(
            navType = navType,
            lists = state,
            initialDeviceIndex = settingsViewModel.initialDeviceIndex,
            deviceChanged = {
                settingsViewModel.saveSelectedDevice(it)
                updateMismatchStatus()
            },
            initialMethodIndex = settingsViewModel.initialMethodIndex,
            methodChanged = {
                settingsViewModel.saveSelectedMethod(it)

                if (checkPlayServices(this@MainActivity, true)) {
                    // Subscribe to notifications for the newly selected device and update method
                    NotificationTopicSubscriber.resubscribeIfNeeded(state.enabledDevices, state.methodsForDevice)
                } else showToast(R.string.notification_no_notification_support)
            },
            adFreePrice = adFreePrice,
            adFreeConfig = adFreeConfig,
            isPrivacyOptionsRequired = consentInformation.privacyOptionsRequirementStatus == PrivacyOptionsRequirementStatus.REQUIRED,
            showPrivacyOptionsForm = {
                UserMessagingPlatform.showPrivacyOptionsForm(this@MainActivity) { error ->
                    logUmpConsentFormError(TAG, error)
                }
            },
            openAboutScreen = openAboutScreen,
        )
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

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setupUmp()

        val analytics by inject<FirebaseAnalytics>()
        analytics.setUserProperty("device_name", SystemVersionProperties.oxygenDeviceName)

        if (!checkPlayServices(this, false)) showToast(R.string.notification_no_notification_support)

        lifecycle.addObserver(billingViewModel.lifecycleObserver)

        // Must be before calling setContent, because it uses intent-values
        handleIntent(intent)

        setContent {
            val windowSize = calculateWindowSizeClass(this)

            AppTheme {
                EdgeToEdge()

                // We're using Surface to avoid Scaffold's recomposition-on-scroll issue (when using scrollBehaviour and consuming innerPadding)
                Surface {
                    Crossfade(
                        targetState = viewModel.shouldShowOnboarding,
                        label = "OnboardingMainCrossfade",
                    ) {
                        if (it) OnboardingContent(windowSize) else {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) NotificationPermission()
                            Content(windowSize)
                        }
                    }
                }
            }
        }

        // TODO(root): move this to the proper place
        hasRootAccess {
            if (!it) return@hasRootAccess ContributorUtils.stopDbCheckingProcess(this)

            ContributorUtils.startDbCheckingProcess(this)
        }
    }

    private fun handleIntent(intent: Intent?) {
        // Action will be set if opened from a shortcut
        startPage = when (intent?.action) {
            ActionPageUpdate -> PageUpdate
            ActionPageNews -> PageNews
            ActionPageDevice -> PageDevice
            else -> PageUpdate
        }

        // Force-show download error dialog if started from an intent with error info
        // TODO(compose/update): download error dialog logic is simplified in UpdateScreen. Verify if the dialog meant
        //  to be shown after clicking the download failed notification (i.e. on activity start) is shown
        //  immediately in the UI itself, in both cases: app in foreground and background/killed.
        //  Otherwise, see KEY_DOWNLOAD_ERROR_MESSAGE in UIF.setupServerResponseObservers()
        downloadErrorMessage = try {
            intent?.getStringExtra(KeyDownloadErrorMessage)
        } catch (ignored: IndexOutOfBoundsException) {
            null
        }
    }

    private lateinit var consentInformation: ConsentInformation
    private fun setupUmp() {
        @SuppressLint("HardwareIds")
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()

        consentInformation = UserMessagingPlatform.getConsentInformation(this)
        consentInformation.requestConsentInfoUpdate(this, params, {
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(this@MainActivity) { error ->
                logUmpConsentFormError(TAG, error)

                if (consentInformation.canRequestAds()) (application as? OxygenUpdater)?.setupMobileAds()
            }
        }, { error -> logUmpConsentFormError(TAG, error) })

        // Check if SDK can be initialized in parallel while checking for new consent info.
        // Consent obtained in the previous session can be used to request ads.
        if (consentInformation.canRequestAds()) (application as? OxygenUpdater)?.setupMobileAds()
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
        val deviceId = PrefManager.getLong(PrefManager.KeyDeviceId, NotSetL)
        val updateMethodId = PrefManager.getLong(PrefManager.KeyUpdateMethodId, NotSetL)

        if (deviceId == NotSetL || updateMethodId == NotSetL) {
            logWarning(TAG, "Required preferences not valid: $deviceId, $updateMethodId")
            showToast(R.string.settings_entered_incorrectly)
        } else showToast(R.string.settings_saving)
    }

    companion object {
        private const val TAG = "MainActivity"

        private const val ActionPageUpdate = "com.oxygenupdater.action.page_update"
        private const val ActionPageNews = "com.oxygenupdater.action.page_news"
        private const val ActionPageDevice = "com.oxygenupdater.action.page_device"

        const val PageUpdate = 0
        const val PageNews = 1
        const val PageDevice = 2
        const val PageAbout = 3
        const val PageSettings = 4
        const val IntentStartPage = "start_page"
    }
}
