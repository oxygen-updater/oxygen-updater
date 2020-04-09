package com.arjanvlek.oxygenupdater.internal.iab

import java.util.*

/**
 * Represents a block of information about in-app items. An Inventory is returned by such methods as
 * [IabHelper.queryInventory].
 */
@Suppress("unused")
class Inventory internal constructor() {

    private val mSkuMap: MutableMap<String?, SkuDetails?> = HashMap()
    private val mPurchaseMap: MutableMap<String?, Purchase?> = HashMap()

    /**
     * Returns the listing details for an in-app product.
     */
    fun getSkuDetails(sku: String?): SkuDetails? {
        return mSkuMap[sku]
    }

    /**
     * Returns purchase information for a given product, or null if there is no purchase.
     */
    fun getPurchase(sku: String?): Purchase? {
        return mPurchaseMap[sku]
    }

    /**
     * Returns whether or not there exists a purchase of the given product.
     */
    fun hasPurchase(sku: String?): Boolean {
        return mPurchaseMap.containsKey(sku)
    }

    /**
     * Return whether or not details about the given product are available.
     */
    fun hasDetails(sku: String?): Boolean {
        return mSkuMap.containsKey(sku)
    }

    /**
     * Erase a purchase (locally) from the inventory, given its product ID. This just modifies the
     * Inventory object locally and has no effect on the server! This is useful when you have an
     * existing Inventory object which you know to be up to date, and you have just consumed an item
     * successfully, which means that erasing its purchase data from the Inventory you already have
     * is quicker than querying for a new Inventory.
     */
    fun erasePurchase(sku: String?) {
        mPurchaseMap.remove(sku)
    }

    /**
     * Returns a list of all owned product IDs.
     */
    val allOwnedSkus: List<String?>
        get() = ArrayList(mPurchaseMap.keys)

    /**
     * Returns a list of all owned product IDs of a given type
     */
    fun getAllOwnedSkus(itemType: String): List<String> {
        val result: MutableList<String> = ArrayList()

        mPurchaseMap.values.forEach {
            if (it?.itemType == itemType) {
                result.add(it.sku)
            }
        }

        return result
    }

    /**
     * Returns a list of all purchases.
     */
    val allPurchases: List<Purchase?>
        get() = ArrayList(mPurchaseMap.values)

    fun addSkuDetails(d: SkuDetails) {
        mSkuMap[d.sku] = d
    }

    fun addPurchase(p: Purchase) {
        mPurchaseMap[p.sku] = p
    }
}
