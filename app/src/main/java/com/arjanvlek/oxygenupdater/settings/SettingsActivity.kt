package com.arjanvlek.oxygenupdater.settings

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.MenuItem
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.core.app.NavUtils
import androidx.fragment.app.FragmentTransaction
import com.arjanvlek.oxygenupdater.BuildConfig
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.internal.SetupUtils
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logDebug
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logError
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logInfo
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logWarning
import com.arjanvlek.oxygenupdater.settings.SettingsFragment.InAppPurchaseDelegate
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_AD_FREE
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_DEVICE_ID
import com.arjanvlek.oxygenupdater.settings.SettingsManager.Companion.PROPERTY_UPDATE_METHOD_ID
import com.arjanvlek.oxygenupdater.settings.adFreeVersion.PurchaseStatus.ALREADY_BOUGHT
import com.arjanvlek.oxygenupdater.settings.adFreeVersion.PurchaseStatus.AVAILABLE
import com.arjanvlek.oxygenupdater.settings.adFreeVersion.PurchaseStatus.UNAVAILABLE
import com.arjanvlek.oxygenupdater.settings.adFreeVersion.PurchaseType
import com.arjanvlek.oxygenupdater.settings.adFreeVersion.util.*
import com.arjanvlek.oxygenupdater.views.SupportActionBarActivity
import java8.util.function.Consumer
import org.joda.time.LocalDateTime

/**
 * @author Adhiraj Singh Chauhan (gjthub.com/adhirajsinghchauhan)
 * @author Arjan Vlek (github.com/arjanvlek)
 */
class SettingsActivity : SupportActionBarActivity(), InAppPurchaseDelegate {

    private var settingsManager: SettingsManager? = null
    private var settingsFragment: SettingsFragment? = null
    private var iabHelper: IabHelper? = null

    private var price = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        settingsFragment = SettingsFragment()
        settingsFragment!!.setInAppPurchaseDelegate(this)

        supportFragmentManager.beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .replace(R.id.settings_container, settingsFragment!!, "Settings")
                .commit()

        settingsManager = SettingsManager(applicationContext)

