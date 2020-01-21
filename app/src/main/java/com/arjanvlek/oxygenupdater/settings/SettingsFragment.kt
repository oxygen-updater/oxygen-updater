package com.arjanvlek.oxygenupdater.settings

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.arjanvlek.oxygenupdater.ActivityLauncher
import com.arjanvlek.oxygenupdater.ApplicationData
import com.arjanvlek.oxygenupdater.BuildConfig
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.internal.ThemeUtils
import com.arjanvlek.oxygenupdater.internal.i18n.AppLocale
import com.arjanvlek.oxygenupdater.models.Device
import com.arjanvlek.oxygenupdater.models.DeviceRequestFilter
import com.arjanvlek.oxygenupdater.models.UpdateMethod
import com.arjanvlek.oxygenupdater.notifications.Dialogs.showAdvancedModeExplanation
import com.arjanvlek.oxygenupdater.notifications.NotificationTopicSubscriber.subscribe
import com.arjanvlek.oxygenupdater.settings.adFreeVersion.PurchaseStatus
import com.crashlytics.android.Crashlytics
import java.util.*

/**
 * @author Adhiraj Singh Chauhan (github.com/adhirajsinghchauhan)
 */
class SettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener {
    private lateinit var mContext: Context
    private lateinit var activity: AppCompatActivity
    private lateinit var application: ApplicationData
    private lateinit var activityLauncher: ActivityLauncher
    private lateinit var devicePreference: BottomSheetPreference
    private lateinit var updateMethodPreference: BottomSheetPreference

    private lateinit var delegate: InAppPurchaseDelegate
    private var settingsManager: SettingsManager? = null

    fun setInAppPurchaseDelegate(delegate: InAppPurchaseDelegate) {
        this.delegate = delegate
    }

    override fun onCreatePreferences(savedInstanceState: Bundle, rootKey: String) {
        init()

        addPreferencesFromResource(R.xml.preferences)

        setupSupportPreferences()
        setupDevicePreferences()
        setupThemePreference()
        setupAdvancedModePreference()
        setupAboutPreferences()
    }

    /**
     * Initialises context, activity, application, and their relevant references
     */
    private fun init() {
        mContext = context!!

        (getActivity() as AppCompatActivity).let {
            activity = it
            application = it.application as ApplicationData
            activityLauncher = ActivityLauncher(it)
        }

        settingsManager = SettingsManager(mContext)
    }

    override fun onResume() {
        super.onResume()
        init()
    }

