package com.oxygenupdater.compose.activities

import android.content.IntentSender
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.AlertDialog
import androidx.compose.material.Badge
import androidx.compose.material.BadgedBox
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.ContentAlpha
import androidx.compose.material.DropdownMenu
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProgressIndicatorDefaults
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SnackbarResult
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlaylistAddCheck
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy
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
import androidx.navigation.NavOptions
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.Purchase
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.android.gms.ads.AdView
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback
import com.google.android.play.core.install.model.ActivityResult
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.oxygenupdater.BuildConfig
import com.oxygenupdater.OxygenUpdater
import com.oxygenupdater.R
import com.oxygenupdater.compose.icons.Announcement
import com.oxygenupdater.compose.icons.CustomIcons
import com.oxygenupdater.compose.icons.PlayStore
import com.oxygenupdater.compose.ui.RefreshAwareState
import com.oxygenupdater.compose.ui.TopAppBar
import com.oxygenupdater.compose.ui.TopAppBarDefaults
import com.oxygenupdater.compose.ui.about.AboutScreen
import com.oxygenupdater.compose.ui.common.BannerAd
import com.oxygenupdater.compose.ui.common.DropdownMenuItem
import com.oxygenupdater.compose.ui.common.ItemDivider
import com.oxygenupdater.compose.ui.common.OutlinedIconButton
import com.oxygenupdater.compose.ui.common.edgeToEdge
import com.oxygenupdater.compose.ui.device.DeviceScreen
import com.oxygenupdater.compose.ui.device.IncorrectDeviceDialog
import com.oxygenupdater.compose.ui.device.UnsupportedDeviceOsSpecDialog
import com.oxygenupdater.compose.ui.device.defaultDeviceName
import com.oxygenupdater.compose.ui.dialogs.AdvancedModeSheet
import com.oxygenupdater.compose.ui.dialogs.ContributorSheet
import com.oxygenupdater.compose.ui.dialogs.LanguageSheet
import com.oxygenupdater.compose.ui.dialogs.ModalBottomSheet
import com.oxygenupdater.compose.ui.dialogs.NonCancellableDialog
import com.oxygenupdater.compose.ui.dialogs.SelectableSheet
import com.oxygenupdater.compose.ui.dialogs.ServerMessagesSheet
import com.oxygenupdater.compose.ui.dialogs.SheetType
import com.oxygenupdater.compose.ui.dialogs.ThemeSheet
import com.oxygenupdater.compose.ui.dialogs.defaultModalBottomSheetState
import com.oxygenupdater.compose.ui.main.AboutRoute
import com.oxygenupdater.compose.ui.main.DeviceRoute
import com.oxygenupdater.compose.ui.main.NewsListRoute
import com.oxygenupdater.compose.ui.main.Screen
import com.oxygenupdater.compose.ui.main.ServerStatusBanner
import com.oxygenupdater.compose.ui.main.SettingsRoute
import com.oxygenupdater.compose.ui.main.UpdateRoute
import com.oxygenupdater.compose.ui.news.NewsListScreen
import com.oxygenupdater.compose.ui.news.NewsListViewModel
import com.oxygenupdater.compose.ui.news.previousUnreadCount
import com.oxygenupdater.compose.ui.settings.SettingsScreen
import com.oxygenupdater.compose.ui.settings.SettingsViewModel
import com.oxygenupdater.compose.ui.theme.AppTheme
import com.oxygenupdater.compose.ui.theme.backgroundVariant
import com.oxygenupdater.compose.ui.theme.light
import com.oxygenupdater.compose.ui.theme.positive
import com.oxygenupdater.compose.ui.update.KEY_DOWNLOAD_ERROR_MESSAGE
import com.oxygenupdater.compose.ui.update.UpdateInformationViewModel
import com.oxygenupdater.compose.ui.update.UpdateScreen
import com.oxygenupdater.enums.PurchaseType
import com.oxygenupdater.exceptions.GooglePlayBillingException
import com.oxygenupdater.extensions.openPlayStorePage
import com.oxygenupdater.extensions.setLocale
import com.oxygenupdater.extensions.startNewsItemActivity
import com.oxygenupdater.extensions.toLanguageCode
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.models.Device
import com.oxygenupdater.models.DeviceOsSpec
import com.oxygenupdater.models.NewsItem
import com.oxygenupdater.models.ServerStatus
import com.oxygenupdater.models.SystemVersionProperties
import com.oxygenupdater.repositories.BillingRepository.SkuState
import com.oxygenupdater.utils.ContributorUtils
import com.oxygenupdater.utils.Logger.logDebug
import com.oxygenupdater.utils.Logger.logError
import com.oxygenupdater.utils.Logger.logWarning
import com.oxygenupdater.utils.NotificationTopicSubscriber
import com.oxygenupdater.utils.SetupUtils
import com.oxygenupdater.utils.Utils
import com.oxygenupdater.utils.Utils.checkPlayServices
import com.oxygenupdater.utils.hasRootAccess
import com.oxygenupdater.viewmodels.BillingViewModel
import com.oxygenupdater.viewmodels.MainViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Locale
import java.util.Timer
import kotlin.concurrent.schedule

