package com.oxygenupdater.utils

import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import android.util.Log
import com.google.android.ump.FormError
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.oxygenupdater.BuildConfig
import com.oxygenupdater.exceptions.GooglePlayBillingException

fun logVerbose(tag: String, message: String, cause: Throwable? = null) {
    if (!BuildConfig.DEBUG) return
    if (cause != null) Log.v(tag.ensureLength(), message, cause) else Log.v(tag.ensureLength(), message)
}

fun logDebug(tag: String, message: String) {
    if (BuildConfig.DEBUG) Log.d(tag.ensureLength(), message)
}

fun logInfo(tag: String, message: String, cause: Throwable? = null) {
    if (!BuildConfig.DEBUG) return
    if (cause != null) Log.i(tag.ensureLength(), message, cause) else Log.i(tag.ensureLength(), message)
}

fun FirebaseCrashlytics.logUmpConsentFormError(
    tag: String,
    error: FormError,
) = logWarning(tag, "UMP consent failed: ${error.errorCode} ${error.message}")

/** Log a recoverable exception at warning level */
fun FirebaseCrashlytics.logWarning(
    tag: String,
    message: String,
    cause: Throwable? = null,
) = if (cause != null) {
    val msg = cause.message ?: message
    Log.w(tag.ensureLength(), msg, cause)

    logException(tag, message, cause)
    log(msg)
} else {
    Log.w(tag.ensureLength(), message)
    setCustomKey(KeyExceptionSeverity, SeverityWarning)
    setCustomKey(KeyErrorTag, tag)
    setCustomKey(KeyErrorMessage, message)
    log(message)
}

/** Log an in-app billing error message */
fun FirebaseCrashlytics.logBillingError(tag: String, message: String) {
    Log.e(tag.ensureLength(), message)
    logException(tag, message, GooglePlayBillingException(message))
}

/** Log a recoverable exception at error level */
fun FirebaseCrashlytics.logError(tag: String, message: String, cause: Throwable) {
    Log.e(tag.ensureLength(), cause.message, cause)
    logException(tag, message, cause)
}

private fun FirebaseCrashlytics.logException(tag: String, message: String, cause: Throwable) {
    setCustomKey(KeyExceptionSeverity, SeverityError)
    setCustomKey(KeyErrorTag, tag)
    setCustomKey(KeyErrorMessage, message)
    if (isNetworkError(cause)) setCustomKey(KeyIsNetworkError, true)
    recordException(cause)
}

/** Log tags must be max 23 characters before Android 8/Oreo */
private fun String.ensureLength() =
    if (length <= MaxTagLength || SDK_INT >= VERSION_CODES.O) this else substring(0, MaxTagLength)

private const val MaxTagLength = 23
private const val KeyExceptionSeverity = "EXCEPTION_SEVERITY"
private const val KeyErrorTag = "ERROR_TAG"
private const val KeyErrorMessage = "ERROR_MESSAGE"
private const val KeyIsNetworkError = "IS_NETWORK_ERROR"
private const val SeverityError = "ERROR"
private const val SeverityWarning = "WARNING"
