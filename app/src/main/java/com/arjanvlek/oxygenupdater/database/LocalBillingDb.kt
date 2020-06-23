package com.arjanvlek.oxygenupdater.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.arjanvlek.oxygenupdater.dao.AugmentedSkuDetailsDao
import com.arjanvlek.oxygenupdater.dao.EntitlementsDao
import com.arjanvlek.oxygenupdater.dao.PurchaseDao
import com.arjanvlek.oxygenupdater.models.billing.AdFreeUnlock
import com.arjanvlek.oxygenupdater.models.billing.AugmentedSkuDetails
import com.arjanvlek.oxygenupdater.models.billing.CachedPurchase
import com.arjanvlek.oxygenupdater.models.billing.PurchaseTypeConverter

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
