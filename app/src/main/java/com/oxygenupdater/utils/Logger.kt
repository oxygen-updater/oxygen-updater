package com.oxygenupdater.utils

import android.util.Log
import com.google.android.ump.FormError
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.ktx.setCustomKeys
import com.oxygenupdater.BuildConfig
import com.oxygenupdater.exceptions.GooglePlayBillingException
import com.oxygenupdater.utils.ExceptionUtils.isNetworkError
import org.koin.java.KoinJavaComponent.getKoin

object Logger {

    private const val CrashlyticsTagExceptionSeverity = "EXCEPTION_SEVERITY"
    private const val CrashlyticsTagErrorDetailMessage = "ERROR_DETAIL_MESSAGE"

    private val crashlytics by getKoin().inject<FirebaseCrashlytics>()

    fun logVerbose(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.v(tag, message)
    }

    fun logVerbose(tag: String, message: String, cause: Throwable?) {
        if (BuildConfig.DEBUG) Log.v(tag, message, cause)
    }

    fun logDebug(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.d(tag, message)
    }

    fun logDebug(tag: String, message: String, cause: Throwable?) {
        if (BuildConfig.DEBUG) Log.d(tag, message, cause)
    }

    fun logInfo(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.i(tag, message)
    }

    fun logInfo(tag: String, message: String, cause: Throwable?) {
        if (BuildConfig.DEBUG) Log.i(tag, message, cause)
    }

    fun logUmpConsentFormError(tag: String, error: FormError?) {
        if (error == null) return
        logWarning(tag, "UMP consent failed: ${error.errorCode} ${error.message}")
    }

    /** Log a recoverable exception at warning level */
    fun logWarning(tag: String, message: String, cause: Throwable? = null) {
        crashlytics.setCustomKeys {
            key(CrashlyticsTagExceptionSeverity, LogLevel.WARNING.name)
            key(CrashlyticsTagErrorDetailMessage, "$tag: $message") // Human readable error description
        }

        Log.w(tag, cause?.message ?: message, cause)
        crashlytics.log(cause?.message ?: message)
        if (cause != null) logException(cause)
    }

    /** Log an in-app billing error message */
    fun logBillingError(tag: String, message: String) {
        crashlytics.setCustomKey(CrashlyticsTagExceptionSeverity, LogLevel.ERROR.name)

        Log.e(tag, message)
        logException(GooglePlayBillingException(message))
    }

    /** Log a recoverable exception at error level */
    fun logError(tag: String, message: String, cause: Throwable) {
        crashlytics.setCustomKeys {
            key(CrashlyticsTagExceptionSeverity, LogLevel.ERROR.name)
            key(CrashlyticsTagErrorDetailMessage, "$tag: $message") // Human readable error description
        }

        Log.e(tag, cause.message, cause)
        logException(cause)
    }

    private fun logException(cause: Throwable) {
        if (isNetworkError(cause)) crashlytics.setCustomKey("IS_NETWORK_ERROR", true)
        crashlytics.recordException(cause)
    }

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
