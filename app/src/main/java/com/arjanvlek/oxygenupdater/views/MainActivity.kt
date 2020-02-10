package com.arjanvlek.oxygenupdater.views

import android.annotation.TargetApi
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.arjanvlek.oxygenupdater.ActivityLauncher
import com.arjanvlek.oxygenupdater.ApplicationData
import com.arjanvlek.oxygenupdater.ApplicationData.Companion.buildAdRequest
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.deviceinformation.DeviceInformationFragment
import com.arjanvlek.oxygenupdater.internal.KotlinCallback
import com.arjanvlek.oxygenupdater.internal.Utils
import com.arjanvlek.oxygenupdater.models.Banner
import com.arjanvlek.oxygenupdater.models.DeviceOsSpec
import com.arjanvlek.oxygenupdater.models.DeviceRequestFilter
import com.arjanvlek.oxygenupdater.news.NewsFragment
import com.arjanvlek.oxygenupdater.notifications.Dialogs
import com.arjanvlek.oxygenupdater.notifications.MessageDialog
import com.arjanvlek.oxygenupdater.notifications.NotificationTopicSubscriber.subscribe
import com.arjanvlek.oxygenupdater.settings.SettingsActivity.Companion.SKU_AD_FREE
import com.arjanvlek.oxygenupdater.settings.SettingsManager
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_AD_FREE
import com.arjanvlek.oxygenupdater.settings.adFreeVersion.util.IabHelper
import com.arjanvlek.oxygenupdater.settings.adFreeVersion.util.IabHelper.IabAsyncInProgressException
import com.arjanvlek.oxygenupdater.settings.adFreeVersion.util.PK1
import com.arjanvlek.oxygenupdater.settings.adFreeVersion.util.PK2
import com.arjanvlek.oxygenupdater.updateinformation.ServerMessageBar
import com.arjanvlek.oxygenupdater.updateinformation.UpdateInformationFragment
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.InterstitialAd
import com.google.android.gms.ads.MobileAds
import com.google.android.material.appbar.AppBarLayout
import kotlinx.android.synthetic.main.activity_main.*
import org.joda.time.LocalDateTime
import java.util.*
import kotlin.math.abs

class MainActivity : AppCompatActivity(), Toolbar.OnMenuItemClickListener {

    private lateinit var viewPager: ViewPager
    private lateinit var settingsManager: SettingsManager
    lateinit var activityLauncher: ActivityLauncher
        private set

    private var serverMessageBars = ArrayList<ServerMessageBar>()
    private var newsAd: InterstitialAd? = null
    private var downloadPermissionCallback: KotlinCallback<Boolean>? = null
    private var adView: AdView? = null

    var deviceOsSpec: DeviceOsSpec? = null
        private set

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        settingsManager = SettingsManager(this)

        // App version 2.4.6: Migrated old setting Show if system is up to date (default: ON) to Advanced mode (default: OFF).
        if (settingsManager.containsPreference(SettingsManager.PROPERTY_SHOW_IF_SYSTEM_IS_UP_TO_DATE)) {
            settingsManager.savePreference(SettingsManager.PROPERTY_ADVANCED_MODE, !settingsManager.getPreference(SettingsManager.PROPERTY_SHOW_IF_SYSTEM_IS_UP_TO_DATE, true))
            settingsManager.deletePreference(SettingsManager.PROPERTY_SHOW_IF_SYSTEM_IS_UP_TO_DATE)
        }

        val application = application as ApplicationData

        // Supported device check
        application.serverConnector!!.getDevices(DeviceRequestFilter.ALL) { result ->
            deviceOsSpec = Utils.checkDeviceOsSpec(application.systemVersionProperties!!, result)

            val showDeviceWarningDialog = !settingsManager.getPreference(SettingsManager.PROPERTY_IGNORE_UNSUPPORTED_DEVICE_WARNINGS, false)

            if (showDeviceWarningDialog && !deviceOsSpec!!.isDeviceOsSpecSupported) {
                displayUnsupportedDeviceOsSpecMessage(deviceOsSpec!!)
            }
        }

        toolbar.setOnMenuItemClickListener(this)

        setupViewPager()

        activityLauncher = ActivityLauncher(this)

        // Set start page to Update Information Screen (middle page)
        try {
            var startPage = PAGE_UPDATE_INFORMATION
            val extras = intent?.extras

            if (extras?.containsKey(INTENT_START_PAGE) == true) {
                startPage = extras.getInt(INTENT_START_PAGE)
            }

            viewPager.currentItem = startPage
        } catch (ignored: IndexOutOfBoundsException) {
            // no-op
        }

