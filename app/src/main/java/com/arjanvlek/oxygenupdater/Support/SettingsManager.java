package com.arjanvlek.oxygenupdater.Support;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;

import com.arjanvlek.oxygenupdater.MainActivity;

public class SettingsManager {

    //Offline cache properties
    public static final String PROPERTY_OFFLINE_UPDATE_NAME = "offlineUpdateName";
    public static final String PROPERTY_OFFLINE_UPDATE_DOWNLOAD_SIZE = "offlineUpdateDownloadSize";
    public static final String PROPERTY_OFFLINE_UPDATE_DESCRIPTION = "offlineUpdateDescription";
    public static final String PROPERTY_OFFLINE_FILE_NAME = "offlineFileName";
    public static final String PROPERTY_OFFLINE_UPDATE_INFORMATION_AVAILABLE = "offlineUpdateInformationAvailable";

    //Settings properties
    public static final String PROPERTY_APP_VERSION = "appVersion";
    public static final String PROPERTY_DEVICE = "device";
    public static final String PROPERTY_DEVICE_ID = "device_id";
    public static final String PROPERTY_UPDATE_METHOD = "update_method";
    public static final String PROPERTY_UPDATE_METHOD_ID = "update_method_id";
    public static final String PROPERTY_UPDATE_CHECKED_DATE = "update_checked_date";
    public static final String PROPERTY_RECEIVE_SYSTEM_UPDATE_NOTIFICATIONS = "receive_system_update_notifications";
    public static final String PROPERTY_RECEIVE_WARNING_NOTIFICATIONS = "recive_warning_notifications";
    public static final String PROPERTY_RECEIVE_NEW_DEVICE_NOTIFICATIONS = "receive_new_device_notifications";
    public static final String PROPERTY_SHOW_NEWS_MESSAGES = "show_news_messages";
    public static final String PROPERTY_SHOW_APP_UPDATE_MESSAGES = "show_app_update_messages";
    public static final String PROPERTY_SHOW_IF_SYSTEM_IS_UP_TO_DATE = "show_if_system_is_up_to_date";
    public static final String PROPERTY_SETUP_DONE = "setup_done";
    public static final String PROPERTY_IGNORE_UNSUPPORTED_DEVICE_WARNINGS = "ignore_unsupported_device_warnings";
    public static final String PROPERTY_DOWNLOAD_ID = "download_id";

    private Context context;

    public SettingsManager(Context context) {
        this.context = context;
    }



    public boolean checkIfSettingsAreValid() {
        return containsPreference(PROPERTY_DEVICE) && containsPreference(PROPERTY_UPDATE_METHOD) && getBooleanPreference(PROPERTY_SETUP_DONE);
    }

    public boolean receiveSystemUpdateNotifications() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(PROPERTY_RECEIVE_SYSTEM_UPDATE_NOTIFICATIONS, true);
    }

    public boolean receiveGenericNotifications() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(PROPERTY_RECEIVE_WARNING_NOTIFICATIONS, true);
    }

    public boolean receiveNewDeviceNotifications() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(PROPERTY_RECEIVE_NEW_DEVICE_NOTIFICATIONS, true);
    }

    public boolean showNewsMessages() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(PROPERTY_SHOW_NEWS_MESSAGES, true);
    }

    public boolean showAppUpdateMessages() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(PROPERTY_SHOW_APP_UPDATE_MESSAGES, true);
    }

    public boolean showIfSystemIsUpToDate() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(PROPERTY_SHOW_IF_SYSTEM_IS_UP_TO_DATE, true);
    }




    /**
     * Saves a String preference to SharedPreferences.
     * @param key Preference Key
     * @param value Preference Value
     */
    public void savePreference(String key, String value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, value);
        editor.apply();
    }

    /**
     * Saves an Integer preference to SharedPreferences.
     * @param key Preference Key
     * @param value Preference Value
     */
    public void saveIntPreference(String key, int value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    /**
     * Saves a Boolean preference to SharedPreferences.
     * @param key Preference Key
     * @param value Preference Value
     */
    public void saveBooleanPreference(String key, boolean value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    /**
     * Saves a Long preference to SharedPreferences.
     * @param key Preference Key
     * @param value Preference Value
     */
    public void saveLongPreference(String key, Long value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(key, value);
        editor.apply();
    }

    /**
     * Checks if a certain preference is set.
     * @param key Preference Key
     * @return Returns if the given key is stored in the preferences.
     */
    public boolean containsPreference(String key) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.contains(key);
    }

    /**
     * Deletes a preference
     * @param key Preference Key
     */
    public void deletePreference(String key) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(key);
        editor.apply();
    }


    /**
     * Get a String preference from Shared Preferences
     * @param key Preference Key
     * @return Preference Value
     */
    public String getPreference(String key) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString(key, null);
    }

    /**
     * Get a String preference from Shared Preferences
     * @param key Preference Key
     * @return Preference Value
     */
    public int getIntPreference(String key) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getInt(key, 0);
    }

    public boolean getBooleanPreference(String key) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getBoolean(key, false);
    }

    /**
     * Get a String preference from Shared Preferences
     * @param key Preference Key
     * @return Preference Value
     */
    public long getLongPreference(String key) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getLong(key, -1);
    }


    /**
     * Fetches the Google Cloud Messaging (GCM) preferences which are stored in a separate file.
     * @return Shared Preferences with GCM preferences.
     */
    public SharedPreferences getGCMPreferences() {
        return context.getSharedPreferences(MainActivity.class.getSimpleName(),
                Context.MODE_PRIVATE);
    }

    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    public int getAppVersion() {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException ignored) {
            return 0;
        }
    }

    /**
     * Checks if a device and update method have been set.
     * @return if the application is set up properly.
     */
    public boolean checkIfDeviceIsSet() {
        return containsPreference(PROPERTY_DEVICE) && containsPreference(PROPERTY_UPDATE_METHOD);
    }


    /**
     * Checks if the update information has been saved before so it can be viewed without a network connection
     * @return true or false.
     */
    public boolean checkIfCacheIsAvailable() {
        try {
            return  containsPreference(PROPERTY_OFFLINE_UPDATE_DOWNLOAD_SIZE) // update description may be null; should not be checked.
                    && containsPreference(PROPERTY_OFFLINE_UPDATE_NAME)
                    && containsPreference(PROPERTY_OFFLINE_FILE_NAME);
        } catch(Exception ignored) {
            return false;
        }
    }

    public void removePreference(String key) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(key);
        editor.apply();
    }

}
