package com.oxygenupdater.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
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

    @Update
    fun update(purchase: CachedPurchase)

    @Delete
    fun delete(vararg purchases: CachedPurchase)

    @Query("DELETE FROM purchase_table")
    fun deleteAll()

    @Query("DELETE FROM purchase_table WHERE data = :purchase")
    fun delete(purchase: Purchase)
}
