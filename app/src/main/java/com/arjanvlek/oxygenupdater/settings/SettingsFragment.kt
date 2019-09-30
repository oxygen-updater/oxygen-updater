package com.arjanvlek.oxygenupdater.settings


import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsIntent.COLOR_SCHEME_DARK
import androidx.browser.customtabs.CustomTabsIntent.COLOR_SCHEME_LIGHT
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceChangeListener
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.arjanvlek.oxygenupdater.ActivityLauncher
import com.arjanvlek.oxygenupdater.ApplicationData
import com.arjanvlek.oxygenupdater.BuildConfig
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.domain.Device
import com.arjanvlek.oxygenupdater.domain.UpdateMethod
import com.arjanvlek.oxygenupdater.internal.ThemeUtils
import com.arjanvlek.oxygenupdater.internal.i18n.Locale
import com.arjanvlek.oxygenupdater.notifications.Dialogs
import com.arjanvlek.oxygenupdater.notifications.NotificationTopicSubscriber
import com.arjanvlek.oxygenupdater.settings.adFreeVersion.PurchaseStatus
import com.crashlytics.android.Crashlytics
import java8.util.function.Consumer
import java.util.*

/**
 * @author Adhiraj Singh Chauhan (github.com/adhirajsinghchauhan)
 */
class SettingsFragment : PreferenceFragmentCompat(), OnPreferenceChangeListener {

    private var context: AppCompatActivity? = null
    private var application: ApplicationData? = null
    private var activityLauncher: ActivityLauncher? = null
    private var delegate: InAppPurchaseDelegate? = null

    private var settingsManager: SettingsManager? = null

    private var devicePreference: BottomSheetPreference? = null
    private var updateMethodPreference: BottomSheetPreference? = null

    internal fun setInAppPurchaseDelegate(delegate: InAppPurchaseDelegate) {
        this.delegate = delegate
    }

    override fun onCreatePreferences(savedInstanceState: Bundle, rootKey: String) {
        context = activity as AppCompatActivity?

        application = context!!.application as ApplicationData
        activityLauncher = ActivityLauncher(context!!)

        settingsManager = SettingsManager(context)

        addPreferencesFromResource(R.xml.preferences)

        setupSupportPreferences()
        setupDevicePreferences()
        setupThemePreference()
        setupAdvancedModePreference()
        setupAboutPreferences()
    }

