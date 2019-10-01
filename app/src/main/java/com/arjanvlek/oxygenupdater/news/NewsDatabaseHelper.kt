package com.arjanvlek.oxygenupdater.news

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java8.util.stream.Collectors
import java8.util.stream.StreamSupport
import java.util.*

class NewsDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    private var readableSqLiteDatabase: SQLiteDatabase? = null
    private var writableSqLiteDatabase: SQLiteDatabase? = null

    val allNewsItems: List<NewsItem>
        get() {

            val items = ArrayList<NewsItem>()

            val sortOrder = "$COLUMN_ID DESC"
            val cursor = readableDatabase!!.query(TABLE_NAME, null, null, null, null, null, sortOrder)

            while (cursor.moveToNext()) {
                items.add(fromDatabaseCursor(cursor))
            }

            cursor.close()

            return items
        }

    override fun getWritableDatabase(): SQLiteDatabase? {
        if (writableSqLiteDatabase == null) {
            writableSqLiteDatabase = super.getWritableDatabase()
        }

        return writableSqLiteDatabase
    }

    override fun getReadableDatabase(): SQLiteDatabase? {
        if (readableSqLiteDatabase == null) {
            readableSqLiteDatabase = super.getReadableDatabase()
        }

        return readableSqLiteDatabase
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_ENTRIES)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Add code if the database needs to be upgraded.
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Add code if the database needs to be downgraded.
    }

    fun getNewsItem(id: Long?): NewsItem? {
        if (id == null) {
            return null
        }
        val selection = "$COLUMN_ID = ?"
        val selectionArgs = arrayOf(id.toString())
        val sortOrder = "$COLUMN_ID ASC"

        val cursor = readableDatabase!!.query(
                TABLE_NAME, null, // Return all columns
                selection, // The columns for the WHERE clause
                selectionArgs, null, null, // don't filter by row groups
                sortOrder           // The sort order
        ) // The table to query
        // The values for the WHERE clause
        // don't group the rows

        var item: NewsItem? = null

        if (cursor.moveToFirst()) {
            item = fromDatabaseCursor(cursor)
        }

        cursor.close()

        return item
    }

    fun saveNewsItems(newsItems: List<NewsItem>?) {
        if (!newsItems.isNullOrEmpty()) {
            val newsItemsIds = StreamSupport
                    .stream(newsItems)
                    .map<Long> { it.id }
                    .collect(Collectors.toList())

            // All IDs that are in the database but are not in the result should be deleted from the database.
            StreamSupport.stream(allNewsItems)
                    .map<Long> { it.id }
                    .filter { id -> !newsItemsIds.contains(id) }
                    .forEach { this.deleteNewsItem(it) }

            for (item in newsItems) {
                saveNewsItem(item)
            }
        }
    }

    fun saveNewsItem(newsItem: NewsItem) {
        // First, check if the item already exists in our database.
        val existing = getNewsItem(newsItem.id)

        // If the item exists, update it. Otherwise, create it.
        if (existing != null) {
            updateNewsItem(existing.id, newsItem)
        } else {
            insertNewsItem(newsItem)
        }
    }

    fun markNewsItemAsRead(newsItem: NewsItem) {
        val values = toDatabaseContents(newsItem)
        values.put(COLUMN_READ, true)
        writableDatabase?.update(TABLE_NAME, values, "$COLUMN_ID  LIKE ?", arrayOf(newsItem.id.toString()))
    }

    private fun insertNewsItem(newsItem: NewsItem) {
        val values = toDatabaseContents(newsItem)
        values.put(COLUMN_READ, false)

        writableDatabase?.insert(TABLE_NAME, null, values)
    }

    private fun updateNewsItem(id: Long?, newsItem: NewsItem) {
        writableDatabase?.update(TABLE_NAME, toDatabaseContents(newsItem), "$COLUMN_ID LIKE ?", arrayOf(id.toString()))
    }

    private fun deleteNewsItem(id: Long?) {
        writableDatabase?.delete(TABLE_NAME, "$COLUMN_ID LIKE ?", arrayOf(id.toString()))
    }

    private fun fromDatabaseCursor(cursor: Cursor): NewsItem {
        val newsItem = NewsItem()

        newsItem.id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID))
        newsItem.dutchTitle = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DUTCH_TITLE))
        newsItem.englishTitle = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ENGLISH_TITLE))
        newsItem.dutchSubtitle = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DUTCH_SUBTITLE))
        newsItem.englishSubtitle = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ENGLISH_SUBTITLE))
        newsItem.imageUrl = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_URL))
        newsItem.dutchText = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DUTCH_TEXT))
        newsItem.englishText = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ENGLISH_TEXT))
        newsItem.datePublished = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE_PUBLISHED))
        newsItem.dateLastEdited = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE_LAST_EDITED))
        newsItem.authorName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_AUTHOR_NAME))
        newsItem.isRead = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_READ)) == 1

        return newsItem
    }

    private fun toDatabaseContents(newsItem: NewsItem?): ContentValues {
        if (newsItem == null) {
            return ContentValues()
        }

        val values = ContentValues()

        values.put(COLUMN_ID, newsItem.id)
        values.put(COLUMN_DUTCH_TITLE, newsItem.dutchTitle)
        values.put(COLUMN_ENGLISH_TITLE, newsItem.englishTitle)
        values.put(COLUMN_DUTCH_SUBTITLE, newsItem.dutchSubtitle)
        values.put(COLUMN_ENGLISH_SUBTITLE, newsItem.englishSubtitle)
        values.put(COLUMN_IMAGE_URL, newsItem.imageUrl)
        values.put(COLUMN_DUTCH_TEXT, newsItem.dutchText)
        values.put(COLUMN_ENGLISH_TEXT, newsItem.englishText)
        values.put(COLUMN_DATE_PUBLISHED, newsItem.datePublished)
        values.put(COLUMN_DATE_LAST_EDITED, newsItem.dateLastEdited)
        values.put(COLUMN_AUTHOR_NAME, newsItem.authorName)
        // COLUMN_READ is NOT edited here, because it can only be triggered once when the item is marked as read.
        // Otherwise, this value would be overwritten.
        return values
    }

    companion object {
        // If you change the database schema, you must increment the database version.
        private val DATABASE_VERSION = 1
        private val DATABASE_NAME = "NewsItems.db"
        private val DATE_FORMAT = "yyyy-MM-dd HH:mm::ss"
        // Table name
        private val TABLE_NAME = "news_item"
        // Table columns (id, dutch_title, english_title, dutch_subtitle, english_subtitle, image_url, dutch_text, english_text, date_published, author_name, read)
        private val COLUMN_ID = "id"
        private val COLUMN_DUTCH_TITLE = "dutch_title"
        private val COLUMN_ENGLISH_TITLE = "english_title"
        private val COLUMN_DUTCH_SUBTITLE = "dutch_subtitle"
        private val COLUMN_ENGLISH_SUBTITLE = "english_subtitle"
        private val COLUMN_IMAGE_URL = "image_url"
        private val COLUMN_DUTCH_TEXT = "dutch_text"
        private val COLUMN_ENGLISH_TEXT = "english_text"
        private val COLUMN_DATE_PUBLISHED = "date_published"
        private val COLUMN_DATE_LAST_EDITED = "date_last_edited"
        private val COLUMN_AUTHOR_NAME = "author_name"
        private val COLUMN_READ = "read"
        // Create database
        private val SQL_CREATE_ENTRIES = "CREATE TABLE " + TABLE_NAME + " (" +
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
                COLUMN_READ + " INTEGER" + // 0 = false, 1 = true

                ")"
    }
}
