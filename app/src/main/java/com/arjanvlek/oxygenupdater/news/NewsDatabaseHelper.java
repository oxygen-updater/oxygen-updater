package com.arjanvlek.oxygenupdater.news;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.joda.time.LocalDateTime;

import java.util.ArrayList;
import java.util.List;

import java8.util.stream.Collectors;
import java8.util.stream.StreamSupport;

public class NewsDatabaseHelper extends SQLiteOpenHelper {

    private SQLiteDatabase readableSqLiteDatabase;
    private SQLiteDatabase writableSqLiteDatabase;

    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "NewsItems.db";

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm::ss";

    // Table name
    private static final String TABLE_NAME = "news_item";

    // Table columns (id, dutch_title, english_title, dutch_subtitle, english_subtitle, image_url, dutch_text, english_text, date_published, author_name, read)
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_DUTCH_TITLE = "dutch_title";
    private static final String COLUMN_ENGLISH_TITLE = "english_title";
    private static final String COLUMN_DUTCH_SUBTITLE = "dutch_subtitle";
    private static final String COLUMN_ENGLISH_SUBTITLE = "english_subtitle";
    private static final String COLUMN_IMAGE_URL = "image_url";
    private static final String COLUMN_DUTCH_TEXT = "dutch_text";
    private static final String COLUMN_ENGLISH_TEXT = "english_text";
    private static final String COLUMN_DATE_PUBLISHED = "date_published";
    private static final String COLUMN_DATE_LAST_EDITED = "date_last_edited";
    private static final String COLUMN_AUTHOR_NAME = "author_name";
    private static final String COLUMN_READ = "read";

    // Create database
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + TABLE_NAME + " (" +
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
                    ")";

    public NewsDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Add code if the database needs to be upgraded.
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Add code if the database needs to be downgraded.
    }

    @Override
    public SQLiteDatabase getReadableDatabase() {
        if(this.readableSqLiteDatabase == null) {
            this.readableSqLiteDatabase = super.getReadableDatabase();
        }

        return this.readableSqLiteDatabase;
    }

    @Override
    public SQLiteDatabase getWritableDatabase() {
        if(this.writableSqLiteDatabase == null) {
            this.writableSqLiteDatabase = super.getWritableDatabase();
        }

        return this.writableSqLiteDatabase;
    }

    public List<NewsItem> getAllNewsItems() {

        List<NewsItem> items = new ArrayList<>();

        String sortOrder = COLUMN_ID + " DESC";
        Cursor cursor = getReadableDatabase().query(TABLE_NAME, null, null, null, null, null, sortOrder);

        while(cursor.moveToNext()) {
            items.add(fromDatabaseCursor(cursor));
        }

        cursor.close();

        return items;
    }

    public NewsItem getNewsItem(Long id) {
        String selection = COLUMN_ID + " = ?";
        String[] selectionArgs = { String.valueOf(id) };
        String sortOrder = COLUMN_ID + " ASC";

        Cursor cursor = getReadableDatabase().query(
                TABLE_NAME,                                 // The table to query
                null,                                       // Return all columns
                selection,                                // The columns for the WHERE clause
                selectionArgs,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                sortOrder                                 // The sort order
        );

        NewsItem item = null;

        if(cursor.moveToFirst()) {
            item = fromDatabaseCursor(cursor);
        }

        cursor.close();

        return item;
    }

    public void saveNewsItems(List<NewsItem> newsItems) {
        if(newsItems != null && !newsItems.isEmpty()) {
            List<Long> newsItemsIds = StreamSupport
                    .stream(newsItems)
                    .map(NewsItem::getId)
                    .collect(Collectors.toList());

            // All IDs that are in the database but are not in the result should be deleted from the database.
            StreamSupport.stream(getAllNewsItems())
                    .map(NewsItem::getId)
                    .filter(id -> !newsItemsIds.contains(id))
                    .forEach(this::deleteNewsItem);

            for(NewsItem item : newsItems) {
                saveNewsItem(item);
            }
        }
    }

    public void saveNewsItem(NewsItem newsItem) {
        // First, check if the item already exists in our database.
        NewsItem existing = getNewsItem(newsItem.getId());

        // If the item exists, update it. Otherwise, create it.
        if(existing != null) {
            updateNewsItem(existing.getId(), newsItem);
        } else {
            insertNewsItem(newsItem);
        }
    }

    public void markNewsItemAsRead(NewsItem newsItem) {
        ContentValues values = toDatabaseContents(newsItem);
        values.put(COLUMN_READ, true);
        getWritableDatabase().update(TABLE_NAME, values, COLUMN_ID + "  LIKE ?", new String[] {String.valueOf(newsItem.getId())});
    }

    private void insertNewsItem(NewsItem newsItem) {
        ContentValues values = toDatabaseContents(newsItem);
        values.put(COLUMN_READ, false);

        getWritableDatabase().insert(TABLE_NAME, null, values);
    }

    private void updateNewsItem(Long id, NewsItem newsItem) {
        getWritableDatabase().update(TABLE_NAME, toDatabaseContents(newsItem), COLUMN_ID + " LIKE ?" , new String[] { String.valueOf(id) });
    }

    private void deleteNewsItem(Long id) {
        getWritableDatabase().delete(TABLE_NAME, COLUMN_ID + " LIKE ?", new String[] {String.valueOf(id)});
    }

    private NewsItem fromDatabaseCursor(Cursor cursor) {
        NewsItem newsItem = new NewsItem();

        newsItem.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)));
        newsItem.setDutchTitle(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DUTCH_TITLE)));
        newsItem.setEnglishTitle(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ENGLISH_TITLE)));
        newsItem.setDutchSubtitle(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DUTCH_SUBTITLE)));
        newsItem.setEnglishSubtitle(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ENGLISH_SUBTITLE)));
        newsItem.setImageUrl(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_URL)));
        newsItem.setDutchText(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DUTCH_TEXT)));
        newsItem.setEnglishText(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ENGLISH_TEXT)));
        newsItem.setDatePublished(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE_PUBLISHED)));
        newsItem.setDateLastEdited(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE_LAST_EDITED)));
        newsItem.setAuthorName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_AUTHOR_NAME)));
        newsItem.setRead(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_READ)) == 1);

        return newsItem;
    }

    private ContentValues toDatabaseContents(NewsItem newsItem) {
        ContentValues values = new ContentValues();

        values.put(COLUMN_ID, newsItem.getId());
        values.put(COLUMN_DUTCH_TITLE, newsItem.getDutchTitle());
        values.put(COLUMN_ENGLISH_TITLE, newsItem.getEnglishTitle());
        values.put(COLUMN_DUTCH_SUBTITLE, newsItem.getDutchSubtitle());
        values.put(COLUMN_ENGLISH_SUBTITLE, newsItem.getEnglishSubtitle());
        values.put(COLUMN_IMAGE_URL, newsItem.getImageUrl());
        values.put(COLUMN_DUTCH_TEXT, newsItem.getDutchText());
        values.put(COLUMN_ENGLISH_TEXT, newsItem.getEnglishText());
        values.put(COLUMN_DATE_PUBLISHED, newsItem.getDatePublished());
        values.put(COLUMN_DATE_LAST_EDITED, newsItem.getDateLastEdited());
        values.put(COLUMN_AUTHOR_NAME, newsItem.getAuthorName());
        // COLUMN_READ is NOT edited here, because it can only be triggered once when the item is marked as read.
        // Otherwise, this value would be overwritten.
        return values;
    }
}
