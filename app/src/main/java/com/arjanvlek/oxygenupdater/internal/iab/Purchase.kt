package com.arjanvlek.oxygenupdater.internal.iab

import org.json.JSONObject

/**
 * Represents an in-app billing purchase.
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
