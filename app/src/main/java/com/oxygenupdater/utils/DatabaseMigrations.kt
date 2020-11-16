package com.oxygenupdater.utils

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import com.oxygenupdater.internal.settings.SettingsManager
import com.oxygenupdater.internal.settings.SettingsManager.PROPERTY_SQL_TO_ROOM_MIGRATION_DONE
import com.oxygenupdater.utils.DatabaseBuilders.APP_DB
import com.oxygenupdater.utils.DatabaseBuilders.NEWS_ITEMS_OLD_DB
import com.oxygenupdater.utils.DatabaseBuilders.SUBMITTED_UPDATE_FILES_OLD_DB
import com.oxygenupdater.utils.Logger.logDebug
import com.oxygenupdater.utils.Logger.logWarning
import org.koin.java.KoinJavaComponent.inject

/**
 * Migrations for all app databases/tables.
 *
 * **Note: No matter what, don't ever edit existing migrations after a release.
 * Create new migrations instead, when schema has been changed.**
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
object DatabaseMigrations {

    private const val TAG = "DatabaseMigrations"
    private const val NEWS_ITEM_TABLE = "news_item"
    private const val SUBMITTED_UPDATE_FILE_TABLE = "submitted_update_file"

    /**
     * Migration that pre-populates Room DB with old SQLite data.
     *
     * The old SQLite implementation had different databases for news items
     * (`NewsItems.db`) and submitted update files (`SubmittedUpdateFiles.db`),
     * which wasn't ideal and had a lot of boilerplate code. The new Room
     * implementation has a common database and child tables, which match the
     * backend structure.
     *
     * Additional problems with the previous implementation:
     * - `SubmittedUpdateFiles.db`
     *    - Table name was `news_item`, likely due to a copy-paste error
     *    - `id`, despite being a primary key, was marked as nullable
     *    - `date_submitted` was marked as nullable, and default value was `null`
     *
     * This migration:
     * 1. Copies all existing data over (if old databases exist)
     * 2. Deletes old databases since they won't be used after this migration completes
     */
    fun prepopulateFromSqlite(db: SupportSQLiteDatabase) = db.run {
        val migrationDone = SettingsManager.getPreference(
            PROPERTY_SQL_TO_ROOM_MIGRATION_DONE,
            false
        )

        // Make sure migration is run only once. This is necessary because this
        // function is called from the `onOpen` callback instead of `onCreate`.
        if (!migrationDone) {
            logDebug(TAG, "Starting migrations for SQLite to Room")
            val context by inject(Context::class.java)

            migrateFromOldDb(
                context,
                SUBMITTED_UPDATE_FILES_OLD_DB,
                NEWS_ITEM_TABLE,
                SUBMITTED_UPDATE_FILE_TABLE,
                "`id`, `name`, `date_submitted`"
            )

            migrateFromOldDb(
                context,
                NEWS_ITEMS_OLD_DB,
                NEWS_ITEM_TABLE,
                NEWS_ITEM_TABLE,
                "`id`, `dutch_title`, `english_title`, `dutch_subtitle`, `english_subtitle`, `image_url`, `dutch_text`, `english_text`, `date_published`, `date_last_edited`, `author_name`, `read`"
            )

            SettingsManager.savePreference(
                PROPERTY_SQL_TO_ROOM_MIGRATION_DONE,
                true
            )
        } else {
            logDebug(TAG, "SQLite to Room migration has already been done")
        }
    }

    private fun SupportSQLiteDatabase.migrateFromOldDb(
        context: Context,
        oldDbName: String,
        oldTableName: String,
        newTableName: String,
        columnSqlStr: String
    ) = context.getDatabasePath(oldDbName).run {
        val migrationLogStr = "`$oldDbName`.`$oldTableName` to `$APP_DB`.`$newTableName`"

        if (exists()) {
            logDebug(TAG, "Migrating $migrationLogStr")

            val oldDbPath = toString()
            try {
                execSQL("ATTACH DATABASE '$oldDbPath' as `$oldDbName`").also {
                    logDebug(TAG, "Attached old database: $oldDbName")
                }

                // Column names & order are explicitly mentioned to avoid mistakes.
                execSQL("INSERT INTO `$newTableName` ($columnSqlStr) SELECT $columnSqlStr FROM `$oldDbName`.`$oldTableName`").also {
                    logDebug(TAG, "Finished copying data")
                }

                execSQL("DETACH `$oldDbName`").also {
                    logDebug(TAG, "Detached old database: $oldDbName")
                }
            } catch (e: Exception) {
                logWarning(TAG, "Error: ${e.message}")
            } finally {
                context.deleteOldAppDb(oldDbName)
            }
        } else {
            logDebug(TAG, "Migration from $migrationLogStr not required since old database doesn't exist")
        }
    }

    private fun Context.deleteOldAppDb(oldDbName: String) {
        deleteDatabase(oldDbName).also {
            logDebug(TAG, "[cleanup] `$oldDbName` deleted: $it")
        }
    }

}
