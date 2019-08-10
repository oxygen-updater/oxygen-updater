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

import org.json.JSONException
import org.json.JSONObject

/**
 * Represents an in-app billing purchase.
 */
class Purchase @Throws(JSONException::class)
constructor(itemType: String, jsonPurchaseInfo: String, signature: String) {
    var itemType: String
        internal set  // ITEM_TYPE_INAPP or ITEM_TYPE_SUBS
    var orderId: String
        internal set
    var packageName: String
        internal set
    var sku: String
        internal set
    var purchaseTime: Long = 0
        internal set
    var purchaseState: Int = 0
        internal set
    var developerPayload: String
        internal set
    var token: String
        internal set
    var originalJson: String
        internal set
    var signature: String
        internal set
    var isAutoRenewing: Boolean = false
        internal set

    init {
        this.itemType = itemType
        originalJson = jsonPurchaseInfo
        val o = JSONObject(originalJson)
        orderId = o.optString("orderId")
        packageName = o.optString("packageName")
        sku = o.optString("productId")
        purchaseTime = o.optLong("purchaseTime")
        purchaseState = o.optInt("purchaseState")
        developerPayload = o.optString("developerPayload")
        token = o.optString("token", o.optString("purchaseToken"))
        isAutoRenewing = o.optBoolean("autoRenewing")
        this.signature = signature
    }

    override fun toString(): String {
        return "PurchaseInfo(type:$itemType):$originalJson"
    }
}
