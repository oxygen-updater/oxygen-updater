package com.arjanvlek.oxygenupdater.contribution;

import android.content.ContentValues;
import android.content.Context;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.joda.time.format.ISODateTimeFormat;


/**
 * Oxygen Updater, copyright 2019 Arjan Vlek. File created by arjan.vlek on 02/05/2019.
 */
@SuppressWarnings("WeakerAccess")
public class SubmittedUpdateFileRepository extends SQLiteOpenHelper {

	// If you change the database schema, you must increment the database version.
	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "SubmittedUpdateFiles.db";
	// Table name
	private static final String TABLE_NAME = "news_item";
	// Table columns (id, name, date_submitted)
	private static final String COLUMN_ID = "id";
	private static final String COLUMN_NAME = "name";
	private static final String COLUMN_DATE_SUBMITTED = "date_submitted";
	// Create database
	private static final String SQL_CREATE_TABLE =
			"CREATE TABLE " + TABLE_NAME + " (" +
					COLUMN_ID + " INTEGER PRIMARY KEY," +
					COLUMN_NAME + " TEXT," +
					COLUMN_DATE_SUBMITTED + " TEXT" + // ISO8601 date
					")";
	private SQLiteDatabase readableSqLiteDatabase;
	private SQLiteDatabase writableSqLiteDatabase;


	public SubmittedUpdateFileRepository(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public SQLiteDatabase getWritableDatabase() {
		if (this.writableSqLiteDatabase == null) {
			this.writableSqLiteDatabase = super.getWritableDatabase();
		}

		return this.writableSqLiteDatabase;
	}

	@Override
	public SQLiteDatabase getReadableDatabase() {
		if (this.readableSqLiteDatabase == null) {
			this.readableSqLiteDatabase = super.getReadableDatabase();
		}

		return this.readableSqLiteDatabase;
	}

	public void onCreate(SQLiteDatabase db) {
		// create DATABASE
		db.execSQL(SQL_CREATE_TABLE);
	}

	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Add code if the database needs to be upgraded.
	}

	public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Add code if the database needs to be downgraded.
	}

	public boolean isFileAlreadySubmitted(String filename) {
		if (filename == null || filename.isEmpty()) {
			return false;
		}

		return DatabaseUtils.queryNumEntries(
				getReadableDatabase(),
				TABLE_NAME,
				COLUMN_NAME + "= ?",
				new String[]{filename}
		) > 0;
	}

	public void store(String filename) {
		if (filename != null && !filename.isEmpty()) {
			ContentValues values = new ContentValues();
			values.put(COLUMN_NAME, filename);
			values.put(COLUMN_DATE_SUBMITTED, LocalDateTime.now(DateTimeZone.forID("Europe/Amsterdam"))
					.toString(ISODateTimeFormat.basicDateTime()));

			getWritableDatabase().insert(TABLE_NAME, null, values);
		}
	}
}
