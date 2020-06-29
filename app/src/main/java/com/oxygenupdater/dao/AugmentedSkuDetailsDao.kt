package com.oxygenupdater.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.SkuDetails
import com.oxygenupdater.models.billing.AugmentedSkuDetails

/**
 * The rest of the app needs a list of the [SkuDetails] so to show users what to buy
 * and for how much. [LiveData] should be used so the appropriate UIs get the most up-to-date
 * data. Notice that two sets is being created: one for subscriptions and one for managed products.
 * That's because in this sample subscriptions and in-app products are listed separately. However,
 * some use cases may have more than two sets; for instance, if each Fragment/Activity must list
 * different set of SKUs.
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
@Dao
interface AugmentedSkuDetailsDao {

    @Query("SELECT * FROM AugmentedSkuDetails WHERE type = '${BillingClient.SkuType.SUBS}'")
    fun getSubscriptionSkuDetails(): LiveData<List<AugmentedSkuDetails>>

    @Query("SELECT * FROM AugmentedSkuDetails WHERE type = '${BillingClient.SkuType.INAPP}'")
    fun getInappSkuDetails(): LiveData<List<AugmentedSkuDetails>>

    @Query("SELECT * FROM AugmentedSkuDetails")
    fun getAllSkuDetails(): List<AugmentedSkuDetails>

    /**
     * Inserts or updates all PBL-provided [SkuDetails] into the table, and removes stale rows
     *
     * This ensures the app's local database cache of SkuDetails remains up-to-date.
     * We should obviously not show users SKUs that aren't available to purchase.
     */
    @Transaction
    fun refreshSkuDetails(skuDetailsList: List<SkuDetails>) {
        val newAugmentedSkuDetailsList = skuDetailsList.map {
            insertOrUpdate(it)
        }

        // Remove stale AugmentedSkuDetails from the table
        delete(
            getAllSkuDetails().subtract(newAugmentedSkuDetailsList)
        )
    }

    @Transaction
    fun insertOrUpdate(skuDetails: SkuDetails) = skuDetails.run {
        val result = getById(sku)
        AugmentedSkuDetails(
            result?.canPurchase ?: true,
            sku,
            type,
            price,
            title,
            description,
            originalJson
        ).also { insert(it) }
    }

    @Transaction
    fun insertOrUpdate(sku: String, canPurchase: Boolean) {
        val result = getById(sku)
        if (result != null) {
            update(sku, canPurchase)
        } else {
            insert(
                AugmentedSkuDetails(
                    canPurchase,
                    sku,
                    null,
                    null,
                    null,
                    null,
                    null
                )
            )
        }
    }

    @Query("SELECT * FROM AugmentedSkuDetails WHERE sku = :sku")
    fun getById(sku: String): AugmentedSkuDetails?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(augmentedSkuDetails: AugmentedSkuDetails)

    @Query("UPDATE AugmentedSkuDetails SET canPurchase = :canPurchase WHERE sku = :sku")
    fun update(sku: String, canPurchase: Boolean)

    @Delete
    fun delete(augmentedSkuDetails: Set<AugmentedSkuDetails>)
}
