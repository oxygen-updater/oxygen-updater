package com.oxygenupdater.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.oxygenupdater.models.billing.AdFreeUnlock
import com.oxygenupdater.models.billing.Entitlement

/**
 * No update methods necessary since for each table there is ever expecting one row, hence why
 * the primary key is hardcoded.
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
@Dao
interface EntitlementsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(adFreeUnlock: AdFreeUnlock)

    @Update
    fun update(adFreeUnlock: AdFreeUnlock)

    @Query("SELECT * FROM ad_free_unlock LIMIT 1")
    fun getAdFreeUnlock(): LiveData<AdFreeUnlock?>

    @Delete
    fun delete(premium: AdFreeUnlock)

    @Query("DELETE FROM ad_free_unlock")
    fun clearAdFreeUnlockTable()

    /**
     * This is purely for convenience. The clients of this DAO don't have to discriminate among
     * different entitlements, and can simply pass in a list of [Entitlement]s.
     *
     * Currently, [AdFreeUnlock] is the only entitlement but others may be added in the future
     */
    @Transaction
    fun insert(vararg entitlements: Entitlement) {
        entitlements.forEach {
            when (it) {
                is AdFreeUnlock -> insert(it)
            }
        }
    }

    @Transaction
    fun update(vararg entitlements: Entitlement) {
        entitlements.forEach {
            when (it) {
                is AdFreeUnlock -> update(it)
            }
        }
    }
}
