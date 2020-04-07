package com.arjanvlek.oxygenupdater.activities

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.MenuItem
import android.view.View
import android.widget.CheckBox
import android.widget.Toast
import androidx.annotation.IntRange
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.Observer
import androidx.lifecycle.observe
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.arjanvlek.oxygenupdater.ActivityLauncher
import com.arjanvlek.oxygenupdater.OxygenUpdater.Companion.buildAdRequest
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.dialogs.MessageDialog
import com.arjanvlek.oxygenupdater.fragments.DeviceInformationFragment
import com.arjanvlek.oxygenupdater.fragments.NewsFragment
import com.arjanvlek.oxygenupdater.fragments.UpdateInformationFragment
import com.arjanvlek.oxygenupdater.internal.KotlinCallback
import com.arjanvlek.oxygenupdater.internal.settings.SettingsManager
import com.arjanvlek.oxygenupdater.models.DeviceOsSpec
import com.arjanvlek.oxygenupdater.models.ServerStatus
import com.arjanvlek.oxygenupdater.utils.ThemeUtils
import com.arjanvlek.oxygenupdater.utils.Utils
import com.arjanvlek.oxygenupdater.utils.Utils.checkPlayServices
import com.arjanvlek.oxygenupdater.viewmodels.MainViewModel
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.MobileAds
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.install.model.ActivityResult
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus.DOWNLOADED
import com.google.android.play.core.install.model.InstallStatus.DOWNLOADING
import com.google.android.play.core.install.model.InstallStatus.FAILED
import com.google.android.play.core.install.model.InstallStatus.PENDING
import com.google.android.play.core.install.model.UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
import com.google.android.play.core.install.model.UpdateAvailability.UPDATE_AVAILABLE
import kotlinx.android.synthetic.main.activity_main.*
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.math.abs

class MainActivity : AppCompatActivity(R.layout.activity_main), Toolbar.OnMenuItemClickListener {

    private lateinit var activityLauncher: ActivityLauncher

    private var downloadPermissionCallback: KotlinCallback<Boolean>? = null