class MainActivity : ComposeBaseActivity() {

    private val viewModel by viewModel<MainViewModel>()
    private val updateViewModel by viewModel<UpdateInformationViewModel>()
    private val newsListViewModel by viewModel<NewsListViewModel>()
    private val settingsViewModel by viewModel<SettingsViewModel>()
    private val billingViewModel by viewModel<BillingViewModel>()
    private val crashlytics by inject<FirebaseCrashlytics>()

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

    @Composable
    private fun SystemBars() {
        val colors = MaterialTheme.colors
        val controller = rememberSystemUiController()
        val darkIcons = colors.light
        controller.setNavigationBarColor(colors.backgroundVariant, darkIcons)
        controller.setStatusBarColor(colors.surface, darkIcons)
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    private fun Content() {
        val navController = rememberNavController()
        val scope = rememberCoroutineScope()
        val sheetState = defaultModalBottomSheetState()

        val allDevices by viewModel.deviceState.collectAsStateWithLifecycle()
        LaunchedEffect(allDevices) {
            viewModel.deviceOsSpec = Utils.checkDeviceOsSpec(allDevices)
            viewModel.mismatchStatus = Utils.checkDeviceMismatch(this@MainActivity, allDevices)
        }

        val showDeviceBadge = viewModel.deviceOsSpec.let {
            it != null && it != DeviceOsSpec.SUPPORTED_OXYGEN_OS
        } || viewModel.mismatchStatus.let { it != null && it.first }
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
            Screen.Device
        }

        val showDeviceWarningDialog = !PrefManager.getBoolean(PrefManager.PROPERTY_IGNORE_UNSUPPORTED_DEVICE_WARNINGS, false)
        val showIncorrectDeviceDialog = !PrefManager.getBoolean(PrefManager.PROPERTY_IGNORE_INCORRECT_DEVICE_WARNINGS, false)
        if (showDeviceWarningDialog) viewModel.deviceOsSpec?.let {
            UnsupportedDeviceOsSpecDialog(it)
        }

        if (showIncorrectDeviceDialog) viewModel.mismatchStatus?.let {
            IncorrectDeviceDialog(it)
        }

        val scaffoldState = rememberScaffoldState()
        Snackbar(scaffoldState.snackbarHostState)
        AppUpdateInfo()

        // Display the "No connection" banner if required
        val isNetworkAvailable by OxygenUpdater.isNetworkAvailable.observeAsState(true)
        if (!isNetworkAvailable) viewModel.snackbarText = NoConnectionSnackbarData
        else if (viewModel.snackbarText?.first == NoConnectionSnackbarData.first) {
            // Dismiss only this snackbar
            viewModel.snackbarText = null
        }

        val serverMessages by viewModel.serverMessages.collectAsStateWithLifecycle()

        val initialSubtitle = startScreen.subtitle ?: stringResource(startScreen.labelResId)
        var subtitle by remember { mutableStateOf(initialSubtitle) }
        var showMarkAllRead by remember { mutableStateOf(false) }
        var sheetType by remember { mutableStateOf(SheetType.None) }

        val colors = MaterialTheme.colors
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
        Scaffold(Modifier.edgeToEdge(), scaffoldState, topBar = {
            TopAppBar(scrollBehavior, {
                navController.navigateWithDefaults(AboutRoute)
            }, subtitle) {
                // Server-provided info & warning messages
                if (serverMessages.isNotEmpty()) IconButton({
                    sheetType = SheetType.ServerMessages
                    scope.launch { sheetState.show() }
                }, Modifier.requiredWidth(40.dp)) {
                    Icon(CustomIcons.Announcement, stringResource(R.string.update_information_banner_server), Modifier.requiredSize(24.dp))
                }

                val showBecomeContributor = ContributorUtils.isAtLeastQAndPossiblyRooted
                // Don't show menu if there are no items in it
                // Box layout is required to make DropdownMenu position correctly (directly under icon)
                if (showMarkAllRead || showBecomeContributor) Box {
                    // Hide other menu items behind overflow icon
                    var showMenu by remember { mutableStateOf(false) }
                    IconButton({ showMenu = true }, Modifier.requiredWidth(40.dp)) {
                        Icon(Icons.Rounded.MoreVert, stringResource(androidx.compose.ui.R.string.dropdown_menu), Modifier.requiredSize(24.dp))
                    }

                    Menu(showMenu, {
                        showMenu = false
                    }, showMarkAllRead, showBecomeContributor, openContributorSheet = {
                        sheetType = SheetType.Contributor
                        scope.launch { sheetState.show() }
                    })
                }
            }
        }, bottomBar = {
            BottomNavigation(Modifier.height(64.dp), colors.backgroundVariant, elevation = 0.dp) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                currentRoute = navBackStackEntry?.destination?.route
                showMarkAllRead = currentRoute == NewsListRoute

                // TODO(compose/main): use filled icons when selected and default colors with M3
                val primary = colors.primary
                val defaultMedium = LocalContentColor.current.copy(alpha = ContentAlpha.medium)

                screens.forEach { screen ->
                    val route = screen.route
                    val label = stringResource(screen.labelResId)
                    val selected = currentRoute == route
                    if (selected) subtitle = screen.subtitle ?: label
                    BottomNavigationItem(selected, {
                        if (sheetState.isVisible) scope.launch { sheetState.hide() }
                        navController.navigateWithDefaults(route)
                    }, icon = {
                        val badge = screen.badge
                        if (badge == null) Icon(screen.icon, label) else BadgedBox({
                            Badge(backgroundColor = colors.primary) {
                                Text("$badge".take(3), Modifier.semantics {
                                    contentDescription = "$badge unread articles"
                                })
                            }
                        }) { Icon(screen.icon, label) }
                    }, label = {
                        Text(label)
                    }, alwaysShowLabel = false, selectedContentColor = primary, unselectedContentColor = defaultMedium)
                }
            }
        }) { innerPadding ->
            Box(Modifier.padding(innerPadding)) {
                val hide: () -> Unit = remember(scope, sheetState) {
                    {
                        sheetType = SheetType.None
                        // Action passed for clicking close button in the content
                        scope.launch { sheetState.hide() }
                    }
                }

                LaunchedEffect(Unit) { // run only on init
                    // Hide empty sheet in case activity was recreated or config was changed
                    if (sheetState.isVisible && sheetType == SheetType.None) sheetState.hide()

                    // Offer contribution to users from app versions below v2.4.0 and v5.10.1
                    if (ContributorUtils.isAtLeastQAndPossiblyRooted && !PrefManager.contains(PrefManager.PROPERTY_CONTRIBUTE)) {
                        sheetType = SheetType.Contributor
                        sheetState.show()
                    }
                }

                var selectedLanguageCode by remember {
                    val defaultLanguageCode = Locale.getDefault().toLanguageCode()
                    mutableStateOf(PrefManager.getString(PrefManager.PROPERTY_LANGUAGE_ID, defaultLanguageCode) ?: defaultLanguageCode)
                }

                val listState = rememberLazyListState()

                // TODO(compose/main): bottom sheets don't draw over top & bottom bars, see if it can be rearranged.
                //  Do it in other places too (e.g. OnboardingScreen)
                ModalBottomSheet({
                    when (sheetType) {
                        SheetType.Device -> SelectableSheet(
                            hide,
                            listState, enabledDevices,
                            settingsViewModel.initialDeviceIndex,
                            R.string.settings_device, R.string.onboarding_page_2_caption,
                            keyId = PrefManager.PROPERTY_DEVICE_ID, keyName = PrefManager.PROPERTY_DEVICE,
                        ) {
                            settingsViewModel.saveSelectedDevice(it)
                            viewModel.mismatchStatus = Utils.checkDeviceMismatch(this@MainActivity, allDevices)
                        }

                        SheetType.Contributor -> ContributorSheet(hide, true)
                        SheetType.ServerMessages -> ServerMessagesSheet(hide, serverMessages)

                        SheetType.Theme -> ThemeSheet(hide) {
                            PrefManager.theme = it
                        }

                        SheetType.Language -> LanguageSheet(hide, selectedLanguageCode) {
                            selectedLanguageCode = it
                            setLocale(it)
                            recreate()
                        }

                        SheetType.AdvancedMode -> AdvancedModeSheet(hide) {
                            PrefManager.putBoolean(PrefManager.PROPERTY_ADVANCED_MODE, it)
                            PrefManager.advancedMode.value = it
                        }

                        else -> {}
                    }
                }, sheetState) {
                    Column(Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)) {
                        val serverStatus by viewModel.serverStatus.collectAsStateWithLifecycle()
                        ServerStatusDialogs(serverStatus.status)
                        ServerStatusBanner(serverStatus)

                        // NavHost can't preload other composables, so in order to get NewsList's unread count early,
                        // we're using the initial state here itself. State is refreshed only once the user visits that
                        // screen, so it's easy on the server too (no unnecessarily eager requests).
                        // Note: can't use `by` here because it doesn't propagate to [newsListScreen]
                        val newsListState = newsListViewModel.state.collectAsStateWithLifecycle().value
                        LaunchedEffect(Unit) { // run only on init
                            val unreadCount = newsListState.data.count { !it.read }
                            if (unreadCount != previousUnreadCount) {
                                Screen.NewsList.badge = if (unreadCount == 0) null else "$unreadCount"
                                previousUnreadCount = unreadCount
                            }
                        }

                        // TODO(compose/main): use SavedStateHandle in ViewModels to work on "cached" data on init
                        NavHost(navController, startScreen.route, Modifier.weight(1f)) {
                            updateScreen { subtitle = it }
                            newsListScreen(newsListState)
                            deviceScreen(allDevices)
                            aboutScreen()
                            settingsScreen(enabledDevices, selectedLanguageCode, showBottomSheet = {
                                sheetType = it
                                scope.launch { sheetState.show() }
                            }) {
                                navController.navigateWithDefaults(AboutRoute)
                            }
                        }

                        // Ads should be shown if user hasn't bought the ad-free unlock
                        val showAds = !billingViewModel.hasPurchasedAdFree.collectAsStateWithLifecycle(
                            !PrefManager.getBoolean(PrefManager.PROPERTY_AD_FREE, false)
                        ).value
                        if (showAds) {
                            ItemDivider()
                            val adLoaded = remember { mutableStateOf(false) }
                            BannerAd(BuildConfig.AD_BANNER_MAIN_ID, adLoaded) { bannerAdView = it }
                        }
                    }

                    // This must be defined on the same level as NavHost, otherwise it won't work
                    // We can safely put it outside Column because it's an inline composable
                    BackHandler {
                        if (sheetState.isVisible) scope.launch { sheetState.hide() }
                        else navController.run {
                            if (shouldStopNavigateAwayFromSettings()) showSettingsWarning()
                            else if (!popBackStack()) finishAffinity() // nothing to back to => exit
                        }
                    }
                }
                FlexibleAppUpdateProgress() // gets placed below TopAppBar
            }
        }
    }

    @Composable
    private fun Snackbar(snackbarHostState: SnackbarHostState) {
        val data = viewModel.snackbarText
        if (data == null) {
            snackbarHostState.currentSnackbarData?.dismiss()
            return
        }

        val context = this
        LaunchedEffect(data) {
            val actionResId = data.second
            val result = snackbarHostState.showSnackbar(
                getString(data.first), getString(actionResId), SnackbarDuration.Indefinite
            )
            if (result != SnackbarResult.ActionPerformed) return@LaunchedEffect

            when (actionResId) {
                AppUpdateFailedSnackbarData.second -> context.openPlayStorePage()
                AppUpdateDownloadedSnackbarData.second -> viewModel.completeAppUpdate()
            }
        }
    }

    @Composable
    private fun AppUpdateInfo() {
        val info = viewModel.appUpdateInfo.collectAsStateWithLifecycle().value ?: return

        val status = info.installStatus()
        if (status == InstallStatus.DOWNLOADED) {
            viewModel.unregisterAppUpdateListener()
            viewModel.snackbarText = AppUpdateDownloadedSnackbarData
        } else {
            if (viewModel.snackbarText?.first == AppUpdateDownloadedSnackbarData.first) {
                // Dismiss only this snackbar
                viewModel.snackbarText = null
            }

            /**
             * Control comes back to the activity in the form of a result only for a [AppUpdateType.FLEXIBLE] update request,
             * since an [AppUpdateType.IMMEDIATE] update is entirely handled by Google Play, with the exception of resuming an installation.
             * Check [onResume] for more info on how this is handled.
             */
            val launcher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartIntentSenderForResult()
            ) {
                val code = it.resultCode
                if (code == RESULT_OK) {
                    // Reset ignore count
                    PrefManager.putInt(PrefManager.PROPERTY_FLEXIBLE_APP_UPDATE_IGNORE_COUNT, 0)
                    if (viewModel.snackbarText?.first == AppUpdateFailedSnackbarData.first) {
                        // Dismiss only this snackbar
                        viewModel.snackbarText = null
                    }
                } else if (code == RESULT_CANCELED) {
                    // Increment ignore count and show app update banner
                    PrefManager.incrementInt(PrefManager.PROPERTY_FLEXIBLE_APP_UPDATE_IGNORE_COUNT)
                    viewModel.snackbarText = AppUpdateFailedSnackbarData
                } else if (code == ActivityResult.RESULT_IN_APP_UPDATE_FAILED) {
                    // Show app update banner
                    viewModel.snackbarText = AppUpdateFailedSnackbarData
                }
            }

            try {
                val availability = info.updateAvailability()
                if (availability == UpdateAvailability.UPDATE_AVAILABLE) {
                    viewModel.requestUpdate(launcher, info)
                } else if (availability == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    // If an IMMEDIATE update is in the stalled state, we should resume it
                    viewModel.requestImmediateAppUpdate(launcher, info)
                }
            } catch (e: IntentSender.SendIntentException) {
                // no-op
            }
        }
    }

    @Composable
    private fun FlexibleAppUpdateProgress() {
        val state = viewModel.appUpdateStatus.collectAsStateWithLifecycle().value ?: return
        val status = state.installStatus()

        if (status == InstallStatus.DOWNLOADED) {
            viewModel.snackbarText = AppUpdateDownloadedSnackbarData
        } else if (viewModel.snackbarText?.first == AppUpdateDownloadedSnackbarData.first) {
            // Dismiss only this snackbar
            viewModel.snackbarText = null
        }

        if (status == InstallStatus.PENDING) LinearProgressIndicator(Modifier.fillMaxWidth())
        else if (status == InstallStatus.DOWNLOADING) {
            val bytesDownloaded = state.bytesDownloaded().toFloat()
            val totalBytesToDownload = state.totalBytesToDownload().coerceAtLeast(1)
            val progress = bytesDownloaded / totalBytesToDownload
            val animatedProgress by animateFloatAsState(
                progress, ProgressIndicatorDefaults.ProgressAnimationSpec,
                label = "FlexibleUpdateProgressAnimation"
            )
            LinearProgressIndicator(animatedProgress, Modifier.fillMaxWidth())
        }
    }

    private fun NavController.navigateWithDefaults(
        route: String,
    ) = if (shouldStopNavigateAwayFromSettings()) showSettingsWarning() else navigate(route, navOptions)

    @Composable
    private fun ServerStatusDialogs(status: ServerStatus.Status?) {
        if (status?.isNonRecoverableError != true) return

        if (status == ServerStatus.Status.MAINTENANCE) {
            var show by remember { mutableStateOf(true) }
            if (show) AlertDialog({ show = false }, confirmButton = {}, dismissButton = {
                TextButton({ show = false }) {
                    Text(stringResource(R.string.download_error_close))
                }
            }, title = {
                Text(stringResource(R.string.error_maintenance))
            }, text = {
                Text(stringResource(R.string.error_maintenance_message))
            }, properties = NonCancellableDialog)
        } else if (status == ServerStatus.Status.OUTDATED) {
            var show by remember { mutableStateOf(true) }
            if (show) AlertDialog({ show = false }, confirmButton = {
                OutlinedIconButton({
                    show = false
                    openPlayStorePage()
                }, CustomIcons.PlayStore, R.string.error_google_play_button_text, MaterialTheme.colors.positive)
            }, dismissButton = {
                TextButton({ show = false }) {
                    Text(stringResource(R.string.download_error_close))
                }
            }, title = {
                Text(stringResource(R.string.error_app_outdated))
            }, text = {
                Text(stringResource(R.string.error_app_outdated_message))
            }, properties = NonCancellableDialog)
        }
    }

    private fun NavGraphBuilder.updateScreen(setSubtitle: (String) -> Unit) = composable(UpdateRoute) {
        if (!PrefManager.checkIfSetupScreenHasBeenCompleted()) return@composable

        LaunchedEffect(Unit) { // runs once every time this screen is visited
            settingsViewModel.updateCrashlyticsUserId()
        }

        // TODO(compose/update): restore savedState when re-visiting this screen
        val state by updateViewModel.state.collectAsStateWithLifecycle()
        val workInfoWithStatus by viewModel.workInfoWithStatus.collectAsStateWithLifecycle()

        UpdateScreen(state, workInfoWithStatus, downloadErrorMessage != null, {
            settingsViewModel.updateCrashlyticsUserId()
            viewModel.fetchServerStatus()
            updateViewModel.refresh()
        }, setSubtitle, enqueueDownload = {
            viewModel.setupDownloadWorkRequest(it)
            viewModel.enqueueDownloadWork()
        }, pauseDownload = {
            viewModel.pauseDownloadWork()
        }, cancelDownload = {
            viewModel.cancelDownloadWork(this@MainActivity, it)
        }, deleteDownload = {
            viewModel.deleteDownloadedFile(this@MainActivity, it)
        }) {
            viewModel.logDownloadError(it)
        }
    }

    private fun NavGraphBuilder.newsListScreen(state: RefreshAwareState<List<NewsItem>>) = composable(NewsListRoute) {
        LaunchedEffect(Unit) {
            // Avoid refreshing every time this screen is visited by guessing
            // if it's the first load (`refreshing` is true only initially)
            if (state.refreshing) newsListViewModel.refresh()
        }

        NewsListScreen(state, refresh = {
            viewModel.fetchServerStatus()
            newsListViewModel.refresh()
        }, markAllRead = {
            newsListViewModel.markAllRead()
        }, toggleRead = {
            newsListViewModel.toggleRead(it)
        }) {
            // TODO(compose/news): handle shared element transition, see `movableContentOf`
            startNewsItemActivity(it)
        }
    }

    private fun NavGraphBuilder.deviceScreen(allDevices: List<Device>) = composable(DeviceRoute) {
        val defaultDeviceName = defaultDeviceName()
        DeviceScreen(remember(allDevices) {
            allDevices.find {
                it.productNames.contains(SystemVersionProperties.oxygenDeviceName)
            }?.name ?: defaultDeviceName
        }, viewModel.deviceOsSpec, viewModel.mismatchStatus)
    }

    private fun NavGraphBuilder.aboutScreen() = composable(AboutRoute) { AboutScreen() }

    private fun NavGraphBuilder.settingsScreen(
        cachedEnabledDevices: List<Device>,
        selectedLanguageCode: String,
        showBottomSheet: (SheetType) -> Unit,
        openAboutScreen: () -> Unit,
    ) = composable(SettingsRoute) {
        LaunchedEffect(cachedEnabledDevices) {
            // Passthrough from MainViewModel to avoid sending a request again
            settingsViewModel.fetchEnabledDevices(cachedEnabledDevices)
        }

        val (enabledDevices, methodsForDevice) = settingsViewModel.state.collectAsStateWithLifecycle().value

        // TODO(compose/settings): test all this thoroughly
        val adFreePrice by billingViewModel.adFreePrice.collectAsStateWithLifecycle(null)
        val adFreeState by billingViewModel.adFreeState.collectAsStateWithLifecycle(null)

        val pendingObserver = remember {
            Observer<Purchase?> {
                Toast.makeText(this@MainActivity, getString(R.string.purchase_error_pending_payment), Toast.LENGTH_LONG).show()
            }
        }

        val adFreeConfig by adFreeConfig(adFreeState, pendingObserver)

        BillingObservers(adFreePrice, pendingObserver)

        SettingsScreen(enabledDevices, methodsForDevice, settingsViewModel.initialMethodIndex, {
            settingsViewModel.saveSelectedMethod(it)

            if (checkPlayServices(this@MainActivity, true)) {
                // Subscribe to notifications for the newly selected device and update method
                NotificationTopicSubscriber.resubscribeIfNeeded(enabledDevices, methodsForDevice)
            } else Toast.makeText(
                this@MainActivity,
                this@MainActivity.getString(R.string.notification_no_notification_support),
                Toast.LENGTH_LONG
            ).show()
        }, selectedLanguageCode, adFreePrice, adFreeConfig, openAboutScreen, showBottomSheet)
    }

    @Composable
    private fun adFreeConfig(state: SkuState?, pendingObserver: Observer<Purchase?>) = remember {
        derivedStateOf(structuralEqualityPolicy()) {
            when (state) {
                SkuState.UNKNOWN -> {
                    logError(TAG, GooglePlayBillingException("SKU '${PurchaseType.AD_FREE.sku}' is not available"))
                    Triple(false, R.string.settings_buy_button_not_possible, null)
                }

                SkuState.NOT_PURCHASED -> Triple(true, R.string.settings_buy_button_buy) {
                    // TODO(compose/settings): disable the Purchase button and set its text to "Processing…"
                    // isEnabled = false
                    // summary = mContext.getString(R.string.processing)

                    // [newPurchaseObserver] handles the result
                    billingViewModel.makePurchase(this@MainActivity, PurchaseType.AD_FREE)
                }

                SkuState.PENDING -> {
                    pendingObserver.onChanged(null)
                    Triple(false, R.string.processing, null)
                }

                SkuState.PURCHASED_AND_ACKNOWLEDGED -> Triple(false, R.string.settings_buy_button_bought, null)

                // PURCHASED => already bought, but not yet acknowledged by the app.
                // This should never happen, as it's already handled within BillingDataSource.
                null, SkuState.PURCHASED -> null
            }
        }
    }

    @Composable
    private fun BillingObservers(adFreePrice: String?, pendingObserver: Observer<Purchase?>) {
        // Note: we use `this` instead of LocalLifecycleOwner because the latter can change, which results
        // in an IllegalArgumentException (can't reuse the same observer with different lifecycles)

        // no-op observe because the actual work is being done in BillingViewModel
        billingViewModel.purchaseStateChange.observe(this, remember { Observer<Purchase> {} })
        billingViewModel.pendingPurchase.observe(this, pendingObserver)
        billingViewModel.newPurchase.observe(this, remember {
            Observer<Pair<Int, Purchase?>> {
                val (responseCode, purchase) = it
                when (responseCode) {
                    BillingResponseCode.OK -> if (purchase != null) validateAdFreePurchase(
                        purchase, adFreePrice, PurchaseType.AD_FREE
                    )

                    BillingResponseCode.USER_CANCELED -> logDebug(TAG, "Purchase of ad-free version was cancelled by the user")

                    else -> {
                        logError(TAG, GooglePlayBillingException("Purchase of the ad-free version failed due to an unknown error during the purchase flow: $responseCode"))
                        Toast.makeText(this, getString(R.string.purchase_error_after_payment), Toast.LENGTH_LONG).show()
                    }
                }
            }
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

            !it.success -> logError(
                TAG,
                GooglePlayBillingException("[validateAdFreePurchase] couldn't purchase ad-free: (${it.errorMessage})")
            )
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    override fun onCreate(
        savedInstanceState: Bundle?,
    ) = super.onCreate(savedInstanceState).also {
        val analytics by inject<FirebaseAnalytics>()
        analytics.setUserProperty("device_name", SystemVersionProperties.oxygenDeviceName)

        if (!checkPlayServices(this, false)) Toast.makeText(
            this, getString(R.string.notification_no_notification_support), Toast.LENGTH_LONG
        ).show()

        setContent {
            AppTheme {
                SystemBars()
                Content()
            }
        }

        setExitSharedElementCallback(MaterialContainerTransformSharedElementCallback())
        lifecycle.addObserver(billingViewModel.lifecycleObserver)

        // TODO(root): move this to the proper place
        hasRootAccess {
            if (!it) return@hasRootAccess ContributorUtils.stopDbCheckingProcess(this)

            ContributorUtils.startDbCheckingProcess(this)
        }
    }

    @Composable
    private inline fun Menu(
        expanded: Boolean,
        noinline onDismiss: () -> Unit,
        showMarkAllRead: Boolean,
        showBecomeContributor: Boolean,
        crossinline openContributorSheet: () -> Unit,
    ) = DropdownMenu(expanded, onDismiss) {
        // Mark all articles read
        if (showMarkAllRead) DropdownMenuItem(Icons.Rounded.PlaylistAddCheck, R.string.news_mark_all_read) {
            newsListViewModel.markAllRead()
            onDismiss()
        }

        // OTA URL contribution
        if (showBecomeContributor) DropdownMenuItem(Icons.Outlined.GroupAdd, R.string.contribute) {
            openContributorSheet()
            onDismiss()
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
        val deviceId = PrefManager.getLong(PrefManager.PROPERTY_DEVICE_ID, -1L)
        val updateMethodId = PrefManager.getLong(PrefManager.PROPERTY_UPDATE_METHOD_ID, -1L)

        if (deviceId == -1L || updateMethodId == -1L) {
            logWarning(TAG, SetupUtils.getAsError("Settings screen", deviceId, updateMethodId))
            Toast.makeText(this, getString(R.string.settings_entered_incorrectly), Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, getString(R.string.settings_saving), Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        private const val TAG = "MainActivity"

        private val NoConnectionSnackbarData = Pair(
            R.string.error_no_internet_connection, android.R.string.ok
        )

        private val AppUpdateDownloadedSnackbarData = Pair(
            R.string.new_app_version_inapp_downloaded, R.string.error_reload
        )

        private val AppUpdateFailedSnackbarData = Pair(
            R.string.new_app_version_inapp_failed, R.string.error_google_play_button_text
        )

        const val PAGE_UPDATE = 0
        const val PAGE_NEWS = 1
        const val PAGE_DEVICE = 2
        const val PAGE_ABOUT = 3
        const val PAGE_SETTINGS = 4
        const val INTENT_START_PAGE = "start_page"
    }
}
