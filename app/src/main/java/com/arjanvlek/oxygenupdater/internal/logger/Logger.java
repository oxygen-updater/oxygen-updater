package com.arjanvlek.oxygenupdater.internal.logger;

import android.util.Log;

import com.arjanvlek.oxygenupdater.BuildConfig;
import com.arjanvlek.oxygenupdater.internal.ExceptionUtils;
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

    public static void logWarning(String tag, String message) {
        Crashlytics.log(Log.WARN, tag, message);
    }

    public static void logWarning(String tag, String message, Throwable cause) {
        Crashlytics.setString(CRASHLYTICS_TAG_EXCEPTION_SEVERITY, LogLevel.WARNING.name());
        Crashlytics.setString(CRASHLYTICS_TAG_ERROR_DETAIL_MESSAGE, tag + ": " + message);
        logException(cause);
    }

    public static void logError(String tag, String message) {
        Crashlytics.log(Log.ERROR, tag, message);
    }

    public static void logError(String tag, String message, Throwable cause) {
        Crashlytics.setString(CRASHLYTICS_TAG_EXCEPTION_SEVERITY, LogLevel.ERROR.name());
        Crashlytics.setString(CRASHLYTICS_TAG_ERROR_DETAIL_MESSAGE, tag + ": " + message);
        logException(cause);
    }

    public static void logNetworkError(String tag, String message) {
        Crashlytics.setBool("IS_NETWORK_ERROR", true);
        Crashlytics.log(Log.WARN, tag, message);
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
