package com.oxygenupdater.enums

import com.android.billingclient.api.BillingClient.SkuType

enum class PurchaseType(
    @SkuType val type: String
) {
    AD_FREE(SkuType.INAPP);
    // Other purchase types may be added in the future here...

    override fun toString() = name
}
