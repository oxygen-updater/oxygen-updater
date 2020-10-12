package com.oxygenupdater.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.CheckBox
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.Observer
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.ads.MobileAds
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.install.model.ActivityResult
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus.DOWNLOADED
import com.google.android.play.core.install.model.InstallStatus.DOWNLOADING
import com.google.android.play.core.install.model.InstallStatus.FAILED
import com.google.android.play.core.install.model.InstallStatus.PENDING
import com.google.android.play.core.install.model.UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
import com.google.android.play.core.install.model.UpdateAvailability.UPDATE_AVAILABLE
import com.oxygenupdater.ActivityLauncher
import com.oxygenupdater.OxygenUpdater
import com.oxygenupdater.OxygenUpdater.Companion.buildAdRequest
import com.oxygenupdater.R
import com.oxygenupdater.dialogs.MessageDialog
import com.oxygenupdater.extensions.reduceDragSensitivity
import com.oxygenupdater.fragments.AboutFragment
import com.oxygenupdater.fragments.DeviceInformationFragment
import com.oxygenupdater.fragments.NewsFragment
import com.oxygenupdater.fragments.SettingsFragment
import com.oxygenupdater.fragments.UpdateInformationFragment
import com.oxygenupdater.internal.KotlinCallback
import com.oxygenupdater.internal.settings.SettingsManager
import com.oxygenupdater.models.DeviceOsSpec
import com.oxygenupdater.models.ServerStatus
import com.oxygenupdater.utils.Utils
import com.oxygenupdater.utils.Utils.checkPlayServices
import com.oxygenupdater.viewmodels.BillingViewModel
import com.oxygenupdater.viewmodels.MainViewModel
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.appBar
import kotlinx.android.synthetic.main.activity_main.toolbar
import kotlinx.android.synthetic.main.activity_main.viewPager
import kotlinx.android.synthetic.main.activity_onboarding.*
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.math.abs

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    private lateinit var noNetworkDialog: MessageDialog
    private lateinit var noConnectionSnackbar: Snackbar
    private lateinit var activityLauncher: ActivityLauncher

    private var downloadPermissionCallback: KotlinCallback<Boolean>? = null

    private val settingsManager by inject<SettingsManager>()
    private val mainViewModel by viewModel<MainViewModel>()
    private val billingViewModel by viewModel<BillingViewModel>()

    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            bottomNavigationView.menu.getItem(position)?.run {
                bottomNavigationView.selectedItemId = itemId

                when (position) {
                    PAGE_UPDATE, PAGE_NEWS, PAGE_DEVICE -> hideTabBadge(itemId, 1000)
                    else -> {
                        // no-op
                    }
                }

                updateToolbarForPage(itemId, title)
            }
        }
    }

    private val appUpdateAvailableObserver = Observer<AppUpdateInfo> { updateInfo ->
        if (updateInfo.installStatus() == DOWNLOADED) {
            mainViewModel.unregisterAppUpdateListener()
            showAppUpdateSnackbar()
        } else {
            try {
                when (updateInfo.updateAvailability()) {
                    UPDATE_AVAILABLE -> mainViewModel.requestUpdate(this, updateInfo)
                    // If an IMMEDIATE update is in the stalled state, we should resume it
                    DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> mainViewModel.requestImmediateAppUpdate(
                        this,
                        updateInfo
                    )
                }
            } catch (e: IntentSender.SendIntentException) {
                showAppUpdateBanner()
            }
        }
    }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) = super.onCreate(savedInstanceState).also {
        activityLauncher = ActivityLauncher(this)

        setupViewPager()

        // Offer contribution to users from app versions below 2.4.0
        if (!settingsManager.containsPreference(SettingsManager.PROPERTY_CONTRIBUTE)
            && settingsManager.containsPreference(SettingsManager.PROPERTY_SETUP_DONE)
        ) {
            activityLauncher.Contribute()
        }

        noConnectionSnackbar = Snackbar.make(
            coordinatorLayout,
            R.string.error_no_internet_connection,
            Snackbar.LENGTH_INDEFINITE
        ).apply {
            anchorView = bannerAdView
            setBackgroundTint(ContextCompat.getColor(this@MainActivity, R.color.colorError))
            setAction(getString(android.R.string.ok)) {
                dismiss()
            }
        }

        noNetworkDialog = MessageDialog(
            this,
            title = getString(R.string.error_app_requires_network_connection),
            message = getString(R.string.error_app_requires_network_connection_message),
            negativeButtonText = getString(R.string.download_error_close),
            cancellable = false
        )

        if (!Utils.checkNetworkConnection()) {
            noNetworkDialog.show()
        }

        setupLiveDataObservers()
    }

    override fun onResume() = super.onResume().also {
        bannerAdView?.resume()
        mainViewModel.checkForStalledAppUpdate().observe(
            this,
            appUpdateAvailableObserver
        )
    }

    override fun onPause() = super.onPause().also {
        bannerAdView?.pause()
    }

    override fun onDestroy() = super.onDestroy().also {
        bannerAdView?.destroy()
        noNetworkDialog.bypassListenerAndDismiss()
        mainViewModel.unregisterAppUpdateListener()
    }

    override fun onBackPressed() = if (viewPager.currentItem == 0) {
        // If the user is currently looking at the first step, allow the system to handle the
        // Back button. This calls finish() on this activity and pops the back stack.
        super.onBackPressed()
    } else {
        // Otherwise, reset to first page
        viewPager.currentItem = 0
    }

    override fun onRequestPermissionsResult(
        permsRequestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (permsRequestCode == PERMISSION_REQUEST_CODE && grantResults.isNotEmpty()) {
            downloadPermissionCallback?.invoke(grantResults[0] == PERMISSION_GRANTED)
        }
    }

    /**
     * Control comes back to the activity in the form of a result only for a [AppUpdateType.FLEXIBLE] update request,
     * since an [AppUpdateType.IMMEDIATE] update is entirely handled by Google Play, with the exception of resuming an installation.
     * Check [onResume] for more info on how this is handled.
     */
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) = super.onActivityResult(requestCode, resultCode, data).also {
        if (requestCode == REQUEST_CODE_APP_UPDATE) {
            when (resultCode) {
                // Reset ignore count
                Activity.RESULT_OK -> settingsManager.savePreference(
                    SettingsManager.PROPERTY_FLEXIBLE_APP_UPDATE_IGNORE_COUNT,
                    0
                )
                // Increment ignore count and show app update banner
                Activity.RESULT_CANCELED -> settingsManager.getPreference(
                    SettingsManager.PROPERTY_FLEXIBLE_APP_UPDATE_IGNORE_COUNT,
                    0
                ).let { ignoreCount ->
                    settingsManager.savePreference(
                        SettingsManager.PROPERTY_FLEXIBLE_APP_UPDATE_IGNORE_COUNT,
                        ignoreCount + 1
                    )
                    showAppUpdateBanner()
                }
                // Show app update banner
                ActivityResult.RESULT_IN_APP_UPDATE_FAILED -> showAppUpdateBanner()
            }
        }
    }

    private fun setupLiveDataObservers() {
        // display the "No connection" banner if required
        OxygenUpdater.isNetworkAvailable.observe(this) {
            if (it) {
                noConnectionSnackbar.dismiss()

                // Dismiss no network dialog if needed
                if (noNetworkDialog.isShowing) {
                    noNetworkDialog.bypassListenerAndDismiss()
                }
            } else {
                noConnectionSnackbar.show()
            }
        }

        billingViewModel.adFreeUnlockLiveData.observe(this) {
            // If it's null, user has not bought the ad-free unlock
            // Thus, ads should be shown
            setupAds(it == null || !it.entitled)
        }

        mainViewModel.maybeCheckForAppUpdate().observe(
            this,
            appUpdateAvailableObserver
        )

        mainViewModel.fetchAllDevices().observe(this) { deviceList ->
            mainViewModel.deviceOsSpec = Utils.checkDeviceOsSpec(deviceList)

            val showDeviceWarningDialog = !settingsManager.getPreference(
                SettingsManager.PROPERTY_IGNORE_UNSUPPORTED_DEVICE_WARNINGS,
                false
            )

            if (showDeviceWarningDialog && !mainViewModel.deviceOsSpec!!.isDeviceOsSpecSupported) {
                displayUnsupportedDeviceOsSpecMessage()
            } else {
                mainViewModel.deviceMismatchStatus = Utils.checkDeviceMismatch(this, deviceList)

                val showIncorrectDeviceDialog = !settingsManager.getPreference(
                    SettingsManager.PROPERTY_IGNORE_INCORRECT_DEVICE_WARNINGS,
                    false
                )

                if (showIncorrectDeviceDialog && mainViewModel.deviceMismatchStatus!!.first) {
                    displayIncorrectDeviceSelectedMessage()
                }
            }

            if (!mainViewModel.deviceOsSpec!!.isDeviceOsSpecSupported || mainViewModel.deviceMismatchStatus!!.first) {
                updateTabBadge(R.id.page_device)
            }

            // subscribe to notification topics
            // we're doing it here, instead of [SplashActivity], because it requires the app to be setup first
            // (`deviceId`, `updateMethodId`, etc need to be saved in [SharedPreferences])
            if (!settingsManager.containsPreference(SettingsManager.PROPERTY_NOTIFICATION_TOPIC)) {
                mainViewModel.subscribeToNotificationTopics(deviceList.filter { it.enabled })
            }
        }

        mainViewModel.serverStatus.observe(this) { serverStatus ->
            val shouldShowAppUpdateBanner = settingsManager.getPreference(
                SettingsManager.PROPERTY_SHOW_APP_UPDATE_MESSAGES,
                true
            )

            // Banner is displayed if app version is outdated
            if (shouldShowAppUpdateBanner && !serverStatus.checkIfAppIsUpToDate()) {
                showAppUpdateBanner(serverStatus)
            }
        }

        mainViewModel.appUpdateInstallStatus.observe(this) {
            when (it.installStatus()) {
                PENDING -> flexibleAppUpdateProgressBar.apply {
                    isVisible = true
                    isIndeterminate = true
                }
                DOWNLOADING -> flexibleAppUpdateProgressBar.apply {
                    isVisible = true
                    isIndeterminate = false
                    progress = (it.bytesDownloaded() * 100 / it.totalBytesToDownload()).toInt()
                }
                DOWNLOADED -> {
                    flexibleAppUpdateProgressBar.isVisible = false
                    showAppUpdateSnackbar()
                }
                FAILED -> {
                    flexibleAppUpdateProgressBar.isVisible = false
                    showAppUpdateBanner()
                }
                else -> flexibleAppUpdateProgressBar.isVisible = false
            }
        }

        mainViewModel.pageToolbarTextUpdated.observe(this) {
            if (it.first == bottomNavigationView.selectedItemId) {
                toolbar.subtitle = it.second
            }
        }
    }

    private fun showAppUpdateBanner(
        serverStatus: ServerStatus? = null
    ) = appUpdateBannerTextView.run {
        isVisible = true
        appUpdateBannerDivider.isVisible = true
        text = if (serverStatus == null) {
            getString(R.string.new_app_version_inapp_failed)
        } else {
            getString(R.string.new_app_version, serverStatus.latestAppVersion)
        }

        setOnClickListener {
            activityLauncher.openPlayStorePage(this@MainActivity)
        }
    }

    private fun showAppUpdateSnackbar() {
        Snackbar.make(
            coordinatorLayout,
            R.string.new_app_version_inapp_downloaded,
            Snackbar.LENGTH_INDEFINITE
        ).apply {
            anchorView = viewPager
            setAction(getString(R.string.error_reload)) {
                mainViewModel.completeAppUpdate()
            }
            show()
        }
    }

    private fun setupViewPager() {
        viewPager.apply {
            offscreenPageLimit = 1 // Enough to preload the "News" page
            adapter = MainPagerAdapter()
            reduceDragSensitivity()

            registerOnPageChangeCallback(pageChangeCallback)

            currentItem = try {
                intent?.extras?.getInt(INTENT_START_PAGE) ?: PAGE_UPDATE
            } catch (ignored: IndexOutOfBoundsException) {
                PAGE_UPDATE
            }
        }

        setupBottomNavigation()
        setupAppBarForViewPager()
        toolbar.setNavigationOnClickListener { showAboutPage() }
    }

    private fun setupAppBarForViewPager() {
        appBar.post {
            val totalScrollRange = appBar.totalScrollRange

            // Adjust bottom margin on first load
            contentLayout.updateLayoutParams<LayoutParams> {
                bottomMargin = totalScrollRange
            }

            // Adjust bottom margin on scroll
            appBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
                contentLayout.updateLayoutParams<LayoutParams> {
                    bottomMargin = totalScrollRange - abs(verticalOffset)
                }
            })
        }
    }

    private fun setupBottomNavigation() = bottomNavigationView.setOnNavigationItemSelectedListener {
        viewPager.currentItem = when (it.itemId) {
            R.id.page_update -> 0
            R.id.page_news -> 1
            R.id.page_device -> 2
            R.id.page_about -> 3
            R.id.page_settings -> 4
            else -> 0
        }
        true
    }

    fun showAboutPage() {
        viewPager.currentItem = PAGE_ABOUT
    }

    fun updateToolbarForPage(@IdRes pageId: Int, subtitle: CharSequence? = null) {
        if (pageId == bottomNavigationView.selectedItemId) {
            toolbar.subtitle = mainViewModel.pageToolbarSubtitle[pageId] ?: subtitle
        }
    }

    /**
     * Update the state of a [com.google.android.material.bottomnavigation.BottomNavigationView]'s [com.google.android.material.badge.BadgeDrawable]
     *
     * @param pageId pageId of the tab/fragment
     * @param show flag to control the badge's visibility
     * @param count optional number to display in the badge
     *
     * @see hideTabBadge
     */
    fun updateTabBadge(
        @IdRes pageId: Int,
        show: Boolean = true,
        count: Int? = null
    ) = bottomNavigationView.getOrCreateBadge(pageId)?.apply {
        isVisible = show

        if (isVisible && count != null /*&& count != 0*/) {
            number = count
            maxCharacterCount = 3
        }
    }

    /**
     * Hide the [com.google.android.material.bottomnavigation.BottomNavigationView]'s [com.google.android.material.badge.BadgeDrawable] after a specified delay
     *
     * Even though [updateTabBadge] can be used to hide a badge, this function is different because it only hides an existing badge, after a specified delay.
     * It's meant to be called from the [viewPager]'s `onPageSelected` callback, within this class.
     * [updateTabBadge] can be called from child fragments to hide the badge immediately, for example, if required after refreshing
     *
     * @param pageId pageId of the tab/fragment
     * @param delayMillis the delay, in milliseconds
     *
     * @see updateTabBadge
     */
    @Suppress("SameParameterValue")
    private fun hideTabBadge(
        @IdRes pageId: Int,
        delayMillis: Long = 0
    ) = bottomNavigationView.getBadge(pageId)?.apply {
        Handler().postDelayed({
            if (bottomNavigationView.selectedItemId == pageId) {
                isVisible = false
            }
        }, delayMillis)
    }

    /**
     * Checks for Play Services and initialises [MobileAds] if found
     */
    private fun setupAds(shouldShowAds: Boolean) {
        if (!checkPlayServices(this, false)) {
            Toast.makeText(this, getString(R.string.notification_no_notification_support), Toast.LENGTH_LONG).show()
        }

        bannerAdView?.apply {
            if (shouldShowAds) {
                isVisible = true
                loadAd(buildAdRequest())

                bannerAdDivider.isVisible = true
            } else {
                isVisible = false
                bannerAdDivider.isVisible = false
            }
        }
    }

    fun displayUnsupportedDeviceOsSpecMessage() {
        // Do not show dialog if app was already exited upon receiving of devices from the server.
        if (isFinishing) {
            return
        }

        val resourceId = when (mainViewModel.deviceOsSpec) {
            DeviceOsSpec.CARRIER_EXCLUSIVE_OXYGEN_OS -> R.string.carrier_exclusive_device_warning_message
            DeviceOsSpec.UNSUPPORTED_OXYGEN_OS -> R.string.unsupported_device_warning_message
            DeviceOsSpec.UNSUPPORTED_OS -> R.string.unsupported_os_warning_message
            else -> R.string.unsupported_os_warning_message
        }

        val checkBoxView = View.inflate(this@MainActivity, R.layout.message_dialog_checkbox, null)

        MaterialAlertDialogBuilder(this)
            .setView(checkBoxView)
            .setTitle(getString(R.string.unsupported_device_warning_title))
            .setMessage(getString(resourceId))
            .setPositiveButton(getString(R.string.download_error_close)) { dialog, _ ->
                val checkbox = checkBoxView.findViewById<CheckBox>(R.id.device_warning_checkbox)
                settingsManager.savePreference(SettingsManager.PROPERTY_IGNORE_UNSUPPORTED_DEVICE_WARNINGS, checkbox.isChecked)
                dialog.dismiss()
            }.show()
    }

    fun displayIncorrectDeviceSelectedMessage() {
        // Do not show dialog if app was already exited upon receiving of devices from the server.
        if (isFinishing) {
            return
        }

        val checkBoxView = View.inflate(this@MainActivity, R.layout.message_dialog_checkbox, null)

        MaterialAlertDialogBuilder(this)
            .setView(checkBoxView)
            .setTitle(getString(R.string.incorrect_device_warning_title))
            .setMessage(
                getString(
                    R.string.incorrect_device_warning_message,
                    mainViewModel.deviceMismatchStatus!!.second,
                    mainViewModel.deviceMismatchStatus!!.third
                )
            )
            .setPositiveButton(getString(R.string.download_error_close)) { dialog, _ ->
                val checkbox = checkBoxView.findViewById<CheckBox>(R.id.device_warning_checkbox)
                settingsManager.savePreference(SettingsManager.PROPERTY_IGNORE_INCORRECT_DEVICE_WARNINGS, checkbox.isChecked)
                dialog.dismiss()
            }.show()
    }

    fun requestDownloadPermissions(callback: KotlinCallback<Boolean>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            downloadPermissionCallback = callback
            requestPermissions(
                arrayOf(DOWNLOAD_FILE_PERMISSION, VERIFY_FILE_PERMISSION), PERMISSION_REQUEST_CODE
            )
        }
    }

    // Android 6.0 Run-time permissions
    fun hasDownloadPermissions() = ContextCompat.checkSelfPermission(this, VERIFY_FILE_PERMISSION) == PERMISSION_GRANTED
            && ContextCompat.checkSelfPermission(this, DOWNLOAD_FILE_PERMISSION) == PERMISSION_GRANTED

    /**
     * A [FragmentPagerAdapter] that returns a fragment corresponding to one of the sections/tabs/pages.
     */
    inner class MainPagerAdapter : FragmentStateAdapter(this) {

        /**
         * This is called to instantiate the fragment for the given page.
         * Return one of:
         * * [UpdateInformationFragment]
         * * [NewsFragment]
         * * [DeviceInformationFragment]
         * * [SettingsFragment]
         * * [SettingsFragment]
         */
        override fun createFragment(position: Int) = when (position) {
            PAGE_UPDATE -> UpdateInformationFragment()
            PAGE_NEWS -> NewsFragment()
            PAGE_DEVICE -> DeviceInformationFragment()
            PAGE_ABOUT -> AboutFragment()
            PAGE_SETTINGS -> SettingsFragment()
            else -> TODO()
        }

        /**
         * Show 5 total pages: Update, News, Device, About, and Settings
         */
        override fun getItemCount() = 5
    }

    companion object {
        private const val INTENT_START_PAGE = "start_page"

        private const val PAGE_UPDATE = 0
        private const val PAGE_NEWS = 1
        private const val PAGE_DEVICE = 2
        private const val PAGE_ABOUT = 3
        private const val PAGE_SETTINGS = 4

        // Permissions constants
        private const val DOWNLOAD_FILE_PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE
        const val PERMISSION_REQUEST_CODE = 200
        const val VERIFY_FILE_PERMISSION = Manifest.permission.READ_EXTERNAL_STORAGE

        const val REQUEST_CODE_APP_UPDATE = 1000
        const val DAYS_FOR_APP_UPDATE_CHECK = 2L
        const val MAX_APP_FLEXIBLE_UPDATE_STALE_DAYS = 14
        const val MAX_APP_FLEXIBLE_UPDATE_IGNORE_COUNT = 7
    }
}
