package com.oxygenupdater.workers

import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.collection.ArraySet
import androidx.collection.MutableScatterMap
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.oxygenupdater.dao.SubmittedUpdateFileDao
import com.oxygenupdater.extensions.get
import com.oxygenupdater.extensions.set
import com.oxygenupdater.internal.settings.KeyContributionCount
import com.oxygenupdater.models.SubmittedUpdateFile
import com.oxygenupdater.repositories.ServerRepository
import com.oxygenupdater.services.RootFileService
import com.oxygenupdater.services.RootFileService.Companion.FILENAME
import com.oxygenupdater.utils.LocalNotifications
import com.oxygenupdater.utils.Utils
import com.oxygenupdater.utils.logDebug
import com.oxygenupdater.utils.logInfo
import com.oxygenupdater.utils.logWarning
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime

/**
 * Reads `ota.db` copied by [RootFileService].
 *
 * A non-root service is required because of https://github.com/topjohnwu/libsu/issues/148.
 * Basically, when opening a database, this is the code path:
 * - `SQLiteDatabase()`
 * - `SQLiteCompatibilityWalFlags.isLegacyCompatibilityWalEnabled()` -> `initIfNeeded()`
 * - …
 * - `ContentProviderHelper.getContentProviderImpl()` throws
 *   ```
 *   SecurityException: Unable to find app for caller android.app.IApplicationThread$Stub$Proxy (pid=<root-pid>) when getting content provider settings
 *   ```
 * - because `ActivityManagerService.getRecordForAppLOSP()` can't find the process record for daemon [RootFileService]
 */
