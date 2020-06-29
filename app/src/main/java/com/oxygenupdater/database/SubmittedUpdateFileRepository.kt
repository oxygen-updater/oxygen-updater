package com.oxygenupdater.database

import android.content.Context
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.content.contentValuesOf
import com.oxygenupdater.utils.Utils.SERVER_TIME_ZONE
import org.threeten.bp.LocalDateTime

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 * @author [Arjan Vlek](https://github.com/arjanvlek)
 */
class SubmittedUpdateFileRepository(context: Context?) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    private var readableSqLiteDatabase: SQLiteDatabase? = null
    private var writableSqLiteDatabase: SQLiteDatabase? = null

    override fun getWritableDatabase(): SQLiteDatabase {
        if (writableSqLiteDatabase == null) {
            writableSqLiteDatabase = super.getWritableDatabase()
        }

        return writableSqLiteDatabase!!
    }

    override fun getReadableDatabase(): SQLiteDatabase {
        if (readableSqLiteDatabase == null) {
            readableSqLiteDatabase = super.getReadableDatabase()
        }

        return readableSqLiteDatabase!!
    }

    override fun onCreate(db: SQLiteDatabase) = db.execSQL(SQL_CREATE_TABLE)

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Add code if the database needs to be upgraded.
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Add code if the database needs to be downgraded.
    }

    fun isFileAlreadySubmitted(filename: String?) = if (filename.isNullOrEmpty()) {
        false
    } else {
        DatabaseUtils.queryNumEntries(
            readableDatabase,
            TABLE_NAME, "$COLUMN_NAME= ?", arrayOf(filename)
        ) > 0
    }

    fun store(filename: String?) {
        if (!filename.isNullOrEmpty()) {
            writableDatabase.insert(
                TABLE_NAME, null, contentValuesOf(
                    COLUMN_NAME to filename,
                    COLUMN_DATE_SUBMITTED to LocalDateTime.now(SERVER_TIME_ZONE).toString()
                )
            )
        }
    }

    companion object {
        // If you change the database schema, you must increment the database version.
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "SubmittedUpdateFiles.db"

        // Table name
        private const val TABLE_NAME = "news_item"

        // Table columns (id, name, date_submitted)
        private const val COLUMN_ID = "id"
        private const val COLUMN_NAME = "name"
        private const val COLUMN_DATE_SUBMITTED = "date_submitted"

        // Create database
        private const val SQL_CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY," +
                COLUMN_NAME + " TEXT," +
                COLUMN_DATE_SUBMITTED + " TEXT" +  // ISO8601 date
                ")"
    }
}
