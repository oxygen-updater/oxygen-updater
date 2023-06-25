package com.oxygenupdater.workers

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.collection.ArrayMap
import androidx.collection.ArraySet
import androidx.core.os.bundleOf
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.analytics.FirebaseAnalytics
import com.oxygenupdater.database.LocalAppDb
import com.oxygenupdater.exceptions.NetworkException
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.internal.settings.PrefManager.PROPERTY_CONTRIBUTION_COUNT
import com.oxygenupdater.models.SubmittedUpdateFile
import com.oxygenupdater.repositories.ServerRepository
import com.oxygenupdater.services.RootFileService
import com.oxygenupdater.services.RootFileService.Companion.FILENAME
import com.oxygenupdater.utils.LocalNotifications
import com.oxygenupdater.utils.Logger.logDebug
import com.oxygenupdater.utils.Logger.logError
import com.oxygenupdater.utils.Logger.logInfo
import com.oxygenupdater.utils.Logger.logWarning
import com.oxygenupdater.utils.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent.getKoin
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
class ReadOtaDbWorker(
    context: Context,
    parameters: WorkerParameters
) : CoroutineWorker(context, parameters) {

    private val database: LocalAppDb
    private val analytics: FirebaseAnalytics
    private val serverRepository: ServerRepository

    init {
        val koin = getKoin()

        database = koin.inject<LocalAppDb>().value
        analytics = koin.inject<FirebaseAnalytics>().value
        serverRepository = koin.inject<ServerRepository>().value
    }

    /** Used to ensure only unique URL rows are considered in [toMap] */
    private lateinit var urls: ArraySet<String>

    private val submittedUpdateFilesDao by lazy(LazyThreadSafetyMode.NONE) {
        database.submittedUpdateFileDao()
    }

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
        val rows = ArrayList<ArrayMap<String, Any?>>(size) // Cursor.toMap ensures unique (based on URL) entries
        otaDbCopies.forEach { file ->
            val filename = file.name
            if (!file.canRead()) return@forEach logDebug(TAG, "Can't read copied $filename")

            val db = try {
                SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY)
            } catch (e: Exception) {
                return@forEach logDebug(TAG, "Can't open copied $filename")
            }

            try {
                db.rawQuery("SELECT * FROM `pkgList`", null).use { cursor ->
                    if (!cursor.moveToFirst()) return@use logDebug(TAG, "empty table for $filename")

                    do cursor.toMap()?.let { rows.add(it) } ?: logDebug(
                        TAG, "Already submitted or null"
                    ) while (cursor.moveToNext())
                }
            } catch (e: Exception) {
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
            logWarning(TAG, "Error submitting URLs: no network connection or empty response")
            failed = true
        } else if (!result.success) {
            val errorMessage = result.errorMessage
            // If file is already in our database or if file is an invalid temporary file (server decides when this is the case),
            // mark this file as submitted but don't inform the user about it.
            if (errorMessage != null && (errorMessage == URL_ALREADY_IN_DATABASE || errorMessage == URL_INVALID)) {
                logInfo(TAG, "Ignoring submitted URLs, already in database or not relevant")
                otaDbCopies.forEach { it.delete() }
                insertInDb(rows, false)
            } else {
                // Server error, try again later
                logError(TAG, NetworkException("Error submitting URLs: ${result.errorMessage}"))
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
        if (count != 0) LocalNotifications.showContributionSuccessfulNotification(
            context,
            validSubmittedFilenames
        )

        // Increase number of submitted updates. Not currently shown in the UI, but may come in handy later.
        PrefManager.putInt(
            PROPERTY_CONTRIBUTION_COUNT,
            PrefManager.getInt(PROPERTY_CONTRIBUTION_COUNT, 0) + count
        )

        if (failed) Result.failure() else Result.success()
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun Cursor.toMap() = columnCount.let { columnCount ->
        // Memory efficient, albeit slower compared to HashMap
        // Note: SimpleArrayMap can't be used; Jackson can't serialize it
        val map = ArrayMap<String, Any?>(columnCount)
        for (index in 0 until columnCount) when (getType(index)) {
            Cursor.FIELD_TYPE_NULL -> map[getColumnName(index)] = null
            Cursor.FIELD_TYPE_BLOB -> {} // ignore
            Cursor.FIELD_TYPE_FLOAT -> map[getColumnName(index)] = getFloat(index)
            Cursor.FIELD_TYPE_INTEGER -> map[getColumnName(index)] = getInt(index)
            Cursor.FIELD_TYPE_STRING -> map[getColumnName(index)] = getString(index)
        }

        // TODO(root): return only if it's an OTA ZIP, not component (non-AB devices like Nord 2, Nord CE 2).
        //  I think only OTA ZIPs have payload.bin, so we could check if `streaming_property_files` contains it.

        val url = (map["active_url"] ?: map["url"]) as? String
        if (url.isNullOrBlank() || submittedUpdateFilesDao.isUrlAlreadySubmitted(url)) null
        else if (!urls.add(url)) null else map
    }

    /**
     * Saves rows mapped as [SubmittedUpdateFile] in local DB and log URLs to analytics
     *
     * @param action (optional) act on each URL
     *
     * @return array of [SubmittedUpdateFile]s
     */
    private inline fun CoroutineScope.insertInDb(
        rows: List<ArrayMap<String, Any?>>,
        success: Boolean,
        action: (String) -> Unit = {}
    ) {
        // Perf: just once out of loop
        val now = LocalDateTime.now(Utils.SERVER_TIME_ZONE).toString()
        val mapped = rows.map {
            // By this point, any row in this list is guaranteed to have either `active_url` or `url`,
            // because those without either of these fields are discarded in the `Cursor.toMap` extension.
            val url = (it["active_url"] ?: it["url"]) as String

            action(url)
            // Analytics events shouldn't block the current thread
            @Suppress("DeferredResultUnused") async {
                analytics.logEvent(
                    if (success) "CONTRIBUTION_SUCCESSFUL" else "CONTRIBUTION_NOT_NEEDED",
                    bundleOf("CONTRIBUTION_URL" to url)
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

        private const val URL_ALREADY_IN_DATABASE = "E_URL_ALREADY_IN_DB"
        private const val URL_INVALID = "E_URL_INVALID"
    }
}
