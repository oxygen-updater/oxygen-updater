package com.oxygenupdater.models

import com.android.billingclient.api.Purchase
import com.oxygenupdater.enums.PurchaseType
import kotlinx.serialization.Serializable

@Serializable
class VerifyPurchaseBody private constructor(
    /** [Purchase.getOrderId] */
    val orderId: String?,
    /** [Purchase.getPackageName] */
    val packageName: String,
    /** [Purchase.getProducts] joined to string */
    val productId: String,
    /** [Purchase.getPurchaseTime] */
    val purchaseTime: Long,
    /** [Purchase.getPurchaseState] */
    val purchaseState: Int,
    /** [Purchase.getDeveloperPayload] */
    val developerPayload: String,
    /** [Purchase.getPurchaseToken] */
    val token: String,
    /** [Purchase.getPurchaseToken] */
    val purchaseToken: String,
    /** [Purchase.isAutoRenewing] */
    val autoRenewing: Boolean,
    /** [Purchase.getOrderId] */
    val signature: String?,

    /** [com.oxygenupdater.enums.PurchaseType.name] */
    val purchaseType: String,
    /** [com.oxygenupdater.enums.PurchaseType.type] */
    val itemType: String,

    val amount: String?,
) {
    companion object {
        fun from(
            purchase: Purchase,
            purchaseType: PurchaseType,
            amount: String?,
        ) = VerifyPurchaseBody(
            orderId = purchase.orderId,
            packageName = purchase.packageName,
            productId = purchase.products.joinToString(","),
            purchaseTime = purchase.purchaseTime,
            purchaseState = purchase.purchaseState,
            developerPayload = purchase.developerPayload,
            token = purchase.purchaseToken,
            purchaseToken = purchase.purchaseToken,
            autoRenewing = purchase.isAutoRenewing,
            signature = purchase.signature,
            purchaseType = purchaseType.name,
            itemType = purchaseType.type,
            amount = amount,
        )
    }
}
