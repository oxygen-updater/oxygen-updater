package com.oxygenupdater.fragments

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.Purchase
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.oxygenupdater.ActivityLauncher
import com.oxygenupdater.BuildConfig
import com.oxygenupdater.OxygenUpdater
import com.oxygenupdater.R
import com.oxygenupdater.dialogs.Dialogs.showAdvancedModeExplanation
import com.oxygenupdater.enums.PurchaseStatus
import com.oxygenupdater.enums.PurchaseType
import com.oxygenupdater.exceptions.GooglePlayBillingException
import com.oxygenupdater.internal.settings.BottomSheetItem
import com.oxygenupdater.internal.settings.BottomSheetPreference
import com.oxygenupdater.internal.settings.SettingsManager
import com.oxygenupdater.models.AppLocale
import com.oxygenupdater.models.Device
import com.oxygenupdater.models.SystemVersionProperties
import com.oxygenupdater.models.UpdateMethod
import com.oxygenupdater.models.billing.AugmentedSkuDetails
import com.oxygenupdater.repositories.BillingRepository.Sku
import com.oxygenupdater.utils.Logger.logDebug
import com.oxygenupdater.utils.Logger.logError
import com.oxygenupdater.utils.NotificationTopicSubscriber
import com.oxygenupdater.utils.ThemeUtils
import com.oxygenupdater.utils.Utils.checkPlayServices
import com.oxygenupdater.viewmodels.BillingViewModel
import com.oxygenupdater.viewmodels.SettingsViewModel
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import java.util.*

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
class SettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener {

    private lateinit var mContext: Context
    private lateinit var activity: AppCompatActivity
    private lateinit var application: OxygenUpdater
    private lateinit var activityLauncher: ActivityLauncher
    private lateinit var adFreePreference: Preference
    private lateinit var devicePreference: BottomSheetPreference
    private lateinit var updateMethodPreference: BottomSheetPreference

    private val inappSkuDetailsListObserver = Observer<List<AugmentedSkuDetails>> { list ->
        list.find {
            it.sku == Sku.AD_FREE
        }.let { augmentedSkuDetails ->
            val purchaseStatus = when {
                augmentedSkuDetails == null -> PurchaseStatus.UNAVAILABLE
                augmentedSkuDetails.canPurchase -> PurchaseStatus.AVAILABLE
                !augmentedSkuDetails.canPurchase -> PurchaseStatus.ALREADY_BOUGHT
                else -> PurchaseStatus.UNAVAILABLE
            }

            setupBuyAdFreePreference(purchaseStatus, augmentedSkuDetails)
        }
    }

    private val pendingPurchasesObserver = Observer<Purchase> { pendingAdFreeUnlockPurchase ->
        if (pendingAdFreeUnlockPurchase != null) {
            Toast.makeText(mContext, getString(R.string.purchase_error_pending_payment), Toast.LENGTH_LONG).show()

            // Disable the Purchase button and set its text to "Processing..."
            // This can conflict with summary updates from [inappSkuDetailsListObserver], but eh. It's harmless.
            adFreePreference.apply {
                isEnabled = false
                summary = mContext.getString(R.string.processing)
            }
        }
    }

    private val systemVersionProperties by inject<SystemVersionProperties>()
    private val crashlytics by inject<FirebaseCrashlytics>()
    private val settingsManager by inject<SettingsManager>()
    private val settingsViewModel by sharedViewModel<SettingsViewModel>()
    private val billingViewModel by sharedViewModel<BillingViewModel>()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        init()

