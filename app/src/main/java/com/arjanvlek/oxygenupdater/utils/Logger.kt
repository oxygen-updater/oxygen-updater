package com.arjanvlek.oxygenupdater.utils

import android.util.Log
import com.arjanvlek.oxygenupdater.BuildConfig
import com.arjanvlek.oxygenupdater.exceptions.OxygenUpdaterException
import com.arjanvlek.oxygenupdater.utils.ExceptionUtils.isNetworkError
import com.crashlytics.android.Crashlytics

@Suppress("unused")
object Logger {

    private const val CRASHLYTICS_TAG_EXCEPTION_SEVERITY = "EXCEPTION_SEVERITY"
    private const val CRASHLYTICS_TAG_ERROR_DETAIL_MESSAGE = "ERROR_DETAIL_MESSAGE"

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
        Crashlytics.setString(CRASHLYTICS_TAG_EXCEPTION_SEVERITY, LogLevel.WARNING.name)

        Log.w(tag, cause.message ?: "OxygenUpdaterException: unknown")
        logException(cause)
    }

    /**
     * Log a recoverable exception at warning level
     */
    fun logWarning(tag: String, message: String, cause: Throwable? = null) {
        Crashlytics.setString(CRASHLYTICS_TAG_EXCEPTION_SEVERITY, LogLevel.WARNING.name)
        Crashlytics.setString(CRASHLYTICS_TAG_ERROR_DETAIL_MESSAGE, "$tag: $message") // Human readable error description

        Log.w(tag, cause?.message ?: message, cause)
        if (cause != null) {
            logException(cause)
        }
    }

    /**
     * Log an error message. Must be wrapped in OxygenUpdaterException before so Firebase reads the correct line number.
     */
    fun logError(tag: String?, cause: OxygenUpdaterException) {
        Crashlytics.setString(CRASHLYTICS_TAG_EXCEPTION_SEVERITY, LogLevel.ERROR.name)

        Log.e(tag, cause.message ?: "OxygenUpdaterException: unknown")
        logException(cause)
    }

    /**
     * Log a recoverable exception at error level
     */
    fun logError(tag: String, message: String, cause: Throwable) {
        Crashlytics.setString(CRASHLYTICS_TAG_EXCEPTION_SEVERITY, LogLevel.ERROR.name)
        Crashlytics.setString(CRASHLYTICS_TAG_ERROR_DETAIL_MESSAGE, "$tag: $message") // Human readable error description

        Log.e(tag, cause.message, cause)
        logException(cause)
    }

    private fun logException(cause: Throwable) {
        if (isNetworkError(cause)) {
            Crashlytics.setBool("IS_NETWORK_ERROR", true)
        }
        Crashlytics.logException(cause)
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