        setupAds()

        // Support functions for Android 8.0 "Oreo" and up.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createPushNotificationChannel()
            createProgressNotificationChannel()
        }

        // Remove "long" download ID in favor of "int" id
        try {
            // Do not remove this int assignment, even though the IDE warns it's unused.
            // getPreference method has a generic signature, we need to force its return type to be an int,
            // otherwise it triggers a ClassCastException (which occurs when coming from older app versions)
            // whilst not assigning it converts it to a long
            @Suppress("UNUSED_VARIABLE")
            val downloadId = settingsManager.getPreference(SettingsManager.PROPERTY_DOWNLOAD_ID, -1)
        } catch (e: ClassCastException) {
            settingsManager.deletePreference(SettingsManager.PROPERTY_DOWNLOAD_ID)
        }

        // Offer contribution to users from app versions below 2.4.0
        if (!settingsManager.containsPreference(SettingsManager.PROPERTY_CONTRIBUTE) && settingsManager.containsPreference(SettingsManager.PROPERTY_SETUP_DONE)) {
            activityLauncher.Contribute()
        }
    }

    public override fun onStart() {
        super.onStart()

        // Mark the welcome tutorial as finished if the user is moving from older app version.
        // This is checked by either having stored update information for offline viewing,
        // or if the last update checked date is set (if user always had up to date system and never viewed update information before)
        if (!settingsManager.getPreference(SettingsManager.PROPERTY_SETUP_DONE, false)
            && (settingsManager.checkIfOfflineUpdateDataIsAvailable() || settingsManager.containsPreference(SettingsManager.PROPERTY_UPDATE_CHECKED_DATE))
        ) {
            settingsManager.savePreference(SettingsManager.PROPERTY_SETUP_DONE, true)
        }

        // Show the welcome tutorial if the app needs to be set up
        if (!settingsManager.getPreference(SettingsManager.PROPERTY_SETUP_DONE, false)) {
            if (Utils.checkNetworkConnection(applicationContext)) {
                activityLauncher.Tutorial()
            } else {
                showNetworkError()
            }
        } else {
            if (!settingsManager.containsPreference(SettingsManager.PROPERTY_NOTIFICATION_TOPIC)) {
                subscribe((application as ApplicationData))
            }
        }

        val application = application as ApplicationData
        val online = Utils.checkNetworkConnection(application)
        application.serverConnector?.getInAppMessages(online, { displayServerMessageBars(it) }) { error ->
            when (error) {
                ApplicationData.SERVER_MAINTENANCE_ERROR -> Dialogs.showServerMaintenanceError(this)
                ApplicationData.APP_OUTDATED_ERROR -> Dialogs.showAppOutdatedError(this)
            }
        }
    }

    /*
      -------------- METHODS FOR DISPLAYING DATA ON THE FRAGMENT -------------------
     */
    private fun addServerMessageBar(view: ServerMessageBar) {
        // Add the message to the update information screen.
        // Set the layout params based on the view count.
        // First view should go below the app update message bar (if visible)
        // Consecutive views should go below their parent / previous view.
        val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        // position each bar below the previous one
        if (serverMessageBars.isNotEmpty()) {
            // params.topToBottom = serverMessageBars[serverMessageBars.size - 1].id
        }

        view.id = View.generateViewId()
        view.visibility = GONE
        serverMessageLayout.addView(view, params)
        serverMessageBars.add(view)
    }

    private fun deleteAllServerMessageBars() {
        serverMessageBars.forEach {
            serverMessageLayout.removeView(it)
        }

        serverMessageBars = ArrayList()
    }

    private fun displayServerMessageBars(banners: List<Banner>) {
        if (isFinishing) {
            return
        }

        deleteAllServerMessageBars()

        val createdServerMessageBars = ArrayList<ServerMessageBar>()

        banners.forEach {
            val bar = ServerMessageBar(this)
            val backgroundBar = bar.backgroundBar
            val textView = bar.textView

            backgroundBar.setBackgroundColor(it.getColor(this))
            textView.text = it.getBannerText(this)

            if (it.getBannerText(this) is Spanned) {
                textView.movementMethod = LinkMovementMethod.getInstance()
            }

            addServerMessageBar(bar)
            createdServerMessageBars.add(bar)
        }

        // Position the app UI  to be below the last added server message bar
        if (createdServerMessageBars.isNotEmpty()) {
            serverMessageLayout.apply {
                // visibility = VISIBLE

                setOnClickListener {
                    children.forEach {
                        if (it is ServerMessageBar) {
                            it.visibility = if (it.visibility == VISIBLE) GONE else VISIBLE
                        } else if (it is TextView) {
                            it.text = if (it.text == "Tap here to view notices") "Tap here to collapse" else "Tap here to view notices"
                        }
                    }
                }
            }
        } else {
            serverMessageLayout.visibility = GONE
        }

        serverMessageBars = createdServerMessageBars
    }

    /**
     * Handles toolbar menu clicks
     *
     * @param item the menu item
     *
     * @return true if clicked
     */
    override fun onMenuItemClick(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                activityLauncher.Settings()
                true
            }
            R.id.action_about -> {
                activityLauncher.About()
                true
            }
            R.id.action_help -> {
                activityLauncher.Help()
                true
            }
            R.id.action_faq -> {
                activityLauncher.FAQ()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupViewPager() {
        viewpager.apply {
            viewPager = this

            offscreenPageLimit = 2
            adapter = SectionsPagerAdapter(supportFragmentManager)
            tabs.setupWithViewPager(this)

            appBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
                val params = this.layoutParams as CoordinatorLayout.LayoutParams
                params.bottomMargin = appBarLayout.totalScrollRange - abs(verticalOffset)

                this.layoutParams = params
            })
        }
    }

    /**
     * Checks for Play Services and initialises [MobileAds] if found
     */
    private fun setupAds() {
        val application = application as ApplicationData

        if (application.checkPlayServices(this, false)) {
            MobileAds.initialize(this, getString(R.string.advertising_app_id))
        } else {
            Toast.makeText(application, getString(R.string.notification_no_notification_support), Toast.LENGTH_LONG).show()
        }

        if (!settingsManager.getPreference(PROPERTY_AD_FREE, false)) {
            InterstitialAd(this).apply {
                newsAd = this

                adUnitId = getString(R.string.advertising_interstitial_unit_id)
                loadAd(buildAdRequest())
            }
        }

        adView = updateInformationAdView
        checkAdSupportStatus { adsAreSupported ->
            if (adsAreSupported) {
                // need to add spacing between ViewPager contents and the AdView to avoid overlapping the last item
                // Since the AdView's size is BANNER, bottom padding should be 50dp
                viewPager.setPadding(0, 0, 0, Utils.dpToPx(this, 66f).toInt())

                adView?.visibility = VISIBLE
                showAds()
            } else {
                // reset viewPager padding
                viewPager.setPadding(0, 0, 0, 0)

                adView?.visibility = GONE
            }
        }
    }

    public override fun onResume() {
        super.onResume()
        adView?.resume()
    }

    public override fun onPause() {
        super.onPause()
        adView?.pause()
    }

    public override fun onDestroy() {
        super.onDestroy()
        adView?.destroy()
    }

    /**
     * Checks if ads should be shown
     *
     * @param callback the callback that receives the result: false if user has purchased ad-free, true otherwise
     */
    private fun checkAdSupportStatus(callback: KotlinCallback<Boolean>) {
        val helper = IabHelper(this, PK1.A + "/" + PK2.B)

        helper.startSetup { setupResult ->
            if (!setupResult.success) {
                // Failed to setup IAB, so we might be offline or the device does not support IAB. Return the last stored value of the ad-free status.
                callback.invoke(!settingsManager.getPreference(PROPERTY_AD_FREE, false))
                return@startSetup
            }

            try {
                helper.queryInventoryAsync(true, listOf(SKU_AD_FREE), null) { queryResult, inventory ->
                    if (!queryResult.success) {
                        // Failed to check inventory, so we might be offline. Return the last stored value of the ad-free status.
                        callback.invoke(!settingsManager.getPreference(PROPERTY_AD_FREE, false))
                    } else {
                        if (inventory != null && inventory.hasPurchase(SKU_AD_FREE)) {
                            // User has bought the upgrade. Save this to the app's settings and return that ads may not be shown.
                            settingsManager.savePreference(PROPERTY_AD_FREE, true)
                            callback.invoke(false)
                        } else {
                            // User has not bought the item and we're online, so ads are definitely supported
                            callback.invoke(true)
                        }
                    }
                }
            } catch (e: IabAsyncInProgressException) {
                // A check is already in progress, so wait 3 secs and try to check again.
                Handler().postDelayed({ checkAdSupportStatus(callback) }, 3000)
            }
        }
    }

    /**
     * Loads an ad
     */
    private fun showAds() {
        adView!!.loadAd(buildAdRequest())
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

        AlertDialog.Builder(this)
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

    fun getNewsAd(): InterstitialAd? {
        return when {
            newsAd != null -> newsAd
            mayShowNewsAd() -> {
                InterstitialAd(this).apply {
                    adUnitId = getString(R.string.advertising_interstitial_unit_id)
                    loadAd(buildAdRequest())

                    newsAd = this
                    newsAd
                }
            }
            else -> null
        }
    }

    fun mayShowNewsAd(): Boolean {
        return (!settingsManager.getPreference(PROPERTY_AD_FREE, false)
                && LocalDateTime.parse(settingsManager.getPreference(SettingsManager.PROPERTY_LAST_NEWS_AD_SHOWN, "1970-01-01T00:00:00.000"))
            .isBefore(LocalDateTime.now().minusMinutes(5)))
    }

    fun requestDownloadPermissions(callback: KotlinCallback<Boolean>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            downloadPermissionCallback = callback
            requestPermissions(arrayOf(DOWNLOAD_FILE_PERMISSION, VERIFY_FILE_PERMISSION), PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(permsRequestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (permsRequestCode == PERMISSION_REQUEST_CODE && grantResults.isNotEmpty()) {
            downloadPermissionCallback?.invoke(grantResults[0] == PERMISSION_GRANTED)
        }
    }

    // Android 6.0 Run-time permissions
    fun hasDownloadPermissions(): Boolean {
        return (ContextCompat.checkSelfPermission(application, VERIFY_FILE_PERMISSION) == PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(application, DOWNLOAD_FILE_PERMISSION) == PERMISSION_GRANTED)
    }

    @TargetApi(26)
    private fun createPushNotificationChannel() {
        // The id of the channel.
        val id = ApplicationData.PUSH_NOTIFICATION_CHANNEL_ID

        // The user-visible name of the channel.
        val name: CharSequence = getString(R.string.push_notification_channel_name)

        // The user-visible description of the channel.
        NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH).apply {
            // Configure the notification channel.
            description = getString(R.string.push_notification_channel_description)
            enableLights(true)

            // Sets the notification light color for notifications posted to this
            // channel, if the device supports this feature.
            lightColor = Color.RED
            enableVibration(true)
            vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
            notificationManager?.createNotificationChannel(this)
        }
    }

    @TargetApi(26)
    private fun createProgressNotificationChannel() {
        // The id of the channel.
        val id = ApplicationData.PROGRESS_NOTIFICATION_CHANNEL_ID

        // The user-visible name of the channel.
        val name = getString(R.string.progress_notification_channel_name)

        // The user-visible description of the channel.
        NotificationChannel(id, name, NotificationManager.IMPORTANCE_LOW).apply {
            // Configure the notification channel.
            description = getString(R.string.progress_notification_channel_description)

            enableLights(false)
            enableVibration(false)

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
            notificationManager?.createNotificationChannel(this)
        }
    }
    // Android 8.0 Notification Channels
    /**
     * A [FragmentPagerAdapter] that returns a fragment corresponding to one of the sections/tabs/pages.
     */
    inner class SectionsPagerAdapter internal constructor(fragmentManager: FragmentManager) : FragmentPagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        override fun getItem(position: Int): Fragment {
            // getItem is called to instantiate the fragment for the given page.
            return when (position) {
                PAGE_NEWS -> NewsFragment()
                PAGE_UPDATE_INFORMATION -> UpdateInformationFragment()
                PAGE_DEVICE_INFORMATION -> DeviceInformationFragment()
                else -> TODO()
            }
        }

        override fun getCount(): Int {
            // Show 3 total pages.
            return 3
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return when (position) {
                PAGE_NEWS -> getString(R.string.news)
                PAGE_UPDATE_INFORMATION -> getString(R.string.update_information_header_short)
                PAGE_DEVICE_INFORMATION -> getString(R.string.device_information_header_short)
                else -> null
            }
        }
    }

    interface DeviceOsSpecCheckedListener {
        fun onDeviceOsSpecChecked(deviceOsSpec: DeviceOsSpec)
    }

    companion object {
        const val PAGE_NEWS = 0
        const val PAGE_UPDATE_INFORMATION = 1
        const val PAGE_DEVICE_INFORMATION = 2
        // Permissions constants
        const val PERMISSION_REQUEST_CODE = 200
        const val DOWNLOAD_FILE_PERMISSION = "android.permission.WRITE_EXTERNAL_STORAGE"
        const val VERIFY_FILE_PERMISSION = "android.permission.READ_EXTERNAL_STORAGE"
        const val INTENT_START_PAGE = "start_page"
    }
}
