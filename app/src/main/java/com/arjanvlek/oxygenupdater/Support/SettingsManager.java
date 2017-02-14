package com.arjanvlek.oxygenupdater.Support;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.HashSet;
import java.util.Set;

public class SettingsManager {

    //Offline cache properties
    public static final String PROPERTY_OFFLINE_UPDATE_NAME = "offlineUpdateName";
    public static final String PROPERTY_OFFLINE_UPDATE_DOWNLOAD_SIZE = "offlineUpdateDownloadSize";
    public static final String PROPERTY_OFFLINE_UPDATE_DESCRIPTION = "offlineUpdateDescription";
    public static final String PROPERTY_OFFLINE_FILE_NAME = "offlineFileName";
    public static final String PROPERTY_OFFLINE_UPDATE_INFORMATION_AVAILABLE = "offlineUpdateInformationAvailable";

    //Settings properties
    public static final String PROPERTY_DEVICE = "device";
    public static final String PROPERTY_DEVICE_ID = "device_id";
    public static final String PROPERTY_UPDATE_METHOD = "update_method";
    public static final String PROPERTY_UPDATE_METHOD_ID = "update_method_id";
    public static final String PROPERTY_UPDATE_CHECKED_DATE = "update_checked_date";
    public static final String PROPERTY_RECEIVE_SYSTEM_UPDATE_NOTIFICATIONS = "receive_system_update_notifications";
    public static final String PROPERTY_RECEIVE_GENERAL_NOTIFICATIONS = "recive_general_notifications";
    public static final String PROPERTY_RECEIVE_NEW_DEVICE_NOTIFICATIONS = "receive_new_device_notifications";
    public static final String PROPERTY_SHOW_NEWS_MESSAGES = "show_news_messages";
    public static final String PROPERTY_SHOW_APP_UPDATE_MESSAGES = "show_app_update_messages";
    public static final String PROPERTY_SHOW_IF_SYSTEM_IS_UP_TO_DATE = "show_if_system_is_up_to_date";
    public static final String PROPERTY_SETUP_DONE = "setup_done";
    public static final String PROPERTY_IGNORE_UNSUPPORTED_DEVICE_WARNINGS = "ignore_unsupported_device_warnings";
    public static final String PROPERTY_DOWNLOAD_ID = "download_id";

    // Notifications properties
    public static final String PROPERTY_NOTIFICATION_TOPIC = "notification_topic";

    private final Context context;

    public SettingsManager(Context context) {
        this.context = context;
    }


    public <T> T getPreference(String key) {
        return getPreference(key, null);
    }

    public <T> T getPreference(String key, T defaultValue) {
        SharedPreferences preferences = getSharedPreferences();
        return preferences.contains(key) ? (T) preferences.getAll().get(key) : defaultValue;
    }

    /**
     * Checks if a certain preference is set.
     * @param key Preference Key
     * @return Returns if the given key is stored in the preferences.
     */
    public boolean containsPreference(String key) {
        return getSharedPreferences().contains(key);
    }

    /**
     * Deletes a preference
     * @param key Preference Key
     */
    public void deletePreference(String key) {
        getSharedPreferencesEditor()
                .remove(key)
                .apply();
    }

    /**
     * Saves a preference to sharedPreferences
     * @param key Item key to later retrieve the item back
     * @param value Item that needs to be saved in shared preferences.
     */
    public void savePreference(String key, Object value) {
        if (value instanceof String) saveStringPreference(key, (String) value);
        else if (value instanceof Integer) saveIntPreference(key, (Integer) value);
        else if (value instanceof Boolean) saveBooleanPreference(key, (Boolean) value);
        else if (value instanceof Long) saveLongPreference(key, (Long) value);
        else if (value instanceof Float) saveFloatPreference(key, (Float) value);
        else if (value instanceof Set<?>)
            saveStringSetPreference(key, convertToStringSet((Set<?>) value));
        else
            throw new UnsupportedOperationException("SettingsManager can't save preferences of the class type " + value.getClass().getName() + "!");
    }

    // Helper methods for the app

    /**
     * Checks if a device and update method have been set.
     * @return if the user has chosen a device and an update method.
     */
    public boolean checkIfSetupScreenIsFilledIn() {
        return containsPreference(PROPERTY_DEVICE) && containsPreference(PROPERTY_UPDATE_METHOD);
    }


    /**
     * Checks if a user has completed the initial setup screen.
     * @return if the user has completed the setup screen.
     */
    public boolean checkIfSetupScreenHasBeenCompleted() {
        return containsPreference(PROPERTY_DEVICE) && containsPreference(PROPERTY_UPDATE_METHOD) && (boolean) getPreference(PROPERTY_SETUP_DONE);
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

    // Helper methods

    private SharedPreferences getSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    private SharedPreferences.Editor getSharedPreferencesEditor() {
        return getSharedPreferences().edit();
    }

    /**
     * Saves a String preference to SharedPreferences.
     * @param key Preference Key
     * @param value Preference Value
     */
    private void saveStringPreference(String key, String value) {
        getSharedPreferencesEditor()
                .putString(key, value)
                .apply();
    }

    /**
     * Saves an Integer preference to SharedPreferences.
     * @param key Preference Key
     * @param value Preference Value
     */
    private void saveIntPreference(String key, int value) {
        getSharedPreferencesEditor()
                .putInt(key, value)
                .apply();
    }

    /**
     * Saves a Boolean preference to SharedPreferences.
     * @param key Preference Key
     * @param value Preference Value
     */
    private void saveBooleanPreference(String key, boolean value) {
        getSharedPreferencesEditor()
                .putBoolean(key, value)
                .apply();
    }

    /**
     * Saves a Long preference to SharedPreferences.
     * @param key Preference Key
     * @param value Preference Value
     */
    private void saveLongPreference(String key, Long value) {
        getSharedPreferencesEditor()
                .putLong(key, value)
                .apply();
    }

    /**
     * Saves a Long preference to SharedPreferences.
     * @param key Preference Key
     * @param value Preference Value
     */
    private void saveFloatPreference(String key, Float value) {
        getSharedPreferencesEditor()
                .putFloat(key, value)
                .apply();
    }

    /**
     * Saves a set of strings as preferences in SharedPreferences.
     * @param key Preference Key
     * @param values Preference Values
     */
    private void saveStringSetPreference(String key, Set<String> values) {
        getSharedPreferencesEditor()
                .putStringSet(key, values)
                .apply();
    }

    private Set<String> convertToStringSet(Set<?> values) {
        Set<String> results = new HashSet<>();
        for(Object o : values) {
            if (o instanceof String) results.add((String) o);
            else throw new IllegalArgumentException("SettingsManager can only save preferences of Sets that contain Strings.");
        }
        return results;
    }

}
