package com.arjanvlek.oxygenupdater.internal.iab

import org.json.JSONObject

/**
 * Represents an in-app product's listing details.
 */
class SkuDetails(private val mItemType: String, private val mJson: String) {

    val sku: String
    val type: String
    val price: String
    val priceAmountMicros: Long
    val priceCurrencyCode: String
    val title: String
    val description: String

    constructor(jsonSkuDetails: String) : this(IabHelper.ITEM_TYPE_INAPP, jsonSkuDetails)

    override fun toString(): String {
        return "SkuDetails:$mJson"
    }

    init {
        JSONObject(mJson).apply {
            sku = optString("productId")
            type = optString("type")
            price = optString("price")
            priceAmountMicros = optLong("price_amount_micros")
            priceCurrencyCode = optString("price_currency_code")
            title = optString("title")
            description = optString("description")
        }
    }
}
