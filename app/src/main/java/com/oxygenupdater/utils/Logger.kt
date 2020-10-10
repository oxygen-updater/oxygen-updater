package com.oxygenupdater.utils

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.ktx.setCustomKeys
import com.oxygenupdater.BuildConfig
import com.oxygenupdater.exceptions.OxygenUpdaterException
import com.oxygenupdater.utils.ExceptionUtils.isNetworkError
import com.oxygenupdater.utils.Logger.LogLevel.ERROR
import com.oxygenupdater.utils.Logger.LogLevel.WARNING
import org.koin.java.KoinJavaComponent.inject

@Suppress("unused")
object Logger {

    private const val CRASHLYTICS_TAG_EXCEPTION_SEVERITY = "EXCEPTION_SEVERITY"
    private const val CRASHLYTICS_TAG_ERROR_DETAIL_MESSAGE = "ERROR_DETAIL_MESSAGE"

    private val crashlytics by inject(FirebaseCrashlytics::class.java)

    fun logVerbose(tag: String?, message: String) {
        if (isDebugBuild) {
            Log.v(tag, message)
        }
    }

    fun logVerbose(tag: String?, message: String, cause: Throwable?) {
        if (isDebugBuild) {
            Log.v(tag, message, cause)
        }
    }

    fun logDebug(tag: String?, message: String) {
        if (isDebugBuild) {
            Log.d(tag, message)
        }
    }

    fun logDebug(tag: String?, message: String, cause: Throwable?) {
        if (isDebugBuild) {
            Log.d(tag, message, cause)
        }
    }

    fun logInfo(tag: String?, message: String) {
        if (isDebugBuild) {
            Log.i(tag, message)
        }
    }

    fun logInfo(tag: String?, message: String, cause: Throwable?) {
        if (isDebugBuild) {
            Log.i(tag, message, cause)
        }
    }

    /**
     * Log a warning message. Must be wrapped in OxygenUpdaterException before so Firebase reads the correct line number.
     */
    fun logWarning(tag: String?, cause: OxygenUpdaterException) {
        crashlytics.setCustomKey(CRASHLYTICS_TAG_EXCEPTION_SEVERITY, WARNING.name)

        Log.w(tag, cause.message ?: "OxygenUpdaterException: unknown")
        logException(cause)
    }

    /**
     * Log a recoverable exception at warning level
     */
    fun logWarning(tag: String, message: String, cause: Throwable? = null) {
        crashlytics.setCustomKeys {
            key(CRASHLYTICS_TAG_EXCEPTION_SEVERITY, WARNING.name)
            key(CRASHLYTICS_TAG_ERROR_DETAIL_MESSAGE, "$tag: $message") // Human readable error description
        }

        Log.w(tag, cause?.message ?: message, cause)
        if (cause != null) {
            logException(cause)
        }
    }

    /**
     * Log an error message. Must be wrapped in OxygenUpdaterException before so Firebase reads the correct line number.
     */
    fun logError(tag: String?, cause: OxygenUpdaterException) {
        crashlytics.setCustomKey(CRASHLYTICS_TAG_EXCEPTION_SEVERITY, ERROR.name)

        Log.e(tag, cause.message ?: "OxygenUpdaterException: unknown")
        logException(cause)
    }

    /**
     * Log a recoverable exception at error level
     */
    fun logError(tag: String, message: String, cause: Throwable) {
        crashlytics.setCustomKeys {
            key(CRASHLYTICS_TAG_EXCEPTION_SEVERITY, ERROR.name)
            key(CRASHLYTICS_TAG_ERROR_DETAIL_MESSAGE, "$tag: $message") // Human readable error description
        }

        Log.e(tag, cause.message, cause)
        logException(cause)
    }

    private fun logException(cause: Throwable) {
        if (isNetworkError(cause)) {
            crashlytics.setCustomKey("IS_NETWORK_ERROR", true)
        }
        crashlytics.recordException(cause)
    }

    private val isDebugBuild = BuildConfig.DEBUG

    @Suppress("unused")
    private enum class LogLevel {
        VERBOSE,
        DEBUG,
        INFO,
        WARNING,
        ERROR,
        CRASH,
        NETWORK_ERROR
    }
}
