package com.oxygenupdater.enums

import com.android.billingclient.api.BillingClient.ProductType

enum class PurchaseType(
    @ProductType val type: String,
    val sku: String,
) {
    AD_FREE(ProductType.INAPP, "oxygen_updater_ad_free"),

    // Subscriptions aren't used, but some code related to it is kept in the
    // form of templates, just in case we implement them in the future
    AD_FREE_MONTHLY(ProductType.SUBS, "oxygen_updater_ad_free_monthly"),
    AD_FREE_YEARLY(ProductType.SUBS, "oxygen_updater_ad_free_yearly");

    // Other purchase types may be added in the future here

    override fun toString() = name
}
