package com.arjanvlek.oxygenupdater.Support;

import android.os.Build;
import android.util.Log;

import com.arjanvlek.oxygenupdater.BuildConfig;
import com.arjanvlek.oxygenupdater.Model.SystemVersionProperties;
import com.arjanvlek.oxygenupdater.Server.ServerConnector;

import org.joda.time.LocalDateTime;
import org.json.JSONObject;

import java.io.CharArrayWriter;
import java.io.PrintWriter;

import static com.arjanvlek.oxygenupdater.ApplicationData.NO_OXYGEN_OS;

public class Logger {

    public static String TAG = "Logger";


    public static void logVerbose(String tag, String message) {
        if (isDebugBuild()) {
            Log.v(tag, message);
            storeLog(LogLevel.VERBOSE, tag, message);
        }
    }

    public static void logVerbose(String tag, String message, Throwable cause) {
        if (isDebugBuild()) {
            Log.v(tag, message, cause);
            storeLog(LogLevel.VERBOSE, tag, message, cause);
        }
    }

    public static void logDebug(String tag, String message) {
        if (isDebugBuild()) {
            Log.d(tag, message);
            storeLog(LogLevel.DEBUG, tag, message);
        }
    }

    public static void logDebug(String tag, String message, Throwable cause) {
        if (isDebugBuild()) {
            Log.d(tag, message, cause);
            storeLog(LogLevel.DEBUG, tag, message, cause);
        }
    }

    public static void logInfo(String tag, String message) {
        if (isDebugBuild()) {
            Log.i(tag, message);
            storeLog(LogLevel.INFO, tag, message);
        }
    }

    public static void logInfo(String tag, String message, Throwable cause) {
        if (isDebugBuild()) {
            Log.i(tag, message, cause);
            storeLog(LogLevel.INFO, tag, message, cause);
        }
    }

    public static void logWarning(String tag, String message) {
        Log.w(tag, message);
        storeLog(LogLevel.WARNING, tag, message);
    }

    public static void logWarning(String tag, String message, Throwable cause) {
        Log.w(tag, message, cause);
        storeLog(LogLevel.WARNING, tag, message, cause);
    }

    public static void logError(String tag, String message) {
        Log.e(tag, message);
        storeLog(LogLevel.ERROR, tag, message);
    }

    public static void logError(String tag, String message, Throwable cause) {
        Log.e(tag, message, cause);
        storeLog(LogLevel.ERROR, tag, message, cause);
    }

    private static void storeLog(LogLevel logLevel, String tag, String message, Throwable cause) {
        CharArrayWriter charArrayWriter = new CharArrayWriter();
        PrintWriter printWriter = new PrintWriter(charArrayWriter);
        cause.printStackTrace(printWriter);
        printWriter.close();
        String stackTrace = charArrayWriter.toString();
        storeLog(logLevel, tag, message + ":\n\n" + stackTrace);
    }

    private static void storeLog(LogLevel logLevel, String tag, String message) {
        ServerConnector serverConnector = new ServerConnector(null);
        SystemVersionProperties systemVersionProperties = new SystemVersionProperties();

        serverConnector.getDevices((devices) -> {
            boolean deviceIsSupported = SupportedDeviceManager.isSupportedDevice(systemVersionProperties, devices);

            try {
                JSONObject logData = new JSONObject();
                logData.put("incident_type", logLevel.toString());
                logData.put("device_is_supported", deviceIsSupported);
                logData.put("device", Build.BRAND + " " + Build.PRODUCT);
                logData.put("os_version", !systemVersionProperties.getOxygenOSVersion().equals(NO_OXYGEN_OS) ? systemVersionProperties.getOxygenOSVersion() : Build.VERSION.RELEASE);
                logData.put("error_message", tag + " : " + message);
                logData.put("event_date", LocalDateTime.now().toString());
                serverConnector.log(logData, (logResult) -> {
                    if (logResult != null && !logResult.isSuccess())
                        Log.e(TAG, "Error uploading log to server:" + logResult.getErrorMessage());
                });
            } catch (Exception e) {
                Log.e(TAG, "Error preparing log data for uploading to the server:", e);
            }
        });
    }

    private enum LogLevel {
        VERBOSE, DEBUG, INFO, WARNING, ERROR
    }


    private static boolean isDebugBuild() {
        return BuildConfig.DEBUG;
    }
}
