package com.arjanvlek.oxygenupdater.internal.logger;

import android.util.Log;

import com.arjanvlek.oxygenupdater.BuildConfig;
import com.arjanvlek.oxygenupdater.internal.ExceptionUtils;
import com.arjanvlek.oxygenupdater.internal.OxygenUpdaterException;
import com.crashlytics.android.Crashlytics;

public class Logger {

    private static final String CRASHLYTICS_TAG_EXCEPTION_SEVERITY = "EXCEPTION_SEVERITY";
    private static final String CRASHLYTICS_TAG_ERROR_DETAIL_MESSAGE = "ERROR_DETAIL_MESSAGE";

    public static void logVerbose(String tag, String message) {
        if (isDebugBuild()) {
            Log.v(tag, message);
        }
    }

    public static void logVerbose(String tag, String message, Throwable cause) {
        if (isDebugBuild()) {
            Log.v(tag, message, cause);
        }
    }

    public static void logDebug(String tag, String message) {
        if (isDebugBuild()) {
            Log.d(tag, message);
        }
    }

    public static void logDebug(String tag, String message, Throwable cause) {
        if (isDebugBuild()) {
            Log.d(tag, message, cause);
        }
    }

    public static void logInfo(String tag, String message) {
        if (isDebugBuild()) {
            Log.i(tag, message);
        }
    }

    public static void logInfo(String tag, String message, Throwable cause) {
        if (isDebugBuild()) {
            Log.i(tag, message, cause);
        }
    }

    // Log a warning message. Must be wrapped in OxygenUpdaterException before so Firebase reads the correct line number.
    public static void logWarning(String tag, OxygenUpdaterException cause) {
        Log.w(tag, cause.getMessage());
        Crashlytics.setString(CRASHLYTICS_TAG_EXCEPTION_SEVERITY, LogLevel.WARNING.name());
        logException(cause);
    }

    // Log a recoverable exception at warning level
    public static void logWarning(String tag, String message, Throwable cause) {
        Log.w(tag, cause.getMessage(), cause);
        Crashlytics.setString(CRASHLYTICS_TAG_EXCEPTION_SEVERITY, LogLevel.WARNING.name());
        Crashlytics.setString(CRASHLYTICS_TAG_ERROR_DETAIL_MESSAGE, tag + ": " + message); // Human readable error description
        logException(cause);
    }

    // Log an error message. Must be wrapped in OxygenUpdaterException before so Firebase reads the correct line number.
    public static void logError(String tag, OxygenUpdaterException cause) {
        Log.e(tag, cause.getMessage());
        Crashlytics.setString(CRASHLYTICS_TAG_EXCEPTION_SEVERITY, LogLevel.ERROR.name());
        logException(cause);
    }

    // Log a recoverable exception at error level
    public static void logError(String tag, String message, Throwable cause) {
        Log.e(tag, cause.getMessage(), cause);
        Crashlytics.setString(CRASHLYTICS_TAG_EXCEPTION_SEVERITY, LogLevel.ERROR.name());
        Crashlytics.setString(CRASHLYTICS_TAG_ERROR_DETAIL_MESSAGE, tag + ": " + message); // Human readable error description
        logException(cause);
    }

    private static void logException(Throwable cause) {
        if (ExceptionUtils.isNetworkError(cause)) {
            Crashlytics.setBool("IS_NETWORK_ERROR", true);
        }
        Crashlytics.logException(cause);
    }

    private enum LogLevel {
        VERBOSE, DEBUG, INFO, WARNING, ERROR, CRASH, NETWORK_ERROR
    }

    private static boolean isDebugBuild() {
        return BuildConfig.DEBUG;
    }
}
