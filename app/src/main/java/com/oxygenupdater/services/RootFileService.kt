package com.oxygenupdater.services

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.FileObserver
import android.util.Log
import androidx.annotation.RequiresApi
import com.oxygenupdater.BuildConfig
import com.topjohnwu.superuser.ShellUtils
import com.topjohnwu.superuser.ipc.RootService
import com.topjohnwu.superuser.nio.ExtendedFile
import com.topjohnwu.superuser.nio.FileSystemManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import java.util.Timer
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.schedule

/** Creates file system service in the root process */
@RequiresApi(Build.VERSION_CODES.Q) // for FileObserver
class RootFileService : RootService() {

    private var noBackupFilesDir: String? = null

    override fun onAttach(base: Context) = super.onAttach(base).also {
        bound.set(true)
        log("onAttach")
        noBackupFilesDir = it.noBackupFilesDir.absolutePath
    }

    override fun onBind(intent: Intent) = log("onBind").let {
        bound.set(true)
        @Suppress("DeferredResultUnused") doWorkAsync()
        null
    }

    override fun onRebind(intent: Intent) = super.onRebind(intent).also {
        bound.set(true)
        log("onRebind")
        @Suppress("DeferredResultUnused") doWorkAsync()
    }

    override fun onDestroy() = super.onDestroy().also {
        bound.set(false)
        log("onDestroy")
    }

    private fun doWorkAsync() = CoroutineScope(Dispatchers.IO).async {
        log("doWorkAsync")

        if (noBackupFilesDir.isNullOrEmpty()) return@async log("null/empty noBackupFilesDir")

        val localFs = try {
            FileSystemManager.getLocal()
        } catch (e: Exception) {
            return@async log("Failed to get local file system from root service: $e")
        }

        val folder = localFs.getFile(FOLDER)

        var retryCount = 1
        while (true) if (folder.exists()) break else if (retryCount > 2) {
            log("$FOLDER doesn't exist, exiting")
            return@async stopSelf()
        } else log("Re-checking for folder in 2s (${retryCount++}/2)").also {
            delay(2000) // 2s
        }

        // Read ota.db for the first (if exists) before starting FolderObserver
        val otaDb = folder.getChildFile(FILENAME)
        if (otaDb.canRead()) otaDb.copyToNoBackupFilesDir()

        FolderObserver(folder) { event ->
            when (event) {
                FileObserver.CLOSE_WRITE -> {
                    if (!otaDb.canRead()) return@FolderObserver log("Can't read original $FILENAME")

                    otaDb.copyToNoBackupFilesDir() // ReadOtaDbWorker will pick it up later
                }
                // DELETE_SELF effectively stops monitoring, so stop this service too
                // It will run on next app launch, and exit early if folder still doesn't exist
                FileObserver.DELETE_SELF -> stopSelf()
            }
        }.startWatching()
    }

    /**
     * Even though we're running under a root process, we can't pass root access over to SQLiteDatabase.
     * So we need to copy ota.db to our own app's [noBackupFilesDir], ensuring user:group is the app's, not root.
     *
     * An alternative would be `otaDb.setReadable(true, false)`, but that would make it world-readable.
     *
     * These copies would eventually be read by [com.oxygenupdater.workers.ReadOtaDbWorker].
     */
    private fun ExtendedFile.copyToNoBackupFilesDir(): Boolean {
        log("Copying original $FILENAME")
        val copiedPath = "$noBackupFilesDir/$FILENAME-${lastModified()}"
        val copyCommand = "cp -pf '$path' '$copiedPath'"
        val chownCommand = "chown \"\$(stat -c '%U:%G' '$noBackupFilesDir')\" '$copiedPath'"
        return ShellUtils.fastCmdResult("$copyCommand && $chownCommand")
    }

    private class FolderObserver(
        folder: ExtendedFile,
        private val callback: (event: Int) -> Unit,
    ) : FileObserver(folder, ObserverMask) {

        private val timer = Timer()

        override fun onEvent(event: Int, path: String?) {
            // Sometimes event is an unknown Int
            if (event != CLOSE_WRITE && event != DELETE_SELF) return
            // We care only about one specific file
            if (path != FILENAME) return

            log("FolderObserver: $event")

            timer.cancel() // ensure only the latest task goes through
            // Debounce by 1s to act on a settled state
            timer.schedule(1000) { callback(event) }
        }
    }

    companion object {
        private const val TAG = "RootFileService"

        @SuppressLint("SdCardPath")
        private const val FOLDER = "/data/data/com.oplus.ota/databases"
        const val FILENAME = "ota.db"

        private const val ObserverMask = FileObserver.CLOSE_WRITE or FileObserver.DELETE_SELF

        /**
         *  [Log] must be used directly, because [com.oxygenupdater.utils.logDebug]
         *  requires Hilt to be initialized
         */
        private fun log(message: String, t: Throwable? = null) {
            if (BuildConfig.DEBUG) Log.d(TAG, message, t)
        }

        val bound = AtomicBoolean(false)
    }
}
