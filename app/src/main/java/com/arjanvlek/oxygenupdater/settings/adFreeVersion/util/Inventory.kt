/* Copyright (c) 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arjanvlek.oxygenupdater.settings.adFreeVersion.util

import java.util.*

/**
 * Represents a block of information about in-app items. An Inventory is returned by such methods as
 * [IabHelper.queryInventory].
 */
class Inventory internal constructor() {
    private val mSkuMap = HashMap<String, SkuDetails>()
    private val mPurchaseMap = HashMap<String, Purchase>()

    /**
     * Returns a list of all owned product IDs.
     */
    internal val allOwnedSkus: List<String>
        get() = ArrayList(mPurchaseMap.keys)

    /**
     * Returns a list of all purchases.
     */
    internal val allPurchases: List<Purchase>
        get() = ArrayList(mPurchaseMap.values)

    /**
     * Returns the listing details for an in-app product.
     */
    fun getSkuDetails(sku: String): SkuDetails? {
        return mSkuMap[sku]
    }

    /**
     * Returns purchase information for a given product, or null if there is no purchase.
     */
    fun getPurchase(sku: String): Purchase? {
        return mPurchaseMap[sku]
    }

    /**
     * Returns whether or not there exists a purchase of the given product.
     */
    fun hasPurchase(sku: String): Boolean {
        return mPurchaseMap.containsKey(sku)
    }

    /**
     * Return whether or not details about the given product are available.
     */
    fun hasDetails(sku: String): Boolean {
        return mSkuMap.containsKey(sku)
    }

    /**
     * Erase a purchase (locally) from the inventory, given its product ID. This just modifies the
     * Inventory object locally and has no effect on the server! This is useful when you have an
     * existing Inventory object which you know to be up to date, and you have just consumed an item
     * successfully, which means that erasing its purchase data from the Inventory you already have
     * is quicker than querying for a new Inventory.
     */
    fun erasePurchase(sku: String) {
        mPurchaseMap.remove(sku)
    }

    /**
     * Returns a list of all owned product IDs of a given type
     */
    internal fun getAllOwnedSkus(itemType: String): List<String> {
        val result = ArrayList<String>()
        for (p in mPurchaseMap.values) {
            if (p.itemType == itemType) {
                result.add(p.sku)
            }
        }
        return result
    }

    internal fun addSkuDetails(d: SkuDetails) {
        mSkuMap[d.sku] = d
    }

    internal fun addPurchase(p: Purchase) {
        mPurchaseMap[p.sku] = p
    }
}
