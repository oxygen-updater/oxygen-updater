package com.arjanvlek.oxygenupdater.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.MenuItem
import android.widget.Toast
import androidx.core.app.NavUtils
import androidx.fragment.app.FragmentTransaction
import com.arjanvlek.oxygenupdater.BuildConfig
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.enums.PurchaseStatus
import com.arjanvlek.oxygenupdater.enums.PurchaseType
import com.arjanvlek.oxygenupdater.exceptions.GooglePlayBillingException
import com.arjanvlek.oxygenupdater.fragments.SettingsFragment
import com.arjanvlek.oxygenupdater.fragments.SettingsFragment.InAppPurchaseDelegate
import com.arjanvlek.oxygenupdater.internal.KotlinCallback
import com.arjanvlek.oxygenupdater.internal.QueryInventoryFinishedListener
import com.arjanvlek.oxygenupdater.internal.iab.IabHelper
import com.arjanvlek.oxygenupdater.internal.iab.IabHelper.IabAsyncInProgressException
import com.arjanvlek.oxygenupdater.internal.iab.IabResult
import com.arjanvlek.oxygenupdater.internal.iab.Inventory
import com.arjanvlek.oxygenupdater.internal.iab.PK1
import com.arjanvlek.oxygenupdater.internal.iab.PK2
import com.arjanvlek.oxygenupdater.internal.iab.Purchase
import com.arjanvlek.oxygenupdater.internal.settings.SettingsManager
import com.arjanvlek.oxygenupdater.models.ServerPostResult
import com.arjanvlek.oxygenupdater.utils.Logger.logDebug
import com.arjanvlek.oxygenupdater.utils.Logger.logError
import com.arjanvlek.oxygenupdater.utils.Logger.logInfo
import com.arjanvlek.oxygenupdater.utils.Logger.logWarning
import com.arjanvlek.oxygenupdater.utils.SetupUtils
import org.joda.time.LocalDateTime
import org.koin.android.ext.android.inject

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 * @author Arjan Vlek (github.com/arjanvlek)
 */
class SettingsActivity : SupportActionBarActivity(), InAppPurchaseDelegate {
    private lateinit var settingsFragment: SettingsFragment
    private var iabHelper: IabHelper? = null
    private var price: String? = ""

