package com.arjanvlek.oxygenupdater.internal.root;

import android.os.AsyncTask;

import com.arjanvlek.oxygenupdater.internal.logger.Logger;

import eu.chainfire.libsuperuser.Shell;
import java8.util.function.Consumer;

public class RootAccessChecker {

    private static boolean hasCheckedOnce = false;
    private static boolean isRooted = false;

    /**
     * Checks if the device is rooted.
     *
     * Callback gets as argument whether or not the device is rooted. The result of Shell.SU.available is cached.
     */
    public static void checkRootAccess(Consumer<Boolean> callback) {
        if(hasCheckedOnce) {
            callback.accept(isRooted);
            return;
        }

        new RootCheckerImpl(callback).execute();
    }

    private static class RootCheckerImpl extends AsyncTask<Void, Void, Boolean> {

        private final Consumer<Boolean> callback;

        RootCheckerImpl(Consumer<Boolean> callback) {
            this.callback = callback;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                Thread.sleep(2000); // Give the user the time to read what's happening.
                return Shell.SU.available();
            } catch (Exception e) {
                Logger.logError("ApplicationData", "Failed to check for root access", e);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            hasCheckedOnce = true;
            isRooted = result;
            callback.accept(isRooted);
        }
    }
}
