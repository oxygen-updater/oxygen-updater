package com.arjanvlek.oxygenupdater.repositories

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.os.Build
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.FeatureType
import com.android.billingclient.api.BillingClient.SkuType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.Purchase.PurchaseState
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.SkuDetails
import com.android.billingclient.api.SkuDetailsParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.consumePurchase
import com.android.billingclient.api.querySkuDetails
import com.arjanvlek.oxygenupdater.database.LocalBillingDb
import com.arjanvlek.oxygenupdater.exceptions.GooglePlayBillingException
import com.arjanvlek.oxygenupdater.internal.OnPurchaseFinishedListener
import com.arjanvlek.oxygenupdater.internal.billing.PK1
import com.arjanvlek.oxygenupdater.internal.billing.PK2
import com.arjanvlek.oxygenupdater.internal.billing.Security
import com.arjanvlek.oxygenupdater.internal.billing.Security.verifyPurchase
import com.arjanvlek.oxygenupdater.models.billing.AdFreeUnlock
import com.arjanvlek.oxygenupdater.models.billing.AugmentedSkuDetails
import com.arjanvlek.oxygenupdater.models.billing.Entitlement
import com.arjanvlek.oxygenupdater.repositories.BillingRepository.Sku.ALL_SKUS
import com.arjanvlek.oxygenupdater.repositories.BillingRepository.Sku.CONSUMABLE_SKUS
import com.arjanvlek.oxygenupdater.repositories.BillingRepository.Sku.INAPP_SKUS
import com.arjanvlek.oxygenupdater.repositories.BillingRepository.Sku.SUBS_SKUS
import com.arjanvlek.oxygenupdater.utils.Logger.logDebug
import com.arjanvlek.oxygenupdater.utils.Logger.logError
import com.arjanvlek.oxygenupdater.utils.Logger.logWarning
import com.arjanvlek.oxygenupdater.viewmodels.BillingViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

/**
 * @param application the [Application] context
 *
 * @author [Adhiraj Singh Chauhan](https:// github.com/adhirajsinghchauhan)
 */