    private val settingsManager by inject<SettingsManager>()
    private val mainViewModel by viewModel<MainViewModel>()

    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            when (position) {
                0, 1 -> hideTabBadge(position, 1000)
                else -> {
                    // no-op
                }
            }
        }
    }

    private val appUpdateAvailableObserver = Observer<AppUpdateInfo> { updateInfo ->
        if (updateInfo.installStatus() == DOWNLOADED) {
            mainViewModel.unregisterAppUpdateListener()
            showAppUpdateSnackbar()
        } else {
            when (updateInfo.updateAvailability()) {
                UPDATE_AVAILABLE -> mainViewModel.requestUpdate(this, updateInfo)
                // If an IMMEDIATE update is in the stalled state, we should resume it
                DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> mainViewModel.requestImmediateAppUpdate(
                    this,
                    updateInfo
                )
            }
        }
    }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) = super.onCreate(savedInstanceState).also {
        activityLauncher = ActivityLauncher(this)

        mainViewModel.maybeCheckForAppUpdate().observe(
            this,
            appUpdateAvailableObserver
        )
        mainViewModel.fetchAllDevices().observe(this) { deviceList ->
            val deviceOsSpec = Utils.checkDeviceOsSpec(deviceList)

            val showDeviceWarningDialog = !settingsManager.getPreference(SettingsManager.PROPERTY_IGNORE_UNSUPPORTED_DEVICE_WARNINGS, false)

            if (showDeviceWarningDialog && !deviceOsSpec.isDeviceOsSpecSupported) {
                displayUnsupportedDeviceOsSpecMessage(deviceOsSpec)
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

        toolbar.setOnMenuItemClickListener(this)

        setupViewPager()
        setupAds()

        // Offer contribution to users from app versions below 2.4.0
        if (!settingsManager.containsPreference(SettingsManager.PROPERTY_CONTRIBUTE)
            && settingsManager.containsPreference(SettingsManager.PROPERTY_SETUP_DONE)
        ) {
            activityLauncher.Contribute()
        }

        if (!Utils.checkNetworkConnection()) {
            showNetworkError()
        }
    }

    private fun showAppUpdateBanner(serverStatus: ServerStatus? = null) {
        appUpdateBannerLayout.isVisible = true
        appUpdateBannerLayout.setOnClickListener {
            ActivityLauncher(this).openPlayStorePage(this)
        }
        appUpdateBannerTextView.text = if (serverStatus == null) {
            getString(R.string.new_app_version_inapp_failed)
        } else {
            getString(R.string.new_app_version, serverStatus.latestAppVersion)
        }
    }

    private fun showAppUpdateSnackbar() {
        Snackbar.make(
            coordinatorLayout,
            R.string.new_app_version_inapp_downloaded,
            Snackbar.LENGTH_INDEFINITE
        ).apply {
            setAction(getString(R.string.error_reload)) {
                mainViewModel.completeAppUpdate()
            }
            show()
        }
    }

    /**
     * Handles toolbar menu clicks
     *
     * @param item the menu item
     *
     * @return true if clicked
     */
    override fun onMenuItemClick(item: MenuItem) = when (item.itemId) {
        R.id.action_faq -> activityLauncher.FAQ().let { true }
        R.id.action_help -> activityLauncher.Help().let { true }
        R.id.action_settings -> activityLauncher.Settings().let { true }
        R.id.action_contribute -> activityLauncher.Contribute().let { true }
        R.id.action_about -> activityLauncher.About().let { true }
        else -> super.onOptionsItemSelected(item)
    }

    private fun setupViewPager() {
        viewPager.apply {
            offscreenPageLimit = 2 // 3 tabs, so there can be only 2 off-screen
            adapter = MainPagerAdapter()

            // attach TabLayout to ViewPager2
            TabLayoutMediator(tabLayout, this) { tab, position ->
                tab.text = when (position) {
                    PAGE_NEWS -> getString(R.string.news)
                    PAGE_UPDATE_INFORMATION -> getString(R.string.update_information_header_short)
                    PAGE_DEVICE_INFORMATION -> getString(R.string.device_information_header_short)
                    else -> null
                }
            }.attach()

            registerOnPageChangeCallback(pageChangeCallback)

            // Set start page to Update Information Screen (middle page)
            try {
                var startPage = PAGE_UPDATE_INFORMATION
                intent?.extras?.getInt(INTENT_START_PAGE)?.let {
                    startPage = it
                }

                currentItem = startPage
            } catch (ignored: IndexOutOfBoundsException) {
                // no-op
            }
        }

        setupAppBarForViewPager()
    }

    private fun setupAppBarForViewPager() {
        appBar.post {
            val totalScrollRange = appBar.totalScrollRange

            // adjust bottom margin on first load
            viewPager.updateLayoutParams<CoordinatorLayout.LayoutParams> { bottomMargin = totalScrollRange }

            // adjust bottom margin on scroll
            appBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
                viewPager.updateLayoutParams<CoordinatorLayout.LayoutParams> {
                    bottomMargin = totalScrollRange - abs(verticalOffset)
                }
            })
        }
    }

    /**
     * Update the state of a [Tab](com.google.android.material.tabs.TabLayout.Tab)'s [BadgeDrawable](com.google.android.material.badge.BadgeDrawable)
     *
     * @param position position of the tab/fragment
     * @param show flag to control the badge's visibility
     * @param count optional number to display in the badge
     *
     * @see hideTabBadge
     */
    fun updateTabBadge(
        @IntRange(from = 0, to = 2) position: Int,
        show: Boolean = true,
        count: Int? = null
    ) = tabLayout.getTabAt(position)?.orCreateBadge?.apply {
        isVisible = show

        if (isVisible) {
            backgroundColor = if (ThemeUtils.isNightModeActive(this@MainActivity)) {
                ContextCompat.getColor(this@MainActivity, R.color.colorPrimary)
            } else {
                Color.WHITE
            }

            if (count != null /*&& count != 0*/) {
                badgeTextColor = ContextCompat.getColor(this@MainActivity, R.color.foreground)
                number = count
                maxCharacterCount = 3
            }
        }
    }

    /**
     * Hide the [com.google.android.material.tabs.TabLayout.Tab]'s [com.google.android.material.badge.BadgeDrawable] after a specified delay
     *
     * Even though [updateTabBadge] can be used to hide a badge, this function is different because it only hides an existing badge, after a specified delay.
     * It's meant to be called from the [viewPager]'s `onPageSelected` callback, within this class.
     * [updateTabBadge] can be called from child fragments to hide the badge immediately, for example, if required after refreshing
     *
     * @param position position of the tab/fragment
     * @param delayMillis the delay, in milliseconds
     *
     * @see updateTabBadge
     */
    @Suppress("SameParameterValue")
    private fun hideTabBadge(
        @IntRange(from = 0, to = 2)
        position: Int,
        delayMillis: Long = 0
    ) = tabLayout.getTabAt(position)?.let { tab ->
        tab.badge?.apply {
            Handler().postDelayed({
                if (tab.isSelected) {
                    isVisible = false
                }
            }, delayMillis)
        }
    }

    /**
     * Checks for Play Services and initialises [MobileAds] if found
     */
    private fun setupAds() {
        if (!checkPlayServices(this, false)) {
            Toast.makeText(this, getString(R.string.notification_no_notification_support), Toast.LENGTH_LONG).show()
        }

        Utils.checkAdSupportStatus(this) { adsAreSupported ->
            if (!isFinishing) {
                if (adsAreSupported) {
                    bannerAdView?.apply {
                        isVisible = true
                        loadAd(buildAdRequest())
                        adListener = object : AdListener() {
                            override fun onAdLoaded() = super.onAdLoaded().also {
                                // need to add spacing between ViewPager contents and the AdView to avoid overlapping the last item
                                // Since the AdView's size is SMART_BANNER, bottom padding should be exactly the AdView's height,
                                // which can only be calculated once the AdView has been drawn on the screen
                                post { viewPager.updatePadding(bottom = height) }
                            }
                        }
                    }
                } else {
                    bannerAdView?.isVisible = false

                    // reset viewPager padding
                    viewPager.setPadding(0, 0, 0, 0)
                }
            }
        }
    }

    public override fun onResume() = super.onResume().also {
        bannerAdView?.resume()
        mainViewModel.checkForStalledAppUpdate().observe(
            this,
            appUpdateAvailableObserver
        )
    }

    public override fun onPause() = super.onPause().also {
        bannerAdView?.pause()
    }

    public override fun onDestroy() = super.onDestroy().also {
        bannerAdView?.destroy()
        mainViewModel.unregisterAppUpdateListener()
    }

    fun displayUnsupportedDeviceOsSpecMessage(deviceOsSpec: DeviceOsSpec) {
        // Do not show dialog if app was already exited upon receiving of devices from the server.
        if (isFinishing) {
            return
        }

        val resourceId = when (deviceOsSpec) {
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
                val checkbox = checkBoxView.findViewById<CheckBox>(R.id.unsupported_device_warning_checkbox)
                settingsManager.savePreference(SettingsManager.PROPERTY_IGNORE_UNSUPPORTED_DEVICE_WARNINGS, checkbox.isChecked)
                dialog.dismiss()
            }
            .show()
    }

    private fun showNetworkError() {
        if (!isFinishing) {
            MessageDialog(
                this,
                title = getString(R.string.error_app_requires_network_connection),
                message = getString(R.string.error_app_requires_network_connection_message),
                negativeButtonText = getString(R.string.download_error_close),
                cancellable = false
            ).show()
        }
    }

    fun requestDownloadPermissions(callback: KotlinCallback<Boolean>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            downloadPermissionCallback = callback
            requestPermissions(
                arrayOf(DOWNLOAD_FILE_PERMISSION, VERIFY_FILE_PERMISSION), PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(permsRequestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (permsRequestCode == PERMISSION_REQUEST_CODE && grantResults.isNotEmpty()) {
            downloadPermissionCallback?.invoke(grantResults[0] == PERMISSION_GRANTED)
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
         * * [NewsFragment]
         * * [UpdateInformationFragment]
         * * [DeviceInformationFragment]
         */
        override fun createFragment(position: Int) = when (position) {
            PAGE_NEWS -> NewsFragment()
            PAGE_UPDATE_INFORMATION -> UpdateInformationFragment()
            PAGE_DEVICE_INFORMATION -> DeviceInformationFragment()
            else -> TODO()
        }

        /**
         * Show 3 total pages: News, Update Information, and Device Information
         */
        override fun getItemCount() = 3
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

    companion object {
        private const val INTENT_START_PAGE = "start_page"
        private const val PAGE_NEWS = 0
        private const val PAGE_UPDATE_INFORMATION = 1
        private const val PAGE_DEVICE_INFORMATION = 2

        // Permissions constants
        private const val DOWNLOAD_FILE_PERMISSION = "android.permission.WRITE_EXTERNAL_STORAGE"
        const val PERMISSION_REQUEST_CODE = 200
        const val VERIFY_FILE_PERMISSION = "android.permission.READ_EXTERNAL_STORAGE"

        const val REQUEST_CODE_APP_UPDATE = 1000
        const val DAYS_FOR_APP_UPDATE_CHECK = 2L
        const val MAX_APP_FLEXIBLE_UPDATE_STALE_DAYS = 14
        const val MAX_APP_FLEXIBLE_UPDATE_IGNORE_COUNT = 7
    }
}
