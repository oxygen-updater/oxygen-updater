package com.oxygenupdater.repositories

import android.app.Activity
import android.app.Application
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.annotation.UiThread
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.Purchase.PurchaseState
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.consumePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.oxygenupdater.BuildConfig
import com.oxygenupdater.enums.PurchaseType
import com.oxygenupdater.internal.billing.Security
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.internal.settings.PrefManager.PROPERTY_AD_FREE
import com.oxygenupdater.utils.Logger.logBillingError
import com.oxygenupdater.utils.Logger.logDebug
import com.oxygenupdater.utils.Logger.logInfo
import com.oxygenupdater.utils.Logger.logVerbose
import com.oxygenupdater.utils.Logger.logWarning
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.set
import kotlin.math.min

/**
 * The BillingRepository implements all billing functionality for our test application.
 * Purchases can happen while in the app or at any time while out of the app, so the
 * BillingRepository has to account for that.
 *
 * Since every SKU (Product ID) can have an individual state, all SKUs have an associated StateFlow
 * to allow their state to be observed.
 *
 * This BillingRepository knows nothing about the application; all necessary information is either
 * passed into the constructor, exported as observable Flows, or exported through callbacks.
 * This code can be reused in a variety of apps.
 *
 * Beginning a purchase flow involves passing an Activity into the Billing Library, but we merely
 * pass it along to the API.
 *
 * This data source has a few automatic features:
 * 1) It checks for a valid signature on all purchases before attempting to acknowledge them.
 * 2) It automatically acknowledges all known SKUs for non-consumables, and doesn't set the state
 * to purchased until the acknowledgement is complete.
 * 3) The data source will automatically consume skus that are set in CONSUMABLE_SKUS. As
 * SKUs are consumed, a Flow will emit.
 * 4) If the BillingService is disconnected, it will attempt to reconnect with exponential
 * fallback.
 *
 * This data source attempts to keep billing library specific knowledge confined to this file;
 * The only thing that clients of the BillingRepository need to know are the SKUs used by their
 * application.
 *
 * The BillingClient needs access to the Application context in order to bind the remote billing
 * service.
 *
 * The BillingRepository can also act as a LifecycleObserver for an Activity; this allows it to
 * refresh purchases during onResume.
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 *
 * @see <a href="https://github.com/android/play-billing-samples/blob/3f75352320c232fc6a14526b67fef07a49cc6d17/TrivialDriveKotlin/app/src/main/java/com/sample/android/trivialdrivesample/billing/BillingRepository.kt">android/play-billing-samples@3f75352:BillingRepository.kt</a>
 */
