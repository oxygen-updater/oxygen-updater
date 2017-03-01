package com.arjanvlek.oxygenupdater.support;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.arjanvlek.oxygenupdater.ApplicationData;
import com.arjanvlek.oxygenupdater.BuildConfig;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;

public class Logger {

    public static final String TAG = "Logger";
    private static final String ERROR_FILE = "error.txt";
    public static ApplicationData applicationData;

    public static void init (ApplicationData data) {
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

                logError("ApplicationData", "The application has crashed: " + stackTrace.toString());
            }
        } catch (Exception e) {
            logError(false, TAG, "Failed to read crashdump: ", e);

        }
    }


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

    public static void logApplicationCrash(Context context, Throwable cause) {
        try {
            File errorFile = new File(context.getFilesDir(), ERROR_FILE);

            if(errorFile.exists()) {
                errorFile.delete();
            }

            BufferedWriter writer = new BufferedWriter(new FileWriter(errorFile));
            writer.write(stacktraceToString(cause));
            writer.flush();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void logError(String tag, String message, Throwable cause) {
        logError(true, tag, message, cause);
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
            logError(false, TAG, "", throwable);
        }
    }

    private static void uploadLog(LogLevel logLevel, String tag, String message) {

        if(applicationData != null) {
            NetworkConnectionManager nm = new NetworkConnectionManager(applicationData.getApplicationContext());
            if(nm.checkNetworkConnection()) {
                Intent intent = new Intent(applicationData, LoggerService.class);
                intent.putExtra("event_type", logLevel.toString());
                intent.putExtra("tag", tag);
                intent.putExtra("message", message);
                applicationData.startService(intent);
            }
        }
    }

    private enum LogLevel {
        VERBOSE, DEBUG, INFO, WARNING, ERROR
    }


    private static boolean isDebugBuild() {
        return BuildConfig.DEBUG;
    }
}
