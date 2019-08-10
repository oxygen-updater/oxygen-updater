package com.arjanvlek.oxygenupdater.internal.root

import android.os.AsyncTask
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logError
import eu.chainfire.libsuperuser.Shell
import java8.util.function.Consumer

object RootAccessChecker {

    private var hasCheckedOnce = false
    private var isRooted = false

    /**
     * Checks if the device is rooted.
     *
     *
     * Callback gets as argument whether or not the device is rooted. The result of
     * Shell.SU.available is cached.
     */
    fun checkRootAccess(callback: Consumer<Boolean>) {
        if (hasCheckedOnce) {
            callback.accept(isRooted)
            return
        }

        RootCheckerImpl(callback).execute()
    }

    private class RootCheckerImpl internal constructor(private val callback: Consumer<Boolean>) : AsyncTask<Void, Void, Boolean>() {

        override fun doInBackground(vararg params: Void): Boolean? {
            return try {
                Thread.sleep(2000) // Give the user the time to read what's happening.
                Shell.SU.available()
            } catch (e: Exception) {
                logError("ApplicationData", "Failed to check for root access", e)
                false
            }

        }

        override fun onPostExecute(result: Boolean?) {
            hasCheckedOnce = true
            isRooted = result!!
            callback.accept(isRooted)
        }
    }
}
