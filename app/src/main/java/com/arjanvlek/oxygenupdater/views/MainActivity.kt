package com.arjanvlek.oxygenupdater.views

import android.annotation.TargetApi
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.CheckBox
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.widget.Toolbar.OnMenuItemClickListener
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.arjanvlek.oxygenupdater.ActivityLauncher
import com.arjanvlek.oxygenupdater.ApplicationData
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.deviceinformation.DeviceInformationFragment
import com.arjanvlek.oxygenupdater.internal.Utils
import com.arjanvlek.oxygenupdater.news.NewsFragment
import com.arjanvlek.oxygenupdater.notifications.MessageDialog
import com.arjanvlek.oxygenupdater.notifications.NotificationTopicSubscriber
import com.arjanvlek.oxygenupdater.settings.SettingsManager
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_ADVANCED_MODE
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_AD_FREE
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_CONTRIBUTE
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_DOWNLOAD_ID
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_LAST_NEWS_AD_SHOWN
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_NOTIFICATION_TOPIC
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_SETUP_DONE
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_SHOW_IF_SYSTEM_IS_UP_TO_DATE
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_UPDATE_CHECKED_DATE
import com.arjanvlek.oxygenupdater.updateinformation.UpdateInformationFragment
import com.google.android.gms.ads.InterstitialAd
import com.google.android.gms.ads.MobileAds
import com.google.android.material.tabs.TabLayout
import java8.util.function.Consumer
import org.joda.time.LocalDateTime

class MainActivity : AppCompatActivity(), OnMenuItemClickListener {

    private var viewPager: ViewPager? = null
    private var settingsManager: SettingsManager? = null
    var activityLauncher: ActivityLauncher? = null
        private set
    private var newsAd: InterstitialAd? = null

    private var downloadPermissionCallback: Consumer<Boolean>? = null
    private val activeFragmentPosition: Int = 0

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val context = applicationContext
        settingsManager = SettingsManager(context)

        // App version 2.4.6: Migrated old setting Show if system is up to date (default: ON) to Advanced mode (default: OFF).
        if (settingsManager!!.containsPreference(PROPERTY_SHOW_IF_SYSTEM_IS_UP_TO_DATE)) {
            settingsManager!!.savePreference(PROPERTY_ADVANCED_MODE, !settingsManager!!.getPreference(PROPERTY_SHOW_IF_SYSTEM_IS_UP_TO_DATE, true))
            settingsManager!!.deletePreference(PROPERTY_SHOW_IF_SYSTEM_IS_UP_TO_DATE)
        }

        // Supported device check
        if (!settingsManager!!.getPreference(SettingsManager.PROPERTY_IGNORE_UNSUPPORTED_DEVICE_WARNINGS, false)) {
            val application = application as ApplicationData
            application.serverConnector.getDevices { result ->
                if (!Utils.isSupportedDevice(application.systemVersionProperties, result)) {
                    displayUnsupportedDeviceMessage()
                }
            }
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.setOnMenuItemClickListener(this)

        setupViewPager()

        activityLauncher = ActivityLauncher(this)

        // Set start page to Update Information Screen (middle page)
        try {
            var startPage = PAGE_UPDATE_INFORMATION

            val intent = intent

            if (intent != null && intent.extras != null && intent.extras!!.containsKey(INTENT_START_PAGE)) {
                startPage = intent.extras!!.getInt(INTENT_START_PAGE)
            }

            viewPager!!.currentItem = startPage
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
            @Suppress("UNUSED_VARIABLE") val downloadId = settingsManager!!.getPreference(PROPERTY_DOWNLOAD_ID, -1)
        } catch (e: ClassCastException) {
            settingsManager!!.deletePreference(PROPERTY_DOWNLOAD_ID)
        }

        // Offer contribution to users from app versions below 2.4.0
        if (!settingsManager!!.containsPreference(PROPERTY_CONTRIBUTE) && settingsManager!!.containsPreference(PROPERTY_SETUP_DONE)) {
            activityLauncher!!.Contribute()
        }
    }

