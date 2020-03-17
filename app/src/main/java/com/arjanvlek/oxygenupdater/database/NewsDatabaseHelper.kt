package com.arjanvlek.oxygenupdater.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.content.contentValuesOf
import com.arjanvlek.oxygenupdater.models.NewsItem
import java.util.*

class NewsDatabaseHelper(context: Context?) : SQLiteOpenHelper(
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

    override fun onCreate(db: SQLiteDatabase) = db.execSQL(SQL_CREATE_ENTRIES)

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Add code if the database needs to be upgraded.
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Add code if the database needs to be downgraded.
    }

    val allNewsItems: List<NewsItem>
        get() {
            val items: MutableList<NewsItem> = ArrayList()
            val sortOrder = "$COLUMN_ID DESC"
            val cursor = readableDatabase.query(TABLE_NAME, null, null, null, null, null, sortOrder)

            while (cursor.moveToNext()) {
                items.add(fromDatabaseCursor(cursor))
            }

            cursor.close()

            return items
        }

    fun getNewsItem(id: Long?) = if (id == null) {
        null
    } else {
        readableDatabase.query(
            TABLE_NAME,  // The table to query
            null,  // Return all columns
            "$COLUMN_ID = ?",  // The columns for the WHERE clause
            arrayOf(id.toString()),  // The values for the WHERE clause
            null,  // don't group the rows
            null,  // don't filter by row groups
            "$COLUMN_ID ASC" // The sort order
        ).let {
            val item = if (it.moveToFirst()) fromDatabaseCursor(it) else null
            it.close()
            item
        }
    }

    fun saveNewsItems(newsItems: List<NewsItem>?) {
        if (!newsItems.isNullOrEmpty()) {
            val newsItemsIds = newsItems.map { it.id }

            // All IDs that are in the database but are not in the result should be deleted from the database.
            allNewsItems.map { it.id }
                .filter { it != null && !newsItemsIds.contains(it) }
                .forEach { deleteNewsItem(it!!) }

            newsItems.forEach {
                saveNewsItem(it)
            }
        }
    }

    /**
     * If the item exists, update it. Otherwise, create it.
     */
    fun saveNewsItem(newsItem: NewsItem) = getNewsItem(newsItem.id)?.let { existing ->
        updateNewsItem(existing.id!!, newsItem)
    } ?: insertNewsItem(newsItem)

    fun markNewsItemRead(newsItem: NewsItem) = toDatabaseContents(newsItem).let { values ->
        values.put(COLUMN_READ, true)
        writableDatabase.update(TABLE_NAME, values, "$COLUMN_ID  LIKE ?", arrayOf(newsItem.id.toString()))
    }

    private fun insertNewsItem(newsItem: NewsItem) = toDatabaseContents(newsItem).let { values ->
        values.put(COLUMN_READ, false)
        writableDatabase.insert(TABLE_NAME, null, values)
    }

    private fun updateNewsItem(id: Long, newsItem: NewsItem) = writableDatabase.update(
        TABLE_NAME,
        toDatabaseContents(newsItem),
        "$COLUMN_ID LIKE ?",
        arrayOf(id.toString())
    )


    private fun deleteNewsItem(id: Long) = writableDatabase.delete(
        TABLE_NAME,
        "$COLUMN_ID LIKE ?",
        arrayOf(id.toString())
    )

    private fun fromDatabaseCursor(cursor: Cursor) = NewsItem(
        cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DUTCH_TITLE)),
        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ENGLISH_TITLE)),
        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DUTCH_SUBTITLE)),
        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ENGLISH_SUBTITLE)),
        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_URL)),
        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DUTCH_TEXT)),
        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ENGLISH_TEXT)),
        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE_PUBLISHED)),
        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE_LAST_EDITED)),
        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_AUTHOR_NAME)),
        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_READ)) == 1
    )

    private fun toDatabaseContents(newsItem: NewsItem?) = if (newsItem == null) {
        ContentValues()
    } else {
        contentValuesOf(
            COLUMN_ID to newsItem.id,
            COLUMN_DUTCH_TITLE to newsItem.dutchTitle,
            COLUMN_ENGLISH_TITLE to newsItem.englishTitle,
            COLUMN_DUTCH_SUBTITLE to newsItem.dutchSubtitle,
            COLUMN_ENGLISH_SUBTITLE to newsItem.englishSubtitle,
            COLUMN_IMAGE_URL to newsItem.imageUrl,
            COLUMN_DUTCH_TEXT to newsItem.dutchText,
            COLUMN_ENGLISH_TEXT to newsItem.englishText,
            COLUMN_DATE_PUBLISHED to newsItem.datePublished,
            COLUMN_DATE_LAST_EDITED to newsItem.dateLastEdited,
            COLUMN_AUTHOR_NAME to newsItem.authorName
            // COLUMN_READ is NOT edited here, because it can only be triggered once when the item is marked as read.
            // Otherwise, this value would be overwritten.
        )
    }

    companion object {
        // If you change the database schema, you must increment the database version.
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "NewsItems.db"

        // Table name
        private const val TABLE_NAME = "news_item"

        // Table columns (id, dutch_title, english_title, dutch_subtitle, english_subtitle, image_url, dutch_text, english_text, date_published, author_name, read)
        private const val COLUMN_ID = "id"
        private const val COLUMN_DUTCH_TITLE = "dutch_title"
        private const val COLUMN_ENGLISH_TITLE = "english_title"
        private const val COLUMN_DUTCH_SUBTITLE = "dutch_subtitle"
        private const val COLUMN_ENGLISH_SUBTITLE = "english_subtitle"
        private const val COLUMN_IMAGE_URL = "image_url"
        private const val COLUMN_DUTCH_TEXT = "dutch_text"
        private const val COLUMN_ENGLISH_TEXT = "english_text"
        private const val COLUMN_DATE_PUBLISHED = "date_published"
        private const val COLUMN_DATE_LAST_EDITED = "date_last_edited"
        private const val COLUMN_AUTHOR_NAME = "author_name"
        private const val COLUMN_READ = "read"

        // Create database
        private const val SQL_CREATE_ENTRIES = "CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY," +
                COLUMN_DUTCH_TITLE + " TEXT," +
                COLUMN_ENGLISH_TITLE + " TEXT," +
                COLUMN_DUTCH_SUBTITLE + " TEXT," +
                COLUMN_ENGLISH_SUBTITLE + " TEXT," +
                COLUMN_IMAGE_URL + " TEXT," +
                COLUMN_DUTCH_TEXT + " TEXT," +
                COLUMN_DATE_PUBLISHED + " TEXT," +
                COLUMN_DATE_LAST_EDITED + " TEXT," +
                COLUMN_ENGLISH_TEXT + " TEXT," +
                COLUMN_AUTHOR_NAME + " TEXT," +
                COLUMN_READ + " INTEGER" +  // 0 = false, 1 = true
                ")"
    }
}