class BillingRepository(
    application: Application,
) : DefaultLifecycleObserver, PurchasesUpdatedListener, BillingClientStateListener {

    private val mainScope = CoroutineScope(Dispatchers.Main)
    private val ioScope = CoroutineScope(Dispatchers.IO)

    private val billingClient = BillingClient.newBuilder(application)
        .enablePendingPurchases()
        .setListener(this)
        .build()

    /**
     * How long before the data source tries to reconnect to Google Play
     */
    private var reconnectMilliseconds = RECONNECT_TIMER_START_MILLISECONDS

    /**
     * When was the last successful ProductDetailsResponse?
     */
    private var productDetailsResponseTime = -PRODUCT_DETAILS_REQUERY_TIME

    // Maps that are mostly maintained so they can be transformed into observables
    private val skuStateMap = mutableMapOf<String, MutableStateFlow<SkuState>>()
    private val productDetailsMap = mutableMapOf<String, MutableStateFlow<ProductDetails?>>()
    private val purchaseConsumptionInProcess = mutableSetOf<Purchase>()

    /**
     * Flow of the [ad-free][PurchaseType.AD_FREE] SKU's [price][ProductDetails.OneTimePurchaseOfferDetails.getFormattedPrice]
     */
    val adFreePrice
        get() = getSkuPrice(SKU_INAPP_AD_FREE).distinctUntilChanged()

    /**
     * Flow of the [ad-free][PurchaseType.AD_FREE] SKU's [SkuState]
     */
    val adFreeState
        get() = getSkuState(SKU_INAPP_AD_FREE).distinctUntilChanged()

    /**
     * Returns whether or not the user has purchased ad-free. It does this by returning
     * a Flow that returns true if the SKU is in the [SkuState.Purchased] state and
     * the [Purchase] has been acknowledged.
     */
    val hasPurchasedAdFree
        get() = adFreeState.map {
            logDebug(TAG, "[adFreeState] $it")

            // Fallback to SharedPreferences if purchases haven't been processed yet
            if (it == SkuState.Unknown) PrefManager.getBoolean(PROPERTY_AD_FREE, false)
            else it == SkuState.PurchasedAndAcknowledged
        }.distinctUntilChanged().onEach {
            logDebug(TAG, "[hasPurchasedAdFree] saving to SharedPreferences: $it")
            // Save, because we can guarantee that the device is online and that the purchase check has succeeded
            PrefManager.putBoolean(PROPERTY_AD_FREE, it)
        }

    /**
     * A Flow that reports on purchases that are in the [PurchaseState.PENDING]
     * state, e.g. if the user chooses an instrument that needs additional steps between
     * when they initiate the purchase, and when the payment method is processed.
     *
     * @see <a href="https://developer.android.com/google/play/billing/integrate#pending">Handling pending transactions</a>
     * @see [setSkuStateFromPurchase]
     */
    private val _pendingPurchase = MutableStateFlow<Purchase?>(null)
    val pendingPurchase: StateFlow<Purchase?>
        /**
         * No need for [distinctUntilChanged], since [StateFlow]s are already distinct
         */
        get() = _pendingPurchase

    /**
     * A Flow that reports on purchases that have changed their state,
     * e.g. [PurchaseState.PENDING] -> [PurchaseState.PURCHASED].
     *
     * @see [setSkuStateFromPurchase]
     */
    private val _purchaseStateChange = MutableStateFlow<Purchase?>(null)
    val purchaseStateChange: StateFlow<Purchase?>
        /**
         * No need for [distinctUntilChanged], since [StateFlow]s are already distinct
         */
        get() = _purchaseStateChange

    private val _newPurchase = MutableSharedFlow<Pair<Int, Purchase?>>(extraBufferCapacity = 1)
    val newPurchase = _newPurchase.distinctUntilChanged().onEach { (responseCode, purchase) ->
        logDebug(TAG, "[newPurchaseFlow] $responseCode: ${purchase?.products}")

        when (responseCode) {
            BillingResponseCode.OK -> PrefManager.putBoolean(PROPERTY_AD_FREE, purchase != null)
            BillingResponseCode.ITEM_ALREADY_OWNED -> {
                // This is tricky to deal with. Even pending purchases show up as "items already owned",
                // so we can't grant entitlement i.e. set [PROPERTY_AD_FREE] to `true`.
                // A message should be shown to the user informing them they may have pending purchases.
                // This case will be handled by observing the pending purchases LiveData.
                // Entitlement is being granted by observing to the in-app SKU details list LiveData anyway.
            }

            else -> {
                PrefManager.putBoolean(PROPERTY_AD_FREE, false)
                setProductState(SKU_INAPP_AD_FREE, SkuState.NotPurchased)
            }
        }

        purchase?.products?.forEach {
            when (it) {
                SKU_SUBS_AD_FREE_MONTHLY, SKU_SUBS_AD_FREE_YEARLY -> {
                    // Make sure that subscriptions upgrades/downgrades
                    // are reflected correctly in the UI
                    refreshPurchases()
                }
            }
        }
    }

    private val _consumedPurchaseSkus = MutableSharedFlow<List<String>>()
    private val consumedPurchaseSkus = _consumedPurchaseSkus.distinctUntilChanged().onEach {
        logDebug(TAG, "[consumedPurchaseSkusFlow] $it")
        // Take action (e.g. update models) on each consumed purchase
    }

    /**
     * A Flow that reports if a billing flow is in process, meaning that
     * [launchBillingFlow] has returned a successful [BillingResponseCode.OK],
     * but [onPurchasesUpdated] hasn't been called yet.
     */
    private val _billingFlowInProcess = MutableStateFlow(false)
    val billingFlowInProcess: StateFlow<Boolean>
        /**
         * No need for [distinctUntilChanged], since [StateFlow]s are already distinct
         */
        get() = _billingFlowInProcess

    init {
        // Initialize flows for all known SKUs, so that state & details can be
        // observed in ViewModels. This repository exposes mappings that are
        // more useful for the rest of the application (via ViewModels).
        addSkuFlows(INAPP_SKUS)
        addSkuFlows(SUBS_SKUS)

        billingClient.startConnection(this)
    }

    /**
     * Called by initializeFlows to create the various Flow objects we're planning to emit
     *
     * @param skuList a List<String> of SKUs representing purchases and subscriptions
     */
    private fun addSkuFlows(skuList: List<String>) = skuList.forEach { sku ->
        val skuState = MutableStateFlow(SkuState.Unknown)
        val details = MutableStateFlow<ProductDetails?>(null)

        // Flow is considered "active" if there's at least one subscriber.
        // `distinctUntilChanged`: ensure we only react to true<->false changes.
        details.subscriptionCount.map { it > 0 }.distinctUntilChanged().onEach { active ->
            if (active && (SystemClock.elapsedRealtime() - productDetailsResponseTime > PRODUCT_DETAILS_REQUERY_TIME)) {
                logVerbose(TAG, "[addSkuFlows] stale SKUs; requerying")
                productDetailsResponseTime = SystemClock.elapsedRealtime()
                queryProductDetails()
            }
        }.launchIn(mainScope) // launch it

        skuStateMap[sku] = skuState
        productDetailsMap[sku] = details
    }

    /**
     * It's recommended to requery purchases during onResume
     */
    override fun onResume(owner: LifecycleOwner) {
        logDebug(TAG, "[onResume]")
        // Avoids an extra purchase refresh after we finish a billing flow
        if (!_billingFlowInProcess.value && billingClient.isReady) ioScope.launch {
            refreshPurchases()
        }
    }

    override fun onBillingSetupFinished(result: BillingResult) {
        val responseCode = result.responseCode
        logDebug(TAG, "[onBillingSetupFinished] $responseCode: ${result.debugMessage}")
        when (responseCode) {
            BillingResponseCode.OK -> ioScope.launch {
                queryProductDetails()
                refreshPurchases()
            }.also {
                reconnectMilliseconds = RECONNECT_TIMER_START_MILLISECONDS
            }

            else -> reconnectWithExponentialBackoff()
        }
    }

    /**
     * Rare occurrence; could happen if Play Store self-updates or is force-closed
     */
    override fun onBillingServiceDisconnected() = reconnectWithExponentialBackoff()

    /**
     * Reconnect to [BillingClient] with exponential backoff, with a max of
     * [RECONNECT_TIMER_MAX_TIME_MILLISECONDS]
     */
    private fun reconnectWithExponentialBackoff() = handler.postDelayed(
        { billingClient.startConnection(this) }, reconnectMilliseconds
    ).let {
        reconnectMilliseconds = min(
            reconnectMilliseconds * 2,
            RECONNECT_TIMER_MAX_TIME_MILLISECONDS
        )
    }

    /**
     * Called by [BillingClient] when new purchases are detected; typically in
     * response to [launchBillingFlow]
     *
     * @param result billing result of the purchase flow
     * @param list of new purchases
     */
    override fun onPurchasesUpdated(result: BillingResult, list: List<Purchase>?) {
        when (result.responseCode) {
            BillingResponseCode.OK -> list?.let {
                processPurchaseList(it, null)
                return
            } ?: logDebug(
                TAG,
                "[onPurchasesUpdated] mull purchase list returned from OK response"
            )

            BillingResponseCode.USER_CANCELED -> logDebug(
                TAG,
                "[onPurchasesUpdated] USER_CANCELED"
            )

            BillingResponseCode.ITEM_ALREADY_OWNED -> logDebug(
                TAG,
                "[onPurchasesUpdated] ITEM_ALREADY_OWNED"
            )

            BillingResponseCode.DEVELOPER_ERROR -> logBillingError(
                TAG,
                "[onPurchasesUpdated] DEVELOPER_ERROR"
            )

            else -> logDebug(TAG, "[onPurchasesUpdated] ${result.responseCode}: ${result.debugMessage}")
        }

        _newPurchase.tryEmit(Pair(result.responseCode, null))

        ioScope.launch {
            _billingFlowInProcess.emit(false)
        }
    }

    private fun getSkuState(sku: String) = skuStateMap[sku] ?: flowOf(SkuState.Unknown).also {
        logWarning(TAG, "[getSkuState] unknown SKU: $sku")
    }

    private fun getSkuPrice(sku: String) = productDetailsMap[sku]?.map {
        // TODO: adjust for subscriptions when needed
        it?.oneTimePurchaseOfferDetails?.formattedPrice
    } ?: flowOf(null).also {
        logWarning(TAG, "[getSkuPrice] unknown SKU: $sku")
    }

    /**
     * GPBLv3 queried purchases synchronously, while v4 supports async.
     *
     * Note that the billing client only returns active purchases.
     */
    private suspend fun refreshPurchases() = withContext(Dispatchers.IO) {
        logDebug(TAG, "[refreshPurchases] start")

        var purchasesResult = billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(ProductType.INAPP)
                .build()
        )
        var billingResult = purchasesResult.billingResult
        var responseCode = billingResult.responseCode
        var debugMessage = billingResult.debugMessage
        if (responseCode == BillingResponseCode.OK) {
            processPurchaseList(purchasesResult.purchasesList, INAPP_SKUS)
        } else logBillingError(TAG, "[refreshPurchases] INAPP $responseCode: $debugMessage")

        purchasesResult = billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(ProductType.SUBS)
                .build()
        )
        billingResult = purchasesResult.billingResult
        responseCode = billingResult.responseCode
        debugMessage = billingResult.debugMessage
        if (responseCode == BillingResponseCode.OK) {
            processPurchaseList(purchasesResult.purchasesList, SUBS_SKUS)
        } else logBillingError(TAG, "[refreshPurchases] SUBS $responseCode: $debugMessage")

        logDebug(TAG, "[refreshPurchases] finish")
    }

    /**
     * Automatic support for upgrading/downgrading subscription
     */
    fun makePurchase(activity: Activity, sku: String) = when (sku) {
        SKU_SUBS_AD_FREE_MONTHLY -> SKU_SUBS_AD_FREE_YEARLY
        SKU_SUBS_AD_FREE_YEARLY -> SKU_SUBS_AD_FREE_MONTHLY
        else -> null
    }?.let { oldSku ->
        launchBillingFlow(activity, sku, arrayOf(oldSku))
    } ?: launchBillingFlow(activity, sku)

    /**
     * Launch the billing flow. This will launch an external Activity for a result, so it requires
     * an Activity reference. For subscriptions, it supports upgrading from one SKU type to another
     * by passing in SKUs to be upgraded.
     *
     * @param activity active activity to launch our billing flow from
     * @param sku SKU (Product ID) to be purchased
     * @param upgradeSkus SKUs that the subscription can be upgraded from
     *
     * @return true if launch is successful
     */
    @UiThread
    fun launchBillingFlow(activity: Activity, sku: String, upgradeSkus: Array<String>? = null) {
        /**
         * Mark initiated so that [com.oxygenupdater.ui.settings.SettingsScreen]
         * disables the button and sets text to "Please wait…".
         */
        setProductState(sku, SkuState.PurchaseInitiated)

        val details = productDetailsMap[sku]?.value ?: return logBillingError(
            TAG, "[launchBillingFlow] unknown SKU: $sku"
        )

        val builder = BillingFlowParams.newBuilder()
        builder.setProductDetailsParamsList(
            listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(details)
                    .build()
            )
        )

        mainScope.launch {
            if (upgradeSkus != null) {
                val heldSubscriptions = getPurchases(ProductType.SUBS, upgradeSkus)
                when (heldSubscriptions.size) {
                    0 -> {} // no-op

                    1 -> builder.setSubscriptionUpdateParams(
                        BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                            .setOldPurchaseToken(heldSubscriptions[0].purchaseToken)
                            .build()
                    )

                    else -> logBillingError(
                        TAG,
                        "[launchBillingFlow] can't upgrade: ${heldSubscriptions.size} subscriptions subscribed to"
                    )
                }
            }
            val result = billingClient.launchBillingFlow(activity, builder.build())
            if (result.responseCode == BillingResponseCode.OK) _billingFlowInProcess.emit(true)
            else logBillingError(TAG, "[launchBillingFlow] ${result.debugMessage}")
        }
    }

    /**
     * Consumes an in-app purchase. Interested listeners can watch the [consumedPurchaseSkus] LiveEvent.
     * To make things easy, you can send in a list of SKUs that are auto-consumed by the
     * [BillingRepository].
     */
    private suspend fun consumeInAppPurchase(
        sku: String,
    ) = withContext(Dispatchers.IO) {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(ProductType.INAPP)
                .build()
        ).let { result ->
            val billingResult = result.billingResult
            val purchasesList = result.purchasesList
            val responseCode = billingResult.responseCode
            if (responseCode == BillingResponseCode.OK) purchasesList.forEach { purchase ->
                // For right now any bundle of SKUs must all be consumable
                purchase.products.find { it == sku }?.also {
                    return@let consumePurchase(purchase)
                }
            } else logBillingError(TAG, "[consumeInAppPurchase] $responseCode: ${billingResult.debugMessage}")

            logBillingError(TAG, "[consumeInAppPurchase] unknown SKU: $sku")
        }
    }

    /**
     * **Only for DEBUG use**: consume an in-app purchase so it can be bought again.
     *
     * This helps to rapidly test billing functionality (Play Store caches all
     * purchases the user owns, and it can take a long time for it be evicted).
     *
     * Note that this function should be unused throughout the app, except
     * while under testing by "license testers", in which case a developer
     * sends them an APK (different from what's publicly distributed) — it'll
     * have a button in the UI somewhere that calls this function.
     *
     * License testers are shown a few test instruments by Google Play (e.g.
     * always approves, always declines, approves after delay, etc).
     *
     * @see <a href="https://developer.android.com/google/play/billing/test">Test Google Play Billing Library integration</a>
     */
    @Deprecated("Only for local testing: should not be part of checked-in code or an APK release", level = DeprecationLevel.ERROR)
    fun debugConsumeAdFree() = ioScope.launch {
        consumeInAppPurchase(SKU_INAPP_AD_FREE)
    }

    /**
     * Receives the result from [queryProductDetails].
     *
     * Store the ProductDetails and post them in the [productDetailsMap]. This allows other
     * parts of the app to use the [ProductDetails] to show SKU information and make purchases.
     */
    private fun onProductDetailsResponse(
        result: BillingResult,
        detailsList: List<ProductDetails>?,
    ) {
        val responseCode = result.responseCode
        val debugMessage = result.debugMessage

        when (responseCode) {
            BillingResponseCode.OK -> {
                logInfo(TAG, "[onProductDetailsResponse] $responseCode: $debugMessage")
                if (detailsList.isNullOrEmpty()) {
                    logBillingError(TAG, "[onProductDetailsResponse] null/empty List<ProductDetails>")
                } else detailsList.forEach {
                    val id = it.productId
                    productDetailsMap[it.productId]?.tryEmit(it) ?: logBillingError(
                        TAG,
                        "[onProductDetailsResponse] unknown product: $id"
                    )
                }
            }

            BillingResponseCode.USER_CANCELED -> logInfo(
                TAG,
                "[onProductDetailsResponse] USER_CANCELED: $debugMessage"
            )

            BillingResponseCode.SERVICE_DISCONNECTED -> logBillingError(
                TAG,
                "[onProductDetailsResponse] SERVICE_DISCONNECTED: $debugMessage"
            )

            BillingResponseCode.SERVICE_UNAVAILABLE -> logBillingError(
                TAG,
                "[onProductDetailsResponse] SERVICE_UNAVAILABLE: $debugMessage"
            )

            BillingResponseCode.BILLING_UNAVAILABLE -> logBillingError(
                TAG,
                "[onProductDetailsResponse] BILLING_UNAVAILABLE: $debugMessage"
            )

            BillingResponseCode.ITEM_UNAVAILABLE -> logBillingError(
                TAG,
                "[onProductDetailsResponse] ITEM_UNAVAILABLE: $debugMessage"
            )

            BillingResponseCode.DEVELOPER_ERROR -> logBillingError(
                TAG,
                "[onProductDetailsResponse] DEVELOPER_ERROR: $debugMessage"
            )

            BillingResponseCode.ERROR -> logBillingError(
                TAG,
                "[onProductDetailsResponse] ERROR: $debugMessage"
            )

            BillingResponseCode.FEATURE_NOT_SUPPORTED -> logBillingError(
                TAG,
                "[onProductDetailsResponse] FEATURE_NOT_SUPPORTED: $debugMessage"
            )

            BillingResponseCode.ITEM_ALREADY_OWNED -> logBillingError(
                TAG,
                "[onProductDetailsResponse] ITEM_ALREADY_OWNED: $debugMessage"
            )

            BillingResponseCode.ITEM_NOT_OWNED -> logBillingError(
                TAG,
                "[onProductDetailsResponse] ITEM_NOT_OWNED: $debugMessage"
            )

            else -> logBillingError(TAG, "[onProductDetailsResponse] $responseCode: $debugMessage")
        }

        productDetailsResponseTime = if (responseCode == BillingResponseCode.OK) {
            SystemClock.elapsedRealtime()
        } else -PRODUCT_DETAILS_REQUERY_TIME
    }

    /**
     * Calls the billing client functions to query sku details for both the inapp and subscription
     * SKUs. SKU details are useful for displaying item names and price lists to the user, and are
     * required to make a purchase.
     */
    private suspend fun queryProductDetails() = withContext(Dispatchers.IO) {
        if (INAPP_SKUS.isNotEmpty() || SUBS_SKUS.isNotEmpty()) {
            val products = buildList {
                addAll(INAPP_SKUS.map {
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(it)
                        .setProductType(ProductType.INAPP)
                        .build()
                })
                addAll(SUBS_SKUS.map {
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(it)
                        .setProductType(ProductType.SUBS)
                        .build()
                })
            }

            billingClient.queryProductDetails(
                QueryProductDetailsParams.newBuilder()
                    .setProductList(products)
                    .build()
            ).also {
                onProductDetailsResponse(it.billingResult, it.productDetailsList)
            }
        }
    }

    /**
     * Used internally to get purchases from a requested set of SKUs. This is particularly
     * important when changing subscriptions, as onPurchasesUpdated won't update the purchase state
     * of a subscription that has been upgraded from.
     *
     * @param products to get purchase information for
     * @param productType inapp or subscription, to get purchase information for
     * @return purchases
     */
    private suspend fun getPurchases(
        productType: String,
        products: Array<String>?,
    ) = withContext(Dispatchers.IO) {
        buildList {
            val purchasesResult = billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(productType)
                    .build()
            )
            val billingResult = purchasesResult.billingResult
            val responseCode = billingResult.responseCode
            if (responseCode == BillingResponseCode.OK) purchasesResult.purchasesList.forEach { purchase ->
                products?.forEach { sku ->
                    purchase.products.find { it == sku }?.also {
                        add(purchase)
                    }
                }
            } else logBillingError(TAG, "[getPurchases] $responseCode: ${billingResult.debugMessage}")
        }
    }

    /**
     * Calling this means that we have the most up-to-date information for an SKU
     * in a purchase object. This uses [PurchaseState]s & the acknowledged state.
     *
     * @param purchase an up-to-date object to set the state for the SKU
     */
    private fun setSkuStateFromPurchase(purchase: Purchase) = purchase.products.forEach {
        val skuStateFlow = skuStateMap[it] ?: logBillingError(
            TAG,
            "[setSkuStateFromPurchase] unknown SKU: $it"
        ).let { return@forEach }

        val state = purchase.purchaseState
        if ((state == PurchaseState.PURCHASED || state == PurchaseState.PENDING) && !isSignatureValid(purchase)) {
            logBillingError(TAG, "[setSkuStateFromPurchase] invalid signature")
            // Don't set SkuState if signature validation fails
            return@forEach
        }

        val oldState = skuStateFlow.value
        when (state) {
            PurchaseState.PENDING -> SkuState.Pending
            PurchaseState.UNSPECIFIED_STATE -> SkuState.NotPurchased
            PurchaseState.PURCHASED -> if (purchase.isAcknowledged) {
                SkuState.PurchasedAndAcknowledged
            } else SkuState.Purchased

            else -> null
        }?.let { newState ->
            if (newState == SkuState.Pending) _pendingPurchase.tryEmit(purchase)
            else if (newState != oldState) _purchaseStateChange.tryEmit(purchase)
            skuStateFlow.tryEmit(newState)
        } ?: logBillingError(TAG, "[setSkuStateFromPurchase] unknown purchase state: $state")
    }

    /**
     * Since we (mostly) are getting sku states when we actually make a purchase or update
     * purchases, we keep some internal state when we do things like acknowledge or consume.
     *
     * @param sku product ID to change the state of
     * @param newState the new state of the sku
     */
    private fun setProductState(
        sku: String,
        newState: SkuState,
    ) = skuStateMap[sku]?.tryEmit(newState) ?: logBillingError(TAG, "[setSkuState] unknown SKU: $sku")

    /**
     * Goes through each purchase and makes sure that the purchase state is processed and the state
     * is available through Flows. Verifies signature and acknowledges purchases. PURCHASED isn't
     * returned until the purchase is acknowledged.
     *
     * https://developer.android.com/google/play/billing/billing_library_releases_notes#2_0_acknowledge
     *
     * Developers can choose to acknowledge purchases from a server using the
     * Google Play Developer API. The server has direct access to the user database,
     * so using the Google Play Developer API for acknowledgement might be more reliable.
     *
     * If the purchase token is not acknowledged within 3 days,
     * then Google Play will automatically refund and revoke the purchase.
     * This behavior helps ensure that users are not charged unless the user has successfully
     * received access to the content.
     * This eliminates a category of issues where users complain to developers
     * that they paid for something that the app is not giving to them.
     *
     * If a [skusToUpdate] list is passed-into this method, any purchases not in the list of
     * purchases will have their state set to [SkuState.NotPurchased].
     *
     * @param purchases the List of purchases to process.
     * @param skusToUpdate a list of skus that we want to update the state from --- this allows us
     * to set the state of non-returned SKUs to [SkuState.NotPurchased].
     */
    private fun processPurchaseList(
        purchases: List<Purchase>?,
        skusToUpdate: List<String>?,
    ) {
        val updatedSkus = HashSet<String>()
        purchases?.forEach { purchase ->
            purchase.products.forEach { sku ->
                skuStateMap[sku]?.let {
                    updatedSkus.add(sku)
                } ?: logBillingError(TAG, "[processPurchaseList] unknown SKU: $sku")
            }

            // Make sure the SkuState is set
            setSkuStateFromPurchase(purchase)

            if (purchase.purchaseState == PurchaseState.PURCHASED) {
                logDebug(TAG, "[processPurchaseList] found purchase with SKUs: ${purchase.products}")

                var isConsumable = false
                ioScope.launch {
                    for (sku in purchase.products) {
                        if (CONSUMABLE_SKUS.contains(sku)) isConsumable = true
                        else if (isConsumable) {
                            isConsumable = false
                            logBillingError(
                                TAG,
                                "[processPurchaseList] purchase can't contain both consumables & non-consumables: ${purchase.products}"
                            )
                            break
                        }
                    }

                    if (isConsumable) {
                        logDebug(TAG, "[processPurchaseList] consuming purchase")
                        consumePurchase(purchase)
                        _newPurchase.tryEmit(Pair(BillingResponseCode.OK, purchase))
                    } else if (!purchase.isAcknowledged) {
                        logDebug(TAG, "[processPurchaseList] acknowledging purchase")
                        // Acknowledge everything — new purchases are ones not yet acknowledged
                        val result = billingClient.acknowledgePurchase(
                            AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.purchaseToken)
                                .build()
                        )

                        if (result.responseCode == BillingResponseCode.OK) {
                            // Purchase acknowledged
                            purchase.products.forEach {
                                setProductState(it, SkuState.PurchasedAndAcknowledged)
                            }
                            _newPurchase.tryEmit(Pair(BillingResponseCode.OK, purchase))
                        } else logBillingError(TAG, "[processPurchaseList] error acknowledging: ${purchase.products}")
                    }
                }
            }
        } ?: logDebug(TAG, "[processPurchaseList] null purchase list")

        // Clear purchase state of anything that didn't come with this purchase list if this is
        // part of a refresh.
        skusToUpdate?.forEach {
            if (productDetailsMap[it]?.value == null) setProductState(it, SkuState.Unknown)
            else if (!updatedSkus.contains(it)) setProductState(it, SkuState.NotPurchased)
        }
    }

    /**
     * Internal call only. Assumes that all signature checks have been completed and the purchase
     * is ready to be consumed. If the sku is already being consumed, does nothing.
     * @param purchase purchase to consume
     */
    private suspend fun consumePurchase(purchase: Purchase) = withContext(Dispatchers.IO) {
        // weak check to make sure we're not already consuming the sku
        if (purchaseConsumptionInProcess.contains(purchase)) return@withContext

        purchaseConsumptionInProcess.add(purchase)
        val consumePurchaseResult = billingClient.consumePurchase(
            ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
        )
        purchaseConsumptionInProcess.remove(purchase)

        if (consumePurchaseResult.billingResult.responseCode == BillingResponseCode.OK) {
            logDebug(TAG, "[consumePurchase] successful, emitting SKU")
            _consumedPurchaseSkus.emit(purchase.products)
            // Since we've consumed the purchase
            purchase.products.forEach {
                setProductState(it, SkuState.NotPurchased)
            }
        } else logBillingError(TAG, "[consumePurchase] ${consumePurchaseResult.billingResult.debugMessage}")
    }

    /**
     * Ideally your implementation will comprise a secure server, rendering this check
     * unnecessary.
     *
     * @see [Security]
     */
    private fun isSignatureValid(purchase: Purchase) = Security.verifyPurchase(
        BuildConfig.BASE64_PUBLIC_KEY,
        purchase.originalJson,
        purchase.signature
    )

    @JvmInline
    @Immutable
    value class SkuState(val value: Int) {

        override fun toString() = when (this) {
            Unknown -> "Unknown"
            NotPurchased -> "NotPurchased"
            PurchaseInitiated -> "PurchaseInitiated"
            Pending -> "Pending"
            Purchased -> "Purchased"
            PurchasedAndAcknowledged -> "PurchasedAndAcknowledged"
            else -> "Unknown"
        }

        companion object {
            @Stable
            val Unknown = SkuState(0)

            @Stable
            val NotPurchased = SkuState(1)

            @Stable
            val PurchaseInitiated = SkuState(2)

            @Stable
            val Pending = SkuState(3)

            @Stable
            val Purchased = SkuState(4)

            @Stable
            val PurchasedAndAcknowledged = SkuState(5)
        }
    }

    companion object {
        private const val TAG = "BillingRepository"

        /**
         * 1 second
         */
        private const val RECONNECT_TIMER_START_MILLISECONDS = 1000L

        /**
         * 15 minutes
         */
        private const val RECONNECT_TIMER_MAX_TIME_MILLISECONDS = 1000L * 60L * 15L

        /**
         * 4 hours
         */
        private const val PRODUCT_DETAILS_REQUERY_TIME = 1000L * 60L/* * 60L * 4L*/

        val SKU_INAPP_AD_FREE = PurchaseType.AD_FREE.sku
        private val SKU_SUBS_AD_FREE_MONTHLY = PurchaseType.AD_FREE_MONTHLY.sku
        private val SKU_SUBS_AD_FREE_YEARLY = PurchaseType.AD_FREE_YEARLY.sku

        // Ideally SKUs should be fetched from the server, so that the app
        // doesn't need to be updated every time we add a new product.
        // However, we don't add products at all, so it's fine for now.
        private val INAPP_SKUS = listOf(SKU_INAPP_AD_FREE)
        private val SUBS_SKUS = listOf<String>()
        private val CONSUMABLE_SKUS = listOf<String>()

        private val handler = Handler(Looper.getMainLooper())
    }
}