        addPreferencesFromResource(R.xml.preferences)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) = super.onViewCreated(view, savedInstanceState).also {
        setupSupportPreferences()
        setupDevicePreferences()
        setupThemePreference()
        setupAdvancedModePreference()
        setupAboutPreferences()
    }

    override fun onResume() = super.onResume().also {
        init()
    }

    /**
     * Initialises context, activity, application, and their relevant references
     */
    private fun init() {
        mContext = requireContext()

        (getActivity() as AppCompatActivity).let {
            activity = it
            application = it.application as OxygenUpdater
            activityLauncher = ActivityLauncher(it)
        }
    }

    /**
     * Sets up buy ad-free and contribute preferences
     */
    private fun setupSupportPreferences() {
        setupBuyAdFreePreference()

        findPreference<Preference>(
            mContext.getString(R.string.key_contributor)
        )?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            activityLauncher.Contribute()
            true
        }
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
    ) {
        billingViewModel.verifyPurchase(
            purchase,
            amount,
            purchaseType
        ) { validationResult ->
            if (!isAdded) {
                return@verifyPurchase
            }

            when {
                // If server can't be reached, keep trying until it can
                validationResult == null -> Handler().postDelayed(
                    {
                        validateAdFreePurchase(
                            purchase,
                            amount,
                            purchaseType
                        )
                    },
                    2000
                )
                !validationResult.success -> logError(
                    TAG,
                    GooglePlayBillingException(
                        "Purchase of the ad-free version failed. Failed to verify purchase on the server. Error message: "
                                + validationResult.errorMessage
                    )
                )
            }
        }
    }

    /**
     * Set summary and enable/disable the buy preference depending on purchased status
     */
    private fun setupBuyAdFreePreference() {
        adFreePreference = findPreference(mContext.getString(R.string.key_ad_free))!!

        billingViewModel.inappSkuDetailsListLiveData.observe(
            viewLifecycleOwner,
            inappSkuDetailsListObserver
        )

        billingViewModel.pendingPurchasesLiveData.observe(
            viewLifecycleOwner,
            pendingPurchasesObserver
        )

        // no-op observe because the actual work is being done in BillingViewModel
        billingViewModel.adFreeUnlockLiveData.observe(viewLifecycleOwner) { }

        // no-op observe because the actual work is being done in BillingViewModel
        billingViewModel.purchaseStateChangeLiveData.observe(viewLifecycleOwner) { }
    }

    /**
     * @param augmentedSkuDetails is never null if [purchaseStatus] is [PurchaseStatus.AVAILABLE]
     */
    private fun setupBuyAdFreePreference(
        purchaseStatus: PurchaseStatus,
        augmentedSkuDetails: AugmentedSkuDetails?
    ) {
        when (purchaseStatus) {
            PurchaseStatus.UNAVAILABLE -> {
                logIABError("IAB: SKU ${Sku.AD_FREE} is not available")

                // Unavailable
                adFreePreference.isEnabled = false
                adFreePreference.summary = mContext.getString(R.string.settings_buy_button_not_possible)
                adFreePreference.onPreferenceClickListener = null
            }
            PurchaseStatus.AVAILABLE -> {
                logDebug(TAG, "IAB: Product has not yet been purchased")

                // Available
                adFreePreference.isEnabled = true
                adFreePreference.summary = mContext.getString(R.string.settings_buy_button_buy, augmentedSkuDetails!!.price)
                adFreePreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    onBuyAdFreePreferenceClicked(augmentedSkuDetails)
                    true
                }
            }
            PurchaseStatus.ALREADY_BOUGHT -> {
                logDebug(TAG, "IAB: Product has already been purchased")

                // Already bought
                adFreePreference.isEnabled = false
                adFreePreference.summary = mContext.getString(R.string.settings_buy_button_bought)
                adFreePreference.onPreferenceClickListener = null
            }
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

        settingsViewModel.fetchEnabledDevices().observe(viewLifecycleOwner) {
            populateDeviceSettings(it)
        }
    }

    private fun setupThemePreference() {
        findPreference<Preference>(mContext.getString(R.string.key_theme))?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, _ ->
            AppCompatDelegate.setDefaultNightMode(ThemeUtils.translateThemeToNightMode(mContext))
            true
        }
    }

    private fun setupAdvancedModePreference() {
        findPreference<SwitchPreference>(SettingsManager.PROPERTY_ADVANCED_MODE)?.apply {
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                val isAdvancedMode = settingsManager.getPreference(SettingsManager.PROPERTY_ADVANCED_MODE, false)
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
            activityLauncher.openPlayStorePage(mContext)
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

            // Set the spinner to the previously selected device.
            var recommendedPosition = -1
            var selectedPosition = -1

            val deviceId = settingsManager.getPreference(mContext.getString(R.string.key_device_id), -1L)

            val itemList: MutableList<BottomSheetItem> = ArrayList()
            val deviceMap = HashMap<CharSequence, Long>()

            devices.forEachIndexed { i, device ->
                deviceMap[device.name!!] = device.id

                val productNames = device.productNames

                if (productNames.contains(systemVersionProperties.oxygenDeviceName)) {
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
     * Populates [.updateMethodPreference] and calls [FirebaseCrashlytics.setUserId]
     *
     * @param updateMethods retrieved from the server
     */
    private fun populateUpdateMethods(
        devices: List<Device>,
        updateMethods: List<UpdateMethod>?
    ) {
        if (!updateMethods.isNullOrEmpty()) {
            updateMethodPreference.isEnabled = true

            val currentUpdateMethodId = settingsManager.getPreference(mContext.getString(R.string.key_update_method_id), -1L)

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

            updateMethodPreference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, _ ->
                crashlytics.setUserId(
                    "Device: " + settingsManager.getPreference(mContext.getString(R.string.key_device), "<UNKNOWN>")
                            + ", Update Method: " + settingsManager.getPreference(mContext.getString(R.string.key_update_method), "<UNKNOWN>")
                )

                // Google Play services are not required if the user doesn't use notifications
                if (checkPlayServices(activity, false)) {
                    // Subscribe to notifications for the newly selected device and update method
                    NotificationTopicSubscriber.subscribe(devices, updateMethods)
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
     * Starts the purchase process or initializes a new IabHelper if the current one was disposed early
     */
    private fun onBuyAdFreePreferenceClicked(
        augmentedSkuDetails: AugmentedSkuDetails
    ) {
        // Disable the Purchase button and set its text to "Processing...".
        adFreePreference.apply {
            isEnabled = false
            summary = mContext.getString(R.string.processing)
        }

        billingViewModel.makePurchase(
            activity,
            augmentedSkuDetails
        ) { responseCode, purchase ->
            if (!isAdded) {
                return@makePurchase
            }

            when (responseCode) {
                BillingResponseCode.OK -> if (purchase != null) {
                    settingsManager.savePreference(SettingsManager.PROPERTY_AD_FREE, true)

                    validateAdFreePurchase(
                        purchase,
                        augmentedSkuDetails.price,
                        PurchaseType.AD_FREE
                    )
                } else {
                    settingsManager.savePreference(SettingsManager.PROPERTY_AD_FREE, false)
                }
                BillingResponseCode.ITEM_ALREADY_OWNED -> {
                    // This is tricky to deal with. Even pending purchases show up as "items already owned",
                    // so we can't grant entitlement i.e. set [SettingsManager.PROPERTY_AD_FREE] to `true`.
                    // A message should be shown to the user informing them they may have pending purchases.
                    // This case will be handled by observing the pending purchases LiveData.
                    // Entitlement is being granted by observing to the in-app SKU details list LiveData anyway.
                }
                BillingResponseCode.USER_CANCELED -> {
                    logDebug(TAG, "Purchase of ad-free version was cancelled by the user.")
                    setupBuyAdFreePreference(PurchaseStatus.AVAILABLE, augmentedSkuDetails)
                    settingsManager.savePreference(SettingsManager.PROPERTY_AD_FREE, false)
                }
                else -> {
                    logIABError("Purchase of the ad-free version failed due to an unknown error DURING the purchase flow: $responseCode")
                    Toast.makeText(mContext, getString(R.string.purchase_error_after_payment), Toast.LENGTH_LONG).show()
                    setupBuyAdFreePreference(PurchaseStatus.AVAILABLE, augmentedSkuDetails)
                    settingsManager.savePreference(SettingsManager.PROPERTY_AD_FREE, false)
                }
            }
        }
    }

    companion object {
        private const val TAG = "SettingsFragment"
    }
}