class BillingRepository constructor(
    private val application: Application,
    /**
     * A local cache billing client is important in that the Play Store may be temporarily
     * unavailable during updates. In such cases, it may be important that the users
     * continue to get access to premium data that they own. Alternatively, you may choose not to
     * provide offline access to your premium content.
     *
     * Even beyond offline access to premium content, however, a local cache billing client makes
     * certain transactions easier. Without an offline cache billing client, for instance, the app
     * would need both the secure server and the Play Billing client to be available in order to
     * process consumable products.
     *
     * The data that lives here should be refreshed at regular intervals so that it reflects what's
     * in the Google Play Store.
     */
    private val localCacheBillingClient: LocalBillingDb
) : PurchasesUpdatedListener, BillingClientStateListener {

    /**
     * The [BillingClient] is the most reliable and primary source of truth for all purchases
     * made through the Google Play Store. The Play Store takes security precautions in guarding
     * the data. Also, the data is available offline in most cases, which means the app incurs no
     * network charges for checking for purchases using the [BillingClient]. The offline bit is
     * because the Play Store caches every purchase the user owns, in an
     * [eventually consistent manner](https://developer.android.com/google/play/billing/billing_library_overview#Keep-up-to-date).
     * This is the only billing client an app is actually required to have on Android. The other
     * two (webServerBillingClient and localCacheBillingClient) are optional.
     *
     * ASIDE. Notice that the connection to [playStoreBillingClient] is created using the
     * applicationContext. This means the instance is not [Activity]-specific. And since it's also
     * not expensive, it can remain open for the life of the entire [Application]. So whether it is
     * (re)created for each [Activity] or [android.app.Fragment] or is kept open for the life of the application
     * is a matter of choice.
     */
    private lateinit var playStoreBillingClient: BillingClient

    /**
     * This list tells clients what in-app products are available for sale
     */
    val inappSkuDetailsListLiveData by lazy {
        localCacheBillingClient.skuDetailsDao().getInappSkuDetails()
    }

    private val _pendingPurchasesLiveData = MutableLiveData<Set<Purchase>>()
    val pendingPurchasesLiveData: LiveData<Set<Purchase>>
        get() = _pendingPurchasesLiveData

    var purchaseFinishedCallback: OnPurchaseFinishedListener? = null

    // START list of each distinct item user may own (i.e. entitlements)

    /*
     * The clients are interested in two types of information: what's for sale, and what the user
     * owns access to. subsSkuDetailsListLiveData and inappSkuDetailsListLiveData tell the clients
     * what's for sale. adFreeUnlockLiveData will tell the client what the user is entitled to.
     * Notice there is nothing billing-specific about these items; they are just properties of the app.
     * So exposing these items to the rest of the app doesn't mean that clients need to understand how billing works.
     *
     * One approach that isn't recommended would be to provide the clients a list of purchases and
      * let them figure it out from there:
     *
     *
     * ```
     *    val purchasesLiveData: LiveData<Set<CachedPurchase>>
     *        by lazy {
     *            queryPurchases()
     *             return localCacheBillingClient.purchaseDao().getPurchases()//assuming liveData
     *       }
     * ```
     *
     * In doing this, however, the [BillingRepository] API would not be client-friendly. Instead,
     * you should specify each item for the clients.
     *
     * For instance, this sample app sells only one item: ad-free unlock.
     * Hence, the rest of the app wants to know -- at all time and as it happens -- the
     * following: does the user have the ad-free unlock. You don't need to expose any additional implementation details.
     * Also you should provide each one of those items as a [LiveData] so that the appropriate UIs get updated
     * automatically.
     */

    /**
     * Tracks whether this user is entitled to an ad free unlock. This call returns data from the app's
     * own local DB; this way if Play and the secure server are unavailable, users still have
     * access to features they purchased.  Normally this would be a good place to update the local
     * cache to make sure it's always up-to-date. However, [onBillingSetupFinished] already called
     * [queryPurchases] for you; so no need.
     */
    val adFreeUnlockLiveData by lazy {
        localCacheBillingClient.entitlementsDao().getAdFreeUnlock()
    }

    // END list of each distinct item user may own (i.e. entitlements)

    /**
     * Correlated data sources belong inside a repository module so that the rest of
     * the app can have appropriate access to the data it needs. Still, it may be effective to
     * track the opening (and sometimes closing) of data source connections based on lifecycle
     * events. One convenient way of doing that is by calling this
     * [startDataSourceConnections] when the [BillingViewModel] is instantiated and
     * [endDataSourceConnections] inside [ViewModel.onCleared]
     */
    @UiThread
    fun startDataSourceConnections() {
        logDebug(TAG, "[startDataSourceConnections]")
        instantiateAndConnectToPlayBillingService()
    }

    @UiThread
    fun endDataSourceConnections() {
        logDebug(TAG, "[endDataSourceConnections]")

        purchaseFinishedCallback = null
        playStoreBillingClient.endConnection()
        // normally you don't worry about closing a DB connection unless you have more than
        // one DB open. so no need to call 'localCacheBillingClient.close()'
    }

    @UiThread
    private fun instantiateAndConnectToPlayBillingService() {
        playStoreBillingClient = BillingClient.newBuilder(application)
            .enablePendingPurchases() // required or app will crash
            .setListener(this)
            .build()

        connectToPlayBillingService()
    }

    @UiThread
    private fun connectToPlayBillingService() = if (!playStoreBillingClient.isReady) {
        playStoreBillingClient.startConnection(this).let { true }
    } else {
        false
    }

    /**
     * This is the callback for when connection to the Play [BillingClient] has been successfully
     * established. It might make sense to get [SkuDetails] and [Purchases][Purchase] at this point.
     */
    override fun onBillingSetupFinished(result: BillingResult) {
        when (result.responseCode) {
            BillingResponseCode.OK -> {
                logDebug(TAG, "[onBillingSetupFinished] success")

                CoroutineScope(Dispatchers.IO).launch {
                    querySkuDetails(SkuType.INAPP, INAPP_SKUS)
                    querySkuDetails(SkuType.SUBS, SUBS_SKUS)
                }
                queryPurchases()
            }
            BillingResponseCode.BILLING_UNAVAILABLE -> {
                // Some apps may choose to make decisions based on this knowledge
                logDebug(TAG, "[onBillingSetupFinished] unavailable")
            }
            else -> {
                // Do nothing. Someone else will connect it through retry policy.
                // May choose to send to server though
                logDebug(TAG, "[onBillingSetupFinished] responseCode: ${result.responseCode}, debugMessage: ${result.debugMessage}")
            }
        }
    }

    /**
     * This method is called when the app has inadvertently disconnected from the [BillingClient].
     * An attempt should be made to reconnect using a retry policy. Note the distinction between
     * [endConnection][BillingClient.endConnection] and disconnected:
     * - disconnected means it's okay to try reconnecting.
     * - endConnection means the [playStoreBillingClient] must be re-instantiated and then start
     *   a new connection because a [BillingClient] instance is invalid after endConnection has
     *   been called.
     **/
    override fun onBillingServiceDisconnected() {
        logDebug(TAG, "[onBillingServiceDisconnected]")
        connectToPlayBillingService()
    }

    /**
     * BACKGROUND
     *
     * Google Play Billing refers to receipts as [Purchases][Purchase]. So when a user buys
     * something, Play Billing returns a [Purchase] object that the app then uses to release the
     * [Entitlement] to the user. Receipts are pivotal within the [BillingRepository]; but they are
     * not part of the repo’s public API, because clients don’t need to know about them. When
     * the release of entitlements occurs depends on the type of purchase. For consumable products,
     * the release may be deferred until after consumption by Google Play; for non-consumable
     * products and subscriptions, the release may be deferred until after
     * [acknowledgePurchase] is called. You should keep receipts in the local
     * cache for augmented security and for making some transactions easier.
     *
     * THIS METHOD
     *
     * [This method][queryPurchases] grabs all the active purchases of this user and makes them
     * available to this app instance. Whereas this method plays a central role in the billing
     * system, it should be called at key junctures, such as when user the app starts.
     *
     * Because purchase data is vital to the rest of the app, this method is called each time
     * the [BillingViewModel] successfully establishes connection with the Play [BillingClient]:
     * the call comes through [onBillingSetupFinished]. Recall also from Figure 4 that this method
     * gets called from inside [onPurchasesUpdated] in the event that a purchase is "already
     * owned," which can happen if a user buys the item around the same time
     * on a different device.
     */
    @UiThread
    fun queryPurchases() {
        val purchasesResult = HashSet<Purchase>()

        var result = playStoreBillingClient.queryPurchases(SkuType.INAPP)
        result.purchasesList?.apply { purchasesResult.addAll(this) }
        logDebug(TAG, "[queryPurchases] INAPP results: ${result.purchasesList?.size}")

        if (isSubscriptionSupported()) {
            result = playStoreBillingClient.queryPurchases(SkuType.SUBS)
            result.purchasesList?.apply { purchasesResult.addAll(this) }
            logDebug(TAG, "[queryPurchases] SUBS results: ${result.purchasesList?.size}")
        }

        processPurchases(purchasesResult)
    }


    /**
     * Can be called either from the [queryPurchases] or the [onPurchasesUpdated] chain.
     *
     * If it's the former, special care needs to be taken to ensure refunds/revocations etc are handled.
     */
    private fun processPurchases(
        purchases: Set<Purchase>
    ) = CoroutineScope(Dispatchers.IO).launch {
        logDebug(TAG, "[processPurchases] purchasesResult: $purchases")

        val validPurchases = HashSet<Purchase>(purchases.size)
        val pendingPurchases = HashSet<Purchase>()
        purchases.forEach {
            if (it.purchaseState == PurchaseState.PURCHASED) {
                if (isSignatureValid(it)) {
                    validPurchases.add(it)
                }
            } else if (it.purchaseState == PurchaseState.PENDING) {
                logDebug(
                    TAG,
                    "[processPurchases] received a pending purchase of SKU: ${it.sku}"
                )

                if (isSignatureValid(it)) {
                    pendingPurchases.add(it)
                }
            }
        }

        // Handle pending purchases, e.g. confirm with users about the pending
        // purchases, prompt them to complete it, etc.
        if (pendingPurchases.isNotEmpty()) {
            _pendingPurchasesLiveData.postValue(pendingPurchases)
        }

        if (validPurchases.isEmpty()) {
            handleNoValidPurchases()
        } else {
            val (consumables, nonConsumables) = validPurchases.partition {
                CONSUMABLE_SKUS.contains(it.sku)
            }

            logDebug(TAG, "[processPurchases] consumables content $consumables")
            logDebug(TAG, "[processPurchases] non-consumables content $nonConsumables")

            /**
             * As is being done in this sample, for extra reliability you may store the
             * receipts/purchases to a your own remote/local database for until after you
             * disburse entitlements. That way if the Google Play Billing library fails at any
             * given point, you can independently verify whether entitlements were accurately
             * disbursed. In this sample, the receipts are then removed upon entitlement
             * disbursement.
             */
            val testing = localCacheBillingClient.purchaseDao().getPurchases()
            logDebug(TAG, "[processPurchases] ${testing.size} purchases in the local db")

            localCacheBillingClient.purchaseDao().insert(*validPurchases.toTypedArray())

            consumeConsumablePurchases(consumables)
            acknowledgeNonConsumablePurchases(nonConsumables)
        }
    }

    /**
     * Purchases can be empty in two known cases:
     * - the user has never purchased anything
     * - the user has been refunded all previous purchases
     *
     * This method mostly deals with handling refunds/revocations, by ensuring the local cache
     * is always up-to-date. The app relies on the local cache to check if the user has any entitlements.
     *
     * If user does not have any active purchases, we should make sure to clean up the local cache as well
     */
    @WorkerThread
    private suspend fun handleNoValidPurchases() = withContext(Dispatchers.IO) {
        logDebug(TAG, "[handleNoValidPurchases] clearing all tables")

        // Clear AdFreeUnlock table
        localCacheBillingClient.entitlementsDao().clearAdFreeUnlockTable()

        ALL_SKUS.forEach {
            // Since the user no longer owns any valid, acknowledged purchases,
            // mark all AugmentedSkuDetails rows as purchasable
            localCacheBillingClient.skuDetailsDao().update(it, true)
        }
    }

    /**
     * Recall that Google Play Billing only supports two SKU types:
     * [in-app products][SkuType.INAPP] and
     * [subscriptions][SkuType.SUBS]. In-app products are actual items that a
     * user can buy, such as a house or food; subscriptions refer to services that a user must
     * pay for regularly, such as auto-insurance. Subscriptions are not consumable.
     *
     * Play Billing provides methods for consuming in-app products because they understand that
     * apps may sell items that users will keep forever (i.e. never consume) such as a house,
     * and consumable items that users will need to keep buying such as food. Nevertheless, Google
     * Play leaves the distinction for which in-app products are consumable entirely up to you.
     *
     * If an app wants its users to be able to keep buying an item, it must call
     * [consumePurchase] each time they buy it. This is because Google Play won't let
     * users buy items that they've previously bought but haven't consumed. In Oxygen Updater, for
     * example, [consumePurchase] is called each time the user buys a donation; otherwise they would never be
     * able to buy donations once already done (NOT IMPLEMENTED YET).
     */
    private suspend fun consumeConsumablePurchases(
        consumables: List<Purchase>
    ) = consumables.forEach {
        logDebug(TAG, "[handleConsumablePurchases] foreach it is $it")

        val (result, _) = playStoreBillingClient.consumePurchase(
            ConsumeParams.newBuilder()
                .setPurchaseToken(it.purchaseToken)
                .build()
        )

        when (result.responseCode) {
            BillingResponseCode.OK -> when (disburseConsumableEntitlements(it)) {
                /**
                 * This disburseConsumableEntitlements method was called because Play called onConsumeResponse.
                 * So if you think of a Purchase as a receipt, you no longer need to keep a copy of
                 * the receipt in the local cache since the user has just consumed the product.
                 */
                true -> localCacheBillingClient.purchaseDao().delete(it)
            }
            else -> logWarning(
                TAG,
                "[handleConsumablePurchases] error responseCode: ${result.responseCode}, debugMessage: ${result.debugMessage}"
            )
        }
    }

    /**
     * If you do not acknowledge a purchase, the Google Play Store will provide a refund to the
     * users within a few days of the transaction. Therefore you have to implement
     * [acknowledgePurchase] inside your app.
     */
    private suspend fun acknowledgeNonConsumablePurchases(
        nonConsumables: List<Purchase>
    ) = nonConsumables.forEach {
        if (it.isAcknowledged) {
            handleAlreadyAcknowledgedEntitlement(it)
        } else {
            val result = playStoreBillingClient.acknowledgePurchase(
                AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(it.purchaseToken)
                    .build()
            )

            when (result.responseCode) {
                BillingResponseCode.OK -> handleSuccessfulAcknowledgment(it)
                // DEVELOPER_ERROR could be because the purchase is already acknowledged
                BillingResponseCode.DEVELOPER_ERROR -> handleAlreadyAcknowledgedEntitlement(it)
                else -> logWarning(
                    TAG,
                    "[acknowledgeNonConsumablePurchases] error responseCode: ${result.responseCode}, debugMessage: ${result.debugMessage}"
                )
            }
        }
    }

    /**
     * This is the final step, where new purchases/receipts are converted to premium contents.
     *
     * Can be called from the [launchBillingFlow] chain only (new purchase flow being completed).
     *
     * In this sample, once the entitlement is disbursed the receipt is thrown out.
     */
    private suspend fun handleSuccessfulAcknowledgment(
        purchase: Purchase
    ) = withContext(Dispatchers.IO) {
        if (disburseNonConsumableEntitlement(purchase.sku)) {
            // Isn't null only in the case of a billing flow being launched
            purchaseFinishedCallback?.invoke(BillingResponseCode.OK, purchase)
        }

        localCacheBillingClient.purchaseDao().delete(purchase)
    }

    /**
     * This is the final step, where already acknowledged purchases/receipts are converted to premium contents.
     *
     * Can be called from the [queryPurchases] chain only (inventory refreshed, or item already owned).
     *
     * In this sample, once the entitlement is disbursed the receipt is thrown out.
     */
    private suspend fun handleAlreadyAcknowledgedEntitlement(
        purchase: Purchase
    ) = withContext(Dispatchers.IO) {
        disburseNonConsumableEntitlement(purchase.sku)

        localCacheBillingClient.purchaseDao().delete(purchase)
    }

    private suspend fun disburseConsumableEntitlements(
        purchase: Purchase
    ) = withContext(Dispatchers.IO) {
        return@withContext when (purchase.sku) {
            // TODO: handle additional cases when app supports consumable purchases
            // Sku.AD_FREE -> true
            else -> false
        }
    }

    private suspend fun disburseNonConsumableEntitlement(
        sku: String
    ) = withContext(Dispatchers.IO) {
        return@withContext when (sku) {
            // TODO: handle additional cases when app supports more non-consumable purchases
            Sku.AD_FREE -> AdFreeUnlock(true).let {
                insert(it)

                localCacheBillingClient.skuDetailsDao().insertOrUpdate(
                    sku,
                    it.mayPurchase()
                )
                true
            }
            else -> false
        }
    }

    /**
     * Ideally your implementation will comprise a secure server, rendering this check
     * unnecessary.
     *
     * @see [Security]
     */
    private fun isSignatureValid(purchase: Purchase) = verifyPurchase(
        PK1.A + "/" + PK2.B,
        purchase.originalJson,
        purchase.signature
    )

    /**
     * Checks if the user's device supports subscriptions
     */
    @UiThread
    private fun isSubscriptionSupported(): Boolean {
        val result = playStoreBillingClient.isFeatureSupported(
            FeatureType.SUBSCRIPTIONS
        )

        var succeeded = false
        when (result.responseCode) {
            BillingResponseCode.SERVICE_DISCONNECTED -> {
                logDebug(TAG, "[isSubscriptionSupported] service disconnected; retrying")
                connectToPlayBillingService()
            }
            BillingResponseCode.OK -> succeeded = true
            else -> logWarning(
                TAG,
                "[isSubscriptionSupported] error responseCode: ${result.responseCode}, debugMessage: ${result.debugMessage}"
            )
        }

        return succeeded
    }

    /**
     * Presumably a set of SKUs has been defined on the Google Play Developer Console. This
     * method is for requesting a (improper) subset of those SKUs. Hence, the method accepts a list
     * of product IDs and returns the matching list of SkuDetails.
     */
    private suspend fun querySkuDetails(
        @SkuType skuType: String,
        skuList: List<String>
    ) {
        logDebug(TAG, "[querySkuDetails] for $skuType")

        if (skuList.isNullOrEmpty()) {
            logDebug(TAG, "[querySkuDetails] skuList is empty, skipping")
            return
        }

        val (result, skuDetailsList) = playStoreBillingClient.querySkuDetails(
            SkuDetailsParams.newBuilder()
                .setSkusList(skuList)
                .setType(skuType)
                .build()
        )

        when (result.responseCode) {
            BillingResponseCode.OK -> withContext(Dispatchers.IO) {
                logDebug(TAG, "[querySkuDetails] success: ${skuDetailsList ?: "[]"}")

                skuDetailsList?.let {
                    localCacheBillingClient.skuDetailsDao().refreshSkuDetails(it)
                }
            }
            else -> logError(
                TAG,
                GooglePlayBillingException("responseCode: ${result.responseCode}, debugMessage: ${result.debugMessage}")
            )
        }
    }

    /**
     * This is the function to call when user wishes to make a purchase. This function will
     * launch the Google Play Billing flow. The response to this call is returned in
     * [onPurchasesUpdated]
     */
    @UiThread
    fun launchBillingFlow(
        activity: Activity,
        augmentedSkuDetails: AugmentedSkuDetails,
        purchaseFinishedCallback: OnPurchaseFinishedListener
    ) = launchBillingFlow(
        activity,
        SkuDetails(augmentedSkuDetails.originalJson ?: ""),
        purchaseFinishedCallback
    )

    @UiThread
    fun launchBillingFlow(
        activity: Activity,
        skuDetails: SkuDetails,
        purchaseFinishedCallback: OnPurchaseFinishedListener
    ) {
        this.purchaseFinishedCallback = purchaseFinishedCallback

        val params = BillingFlowParams.newBuilder()
            .setSkuDetails(skuDetails)
            .build()
        playStoreBillingClient.launchBillingFlow(
            activity,
            params
        )
    }

    /**
     * This method is called by the [playStoreBillingClient] when new purchases are detected.
     * The purchase list in this method is not the same as the one in
     * [queryPurchases][queryPurchases]. Whereas queryPurchases returns everything
     * this user owns, [onPurchasesUpdated] only returns the items that were just now purchased or
     * billed.
     *
     * The purchases provided here should be passed along to the secure server for
     * [verification](https://developer.android.com/google/play/billing/billing_library_overview#Verify)
     * and safekeeping. And if this purchase is consumable, it should be consumed, and the secure
     * server should be told of the consumption. All that is accomplished by calling
     * [queryPurchases].
     */
    override fun onPurchasesUpdated(
        result: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        val shouldInvokeCallback = when (result.responseCode) {
            // Will handle server verification, consumables, and updating the local cache
            BillingResponseCode.OK -> {
                logDebug(TAG, "[onPurchasesUpdated] success: ${purchases ?: "[]"}")
                purchases?.apply { processPurchases(toSet()) }
                false
            }
            // Item already owned? call queryPurchases to verify and process all such items
            BillingResponseCode.ITEM_ALREADY_OWNED -> {
                logDebug(TAG, "[onPurchasesUpdated] item already owned")
                queryPurchases()
                true
            }
            // Retry connection if disconnected
            BillingResponseCode.SERVICE_DISCONNECTED -> {
                logDebug(TAG, "[onPurchasesUpdated] service disconnected; retrying")
                connectToPlayBillingService()
                true
            }
            else -> {
                logDebug(
                    TAG,
                    "[onPurchasesUpdated] responseCode: ${result.responseCode}, debugMessage: ${result.debugMessage}"
                )
                true
            }
        }

        if (shouldInvokeCallback) {
            purchaseFinishedCallback?.invoke(result.responseCode, null)
        }
    }

    @WorkerThread
    private suspend fun insert(
        entitlement: Entitlement
    ) = withContext(Dispatchers.IO) {
        localCacheBillingClient.entitlementsDao().insert(entitlement)
    }

    /**
     * [INAPP_SKUS], [SUBS_SKUS], [CONSUMABLE_SKUS]:
     *
     * If you don't need customization ,then you can define these lists and hardcode them here.
     * That said, there are use cases where you may need customization:
     *
     * - If you don't want to update your APK (or Bundle) each time you change your SKUs, then you
     *   may want to load these lists from your secure server.
     *
     * - If your design is such that users can buy different items from different Activities or
     * Fragments, then you may want to define a list for each of those subsets. I only have two
     * subsets: INAPP_SKUS and SUBS_SKUS
     */
    object Sku {
        const val AD_FREE = "oxygen_updater_ad_free"

        val INAPP_SKUS = listOf(AD_FREE)
        val SUBS_SKUS = listOf<String>()
        val CONSUMABLE_SKUS = listOf<String>()

        val ALL_SKUS = INAPP_SKUS + SUBS_SKUS + CONSUMABLE_SKUS
    }

    companion object {
        private const val TAG = "BillingRepository"

        @Suppress("DEPRECATION")
        @SuppressLint("HardwareIds")
        val DEVELOPER_PAYLOAD_PREFIX = "OxygenUpdater-AdFree-" + if (Build.SERIAL != "unknown") Build.SERIAL + "-" else ""
    }
}