    public override fun onStart() {
        super.onStart()

        // Mark the welcome tutorial as finished if the user is moving from older app version.
        // This is checked by either having stored update information for offline viewing,
        // or if the last update checked date is set (if user always had up to date system and never viewed update information before)
        if (!settingsManager!!.getPreference(PROPERTY_SETUP_DONE, false) && (settingsManager!!.checkIfOfflineUpdateDataIsAvailable() || settingsManager!!.containsPreference(PROPERTY_UPDATE_CHECKED_DATE))) {
            settingsManager!!.savePreference(PROPERTY_SETUP_DONE, true)
        }

        // Show the welcome tutorial if the app needs to be set up
        if (!settingsManager!!.getPreference(PROPERTY_SETUP_DONE, false)) {
            if (Utils.checkNetworkConnection(applicationContext)) {
                activityLauncher!!.Tutorial()
            } else {
                showNetworkError()
            }
        } else {
            if (!settingsManager!!.containsPreference(PROPERTY_NOTIFICATION_TOPIC)) {
                NotificationTopicSubscriber.subscribe(application as ApplicationData)
            }
        }
    }

    /**
     * Handles toolbar menu clicks
     *
     * @param item the menu item
     *
     * @return true if clicked
     */
    override fun onMenuItemClick(item: MenuItem): Boolean {
        val id = item.itemId

        when (id) {
            R.id.action_settings -> {
                activityLauncher!!.Settings()
                return true
            }
            R.id.action_about -> {
                activityLauncher!!.About()
                return true
            }
            R.id.action_help -> {
                activityLauncher!!.Help()
                return true
            }
            R.id.action_faq -> {
                activityLauncher!!.FAQ()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun setupViewPager() {
        val tabLayout = findViewById<TabLayout>(R.id.tabs)
        viewPager = findViewById(R.id.viewpager)

        viewPager!!.offscreenPageLimit = 2
        viewPager!!.adapter = SectionsPagerAdapter(supportFragmentManager)

        tabLayout.setupWithViewPager(viewPager)
    }

    /**
     * Checks for Play Services and initialises [MobileAds] if found
     */
    private fun setupAds() {
        val application = application as ApplicationData

        if (application.checkPlayServices(this, false)) {
            MobileAds.initialize(this, "ca-app-pub-0760639008316468~7665206420")
        } else {
            Toast.makeText(application, getString(R.string.notification_no_notification_support), LENGTH_LONG).show()
        }

        if (!settingsManager!!.getPreference(PROPERTY_AD_FREE, false)) {
            newsAd = InterstitialAd(this)
            newsAd!!.adUnitId = getString(R.string.news_ad_unit_id)
            newsAd!!.loadAd(ApplicationData.buildAdRequest())
        }
    }

    fun displayUnsupportedDeviceMessage() {
        val checkBoxView = View.inflate(this@MainActivity, R.layout.message_dialog_checkbox, null)
        val checkBox = checkBoxView.findViewById<CheckBox>(R.id.unsupported_device_warning_checkbox)

        val builder = AlertDialog.Builder(this)
        builder.setView(checkBoxView)
        builder.setTitle(getString(R.string.unsupported_device_warning_title))
        builder.setMessage(getString(R.string.unsupported_device_warning_message))

        builder.setPositiveButton(getString(R.string.download_error_close)) { dialog, which ->
            settingsManager!!.savePreference(SettingsManager.PROPERTY_IGNORE_UNSUPPORTED_DEVICE_WARNINGS, checkBox
                    .isChecked)
            dialog.dismiss()
        }

        if (!isFinishing) {
            builder.show()
        }
    }

    private fun showNetworkError() {
        if (!isFinishing) {
            val errorDialog = MessageDialog()
                    .setTitle(getString(R.string.error_app_requires_network_connection))
                    .setMessage(getString(R.string.error_app_requires_network_connection_message))
                    .setNegativeButtonText(getString(R.string.download_error_close))
                    .setClosable(false)
            errorDialog.show(supportFragmentManager, "NetworkError")
        }
    }

    fun getNewsAd(): InterstitialAd? {
        if (newsAd != null) {
            return newsAd
        } else if (mayShowNewsAd()) {
            val interstitialAd = InterstitialAd(this)
            interstitialAd.adUnitId = getString(R.string.news_ad_unit_id)
            interstitialAd.loadAd(ApplicationData.buildAdRequest())
            newsAd = interstitialAd
            return newsAd
        } else {
            return null
        }
    }

    fun mayShowNewsAd(): Boolean {
        return !settingsManager!!.getPreference(PROPERTY_AD_FREE, false) && LocalDateTime.parse(settingsManager!!.getPreference(PROPERTY_LAST_NEWS_AD_SHOWN, "1970-01-01T00:00:00.000"))
                .isBefore(LocalDateTime.now().minusMinutes(5))

    }

    fun requestDownloadPermissions(callback: Consumer<Boolean>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            downloadPermissionCallback = callback
            requestPermissions(arrayOf(DOWNLOAD_FILE_PERMISSION, VERIFY_FILE_PERMISSION), PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(permsRequestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (permsRequestCode == PERMISSION_REQUEST_CODE) {
            if (downloadPermissionCallback != null && grantResults.isNotEmpty()) {
                downloadPermissionCallback!!.accept(grantResults[0] == PackageManager.PERMISSION_GRANTED)
            }
        }
    }

    // Android 6.0 Run-time permissions

    fun hasDownloadPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(application, VERIFY_FILE_PERMISSION) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(application, DOWNLOAD_FILE_PERMISSION) == PackageManager.PERMISSION_GRANTED
    }

    @TargetApi(26)
    private fun createPushNotificationChannel() {
        // The id of the channel.
        val id = ApplicationData.PUSH_NOTIFICATION_CHANNEL_ID

        // The user-visible name of the channel.
        val name = getString(R.string.push_notification_channel_name)

        // The user-visible description of the channel.
        val description = getString(R.string.push_notification_channel_description)

        val importance = NotificationManager.IMPORTANCE_HIGH

        val channel = NotificationChannel(id, name, importance)

        // Configure the notification channel.
        channel.description = description
        channel.enableLights(true)
        // Sets the notification light color for notifications posted to this
        // channel, if the device supports this feature.
        channel.lightColor = Color.RED
        channel.enableVibration(true)
        channel.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager?.createNotificationChannel(channel)
    }

    @TargetApi(26)
    private fun createProgressNotificationChannel() {
        // The id of the channel.
        val id = ApplicationData.PROGRESS_NOTIFICATION_CHANNEL_ID

        // The user-visible name of the channel.
        val name = getString(R.string.progress_notification_channel_name)

        // The user-visible description of the channel.
        val description = getString(R.string.progress_notification_channel_description)

        val importance = NotificationManager.IMPORTANCE_LOW

        val channel = NotificationChannel(id, name, importance)

        // Configure the notification channel.
        channel.description = description
        channel.enableLights(false)
        channel.enableVibration(false)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.createNotificationChannel(channel)
    }

    // Android 8.0 Notification Channels

    /**
     * A [FragmentPagerAdapter] that returns a fragment corresponding to one of the sections/tabs/pages.
     */
    inner class SectionsPagerAdapter internal constructor(fragmentManager: FragmentManager) : FragmentPagerAdapter(fragmentManager, FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        override fun getItem(position: Int): Fragment {
            // getItem is called to instantiate the fragment for the given page.
            return when (position) {
                PAGE_NEWS -> NewsFragment()
                PAGE_UPDATE_INFORMATION -> UpdateInformationFragment()
                PAGE_DEVICE_INFORMATION -> DeviceInformationFragment()
                else -> UpdateInformationFragment()
            }
        }

        override fun getCount(): Int {
            // Show 3 total pages.
            return 3
        }

        override fun getPageTitle(position: Int): CharSequence? {
            when (position) {
                PAGE_NEWS -> return getString(R.string.news)
                PAGE_UPDATE_INFORMATION -> return getString(R.string.update_information_header_short)
                PAGE_DEVICE_INFORMATION -> return getString(R.string.device_information_header_short)
            }
            return null
        }
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