    /**
     * Sets up buy ad-free and contribute preferences
     */
    private fun setupSupportPreferences() {
        val contribute = findPreference<Preference>(mContext.getString(R.string.key_contributor))
        contribute?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            activityLauncher.Contribute()
            true
        }
    }

    /**
     * Sets up device and update method list preferences.
     *
     *
     * Entries are retrieved from the server, which calls for dynamically setting `entries` and `entryValues`
     */
    private fun setupDevicePreferences() {
        devicePreference = findPreference(mContext.getString(R.string.key_device))!!
        updateMethodPreference = findPreference(mContext.getString(R.string.key_update_method))!!

        devicePreference.isEnabled = false
        updateMethodPreference.isEnabled = false

        application.serverConnector!!.getDevices(DeviceRequestFilter.ENABLED, true) { populateDeviceSettings(it) }
    }

    private fun setupThemePreference() {
        findPreference<Preference>(mContext.getString(R.string.key_theme))?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _: Preference?, _: Any? ->
            AppCompatDelegate.setDefaultNightMode(ThemeUtils.translateThemeToNightMode(mContext))
            true
        }
    }

    private fun setupAdvancedModePreference() {
        val advancedMode = findPreference<Preference>(SettingsManager.PROPERTY_ADVANCED_MODE)
        advancedMode?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val isAdvancedMode = settingsManager!!.getPreference(SettingsManager.PROPERTY_ADVANCED_MODE, false)
            if (isAdvancedMode) {
                showAdvancedModeExplanation(mContext, activity.supportFragmentManager)
            }

            true
        }
    }

    /**
     * Sets up privacy policy, rating, and version preferences in the 'About' category
     */
    private fun setupAboutPreferences() {
        val privacyPolicy = findPreference<Preference>(mContext.getString(R.string.key_privacy_policy))
        val rateApp = findPreference<Preference>(mContext.getString(R.string.key_rate_app))
        val oxygenUpdater = findPreference<Preference>(mContext.getString(R.string.key_oxygen))

        // Use Chrome Custom Tabs to open the privacy policy link
        val privacyPolicyUri = Uri.parse("https://oxygenupdater.com/legal")
        val customTabsIntent = CustomTabsIntent.Builder()
            .setColorScheme(if (ThemeUtils.isNightModeActive(mContext)) CustomTabsIntent.COLOR_SCHEME_DARK else CustomTabsIntent.COLOR_SCHEME_LIGHT)
            .setToolbarColor(ContextCompat.getColor(mContext, R.color.appBarBackground))
            .setNavigationBarColor(ContextCompat.getColor(mContext, R.color.background))
            .build()

        privacyPolicy?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            customTabsIntent.launchUrl(mContext, privacyPolicyUri)
            true
        }

        // Open the app's Play Store page
        rateApp?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            activityLauncher.launchPlayStorePage(mContext)
            true
        }

        oxygenUpdater?.apply {
            summary = resources.getString(R.string.summary_oxygen, BuildConfig.VERSION_NAME)
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                activityLauncher.About()
                true
            }
        }
    }

    /**
     * Populates [.devicePreference] and sets up a preference change listener to re-populate [.updateMethodPreference]
     *
     * @param devices retrieved from the server
     */
    private fun populateDeviceSettings(devices: List<Device>?) {
        if (!devices.isNullOrEmpty()) {
            devicePreference.isEnabled = true
            val systemVersionProperties = application.systemVersionProperties

            // Set the spinner to the previously selected device.
            var recommendedPosition = -1
            var selectedPosition = -1

            val deviceId = settingsManager!!.getPreference(mContext.getString(R.string.key_device_id), -1L)

            val itemList: MutableList<BottomSheetItem> = ArrayList()
            val deviceMap = HashMap<CharSequence, Long>()

            devices.forEachIndexed { i, device ->
                deviceMap[device.name!!] = device.id

                val productNames = device.productNames

                if (productNames.contains(systemVersionProperties?.oxygenDeviceName)) {
                    recommendedPosition = i
                }

                if (device.id == deviceId) {
                    selectedPosition = i
                }

                itemList.add(BottomSheetItem(title = device.name, value = device.name, secondaryValue = device.id))
            }

            devicePreference.setItemList(itemList)

            // If there's there no device saved in preferences, auto select the recommended device
            if (selectedPosition == -1 && recommendedPosition != -1) {
                devicePreference.setValueIndex(recommendedPosition)
            }

            // Retrieve update methods for the selected device
            application.serverConnector!!.getUpdateMethods(deviceId) { populateUpdateMethods(it) }

            // listen for preference change so that we can save the corresponding device ID,
            // and populate update methods
            devicePreference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _: Preference?, value: Any ->
                // disable the update method preference since device has changed
                updateMethodPreference.isEnabled = false

                // Retrieve update methods for the selected device
                application.serverConnector!!.getUpdateMethods(deviceMap[value.toString()]!!) { populateUpdateMethods(it) }
                true
            }
        } else {
            val deviceCategory = findPreference<PreferenceCategory>(mContext.getString(R.string.key_category_device))
            deviceCategory?.isVisible = false
        }
    }

    /**
     * Populates [.updateMethodPreference] and calls [Crashlytics.setUserIdentifier]
     *
     * @param updateMethods retrieved from the server
     */
    private fun populateUpdateMethods(updateMethods: List<UpdateMethod>?) {
        if (!updateMethods.isNullOrEmpty()) {
            updateMethodPreference.isEnabled = true

            val currentUpdateMethodId = settingsManager!!.getPreference(mContext.getString(R.string.key_update_method_id), -1L)

            val recommendedPositions: MutableList<Int> = ArrayList()
            var selectedPosition = -1

            val itemList: MutableList<BottomSheetItem> = ArrayList()
            updateMethods.forEachIndexed { i, updateMethod ->
                if (updateMethod.recommended) {
                    recommendedPositions.add(i)
                }

                if (updateMethod.id == currentUpdateMethodId) {
                    selectedPosition = i
                }

                val updateMethodName = if (AppLocale.get() == AppLocale.NL) updateMethod.dutchName else updateMethod.englishName

                itemList.add(BottomSheetItem(title = updateMethodName, value = updateMethodName, secondaryValue = updateMethod.id))
            }

            updateMethodPreference.setCaption(mContext.getString(R.string.settings_explanation_incremental_full_update))
            updateMethodPreference.setItemList(itemList)

            // If there's there no update method saved in preferences, auto select the last recommended method
            if (selectedPosition == -1) {
                if (recommendedPositions.isNotEmpty()) {
                    updateMethodPreference.setValueIndex(recommendedPositions[recommendedPositions.size - 1])
                } else {
                    updateMethodPreference.setValueIndex(updateMethods.size - 1)
                }
            }

            updateMethodPreference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _: Preference?, _: Any? ->
                Crashlytics.setUserIdentifier(
                    "Device: " + settingsManager!!.getPreference(mContext.getString(R.string.key_device), "<UNKNOWN>")
                            + ", Update Method: " + settingsManager!!.getPreference(mContext.getString(R.string.key_update_method), "<UNKNOWN>")
                )

                // Google Play services are not required if the user doesn't use notifications
                if (application.checkPlayServices(activity.parent, false)) { // Subscribe to notifications for the newly selected device and update method
                    subscribe(application)
                } else {
                    Toast.makeText(mContext, getString(R.string.notification_no_notification_support), Toast.LENGTH_LONG).show()
                }

                true
            }
        } else {
            updateMethodPreference.isVisible = false
        }
    }

    override fun onPreferenceChange(preference: Preference, value: Any?): Boolean {
        if (value != null) {
            preference.summary = value.toString()
        }

        return true
    }

    /**
     * Handler which is called when the Buy button is clicked
     *
     *
     * Starts the purchase process or initializes a new IabHelper if the current one was disposed early
     *
     * @param preference the buy ad free preference
     */
    private fun onBuyAdFreePreferenceClicked(preference: Preference) {
        // Disable the Purchase button and set its text to "Processing...".
        preference.apply {
            isEnabled = false
            summary = mContext.getString(R.string.processing)
        }

        delegate.performInAppPurchase()
    }

    /**
     * Set summary and enable/disable the buy preference depending on purchased status
     *
     * @param status      purchase status
     * @param adFreePrice price to display if the product can be bought
     */
    fun setupBuyAdFreePreference(status: PurchaseStatus?, adFreePrice: String? = null) {
        findPreference<Preference>(mContext.getString(R.string.key_ad_free))?.apply {
            when (status) {
                PurchaseStatus.UNAVAILABLE -> {
                    isEnabled = false
                    summary = mContext.getString(R.string.settings_buy_button_not_possible)
                    onPreferenceClickListener = null
                }
                PurchaseStatus.AVAILABLE -> {
                    isEnabled = true
                    summary = mContext.getString(R.string.settings_buy_button_buy, adFreePrice)
                    onPreferenceClickListener = Preference.OnPreferenceClickListener { preference: Preference ->
                        onBuyAdFreePreferenceClicked(preference)
                        true
                    }
                }
                PurchaseStatus.ALREADY_BOUGHT -> {
                    isEnabled = false
                    summary = mContext.getString(R.string.settings_buy_button_bought)
                    onPreferenceClickListener = null
                }
                else -> throw IllegalStateException("ShowBuyAdFreeButton: Invalid PurchaseStatus $status!")
            }
        }
    }

    interface InAppPurchaseDelegate {
        fun performInAppPurchase()
    }
}