    /**
     * Sets up buy ad-free and contribute preferences
     */
    private fun setupSupportPreferences() {
        val contribute = findPreference<Preference>(context!!.getString(R.string.key_contributor))


        contribute!!.setOnPreferenceClickListener {
            activityLauncher!!.Contribute()
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
        val updateModeCaption = "settings_explanation_incremental_full_update"

        devicePreference = findPreference(context!!.getString(R.string.key_device))
        updateMethodPreference = findPreference(context!!.getString(R.string.key_update_method))

        devicePreference!!.isEnabled = false
        updateMethodPreference!!.isEnabled = false

        application!!.mServerConnector?.getDevices(true,
                Consumer { this.populateDeviceSettings(it) })
    }

    private fun setupThemePreference() {

        findPreference<Preference>(context!!.getString(R.string.key_theme))!!.setOnPreferenceChangeListener { _, _ ->
            AppCompatDelegate.setDefaultNightMode(ThemeUtils.translateThemeToNightMode(context!!))

            true
        }
    }

    private fun setupAdvancedModePreference() {
        val advancedMode = findPreference<Preference>(SettingsManager.PROPERTY_ADVANCED_MODE)


        advancedMode!!.setOnPreferenceClickListener {
            val isAdvancedMode = settingsManager!!.getPreference(SettingsManager
                    .PROPERTY_ADVANCED_MODE, false)
            if (isAdvancedMode) {
                Dialogs.showAdvancedModeExplanation(application!!, context!!.supportFragmentManager)
            }

            true
        }
    }

    /**
     * Sets up privacy policy, rating, and version preferences in the 'About' category
     */
    private fun setupAboutPreferences() {
        val privacyPolicy = findPreference<Preference>(context!!.getString(R.string.key_privacy_policy))
        val rateApp = findPreference<Preference>(context!!.getString(R.string.key_rate_app))
        val oxygenUpdater = findPreference<Preference>(context!!.getString(R.string.key_oxygen))

        // Use Chrome Custom Tabs to open the privacy policy link
        val privacyPolicyUri = Uri.parse("https://oxygenupdater.com/legal")
        val customTabsIntent = CustomTabsIntent.Builder()
                .setColorScheme(if (ThemeUtils.isNightModeActive(context!!)) COLOR_SCHEME_DARK else COLOR_SCHEME_LIGHT)
                .setToolbarColor(ContextCompat.getColor(context!!, R.color.appBarBackground))
                .setNavigationBarColor(ContextCompat.getColor(context!!, R.color.background))
                .build()


        privacyPolicy!!.setOnPreferenceClickListener {
            customTabsIntent.launchUrl(context!!, privacyPolicyUri)
            true
        }

        // Open the app's Play Store page

        rateApp!!.setOnPreferenceClickListener {
            activityLauncher!!.launchPlayStorePage(context!!)
            true
        }


        oxygenUpdater!!.summary = resources.getString(R.string.summary_oxygen, BuildConfig.VERSION_NAME)

        oxygenUpdater.setOnPreferenceClickListener {
            activityLauncher!!.About()
            true
        }
    }

    /**
     * Populates [.devicePreference] and sets up a preference change listener to re-populate [.updateMethodPreference]
     *
     * @param devices retrieved from the server
     */
    private fun populateDeviceSettings(devices: List<Device>?) {
        if (devices != null && devices.isNotEmpty()) {
            devicePreference!!.isEnabled = true
            val systemVersionProperties = application!!.mSystemVersionProperties

            // Set the spinner to the previously selected device.
            var recommendedPosition = -1

            var selectedPosition = -1

            val deviceId = settingsManager!!.getPreference(context!!.getString(R.string.key_device_id), -1L)

            val itemList = ArrayList<BottomSheetItem>()
            val deviceMap = HashMap<CharSequence, Long>()
            for (i in devices.indices) {
                val device = devices[i]

                deviceMap[device.name] = device.id

                val productNames = device.productNames

                if (productNames != null && productNames.contains(systemVersionProperties?.oxygenDeviceName)) {
                    recommendedPosition = i
                }

                if (device.id == deviceId) {
                    selectedPosition = i
                }

                itemList.add(BottomSheetItem(title = device.name, value = device.name,
                        secondaryValue = device.id))
            }

            devicePreference!!.setItemList(itemList)

            // If there's there no device saved in preferences, auto select the recommended device
            if (selectedPosition == -1 && recommendedPosition != -1) {
                devicePreference!!.setValueIndex(recommendedPosition)
                // settingsManager.savePreference(context.getString(R.string.key_device_id), deviceMap.get(deviceNames[recommendedPosition]));
            }

            // Retrieve update methods for the selected device
            application!!.mServerConnector?.getUpdateMethods(deviceId,
                    Consumer { this.populateUpdateMethods(it) })

            // listen for preference change so that we can save the corresponding device ID,
            // and populate update methods
            devicePreference!!.setOnPreferenceChangeListener { _, value ->
                // disable the update method preference since device has changed
                updateMethodPreference!!.isEnabled = false

                // Retrieve update methods for the selected device

                application!!.mServerConnector?.getUpdateMethods(deviceMap[value.toString()]!!,
                        Consumer { this.populateUpdateMethods(it) })

                true
            }
        } else {
            val deviceCategory = findPreference<PreferenceCategory>(context!!.getString(R.string.key_category_device))

            deviceCategory!!.isVisible = false
        }
    }

    /**
     * Populates [.updateMethodPreference] and calls [Crashlytics.setUserIdentifier]
     *
     * @param updateMethods retrieved from the server
     */
    private fun populateUpdateMethods(updateMethods: List<UpdateMethod>?) {
        if (updateMethods != null && !updateMethods.isEmpty()) {
            updateMethodPreference!!.isEnabled = true

            val currentUpdateMethodId = settingsManager!!.getPreference(context!!.getString(R.string.key_update_method_id), -1L)

            val recommendedPositions = ArrayList<Int>()
            var selectedPosition = -1

            val itemList = ArrayList<BottomSheetItem>()
            for (i in updateMethods.indices) {
                val updateMethod = updateMethods[i]

                if (updateMethod.isRecommended) {
                    recommendedPositions.add(i)
                }

                if (updateMethod.id == currentUpdateMethodId) {
                    selectedPosition = i
                }

                val updateMethodName = if (Locale.locale == Locale.NL)
                    updateMethod.dutchName
                else
                    updateMethod.englishName

                itemList.add(BottomSheetItem(title = updateMethodName!!, value = updateMethodName, secondaryValue = updateMethod.id))
            }

            updateMethodPreference!!.setCaption(context!!.getString(R.string.settings_explanation_incremental_full_update))
            updateMethodPreference!!.setItemList(itemList)

            // If there's there no update method saved in preferences, auto select the last recommended method
            if (selectedPosition == -1) {
                if (recommendedPositions.isNotEmpty()) {
                    updateMethodPreference!!.setValueIndex(recommendedPositions[recommendedPositions.size - 1])
                } else {
                    updateMethodPreference!!.setValueIndex(updateMethods.size - 1)
                }
            }

            updateMethodPreference!!.setOnPreferenceChangeListener { _, _ ->
                Crashlytics.setUserIdentifier("Device: " + settingsManager!!.getPreference(context!!.getString(R.string.key_device), "<UNKNOWN>")
                        + ", Update Method: " + settingsManager!!.getPreference(context!!.getString(R.string.key_update_method), "<UNKNOWN>"))

                // Google Play services are not required if the user doesn't use notifications
                if (application!!.checkPlayServices(context!!.parent, false)) {
                    // Subscribe to notifications for the newly selected device and update method
                    NotificationTopicSubscriber.subscribe(application!!)
                } else {
                    Toast.makeText(context, getString(R.string.notification_no_notification_support), LENGTH_LONG).show()
                }

                true
            }
        } else {
            updateMethodPreference!!.isVisible = false
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
        preference.isEnabled = false
        preference.summary = context!!.getString(R.string.processing)

        delegate!!.performInAppPurchase()
    }

    /**
     * Set summary and enable/disable the buy preference depending on purchased status
     *
     * @param status      purchase status
     * @param adFreePrice price to display if the product can be bought
     */
    @JvmOverloads
    internal fun setupBuyAdFreePreference(status: PurchaseStatus, adFreePrice: String? = null) {
        val buyAdFree = findPreference<Preference>(context!!.getString(R.string.key_ad_free))

        when (status) {
            PurchaseStatus.UNAVAILABLE -> {

                buyAdFree!!.isEnabled = false
                buyAdFree.summary = context!!.getString(R.string.settings_buy_button_not_possible)
                buyAdFree.onPreferenceClickListener = null
            }
            PurchaseStatus.AVAILABLE -> {

                buyAdFree!!.isEnabled = true
                buyAdFree.summary = context!!.getString(R.string.settings_buy_button_buy, adFreePrice)
                buyAdFree.setOnPreferenceClickListener { preference ->
                    onBuyAdFreePreferenceClicked(preference)
                    true
                }
            }
            PurchaseStatus.ALREADY_BOUGHT -> {

                buyAdFree!!.isEnabled = false
                buyAdFree.summary = context!!.getString(R.string.settings_buy_button_bought)
                buyAdFree.onPreferenceClickListener = null
            }
            else -> throw IllegalStateException("ShowBuyAdFreeButton: Invalid PurchaseStatus $status!")
        }
    }

    interface InAppPurchaseDelegate {
        fun performInAppPurchase()
    }
}
/**
 * @param status purchase status
 *
 * @see .setupBuyAdFreePreference
 */
