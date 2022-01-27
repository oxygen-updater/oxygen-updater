package com.oxygenupdater.utils

import com.oxygenupdater.internal.KotlinCallback
import com.oxygenupdater.utils.Logger.logError
import eu.chainfire.libsuperuser.Shell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object RootAccessChecker {

    private var hasCheckedOnce = false
    private var isRooted = false

    private val ioScope by lazy {
        CoroutineScope(Dispatchers.IO)
    }

    /**
     * Checks if the device is rooted.
     *
     * Callback gets as argument whether or not the device is rooted. The result of [Shell.SU.available] is cached.
     */
    fun checkRootAccess(callback: KotlinCallback<Boolean>) {
        if (hasCheckedOnce) {
            callback.invoke(isRooted)
        } else {
            ioScope.launch {
                isRooted = try {
                    delay(2000) // Give the user the time to read what's happening.
                    Shell.SU.available()
                } catch (e: Exception) {
                    logError("ApplicationData", "Failed to check for root access", e)
                    false
                }

                withContext(Dispatchers.Main) {
                    hasCheckedOnce = true
                    callback.invoke(isRooted)
                }
            }
        }
    }
}
