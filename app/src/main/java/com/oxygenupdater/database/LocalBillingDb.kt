package com.oxygenupdater.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.oxygenupdater.dao.AugmentedSkuDetailsDao
import com.oxygenupdater.dao.EntitlementsDao
import com.oxygenupdater.dao.PurchaseDao
import com.oxygenupdater.models.billing.AdFreeUnlock
import com.oxygenupdater.models.billing.AugmentedSkuDetails
import com.oxygenupdater.models.billing.CachedPurchase
import com.oxygenupdater.models.billing.PurchaseTypeConverter

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
@Database(
    entities = [
        AugmentedSkuDetails::class,
        CachedPurchase::class,
        AdFreeUnlock::class
    ],
    version = 1
)
@TypeConverters(PurchaseTypeConverter::class)
abstract class LocalBillingDb : RoomDatabase() {
    abstract fun purchaseDao(): PurchaseDao
    abstract fun entitlementsDao(): EntitlementsDao
    abstract fun skuDetailsDao(): AugmentedSkuDetailsDao
}
