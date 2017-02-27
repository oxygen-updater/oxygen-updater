package com.arjanvlek.oxygenupdater.support;

import android.content.Intent;
import android.util.Log;

import com.arjanvlek.oxygenupdater.ApplicationData;
import com.arjanvlek.oxygenupdater.BuildConfig;

import java.io.CharArrayWriter;
import java.io.PrintWriter;

public class Logger {

    public static String TAG = "Logger";
    public static ApplicationData applicationData;


    public static void logVerbose(boolean upload, String tag, String message) {
        if (isDebugBuild()) {
            Log.v(tag, message);
            if(upload) uploadLog(LogLevel.VERBOSE, tag, message);
        }
    }

    public static void logVerbose(String tag, String message) {
        logVerbose(true, tag, message);
    }

    public static void logVerbose(String tag, String message, Throwable cause) {
        if (isDebugBuild()) {
            Log.v(tag, message, cause);
            uploadLog(LogLevel.VERBOSE, tag, message, cause);
        }
    }

    public static void logDebug(String tag, String message) {
        if (isDebugBuild()) {
            Log.d(tag, message);
            uploadLog(LogLevel.DEBUG, tag, message);
        }
    }

    public static void logDebug(String tag, String message, Throwable cause) {
        if (isDebugBuild()) {
            Log.d(tag, message, cause);
            uploadLog(LogLevel.DEBUG, tag, message, cause);
        }
    }

    public static void logInfo(String tag, String message) {
        if (isDebugBuild()) {
            Log.i(tag, message);
            uploadLog(LogLevel.INFO, tag, message);
        }
    }

    public static void logInfo(String tag, String message, Throwable cause) {
        if (isDebugBuild()) {
            Log.i(tag, message, cause);
            uploadLog(LogLevel.INFO, tag, message, cause);
        }
    }

    public static void logWarning(String tag, String message) {
        Log.w(tag, message);
        uploadLog(LogLevel.WARNING, tag, message);
    }

    public static void logWarning(String tag, String message, Throwable cause) {
        Log.w(tag, message, cause);
        uploadLog(LogLevel.WARNING, tag, message, cause);
    }

    public static void logError(boolean upload, String tag, String  message) {
        Log.e(tag, message);
        if(upload) uploadLog(LogLevel.ERROR, tag, message);
    }

    public static void logError(String tag, String message) {
        logError(true, tag, message);
    }

    public static void logError(boolean upload, String tag, String message, Throwable cause) {
        Log.e(tag, message, cause);
        if(upload) uploadLog(LogLevel.ERROR, tag, message, cause);
    }

    public static void logError(String tag, String message, Throwable cause) {
        logError(true, tag, message, cause);
    }

    private static void uploadLog(LogLevel logLevel, String tag, String message, Throwable cause) {
        CharArrayWriter charArrayWriter = new CharArrayWriter();
        PrintWriter printWriter = new PrintWriter(charArrayWriter);
        cause.printStackTrace(printWriter);
        printWriter.close();
        String stackTrace = charArrayWriter.toString();
        try {
            uploadLog(logLevel, tag, message + ":\n\n" + stackTrace);
        } catch (Throwable throwable) {
            logError(false, TAG, "", throwable);
        }
    }

    private static void uploadLog(LogLevel logLevel, String tag, String message) {

        if(applicationData != null) {
            Intent intent = new Intent(applicationData, LoggerService.class);
            intent.putExtra("event_type", logLevel.toString());
            intent.putExtra("tag", tag);
            intent.putExtra("message", message);
            applicationData.startService(intent);
        }
    }

    private enum LogLevel {
        VERBOSE, DEBUG, INFO, WARNING, ERROR
    }


    private static boolean isDebugBuild() {
        return BuildConfig.DEBUG;
    }
}
