package com.arjanvlek.oxygenupdater.internal.iab

import com.arjanvlek.oxygenupdater.enums.PurchaseType
import org.json.JSONObject

/**
 * Represents an in-app billing
 */
class Purchase(
    // ITEM_TYPE_INAPP or ITEM_TYPE_SUBS
    var itemType: String?,
    var originalJson: String,
    val signature: String
) {
    var orderId: String
    var packageName: String
    var sku: String
    var purchaseTime: Long
    var purchaseState: Int
    var developerPayload: String
    var token: String
    var isAutoRenewing: Boolean

    override fun toString(): String {
        return "PurchaseInfo(type:$itemType):$originalJson"
    }

    /**
     * Used in [com.arjanvlek.oxygenupdater.repositories.ServerRepository.verifyPurchase].
     *
     * This must return a [HashMap] to avoid this exception:
     * ```
     * java.lang.IllegalArgumentException: Parameter type must not include a type variable or wildcard: java.util.Map<java.lang.String, ?>
     * ```
     *
     * [See this comment](https://github.com/square/retrofit/issues/1805#issuecomment-291563717) for more info.
     */
    fun createHashMapForVerificationRequest(
        amount: String?,
        purchaseType: PurchaseType
    ): HashMap<String, Any?> = hashMapOf(
        "orderId" to orderId,
        "packageName" to packageName,
        "productId" to sku,
        "purchaseTime" to purchaseTime,
        "purchaseState" to purchaseState,
        "developerPayload" to developerPayload,
        "token" to token,
        "purchaseToken" to token,
        "autoRenewing" to isAutoRenewing,
        "purchaseType" to purchaseType,
        "itemType" to itemType,
        "signature" to signature,
        "amount" to amount
    )

    init {
        JSONObject(originalJson).apply {
            orderId = optString("orderId")
            packageName = optString("packageName")
            sku = optString("productId")
            purchaseTime = optLong("purchaseTime")
            purchaseState = optInt("purchaseState")
            developerPayload = optString("developerPayload")
            token = optString("token", optString("purchaseToken"))
            isAutoRenewing = optBoolean("autoRenewing")
        }
    }
}
