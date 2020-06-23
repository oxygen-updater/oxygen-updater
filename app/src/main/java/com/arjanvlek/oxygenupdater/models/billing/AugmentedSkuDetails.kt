package com.arjanvlek.oxygenupdater.models.billing

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.SkuDetails
import com.arjanvlek.oxygenupdater.repositories.BillingRepository

/**
 * The Play [BillingClient] provides a [SkuDetails] list that the [BillingRepository] could pass
 * along to clients to tell them what the app sells. With that approach, however, clients would have
 * to figure out all correlations between SkuDetails and [entitlements][Entitlement]. For example:
 * When the [AdFreeUnlock] is purchased, the client would have to figure it out and disable the
 * [SkuDetails] button for buying it.
 *
 * Therefore, in the spirit of being client-friendly, whereas the [BillingRepository] is in a
 * better position to determine the correlations between a [SkuDetails] and its [Entitlement],
 * the API should provide an [AugmentedSkuDetails] object instead of the basic [SkuDetails].
 * This object not only passes to clients the actual [SkuDetails] object from Google, but also
 * tells clients whether a user is allowed to purchase that item at this particular moment.
 *
 * To be thorough, your implementation may be the following
 *
 * ```
 * @Entity
 * @TypeConverters(SkuDetailsTypeConverter::class)
 * class AugmentedSkuDetails(var skuDetails: SkuDetails, var canPurchase:Boolean, @PrimaryKey val sku:String)
 *
 * // and your Dao updates would look like:
 *
 * @Update
 * fun update(skuDetails: SkuDetails, sku:String)
 *
 * @Update
 * fun update(canPurchase:Boolean, sku:String)
 * ```
 * But the actual implementation below shows an alternative where you only include the fields
 * you want your clients to care about. The choice is up to you.
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
@Entity
data class AugmentedSkuDetails(
    val canPurchase: Boolean, /* Not in SkuDetails; it's the augmentation */
    @PrimaryKey val sku: String,
    val type: String?,
    val price: String?,
    val title: String?,
    val description: String?,
    val originalJson: String?
)
