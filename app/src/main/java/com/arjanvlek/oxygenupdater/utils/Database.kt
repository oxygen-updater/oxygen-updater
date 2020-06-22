package com.arjanvlek.oxygenupdater.utils

import android.content.Context
import androidx.room.Room
import com.arjanvlek.oxygenupdater.database.LocalBillingDb

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
object Database {

    private const val PURCHASES_DB = "purchase_db"

    fun buildLocalBillingDatabase(
        context: Context
    ) = Room.databaseBuilder(
        context,
        LocalBillingDb::class.java,
        PURCHASES_DB
    )
        // Data is cached, so it is OK to delete
        .fallbackToDestructiveMigration()
        .build()

}