        iabHelper = IabHelper(this, PK1.A + "/" + PK2.B)
        iabHelper!!.enableDebugLogging(BuildConfig.DEBUG)
        setupIabHelper(iabHelper!!)
    }

    public override fun onDestroy() {
        super.onDestroy()
        try {
            if (iabHelper != null) {
                iabHelper!!.disposeWhenFinished()
            }
            iabHelper = null
        } catch (ignored: Throwable) {

        }

    }

    private fun showSettingsWarning() {
        val deviceId = settingsManager!!.getPreference(PROPERTY_DEVICE_ID, -1L)
        val updateMethodId = settingsManager!!.getPreference(PROPERTY_UPDATE_METHOD_ID, -1L)

        if (deviceId == -1L || updateMethodId == -1L) {
            logWarning(TAG, SetupUtils.getAsError("Settings screen", deviceId, updateMethodId))
            Toast.makeText(this, getString(R.string.settings_entered_incorrectly), LENGTH_LONG).show()
        } else {
            Toast.makeText(this, getString(R.string.settings_saving), LENGTH_LONG).show()
        }
    }

    override fun onBackPressed() {
        if (settingsManager!!.checkIfSetupScreenHasBeenCompleted()) {
            NavUtils.navigateUpFromSameTask(this)
        } else {
            showSettingsWarning()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Respond to the action bar's Up/Home button
        if (item.itemId == android.R.id.home) {
            return if (settingsManager!!.checkIfSetupScreenHasBeenCompleted()) {
                NavUtils.navigateUpFromSameTask(this)
                true
            } else {
                showSettingsWarning()
                true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /* IN APP BILLING (AD FREE PURCHASING) METHODS */

    /**
     * Initialize the In-App Billing helper (IabHelper), query for purchasable products and set the
     * state of the Purchase ad free button accordingly to the purchase information
     *
     * @param iabHelper In-App Billing helper which has been setup and is ready to accept purchases from the user
     */
    private fun setupIabHelper(iabHelper: IabHelper) {
        logDebug(TAG, "IAB: start setup of IAB")

        // Set up the helper. Once it is done, it will call the embedded listener with its setupResult.
        iabHelper.startSetup(object : IabHelper.OnIabSetupFinishedListener {
            override fun onIabSetupFinished(result: IabResult) {
                // Setup error? we can't do anything else but stop here. Purchasing ad-free will be unavailable.
                if (!result.isSuccess) {
                    logIABError("Failed to set up in-app billing", result)
                    return
                }

                logDebug(TAG, "IAB: Setup complete")

                // Have we been disposed of in the meantime? If so, quit.
                if (iabHelper == null) {
                    return
                }

                logDebug(TAG, "IAB: Start querying inventory")

                // Query the billing inventory to get details about the in-app-billing item (such as the price in the right currency).
                queryInAppBillingInventory(object : IabHelper.QueryInventoryFinishedListener {
                    override fun onQueryInventoryFinished(result: IabResult, inv: Inventory?) {
                        logDebug(TAG, "IAB: Queried inventory")
                        // Querying failed? Then the user can't buy anything as we can't determine whether or not the user already bought the item.
                        if (result.isFailure) {
                            logIABError("Failed to obtain in-app billing product list", result)
                            return
                        }

                        val productDetails = inv?.getSkuDetails(SKU_AD_FREE)

                        // If the product details are not found (unlikely to happen, but possible), stop.
                        if (productDetails == null || productDetails.sku != SKU_AD_FREE) {
                            logIABError("In-app billing product $SKU_AD_FREE is not available", result)
                            return
                        }

                        logDebug(TAG, "IAB: Found product. Checking purchased state...")

                        // Check if the user has purchased the item. If so, grant ad-free and set the button to "Purchased". If not, remove ad-free and set the button to the right price.
                        if (inv.hasPurchase(SKU_AD_FREE)) {
                            logDebug(TAG, "IAB: Product has already been purchased")
                            settingsManager!!.savePreference(PROPERTY_AD_FREE, true)

                            settingsFragment!!.setupBuyAdFreePreference(ALREADY_BOUGHT)
                        } else {
                            logDebug(TAG, "IAB: Product has not yet been purchased")

                            // Save, because we can guarantee that the device is online and that the purchase check has succeeded.
                            settingsManager!!.savePreference(PROPERTY_AD_FREE, false)

                            price = productDetails.price
                            settingsFragment!!.setupBuyAdFreePreference(AVAILABLE, productDetails.price)
                        }
                    }

                })

            }

        })

    }

    /**
     * Queries the IAB inventory and retries if an operation is already in progress.
     *
     * @param queryInventoryFinishedListener Listener to execute once the query operation has finished.
     */
    private fun queryInAppBillingInventory(queryInventoryFinishedListener: IabHelper.QueryInventoryFinishedListener) {
        try {
            iabHelper!!.queryInventoryAsync(true, listOf(SKU_AD_FREE), null, queryInventoryFinishedListener)
        } catch (e: IabHelper.IabAsyncInProgressException) {
            Handler().postDelayed({ queryInAppBillingInventory(queryInventoryFinishedListener) }, 3000)
        }

    }

    private fun logIABError(errorMessage: String, result: IabResult) {
        logError(TAG, GooglePlayBillingException("IAB Error: {$errorMessage}. IAB State: {$result}"))
        settingsFragment!!.setupBuyAdFreePreference(UNAVAILABLE)
    }

    /**
     * Validate the in app purchase on the app's server
     *
     * @param purchase Purchase which must be validated
     * @param callback Whether or not the purchase was valid. Contains function to handle after purchase validation.
     */
    private fun validateAdFreePurchase(purchase: Purchase, callback: Consumer<Boolean>) {
        @SuppressLint("HardwareIds")
        val expectedPayload = "OxygenUpdater-AdFree-" + if (Build.SERIAL != "unknown") Build.SERIAL + "-" else ""

        if (!purchase.developerPayload.startsWith(expectedPayload)) {
            logError(TAG, GooglePlayBillingException("Purchase of the ad-free version failed. The returned developer payload was incorrect ("
                    + purchase.developerPayload + ")"))
            callback.accept(false)
        }

        getApplicationData().getServerConnector()
                .verifyPurchase(purchase, price, PurchaseType.AD_FREE, Consumer { validationResult ->
                    when {
                        validationResult == null -> // server can't be reached. Keep trying until it can be reached...
                            Handler().postDelayed({ validateAdFreePurchase(purchase, callback) }, 2000)
                        validationResult.isSuccess -> callback.accept(true)
                        else -> {
                            logError(TAG, GooglePlayBillingException("Purchase of the ad-free version failed. Failed to verify purchase on the server. Error message: " + validationResult.errorMessage!!))
                            callback.accept(false)
                        }
                    }
                })
    }

    override fun performInAppPurchase() {
        if (iabHelper != null) {
            doPurchaseAdFree()
        } else {
            logInfo(TAG, "IAB purchase helper was disposed early. Initiating new instance...")
            iabHelper = IabHelper(this, PK1.A + "/" + PK2.B)
            iabHelper!!.enableDebugLogging(BuildConfig.DEBUG)
            iabHelper!!.startSetup(object : IabHelper.OnIabSetupFinishedListener {
                override fun onIabSetupFinished(result: IabResult) {
                    if (!result.isSuccess) {
                        logIABError("Purchase of the ad-free version failed due to an unknown error BEFORE the purchase screen was opened", result)
                        Toast.makeText(this@SettingsActivity,
                                getString(R.string.purchase_error_before_payment), LENGTH_LONG).show()
                        return
                    }

                    doPurchaseAdFree()
                }

            })
        }
    }

    /**
     * Start the purchase process.
     */
    fun doPurchaseAdFree() {
        assert(iabHelper != null)

        try {
            logDebug(TAG, "IAB: Start purchase flow")

            @SuppressLint("HardwareIds")
            val developerPayload = ("OxygenUpdater-AdFree-"
                    + (if (Build.SERIAL != "unknown") Build.SERIAL + "-" else "")
                    + LocalDateTime.now().toString("yyyy-MM-dd HH:mm:ss"))

            // Open the purchase window.
            iabHelper!!.launchPurchaseFlow(this, SKU_AD_FREE, IAB_REQUEST_CODE, object : IabHelper.OnIabPurchaseFinishedListener {
                override fun onIabPurchaseFinished(result: IabResult, info: Purchase?) {
                    logDebug(TAG, "IAB: Purchase dialog closed. Result: " + result.toString() + if (info != null) ", purchase: $info" else "")

                    // If the purchase failed, but the user did not cancel it, notify the user and log an error. Otherwise, do nothing.
                    if (result.isFailure) {
                        if (result.response != IabHelper.IABHELPER_USER_CANCELLED) {
                            logIABError("Purchase of the ad-free version failed due to an unknown error DURING the purchase flow", result)
                            Toast.makeText(application, getString(R.string.purchase_error_after_payment), LENGTH_LONG).show()
                        } else {
                            logDebug(TAG, "Purchase of ad-free version was cancelled by the user.")
                            settingsFragment!!.setupBuyAdFreePreference(AVAILABLE, price)
                        }
                        return
                    }

                    // if the result is successful and contains purchase data, verify the purchase details on the server and grant the ad-free package to the user.
                    if (result.isSuccess && info != null) {
                        if (info.sku == SKU_AD_FREE) {
                            validateAdFreePurchase(info, Consumer { valid ->
                                if (valid!!) {
                                    settingsFragment!!.setupBuyAdFreePreference(ALREADY_BOUGHT)
                                    settingsManager!!.savePreference(PROPERTY_AD_FREE, true)
                                } else {
                                    settingsFragment!!.setupBuyAdFreePreference(AVAILABLE, price)
                                }
                            })
                        } else {
                            logIABError("Another product than expected was bought. ($info)", result)
                        }
                    }
                }

            }, developerPayload)
        } catch (e: IabHelper.IabAsyncInProgressException) {
            // If the purchase window can't be opened because an operation is in progress, try opening it again in a second (repeated until it can be opened).
            Handler().postDelayed({ this.doPurchaseAdFree() }, 1000)
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
