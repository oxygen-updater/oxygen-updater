package com.arjanvlek.oxygenupdater.internal.logger;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.arjanvlek.oxygenupdater.ApplicationData;
import com.arjanvlek.oxygenupdater.BuildConfig;
import com.arjanvlek.oxygenupdater.internal.Utils;
import com.arjanvlek.oxygenupdater.settings.SettingsManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;

public class Logger {

    private static final String TAG = "Logger";
    private static final String ERROR_FILE = "error.txt";
    public static ApplicationData applicationData;

    public static void init(ApplicationData data) {
        applicationData = data;

        try {
            File errorFile = new File(data.getFilesDir(), ERROR_FILE);
            if (errorFile.exists()) {
                StringBuilder stackTrace = new StringBuilder();
                BufferedReader reader = new BufferedReader(new FileReader(errorFile));

                String line;

                while ((line = reader.readLine()) != null) {
                    stackTrace.append(line);
                    stackTrace.append(System.getProperty("line.separator"));
                }

                uploadLog(LogLevel.CRASH, "ApplicationData", "The application has crashed:\n\n" + stackTrace.toString());

                //noinspection ResultOfMethodCallIgnored
                errorFile.delete();
            }
        } catch (Exception e) {
            logError(false, TAG, "Failed to read crash dump: ", e);
        }
    }

    public static void logVerbose(String tag, String message) {
        logVerbose(true, tag, message);
    }


    public static void logVerbose(boolean uploadLog, String tag, String message) {
        if (isDebugBuild()) {
            Log.v(tag, message);

            if(uploadLog) {
                //uploadLog(LogLevel.VERBOSE, tag, message);
            }
        }
    }

    public static void logVerbose(String tag, String message, Throwable cause) {
        logVerbose(true, tag, message, cause);
    }

    public static void logVerbose(boolean uploadLog, String tag, String message, Throwable cause) {
        if (isDebugBuild()) {
            Log.v(tag, message, cause);
            if(uploadLog) {
                uploadLog(LogLevel.VERBOSE, tag, message, cause);
            }
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

    public static void logWarning(boolean upload, String tag, String message) {
        Log.w(tag, message);
        if (upload) uploadLog(LogLevel.WARNING, tag, message);
    }


    public static void logWarning(boolean upload, String tag, String message, Throwable cause) {
        Log.w(tag, message, cause);
        if (upload) uploadLog(LogLevel.WARNING, tag, message, cause);
    }

    public static void logError(boolean upload, String tag, String message) {
        Log.e(tag, message);
        if (upload) uploadLog(LogLevel.ERROR, tag, message);
    }

    public static void logError(String tag, String message) {
        logError(true, tag, message);
    }

    public static void logError(boolean upload, String tag, String message, Throwable cause) {
        Log.e(tag, message, cause);
        if (upload) uploadLog(LogLevel.ERROR, tag, message, cause);
    }

    public static void logError(String tag, String message, Throwable cause) {
        logError(true, tag, message, cause);
    }

    public static void logApplicationCrash(Context context, Throwable cause) {
        try {
            File errorFile = new File(context.getFilesDir(), ERROR_FILE);

            BufferedWriter writer = new BufferedWriter(new FileWriter(errorFile));
            writer.write(stacktraceToString(cause));
            writer.flush();
            Log.e("ApplicationData", "The application has crashed: ", cause);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String stacktraceToString(Throwable throwable) {
        CharArrayWriter charArrayWriter = new CharArrayWriter();
        PrintWriter printWriter = new PrintWriter(charArrayWriter);
        throwable.printStackTrace(printWriter);
        printWriter.close();
        return charArrayWriter.toString();
    }

    private static void uploadLog(LogLevel logLevel, String tag, String message, Throwable cause) {
        try {
            uploadLog(logLevel, tag, message + ":\n\n" + stacktraceToString(cause));
        } catch (Throwable throwable) {
            logError(false, TAG, "An error has occurred, but it can't be uploaded: \n\n", cause);
        }
    }

    private static void uploadLog(LogLevel logLevel, String tag, String message) {

        if (applicationData != null) {
            SettingsManager settingsManager = new SettingsManager(applicationData);

            if (settingsManager.getPreference(SettingsManager.PROPERTY_UPLOAD_LOGS, true) && Utils.checkNetworkConnection(applicationData)) {
                Intent intent = new Intent(applicationData, LogUploadService.class);
                intent.putExtra("event_type", logLevel.toString());
                intent.putExtra("tag", tag);
                intent.putExtra("message", message);
                applicationData.startService(intent);
            }
        }
    }

    private enum LogLevel {
        VERBOSE, DEBUG, INFO, WARNING, ERROR, CRASH
    }


    private static boolean isDebugBuild() {
        return BuildConfig.DEBUG;
    }
}
