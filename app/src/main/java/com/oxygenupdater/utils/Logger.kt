package com.oxygenupdater.utils

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.ktx.setCustomKeys
import com.oxygenupdater.BuildConfig
import com.oxygenupdater.exceptions.GooglePlayBillingException
import com.oxygenupdater.utils.ExceptionUtils.isNetworkError
import com.oxygenupdater.utils.Logger.LogLevel.ERROR
import com.oxygenupdater.utils.Logger.LogLevel.WARNING
import org.koin.java.KoinJavaComponent.getKoin

object Logger {

    private const val CRASHLYTICS_TAG_EXCEPTION_SEVERITY = "EXCEPTION_SEVERITY"
    private const val CRASHLYTICS_TAG_ERROR_DETAIL_MESSAGE = "ERROR_DETAIL_MESSAGE"

    private val crashlytics by getKoin().inject<FirebaseCrashlytics>()

    fun logVerbose(tag: String, message: String) {
        if (DEBUG) Log.v(tag, message)
    }

    fun logVerbose(tag: String, message: String, cause: Throwable?) {
        if (DEBUG) Log.v(tag, message, cause)
    }

    fun logDebug(tag: String, message: String) {
        if (DEBUG) Log.d(tag, message)
    }

    fun logDebug(tag: String, message: String, cause: Throwable?) {
        if (DEBUG) Log.d(tag, message, cause)
    }

    fun logInfo(tag: String, message: String) {
        if (DEBUG) Log.i(tag, message)
    }

    fun logInfo(tag: String, message: String, cause: Throwable?) {
        if (DEBUG) Log.i(tag, message, cause)
    }

    /** Log a recoverable exception at warning level */
    fun logWarning(tag: String, message: String, cause: Throwable? = null) {
        crashlytics.setCustomKeys {
            key(CRASHLYTICS_TAG_EXCEPTION_SEVERITY, WARNING.name)
            key(CRASHLYTICS_TAG_ERROR_DETAIL_MESSAGE, "$tag: $message") // Human readable error description
        }

        Log.w(tag, cause?.message ?: message, cause)
        crashlytics.log(cause?.message ?: message)
        if (cause != null) logException(cause)
    }

    /** Log an in-app billing error message */
    fun logBillingError(tag: String, message: String) {
        crashlytics.setCustomKey(CRASHLYTICS_TAG_EXCEPTION_SEVERITY, ERROR.name)

        val exception = GooglePlayBillingException(message)
        Log.e(tag, message)
        logException(exception)
    }

    /** Log a recoverable exception at error level */
    fun logError(tag: String, message: String, cause: Throwable) {
        crashlytics.setCustomKeys {
            key(CRASHLYTICS_TAG_EXCEPTION_SEVERITY, ERROR.name)
            key(CRASHLYTICS_TAG_ERROR_DETAIL_MESSAGE, "$tag: $message") // Human readable error description
        }

        Log.e(tag, cause.message, cause)
        logException(cause)
    }

    private fun logException(cause: Throwable) {
        if (isNetworkError(cause)) crashlytics.setCustomKey("IS_NETWORK_ERROR", true)
        crashlytics.recordException(cause)
    }

    private val DEBUG = BuildConfig.DEBUG

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