@RequiresApi(Build.VERSION_CODES.Q) // same as RootFileService
@HiltWorker
class ReadOtaDbWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted parameters: WorkerParameters,
    private val sharedPreferences: SharedPreferences,
    private val submittedUpdateFilesDao: SubmittedUpdateFileDao,
    private val serverRepository: ServerRepository,
    private val analytics: FirebaseAnalytics,
    private val crashlytics: FirebaseCrashlytics,
) : CoroutineWorker(context, parameters) {

    /** Used to ensure only unique URL rows are considered in [toMap] */
    private lateinit var urls: ArraySet<String>

    override suspend fun doWork() = withContext(Dispatchers.IO) {
        logDebug(TAG, "doWork")

        val context = applicationContext
        val noBackupFilesDir = context.noBackupFilesDir?.absolutePath
        if (noBackupFilesDir.isNullOrEmpty()) return@withContext Result.retry().also {
            logDebug(TAG, "null/empty noBackupFilesDir")
        }

        val folder = File(noBackupFilesDir)
        if (!folder.exists()) return@withContext Result.retry().also {
            logDebug(TAG, "noBackupFilesDir doesn't exist")
        }

        // RootFileService copies with file's last modified appended at the end
        val otaDbCopies = folder.listFiles { _, name -> name.startsWith(FILENAME) }
        if (otaDbCopies.isNullOrEmpty()) return@withContext Result.success().also {
            logDebug(TAG, "no $FILENAME copies yet")
        }

        otaDbCopies.sortByDescending { it.name } // newest should be checked first

        val size = otaDbCopies.size
        urls = ArraySet(size)
        val validSubmittedFilenames = ArraySet<String>(size)
        val rows = ArrayList<Map<String, String?>>(size) // Cursor.toMap ensures unique (based on URL) entries
        otaDbCopies.forEach { file ->
            val filename = file.name
            if (!file.canRead()) return@forEach logDebug(TAG, "Can't read copied $filename")

            val db = try {
                SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY)
            } catch (_: Exception) {
                return@forEach logDebug(TAG, "Can't open copied $filename")
            }

            try {
                db.rawQuery("SELECT * FROM `pkgList`", null).use { cursor ->
                    if (!cursor.moveToFirst()) return@use logDebug(TAG, "empty table for $filename")

                    do cursor.toMap()?.let { rows.add(it) } ?: logDebug(
                        TAG, "Already submitted or null"
                    ) while (cursor.moveToNext())
                }
            } catch (_: Exception) {
                // ignore
            }
        }

        if (rows.isEmpty()) return@withContext Result.success().also {
            // Delete our copies
            otaDbCopies.forEach { it.delete() }
            logDebug(TAG, "empty table(s)")
        }

        var failed = false
        val result = serverRepository.submitOtaDbRows(rows)
        if (result == null) {
            crashlytics.logWarning(TAG, "Error submitting URLs: no network connection or empty response")
            failed = true
        } else if (!result.success) {
            val errorMessage = result.errorMessage
            // If file is already in our database or if file is an invalid temporary file (server decides when this is the case),
            // mark this file as submitted but don't inform the user about it.
            if (errorMessage != null && (errorMessage == UrlAlreadyInDatabase || errorMessage == UrlInvalid)) {
                logInfo(TAG, "Ignoring submitted URLs, already in database or not relevant")
                otaDbCopies.forEach { it.delete() }
                insertInDb(rows, false)
            } else {
                // Server error, try again later
                crashlytics.logWarning(TAG, "Error submitting URLs: ${result.errorMessage}")
                failed = true
            }
        } else {
            otaDbCopies.forEach { it.delete() }
            logInfo(TAG, "Successfully submitted URLs")
            insertInDb(rows, true) {
                validSubmittedFilenames.add(it.substringAfterLast('/'))
            }
        }

        val count = validSubmittedFilenames.size
        if (count != 0) LocalNotifications.showContributionSuccessfulNotification(context, validSubmittedFilenames)

        // Increase number of submitted updates. Not currently shown in the UI, but may come in handy later.
        sharedPreferences[KeyContributionCount] = sharedPreferences[KeyContributionCount, 0] + count

        if (failed) Result.failure() else Result.success()
    }

    /**
     * We're saving values as strings to allow easy serialization (there's no serializer for `Any`).
     * Moshi could opaquely handle `Any`, but kotlinx-serialization is stricter. In other areas we've
     * constructed specific data models where Any-maps were previously used, but that's not possible
     * with `SELECT *`. Freeing ourselves from explicit column names allows us to avoid needing to
     * update the app each time any columns are added or removed in the ota.db structure. It isn't
     * feasible, so instead we're going with simply converting all column values to strings. In the
     * backend we can always cast back—in some cases we do that already, to be safe from unexpected
     * type changes in ota.db.
     *
     * Note: main operations are done on [MutableScatterMap], which is memory efficient & free of
     * extraneous allocations, though possibly slower than [HashMap]. We convert back to a regular
     * [Map][MutableScatterMap.asMap] so that it can be serialized.
     */
    private inline fun Cursor.toMap() = columnCount.let { columnCount ->
        val map = MutableScatterMap<String, String?>(columnCount)
        for (index in 0 until columnCount) when (getType(index)) {
            Cursor.FIELD_TYPE_NULL -> map[getColumnName(index)] = null
            Cursor.FIELD_TYPE_BLOB -> {} // ignore
            Cursor.FIELD_TYPE_FLOAT -> map[getColumnName(index)] = try {
                getString(index)
            } catch (_: Exception) { // in case `getString` fails
                getFloat(index).toString()
            }

            Cursor.FIELD_TYPE_INTEGER -> map[getColumnName(index)] = try {
                getString(index)
            } catch (_: Exception) { // in case `getString` fails
                getInt(index).toString()
            }

            Cursor.FIELD_TYPE_STRING -> map[getColumnName(index)] = getString(index)
        }

        // TODO(root): return only if it's an OTA ZIP, not component (non-AB devices like Nord 2, Nord CE 2).
        //  I think only OTA ZIPs have payload.bin, so we could check if `streaming_property_files` contains it.

        val url = map["active_url"] ?: map["url"]
        if (url.isNullOrBlank() || submittedUpdateFilesDao.isUrlAlreadySubmitted(url)) null
        else if (!urls.add(url)) null else map.asMap()
    }

    /**
     * Saves rows mapped as [SubmittedUpdateFile] in local DB and log URLs to analytics
     *
     * @param action (optional) act on each URL
     *
     * @return array of [SubmittedUpdateFile]s
     */
    private inline fun CoroutineScope.insertInDb(
        rows: List<Map<String, Any?>>,
        success: Boolean,
        action: (url: String) -> Unit = {},
    ) {
        // Perf: just once out of loop
        val now = LocalDateTime.now(Utils.ServerTimeZone).toString()
        val mapped = rows.map {
            // By this point, any row in this list is guaranteed to have either `active_url` or `url`,
            // because those without either of these fields are discarded in the `Cursor.toMap` extension.
            val url = (it["active_url"] ?: it["url"]) as String

            action(url)
            // Analytics events shouldn't block the current thread
            @Suppress("DeferredResultUnused") async {
                analytics.logEvent(
                    if (success) "CONTRIBUTION_SUCCESSFUL" else "CONTRIBUTION_NOT_NEEDED",
                    Bundle(1).apply { putString("CONTRIBUTION_URL", url) }
                )
            }

            // Before the Oppo merger, URLs followed a simple format, and only the name was required to complete
            // URLs. App v2.7.0 - v5.8.3 had this old filename contribution feature, which was removed in v5.9.0.
            // v5.11.0 brought it back with adjustments for URLs instead, and behind a root access check.
            // Note: we're taking the lazy approach and saving the entire URL in the `name` field, to avoid
            // bumping DB version and adding column-name-change upgrade logic. Why? Because `ALTER…RENAME`
            // is supported only in SQLite v3.25.0+, and versions vary per device/Android release:
            // https://stackoverflow.com/a/4377116. On older SQLite versions, table recreation was the only
            // way, plus we'd still have to worry about recreating indices etc.
            SubmittedUpdateFile(name = url, dateSubmitted = now)
        }.toTypedArray()

        // Store URLs locally to prevent re-submission
        submittedUpdateFilesDao.insert(*mapped)
    }

    companion object {
        private const val TAG = "ReadOtaDbWorker"

        private const val UrlAlreadyInDatabase = "E_URL_ALREADY_IN_DB"
        private const val UrlInvalid = "E_URL_INVALID"
    }
}
