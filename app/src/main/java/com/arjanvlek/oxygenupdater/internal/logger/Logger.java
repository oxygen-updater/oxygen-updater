package com.arjanvlek.oxygenupdater.internal.logger;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PersistableBundle;
import android.util.Log;

import com.arjanvlek.oxygenupdater.ApplicationData;
import com.arjanvlek.oxygenupdater.BuildConfig;
import com.arjanvlek.oxygenupdater.internal.ExceptionUtils;
import com.arjanvlek.oxygenupdater.internal.Utils;
import com.arjanvlek.oxygenupdater.settings.SettingsManager;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class Logger {

    private static final String TAG = "Logger";
    private static final String ERROR_FILE = "error.txt";
    public static Context context;
    private static final int JOB_ID_MIN = 395819384;
    private static final int JOB_ID_MAX = 395899384;

    public static void init(Context context) {
        context = context;

        try {
            // If the application previously crashed, upload the crash log to the server.
            File errorFile = new File(context.getFilesDir(), ERROR_FILE);
            if (errorFile.exists()) {
                StringBuilder stackTrace = new StringBuilder();
                BufferedReader reader = new BufferedReader(new FileReader(errorFile));

                String line;

                while ((line = reader.readLine()) != null) {
                    stackTrace.append(line);
                    stackTrace.append(System.getProperty("line.separator"));
                }

                uploadLog(LogLevel.CRASH, "Application", "The application has crashed:\n\n" + stackTrace.toString());

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

    public static void logNetworkError(boolean upload, String tag, String message) {
        Log.e(tag, message);

        // Only upload network errors if there is a network connection, because otherwise we already know the cause of the error...
        if (upload && context != null && Utils.checkNetworkConnection(context)) {
            uploadLog(LogLevel.NETWORK_ERROR, tag, message);
        }
    }

    public static void logApplicationCrash(Context context, Throwable cause) {
        // Register an application crash. This will be uploaded when the app is started the next time.
        try {
            File errorFile = new File(context.getFilesDir(), ERROR_FILE);

            BufferedWriter writer = new BufferedWriter(new FileWriter(errorFile));
            writer.write(stacktraceToString(cause));
            writer.flush();
            Log.e("Application", "The application has crashed: ", cause);
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
            // Don't upload network errors if there is no network, because then we know the cause of the error...
            if (ExceptionUtils.isNetworkError(cause) && (context == null || !Utils.checkNetworkConnection(context))) {
                return;
            }

            uploadLog(ExceptionUtils.isNetworkError(cause) ? LogLevel.NETWORK_ERROR : logLevel, tag, message + ":\n\n" + stacktraceToString(cause));
        } catch (Throwable throwable) {
            logError(false, TAG, "An error has occurred, but it can't be uploaded: \n\n", cause);
        }
    }

    private static void uploadLog(LogLevel logLevel, String tag, String message) {

        if (context != null) {
            SettingsManager settingsManager = new SettingsManager(context);

            if (settingsManager.getPreference(SettingsManager.PROPERTY_UPLOAD_LOGS, true) && !isRecursiveCallToLogger()) {
                try {
                    scheduleLogUploadTask(context, buildLogUploadData(logLevel.toString(), tag, message));
                } catch (Exception e) {
                    // The logger should never be the cause of an application crash. Better no logging than users facing a crash!
                    logError(false, TAG, "Failed to schedule log upload", e);
                }
            }
        }
    }

    private static PersistableBundle buildLogUploadData(String eventType, String tag, String message) {

        PersistableBundle logData = new PersistableBundle();

        logData.putString(LogUploadService.DATA_EVENT_TYPE, eventType);
        logData.putString(LogUploadService.DATA_TAG, tag);
        logData.putString(LogUploadService.DATA_MESSAGE, message);
        logData.putString(LogUploadService.DATA_EVENT_DATE, LocalDateTime.now(DateTimeZone.forID("Europe/Amsterdam")).toString());

        return logData;
    }

    private static void scheduleLogUploadTask(Context context, PersistableBundle logData) throws Exception {
        Random rn = new SecureRandom();

        int range = JOB_ID_MAX - JOB_ID_MIN + 1;
        int jobId = rn.nextInt(range) + JOB_ID_MIN;

        JobInfo.Builder task = new JobInfo.Builder(jobId, new ComponentName(context, LogUploadService.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setRequiresDeviceIdle(false)
                .setRequiresCharging(false)
                .setMinimumLatency(1000)
                .setExtras(logData)
                .setBackoffCriteria(3000, JobInfo.BACKOFF_POLICY_EXPONENTIAL);

        if (Build.VERSION.SDK_INT >= 26) {
            task.setRequiresBatteryNotLow(false);
            task.setRequiresStorageNotLow(false);
        }

        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        int resultCode = Objects.requireNonNull(scheduler).schedule(task.build());

        if (resultCode != JobScheduler.RESULT_SUCCESS) {
            logWarning(false, TAG, "Log upload not scheduled. Exit code of scheduler: " + resultCode);
        }
    }

    private enum LogLevel {
        VERBOSE, DEBUG, INFO, WARNING, ERROR, CRASH, NETWORK_ERROR
    }

    // Breaks infinite recursion if the uploading of a log causes another log to get uploaded.
    private static boolean isRecursiveCallToLogger() {
        StackTraceElement[] traceElements = Thread.currentThread().getStackTrace();
        List<String> loggerTraces = new ArrayList<>();

        for (StackTraceElement elem : traceElements) {
            if (elem.getClassName().equals(Logger.class.getName())) {
                loggerTraces.add(elem.toString());
            }
        }

        return loggerTraces.size() > 10;
    }

    private static boolean isDebugBuild() {
        return BuildConfig.DEBUG;
    }
}
