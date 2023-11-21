package com.oxygenupdater.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import com.oxygenupdater.database.DatabaseBuilders.AppDb
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.internal.settings.PrefManager.KeySqlToRoomMigrationDone
import com.oxygenupdater.utils.Logger.logDebug
import com.oxygenupdater.utils.Logger.logWarning
import org.koin.java.KoinJavaComponent.getKoin

/**
 * Migrations for all app databases/tables.
 *
 * **Note: No matter what, don't ever edit existing migrations after a release.
 * Create new migrations instead, when schema has been changed.**
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
object SqliteMigrations {

    private const val TAG = "DatabaseMigrations"
    private const val NewsItemTable = "news_item"
    private const val SubmittedUpdateFileTable = "submitted_update_file"
    private const val PurchasesOldDb = "purchase_db"
    private const val NewsItemsOldDb = "NewsItems.db"
    private const val SubmittedUpdateFilesOldDb = "SubmittedUpdateFiles.db"

    /**
     * Was used from 4.3.0 — 5.3.0, when the app had migrated away from AIDL to
     * Google Play Billing Library v3. It used a Room database as a cache for
     * IAB responses, but in 5.4.0 when we upgraded GPBL to v4, we no longer
     * needed a DB cache (code was simplified quite a bit).
     */
    fun deleteLocalBillingDatabase(context: Context) = context.deleteDatabase(PurchasesOldDb)

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
        val migrationDone = PrefManager.getBoolean(
            KeySqlToRoomMigrationDone,
            false
        )

        // Make sure migration is run only once. This is necessary because this
        // function is called from the `onOpen` callback instead of `onCreate`.
        if (!migrationDone) {
            logDebug(TAG, "Starting migrations for SQLite to Room")
            val context by getKoin().inject<Context>()

            migrateFromOldDb(
                context,
                SubmittedUpdateFilesOldDb,
                NewsItemTable,
                SubmittedUpdateFileTable,
                "`id`, `name`, `date_submitted`"
            )

            migrateFromOldDb(
                context,
                NewsItemsOldDb,
                NewsItemTable,
                NewsItemTable,
                "`id`, `dutch_title`, `english_title`, `dutch_subtitle`, `english_subtitle`, `image_url`, `dutch_text`, `english_text`, `date_published`, `date_last_edited`, `author_name`, `read`"
            )

            PrefManager.putBoolean(
                KeySqlToRoomMigrationDone,
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
        columnSqlStr: String,
    ) = context.getDatabasePath(oldDbName).run {
        val migrationLogStr = "`$oldDbName`.`$oldTableName` to `$AppDb`.`$newTableName`"

        if (exists()) {
            logDebug(TAG, "Migrating $migrationLogStr")

            val oldDbPath = toString()
            try {
                execSQL("ATTACH DATABASE '$oldDbPath' as `$oldDbName`")
                logDebug(TAG, "Attached old database: $oldDbName")

                // Column names & order are explicitly mentioned to avoid mistakes.
                execSQL("INSERT INTO `$newTableName` ($columnSqlStr) SELECT $columnSqlStr FROM `$oldDbName`.`$oldTableName`")
                logDebug(TAG, "Finished copying data")

                execSQL("DETACH `$oldDbName`")
                logDebug(TAG, "Detached old database: $oldDbName")
            } catch (e: Exception) {
                logWarning(TAG, "Error: ${e.message}")
            } finally {
                val deleted = context.deleteDatabase(oldDbName)
                logDebug(TAG, "[cleanup] `$oldDbName` deleted: $deleted")
            }
        } else {
            logDebug(TAG, "Migration from $migrationLogStr not required since old database doesn't exist")
        }
    }
}
