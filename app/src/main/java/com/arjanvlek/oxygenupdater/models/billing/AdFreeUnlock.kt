package com.arjanvlek.oxygenupdater.models.billing

import androidx.room.Entity

/**
 * Indicates whether the user owns the ad-free unlock.
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
@Entity(tableName = "ad_free_unlock")
data class AdFreeUnlock(val entitled: Boolean) : Entitlement() {
    override fun mayPurchase() = !entitled
}
