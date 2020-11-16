package com.oxygenupdater.utils

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.oxygenupdater.database.LocalAppDb
import com.oxygenupdater.database.LocalBillingDb
import com.oxygenupdater.utils.DatabaseMigrations.prepopulateFromSqlite

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
object DatabaseBuilders {

    /**
     * While overriding [RoomDatabase.Callback.onCreate] would be ideal, attaching
     * other databases doesn't work (due to some WAL/transaction restrictions).
     */
    private val prepopulateFromOldData = object : RoomDatabase.Callback() {
        override fun onOpen(
            db: SupportSQLiteDatabase
        ) = super.onOpen(db).also {
            prepopulateFromSqlite(db)
        }
    }

    const val APP_DB = "oxygen_updater"
    const val PURCHASES_DB = "purchase_db"
    const val NEWS_ITEMS_OLD_DB = "NewsItems.db"
    const val SUBMITTED_UPDATE_FILES_OLD_DB = "SubmittedUpdateFiles.db"

    fun buildLocalAppDatabase(
        context: Context
    ) = Room.databaseBuilder(
        context,
        LocalAppDb::class.java,
        APP_DB
    ).addCallback(prepopulateFromOldData)
        .fallbackToDestructiveMigrationOnDowngrade()
        .build()

    fun buildLocalBillingDatabase(
        context: Context
    ) = Room.databaseBuilder(
        context,
        LocalBillingDb::class.java,
        PURCHASES_DB
    ).fallbackToDestructiveMigration() // Data is cached, so it is OK to delete
        .build()

}
