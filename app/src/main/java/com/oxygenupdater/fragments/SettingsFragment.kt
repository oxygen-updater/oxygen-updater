package com.oxygenupdater.fragments

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.Observer
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.Purchase
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.oxygenupdater.BuildConfig
import com.oxygenupdater.R
import com.oxygenupdater.activities.MainActivity
import com.oxygenupdater.dialogs.Dialogs.showAdvancedModeExplanation
import com.oxygenupdater.enums.PurchaseType
import com.oxygenupdater.exceptions.GooglePlayBillingException
import com.oxygenupdater.extensions.openInCustomTab
import com.oxygenupdater.extensions.openPlayStorePage
import com.oxygenupdater.extensions.setLocale
import com.oxygenupdater.extensions.toLanguageCode
import com.oxygenupdater.extensions.toLocale
import com.oxygenupdater.internal.settings.BottomSheetItem
import com.oxygenupdater.internal.settings.BottomSheetPreference
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.models.AppLocale
import com.oxygenupdater.models.Device
import com.oxygenupdater.models.SystemVersionProperties
import com.oxygenupdater.models.UpdateMethod
import com.oxygenupdater.repositories.BillingRepository.SkuState
import com.oxygenupdater.utils.Logger.logDebug
import com.oxygenupdater.utils.Logger.logError
import com.oxygenupdater.utils.NotificationChannels.DownloadAndInstallationGroup.DOWNLOAD_STATUS_NOTIFICATION_CHANNEL_ID
import com.oxygenupdater.utils.NotificationChannels.PushNotificationsGroup.NEWS_NOTIFICATION_CHANNEL_ID
import com.oxygenupdater.utils.NotificationChannels.PushNotificationsGroup.UPDATE_NOTIFICATION_CHANNEL_ID
import com.oxygenupdater.utils.NotificationTopicSubscriber
import com.oxygenupdater.utils.NotificationUtils
import com.oxygenupdater.utils.ThemeUtils
import com.oxygenupdater.utils.Utils.checkPlayServices
import com.oxygenupdater.viewmodels.BillingViewModel
import com.oxygenupdater.viewmodels.MainViewModel
import com.oxygenupdater.viewmodels.SettingsViewModel
import kotlinx.coroutines.Job
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import java.util.*

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var mContext: Context
    private lateinit var adFreePreference: Preference
    private lateinit var devicePreference: BottomSheetPreference
    private lateinit var updateMethodPreference: BottomSheetPreference
    private lateinit var notificationsPreference: Preference

    private val notificationUtils by inject<NotificationUtils>()

    private var adFreePrice: String? = null

    private val adFreePriceObserver = Observer<String?> {
        adFreePrice = it
    }

    private val adFreeStateObserver = Observer<SkuState?> {
        setupBuyAdFreePreference(it)
    }

    private val newPurchaseObserver = Observer<Pair<Int, Purchase?>> { (responseCode, purchase) ->
        when (responseCode) {
            BillingResponseCode.OK -> purchase?.let {
                validateAdFreePurchase(it, adFreePrice, PurchaseType.AD_FREE)
            }
            BillingResponseCode.USER_CANCELED -> {
                logDebug(TAG, "Purchase of ad-free version was cancelled by the user")
                setupBuyAdFreePreference(SkuState.NOT_PURCHASED)
            }
            else -> {
                logIABError("Purchase of the ad-free version failed due to an unknown error during the purchase flow: $responseCode")
                Toast.makeText(mContext, getString(R.string.purchase_error_after_payment), Toast.LENGTH_LONG).show()
                setupBuyAdFreePreference(SkuState.NOT_PURCHASED)
            }
        }
    }

    private val pendingPurchaseObserver = Observer<Purchase?> {
        Toast.makeText(mContext, getString(R.string.purchase_error_pending_payment), Toast.LENGTH_LONG).show()

        // Disable the Purchase button and set its text to "Processing..."
        adFreePreference.apply {
            isEnabled = false
            summary = mContext.getString(R.string.processing)
        }
    }

    private val onSharedPreferenceChangedListener = OnSharedPreferenceChangeListener { _, key ->
        settingsViewModel.hasPreferenceChanged(key) {
            if (isAdded && it) {
                mainViewModel.notifySettingsChanged(key)
            }
        }
    }

    private val crashlytics by inject<FirebaseCrashlytics>()
    private val mainViewModel by activityViewModel<MainViewModel>()
    private val settingsViewModel by activityViewModel<SettingsViewModel>()
    private val billingViewModel by activityViewModel<BillingViewModel>()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        init()

        addPreferencesFromResource(R.xml.preferences)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) = super.onViewCreated(view, savedInstanceState).also {
        setupBuyAdFreePreference()
        setupDevicePreferences()
        setupThemePreference()
        setupLanguagePreference()
        setupAdvancedModePreference()
        setupAboutPreferences()
    }

    override fun onResume() = super.onResume().also {
        init()

        notificationsPreference.summary = if (notificationUtils.isDisabled) {
            mContext.getString(R.string.summary_off)
        } else {
            val disabledList = buildList {
                if (notificationUtils.isDisabled(UPDATE_NOTIFICATION_CHANNEL_ID))
                    add(R.string.update_notification_channel_name)
                if (notificationUtils.isDisabled(NEWS_NOTIFICATION_CHANNEL_ID))
                    add(R.string.news_notification_channel_name)
                if (notificationUtils.isDisabled(DOWNLOAD_STATUS_NOTIFICATION_CHANNEL_ID))
                    add(R.string.download_and_installation_notifications_group_name)
            }
            if (disabledList.isEmpty()) mContext.getString(R.string.summary_on)
            else mContext.getString(
                R.string.summary_important_notifications_disabled,
                disabledList.joinToString("\", \"") {
                    mContext.getString(it)
                }
            )
        }
    }

    override fun onDestroy() = super.onDestroy().also {
        PrefManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(
            onSharedPreferenceChangedListener
        )
    }

    /**
     * Initialises context, activity, application, and their relevant references
     */
    private fun init() {
        mContext = requireContext()

        PrefManager.sharedPreferences.registerOnSharedPreferenceChangeListener(
            onSharedPreferenceChangedListener
        )
    }

    private fun logIABError(errorMessage: String) = logError(
        TAG,
        GooglePlayBillingException(errorMessage)
    )

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
        purchaseType: PurchaseType
    ): Job = billingViewModel.verifyPurchase(
        purchase,
        amount,
        purchaseType
    ) { validationResult ->
        if (!isAdded) {
            return@verifyPurchase
        }

        when {
            // If server can't be reached, keep trying until it can
            validationResult == null -> Handler(Looper.getMainLooper()).postDelayed(
                { validateAdFreePurchase(purchase, amount, purchaseType) },
                2000
            )
            !validationResult.success -> logError(
                TAG,
                GooglePlayBillingException("[validateAdFreePurchase] couldn't purchase ad-free: (${validationResult.errorMessage})")
            )
        }
    }

    /**
     * Set summary and enable/disable the buy preference depending on purchased status
     */
    private fun setupBuyAdFreePreference() {
        adFreePreference = findPreference(PrefManager.PROPERTY_AD_FREE)!!

        val owner = viewLifecycleOwner
        billingViewModel.adFreePrice.observe(owner, adFreePriceObserver)
        billingViewModel.adFreeState.observe(owner, adFreeStateObserver)
        billingViewModel.pendingPurchase.observe(owner, pendingPurchaseObserver)
        billingViewModel.newPurchase.observe(owner, newPurchaseObserver)

        // no-op observe because the actual work is being done in BillingViewModel
        billingViewModel.purchaseStateChange.observe(owner) { }
    }

    private fun setupBuyAdFreePreference(
        state: SkuState?
    ) = logDebug(TAG, "[setupBuyAdFreePreference] ${state?.name}").also {
        when (state) {
            SkuState.UNKNOWN -> adFreePreference.apply {
                isEnabled = false
                summary = mContext.getString(R.string.settings_buy_button_not_possible)
                onPreferenceClickListener = null
            }.also {
                logIABError("[setupBuyAdFreePreference] SKU '${PurchaseType.AD_FREE.sku}' is not available")
            }
            SkuState.NOT_PURCHASED -> adFreePreference.apply {
                isEnabled = true
                summary = mContext.getString(R.string.settings_buy_button_buy, adFreePrice)
                onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    onBuyAdFreePreferenceClicked()
                    true
                }
            }
            SkuState.PENDING -> pendingPurchaseObserver.onChanged(null)
            SkuState.PURCHASED -> {
                // Already bought, but not yet acknowledged by the app.
                // This should never happen, as it's already handled within BillingDataSource.
            }
            SkuState.PURCHASED_AND_ACKNOWLEDGED -> adFreePreference.apply {
                isEnabled = false
                summary = mContext.getString(R.string.settings_buy_button_bought)
                onPreferenceClickListener = null
            }
            else -> {}
        }
    }

    /**
     * Sets up device and update method list preferences.
     *
     * Entries are retrieved from the server, which calls for dynamically setting `entries` and `entryValues`
     */
    private fun setupDevicePreferences() {
        devicePreference = findPreference(PrefManager.PROPERTY_DEVICE)!!
        updateMethodPreference = findPreference(PrefManager.PROPERTY_UPDATE_METHOD)!!
        notificationsPreference = findPreference(mContext.getString(R.string.key_android_notifications))!!

        devicePreference.isEnabled = false
        updateMethodPreference.isEnabled = false

        notificationsPreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val packageName = mContext.packageName
            startActivity(
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> Intent(
                        Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    ).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                    // Works only for API 21+ (Lollipop), which happens to be the min API
                    else -> Intent(
                        Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    ).putExtra("app_package", packageName)
                        .putExtra("app_uid", mContext.applicationInfo.uid)
                }
            )
            true
        }

        settingsViewModel.fetchEnabledDevices().observe(viewLifecycleOwner) {
            populateDeviceSettings(it)
        }
    }

    private fun setupThemePreference() = findPreference<BottomSheetPreference>(
        mContext.getString(R.string.key_theme)
    )!!.apply {
        if (value == null) {
            setValueIndex(resources.getInteger(R.integer.theme_system_id))
        }

        onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, _ ->
            AppCompatDelegate.setDefaultNightMode(ThemeUtils.translateThemeToNightMode(mContext))
            true
        }
    }

    private fun setupLanguagePreference() = findPreference<BottomSheetPreference>(
        mContext.getString(R.string.key_language)
    )!!.apply {
        val defaultLanguageCode = Locale.getDefault().toLanguageCode()
        val savedLanguageCode = PrefManager.getString(
            PrefManager.PROPERTY_LANGUAGE_ID,
            ""
        )

        val systemConfig = Resources.getSystem().configuration
        val systemLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            systemConfig.locales[0]
        } else {
            @Suppress("DEPRECATION")
            systemConfig.locale
        }

        // Set the spinner to the previously selected device.
        var recommendedPosition = -1
        var selectedPosition = -1

        setItemList(
            BuildConfig.SUPPORTED_LANGUAGES.mapIndexed { i, languageCode ->
                val locale = languageCode.toLocale()
                val language = locale.language
                val country = locale.country
                // App-level localized name, which is displayed both as a title and summary
                val appLocalizedName = locale.displayName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
                // System-level localized name, which is displayed as a fallback for better
                // UX (e.g. if user mistakenly clicked some other language, they should still
                // be able to re-select the correct one because we've provided a localization
                // based on their system language). The language code is also shown, which
                // could help translators.
                val systemLocalizedName = locale.getDisplayName(
                    systemLocale
                ).replaceFirstChar { if (it.isLowerCase()) it.titlecase(systemLocale) else it.toString() }

                if (language == systemLocale.language && (country.isBlank() || country == systemLocale.country)) {
                    recommendedPosition = i
                }

                if (languageCode == savedLanguageCode) {
                    selectedPosition = i
                }

                BottomSheetItem(
                    title = appLocalizedName,
                    subtitle = "$systemLocalizedName [$languageCode]",
                    value = appLocalizedName,
                    secondaryValue = languageCode
                )
            }
        )

        // If there's there no language saved in preferences, auto select the recommended language
        if (selectedPosition == -1 && recommendedPosition != -1) {
            setValueIndex(recommendedPosition)
        }

        onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, _ ->
            context.setLocale(
                PrefManager.getString(
                    PrefManager.PROPERTY_LANGUAGE_ID,
                    defaultLanguageCode
                ) ?: defaultLanguageCode
            )
            activity?.recreate()
            true
        }
    }

    private fun setupAdvancedModePreference() {
        findPreference<SwitchPreference>(PrefManager.PROPERTY_ADVANCED_MODE)?.apply {
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                val isAdvancedMode = PrefManager.getBoolean(PrefManager.PROPERTY_ADVANCED_MODE, false)
                if (isAdvancedMode) {
                    isChecked = false
                    showAdvancedModeExplanation(activity) {
                        isChecked = it
                    }
                }

                true
            }
        }
    }

    /**
     * Sets up privacy policy, rating, and version preferences in the 'About' category
     */
    private fun setupAboutPreferences() {
        val privacyPolicy = findPreference<Preference>(mContext.getString(R.string.key_privacy_policy))
        val rateApp = findPreference<Preference>(mContext.getString(R.string.key_rate_app))
        val oxygenUpdater = findPreference<Preference>(mContext.getString(R.string.key_oxygen))

        privacyPolicy?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            // Use Chrome Custom Tabs to open the privacy policy link
            mContext.openInCustomTab("https://oxygenupdater.com/legal")
            true
        }

        // Open the app's Play Store page
        rateApp?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            context?.openPlayStorePage()
            true
        }

        oxygenUpdater?.apply {
            summary = resources.getString(R.string.summary_oxygen, BuildConfig.VERSION_NAME)
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                (activity as MainActivity?)?.showAboutPage()
                true
            }
        }
    }

    /**
     * Populates [devicePreference] and sets up a preference change listener to re-populate [updateMethodPreference]
     *
     * @param devices retrieved from the server
     */
    private fun populateDeviceSettings(devices: List<Device>?) {
        if (!devices.isNullOrEmpty()) {
            devicePreference.isEnabled = true

            // Set the spinner to the previously selected device.
            var recommendedPosition = -1
            var selectedPosition = -1

            val deviceId = PrefManager.getLong(mContext.getString(R.string.key_device_id), -1L)

            val size = devices.size
            val itemList: MutableList<BottomSheetItem> = ArrayList(size)
            val deviceMap = HashMap<String, Long>(size)
            devices.forEachIndexed { i, device ->
                deviceMap[device.name!!] = device.id

                val productNames = device.productNames

                if (productNames.contains(SystemVersionProperties.oxygenDeviceName)) {
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

            // re-use the same observer to avoid duplicated callbacks
            val observer = Observer<List<UpdateMethod>> {
                populateUpdateMethods(devices, it)
            }

            // Retrieve update methods for the selected device
            settingsViewModel.fetchUpdateMethodsForDevice(deviceId).observe(
                viewLifecycleOwner,
                observer
            )

            // listen for preference change so that we can save the corresponding device ID,
            // and populate update methods
            devicePreference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, value ->
                // disable the update method preference since device has changed
                updateMethodPreference.isEnabled = false

                // Retrieve update methods for the selected device
                settingsViewModel.fetchUpdateMethodsForDevice(deviceMap[value.toString()]!!).observe(
                    viewLifecycleOwner,
                    observer
                )
                true
            }
        } else {
            val deviceCategory = findPreference<PreferenceCategory>(mContext.getString(R.string.key_category_device))
            deviceCategory?.isVisible = false
        }
    }

    /**
     * Populates [updateMethodPreference] and calls [FirebaseCrashlytics.setUserId]
     *
     * @param updateMethods retrieved from the server
     */
    private fun populateUpdateMethods(
        devices: List<Device>,
        updateMethods: List<UpdateMethod>?
    ) {
        if (!updateMethods.isNullOrEmpty()) {
            updateMethodPreference.isEnabled = true

            val currentUpdateMethodId = PrefManager.getLong(mContext.getString(R.string.key_update_method_id), -1L)

            val recommendedPositions: MutableList<Int> = ArrayList()
            var selectedPosition = -1

            val itemList: MutableList<BottomSheetItem> = ArrayList(updateMethods.size)
            updateMethods.forEachIndexed { i, updateMethod ->
                if (updateMethod.recommended) {
                    recommendedPositions.add(i)
                }

                if (updateMethod.id == currentUpdateMethodId) {
                    selectedPosition = i
                }

                val updateMethodName = if (AppLocale.get() == AppLocale.NL) updateMethod.dutchName else updateMethod.englishName

                itemList.add(
                    BottomSheetItem(
                        title = updateMethodName,
                        value = updateMethodName,
                        secondaryValue = updateMethod.id
                    )
                )
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

            // This method is re-called after a device is changed, so we need
            // to update Firebase Crashlytics & Messaging stuff here as well
            updateCrashlyticsUserId()
            updateNotificationTopic(devices, updateMethods)

            updateMethodPreference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, _ ->
                // Update Firebase Crashlytics & Messaging stuff
                updateCrashlyticsUserId()
                updateNotificationTopic(devices, updateMethods)

                true
            }
        } else {
            updateMethodPreference.isVisible = false
        }
    }

    private fun updateCrashlyticsUserId() {
        val deviceName = PrefManager.getString(
            mContext.getString(R.string.key_device),
            "<UNKNOWN>"
        )
        val updateMethodName = PrefManager.getString(
            mContext.getString(R.string.key_update_method),
            "<UNKNOWN>"
        )

        crashlytics.setUserId("Device: $deviceName, Update Method: $updateMethodName")
    }

    private fun updateNotificationTopic(
        devices: List<Device>,
        updateMethods: List<UpdateMethod>
    ) {
        // Google Play services are not required if the user doesn't use notifications
        if (checkPlayServices(requireActivity(), false)) {
            // Subscribe to notifications for the newly selected device and update method
            NotificationTopicSubscriber.resubscribeIfNeeded(devices, updateMethods)
        } else {
            Toast.makeText(
                mContext,
                getString(R.string.notification_no_notification_support),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Handler which is called when the Buy button is clicked
     *
     * Starts the purchase process or initializes a new IabHelper if the current one was disposed early
     */
    private fun onBuyAdFreePreferenceClicked() {
        // Disable the Purchase button and set its text to "Processing...".
        adFreePreference.apply {
            isEnabled = false
            summary = mContext.getString(R.string.processing)
        }

        // [newPurchaseObserver] handles the result
        billingViewModel.makePurchase(requireActivity(), PurchaseType.AD_FREE)
    }

    companion object {
        private const val TAG = "SettingsFragment"
    }
}