    private val settingsManager by inject<SettingsManager>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_settings)

        SettingsFragment().let {
            settingsFragment = it
            it.setInAppPurchaseDelegate(this)

            supportFragmentManager.beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .replace(R.id.settings_container, it, "Settings")
                .commitNow()
        }

        IabHelper(this, PK1.A + "/" + PK2.B).apply {
            iabHelper = this

            enableDebugLogging(BuildConfig.DEBUG)
            setupIabHelper(iabHelper)
        }
    }

    public override fun onDestroy() {
        super.onDestroy()

        try {
            iabHelper?.disposeWhenFinished()

            iabHelper = null
        } catch (ignored: Throwable) {

        }
    }

    private fun showSettingsWarning() {
        val deviceId = settingsManager.getPreference(SettingsManager.PROPERTY_DEVICE_ID, -1L)
        val updateMethodId = settingsManager.getPreference(SettingsManager.PROPERTY_UPDATE_METHOD_ID, -1L)

        if (deviceId == -1L || updateMethodId == -1L) {
            logWarning(TAG, SetupUtils.getAsError("Settings screen", deviceId, updateMethodId))
            Toast.makeText(this, getString(R.string.settings_entered_incorrectly), Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, getString(R.string.settings_saving), Toast.LENGTH_LONG).show()
        }
    }

    override fun onBackPressed() = if (settingsManager.checkIfSetupScreenHasBeenCompleted()) {
        NavUtils.navigateUpFromSameTask(this)
    } else {
        showSettingsWarning()
    }

    /**
     * Respond to the action bar's Up/Home button
     */
    override fun onOptionsItemSelected(item: MenuItem) = if (item.itemId == android.R.id.home) {
        if (settingsManager.checkIfSetupScreenHasBeenCompleted()) {
            NavUtils.navigateUpFromSameTask(this)
            true
        } else {
            showSettingsWarning()
            true
        }
    } else {
        super.onOptionsItemSelected(item)
    }

    /* IN APP BILLING (AD FREE PURCHASING) METHODS */

    /**
     * Initialize the In-App Billing helper (IabHelper), query for purchasable products and set the
     * state of the Purchase ad free button accordingly to the purchase information
     *
     * @param iabHelper In-App Billing helper which has been setup and is ready to accept purchases from the user
     */
    private fun setupIabHelper(iabHelper: IabHelper?) {
        logDebug(TAG, "IAB: start setup of IAB")

        // Set up the helper. Once it is done, it will call the embedded listener with its setupResult.
        iabHelper!!.startSetup { setupResult: IabResult ->
            // Setup error? we can't do anything else but stop here. Purchasing ad-free will be unavailable.
            if (!setupResult.success) {
                logIABError("Failed to set up in-app billing", setupResult)
                return@startSetup
            }

            logDebug(TAG, "IAB: Setup complete")

            @Suppress("SENSELESS_COMPARISON")
            // Have we been disposed of in the meantime? If so, quit.
            if (iabHelper == null) {
                return@startSetup
            }

            logDebug(TAG, "IAB: Start querying inventory")

            // Query the billing inventory to get details about the in-app-billing item (such as the price in the right currency).
            queryInAppBillingInventory { result: IabResult, inv: Inventory? ->
                logDebug(TAG, "IAB: Queried inventory")

                // Querying failed? Then the user can't buy anything as we can't determine whether or not the user already bought the item.
                if (result.isFailure) {
                    logIABError("Failed to obtain in-app billing product list", result)
                    return@queryInAppBillingInventory
                }

                val productDetails = inv?.getSkuDetails(SKU_AD_FREE)

                // If the product details are not found (unlikely to happen, but possible), stop.
                if (productDetails == null || productDetails.sku != SKU_AD_FREE) {
                    logIABError("In-app billing product $SKU_AD_FREE is not available", result)
                    return@queryInAppBillingInventory
                }

                logDebug(TAG, "IAB: Found product. Checking purchased state...")

                // Check if the user has purchased the item. If so, grant ad-free and set the button to "Purchased". If not, remove ad-free and set the button to the right price.
                if (inv.hasPurchase(SKU_AD_FREE)) {
                    logDebug(TAG, "IAB: Product has already been purchased")

                    settingsManager.savePreference(SettingsManager.PROPERTY_AD_FREE, true)
                    settingsFragment.setupBuyAdFreePreference(PurchaseStatus.ALREADY_BOUGHT)
                } else {
                    logDebug(TAG, "IAB: Product has not yet been purchased")

                    // Save, because we can guarantee that the device is online and that the purchase check has succeeded.
                    settingsManager.savePreference(SettingsManager.PROPERTY_AD_FREE, false)
                    price = productDetails.price
                    settingsFragment.setupBuyAdFreePreference(PurchaseStatus.AVAILABLE, productDetails.price)
                }
            }
        }
    }

    /**
     * Queries the IAB inventory and retries if an operation is already in progress.
     *
     * @param queryInventoryFinishedListener Listener to execute once the query operation has finished.
     */
    private fun queryInAppBillingInventory(queryInventoryFinishedListener: QueryInventoryFinishedListener) {
        try {
            iabHelper!!.queryInventoryAsync(true, listOf(SKU_AD_FREE), null, queryInventoryFinishedListener)
        } catch (e: IabAsyncInProgressException) {
            Handler().postDelayed({ queryInAppBillingInventory(queryInventoryFinishedListener) }, 3000)
        }
    }

    private fun logIABError(errorMessage: String, result: IabResult) {
        logError(TAG, GooglePlayBillingException("IAB Error: {$errorMessage}. IAB State: {$result}"))

        settingsFragment.setupBuyAdFreePreference(PurchaseStatus.UNAVAILABLE)
    }

    /**
     * Validate the in app purchase on the app's server
     *
     * @param purchase Purchase which must be validated
     * @param callback Whether or not the purchase was valid. Contains function to handle after purchase validation.
     */
    private fun validateAdFreePurchase(purchase: Purchase, callback: KotlinCallback<Boolean>) {
        @Suppress("DEPRECATION")
        @SuppressLint("HardwareIds")
        val expectedPayload = "OxygenUpdater-AdFree-" + if (Build.SERIAL != "unknown") Build.SERIAL + "-" else ""

        if (!purchase.developerPayload.startsWith(expectedPayload)) {
            logError(
                TAG, GooglePlayBillingException(
                    "Purchase of the ad-free version failed. The returned developer payload was incorrect ("
                            + purchase.developerPayload + ")"
                )
            )
            callback.invoke(false)
        }

        application?.serverConnector?.verifyPurchase(purchase, price, PurchaseType.AD_FREE) { validationResult: ServerPostResult? ->
            when {
                validationResult == null -> {
                    // server can't be reached. Keep trying until it can be reached...
                    Handler().postDelayed({ validateAdFreePurchase(purchase, callback) }, 2000)
                }
                validationResult.success -> callback.invoke(true)
                else -> {
                    logError(
                        TAG, GooglePlayBillingException(
                            "Purchase of the ad-free version failed. Failed to verify purchase on the server. Error message: "
                                    + validationResult.errorMessage
                        )
                    )

                    callback.invoke(false)
                }
            }
        }
    }

    override fun performInAppPurchase() {
        if (iabHelper != null) {
            doPurchaseAdFree()
        } else {
            logInfo(TAG, "IAB purchase helper was disposed early. Initiating new instance...")

            IabHelper(this, PK1.A + "/" + PK2.B).let {
                iabHelper = it

                it.enableDebugLogging(BuildConfig.DEBUG)
                it.startSetup { result: IabResult ->
                    if (!result.success) {
                        logIABError("Purchase of the ad-free version failed due to an unknown error BEFORE the purchase screen was opened", result)

                        Toast.makeText(this, getString(R.string.purchase_error_before_payment), Toast.LENGTH_LONG).show()
                        return@startSetup
                    }

                    doPurchaseAdFree()

                }
            }
        }
    }

    /**
     * Start the purchase process.
     */
    fun doPurchaseAdFree() {
        assert(iabHelper != null)
        try {
            logDebug(TAG, "IAB: Start purchase flow")

            @Suppress("DEPRECATION")
            @SuppressLint("HardwareIds")
            val developerPayload = ("OxygenUpdater-AdFree-"
                    + (if (Build.SERIAL != "unknown") Build.SERIAL + "-" else "")
                    + LocalDateTime.now().toString("yyyy-MM-dd HH:mm:ss"))

            // Open the purchase window.
            iabHelper!!.launchPurchaseFlow(this,
                SKU_AD_FREE,
                IAB_REQUEST_CODE, { result: IabResult, purchase: Purchase? ->
                    logDebug(TAG, "IAB: Purchase dialog closed. Result: " + result.toString() + if (purchase != null) ", purchase: $purchase" else "")

                    // If the purchase failed, but the user did not cancel it, notify the user and log an error. Otherwise, do nothing.
                    if (result.isFailure) {
                        if (result.response != IabHelper.IABHELPER_USER_CANCELLED) {
                            logIABError("Purchase of the ad-free version failed due to an unknown error DURING the purchase flow", result)
                            Toast.makeText(this, getString(R.string.purchase_error_after_payment), Toast.LENGTH_LONG).show()
                        } else {
                            logDebug(TAG, "Purchase of ad-free version was cancelled by the user.")
                            settingsFragment.setupBuyAdFreePreference(PurchaseStatus.AVAILABLE, price)
                        }

                        return@launchPurchaseFlow
                    }

                    // if the result is successful and contains purchase data, verify the purchase details on the server and grant the ad-free package to the user.
                    if (result.success && purchase != null) {
                        if (purchase.sku == SKU_AD_FREE) {
                            validateAdFreePurchase(purchase) { valid: Boolean ->
                                if (valid) {
                                    settingsFragment.setupBuyAdFreePreference(PurchaseStatus.ALREADY_BOUGHT)
                                    settingsManager.savePreference(SettingsManager.PROPERTY_AD_FREE, true)
                                } else {
                                    settingsFragment.setupBuyAdFreePreference(PurchaseStatus.AVAILABLE, price)
                                }
                            }
                        } else {
                            logIABError("Another product than expected was bought. ($purchase)", result)
                        }
                    }
                }, developerPayload
            )
        } catch (e: IabAsyncInProgressException) {
            // If the purchase window can't be opened because an operation is in progress, try opening it again in a second (repeated until it can be opened).
            Handler().postDelayed({ doPurchaseAdFree() }, 1000)
        }
    }

    /**
     * Called when the purchase window has been closed.
     *
     * @param requestCode Intent request
     * @param resultCode  Intent result
     * @param data        Intent data
     */
    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // If the activity result can't be processed by IabHelper (because it is for something else or the IabHelper is null), let the parent activity process it.
        if (iabHelper == null || !iabHelper!!.handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    companion object {
        const val SKU_AD_FREE = "oxygen_updater_ad_free"
        private const val IAB_REQUEST_CODE = 1995
        private const val TAG = "SettingsActivity"
    }
}
