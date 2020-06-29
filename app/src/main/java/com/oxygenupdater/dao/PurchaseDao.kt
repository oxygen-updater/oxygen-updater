package com.oxygenupdater.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.android.billingclient.api.Purchase
import com.oxygenupdater.models.billing.CachedPurchase

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
@Dao
interface PurchaseDao {

    @Query("SELECT * FROM purchase_table")
    fun getPurchases(): List<CachedPurchase>

    @Insert
    fun insert(purchase: CachedPurchase)

    /**
     * Inserts multiple [Purchases][Purchase], if they don't already exist in the DB.
     * This is done to avoid any potential bugs due to duplicate purchases being stored. Even if the app
     * supports consumable products in the future (i.e. the user can buy the same product multiple times),
     * no two [Purchase] objects can ever be the exact same unless they're the exact same purchase.
     */
    @Transaction
    fun insert(vararg purchases: Purchase) {
        val existingPurchases = getPurchases()

        purchases.forEach { purchase ->
            // Insert into DB only if the same purchase hasn't already been inserted
            if (existingPurchases.all { it.data != purchase }) {
                insert(CachedPurchase(purchase))
            }
        }
    }

    @Delete
    fun delete(vararg purchases: CachedPurchase)

    @Query("DELETE FROM purchase_table")
    fun deleteAll()

    @Query("DELETE FROM purchase_table WHERE data = :purchase")
    fun delete(purchase: Purchase)
}
