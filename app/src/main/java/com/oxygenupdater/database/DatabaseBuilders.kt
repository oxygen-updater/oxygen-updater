package com.oxygenupdater.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
object DatabaseBuilders {

    /**
     * While overriding [RoomDatabase.Callback.onCreate] would be ideal, attaching
     * other databases doesn't work (due to some WAL/transaction restrictions).
     */
    private val prepopulateFromOldData = object : RoomDatabase.Callback() {
        override fun onOpen(db: SupportSQLiteDatabase) = super.onOpen(db).also {
            SqliteMigrations.prepopulateFromSqlite(db)
        }
    }

    const val APP_DB = "oxygen_updater"

    fun buildLocalAppDatabase(context: Context) = Room.databaseBuilder(
        context,
        LocalAppDb::class.java,
        APP_DB
    ).addCallback(prepopulateFromOldData)
        .fallbackToDestructiveMigrationOnDowngrade()
        .build()

}
