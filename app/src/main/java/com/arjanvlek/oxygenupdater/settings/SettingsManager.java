package com.arjanvlek.oxygenupdater.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.arjanvlek.oxygenupdater.internal.logger.Logger;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class SettingsManager {

    //Settings properties
    public static final String PROPERTY_DEVICE = "device";
    public static final String PROPERTY_DEVICE_ID = "device_id";
    public static final String PROPERTY_UPDATE_METHOD = "update_method";
    public static final String PROPERTY_UPDATE_METHOD_ID = "update_method_id";
    public static final String PROPERTY_UPDATE_CHECKED_DATE = "update_checked_date";
    public static final String PROPERTY_RECEIVE_SYSTEM_UPDATE_NOTIFICATIONS = "receive_system_update_notifications";
    public static final String PROPERTY_RECEIVE_GENERAL_NOTIFICATIONS = "recive_general_notifications"; // Typo can't be fixed due to older versions of the app being released with it.
    public static final String PROPERTY_RECEIVE_NEWS_NOTIFICATIONS = "receive_news_notifications";
    public static final String PROPERTY_RECEIVE_NEW_DEVICE_NOTIFICATIONS = "receive_new_device_notifications";
    public static final String PROPERTY_SHOW_NEWS_MESSAGES = "show_news_messages";
    public static final String PROPERTY_SHOW_APP_UPDATE_MESSAGES = "show_app_update_messages";
    public static final String PROPERTY_SHOW_IF_SYSTEM_IS_UP_TO_DATE = "show_if_system_is_up_to_date"; // Used between 1.0.0 and 2.4.5. Replaced with ADVANCED_MODE but needed for migrations.
    public static final String PROPERTY_ADVANCED_MODE = "advanced_mode";
    public static final String PROPERTY_SETUP_DONE = "setup_done";
    public static final String PROPERTY_IGNORE_UNSUPPORTED_DEVICE_WARNINGS = "ignore_unsupported_device_warnings";
    public static final String PROPERTY_DOWNLOAD_ID = "download_id";
    public static final String PROPERTY_DOWNLOADER_STATE = "downloader_state";
    public static final String PROPERTY_DOWNLOAD_PROGRESS = "download_progress";
    public static final String PROPERTY_UPLOAD_LOGS = "upload_logs";
    public static final String PROPERTY_ADDITIONAL_ZIP_FILE_PATH = "additional_zip_file_path";
    public static final String PROPERTY_BACKUP_DEVICE = "backupDevice";
    public static final String PROPERTY_KEEP_DEVICE_ROOTED = "keepDeviceRooted";
    public static final String PROPERTY_WIPE_CACHE_PARTITION = "wipeCachePartition";
    public static final String PROPERTY_REBOOT_AFTER_INSTALL = "rebootAfterInstall";
    public static final String PROPERTY_VERIFY_SYSTEM_VERSION_ON_REBOOT = "verifySystemVersion";
    public static final String PROPERTY_OLD_SYSTEM_VERSION = "oldSystemVersion";
    public static final String PROPERTY_TARGET_SYSTEM_VERSION = "targetSystemVersion";
    public static final String PROPERTY_INSTALLATION_ID = "installationId";
    public static final String PROPERTY_IS_AUTOMATIC_INSTALLATION_ENABLED = "isAutomaticInstallationEnabled";
    public static final String PROPERTY_LAST_NEWS_AD_SHOWN = "lastNewsAdShown";

    //Offline cache properties
    public static final String PROPERTY_OFFLINE_ID = "offlineId";
    public static final String PROPERTY_OFFLINE_UPDATE_NAME = "offlineUpdateName";
    public static final String PROPERTY_OFFLINE_UPDATE_DOWNLOAD_SIZE = "offlineUpdateDownloadSize";
    public static final String PROPERTY_OFFLINE_UPDATE_DESCRIPTION = "offlineUpdateDescription";
    public static final String PROPERTY_OFFLINE_FILE_NAME = "offlineFileName";
    public static final String PROPERTY_OFFLINE_DOWNLOAD_URL = "offlineDownloadUrl";
    public static final String PROPERTY_OFFLINE_UPDATE_INFORMATION_AVAILABLE = "offlineUpdateInformationAvailable";
    public static final String PROPERTY_OFFLINE_IS_UP_TO_DATE = "offlineIsUpToDate";

    // Notifications properties
    public static final String PROPERTY_NOTIFICATION_TOPIC = "notification_topic";
    public static final String PROPERTY_NOTIFICATION_DELAY_IN_SECONDS = "notification_delay_in_seconds";

    // IAB properties
    public static final String PROPERTY_AD_FREE = "34ejrtgalsJKDf;awljker;2k3jrpwosKjdfpio24uj3tp3oiwfjdscPOKj";

    private final Context context;
    private static final String TAG = "SettingsManager";

    public SettingsManager(Context context) {
        this.context = context;
    }

    public synchronized <T> T getPreference(String key, T defaultValue) {
        SharedPreferences preferences = getSharedPreferences();
        return preferences == null ? defaultValue : preferences.contains(key) ? (T) preferences.getAll().get(key) : defaultValue;
    }

    /**
     * Checks if a certain preference is set.
     * @param key Preference Key
     * @return Returns if the given key is stored in the preferences.
     */
    public synchronized boolean containsPreference(String key) {
        return getSharedPreferences() != null && getSharedPreferences().contains(key);
    }

    /**
     * Deletes a preference
     * @param key Preference Key
     */
    public synchronized void deletePreference(String key) {
        SharedPreferences.Editor editor = getSharedPreferencesEditor();

        if(editor != null) {
            editor.remove(key)
                    .apply();
        }
    }

    /**
     * Saves a preference to sharedPreferences
     * @param key Item key to later retrieve the item back
     * @param value Item that needs to be saved in shared preferences.
     */
    public synchronized void savePreference(String key, Object value) {

        try {
            SharedPreferences.Editor editor = getSharedPreferencesEditor();

            if (editor == null) {
                return;
            }

            if (value == null) {
                editor.putString(key, null);
            } else if (value instanceof String) {
                editor.putString(key, value.toString());
            } else if (value instanceof Boolean) {
                editor.putBoolean(key, (boolean)value);
            } else if (value instanceof Long) {
                editor.putLong(key, (long) value);
            } else if (value instanceof Float) {
                editor.putFloat(key, (float) value);
            } else if (value instanceof Integer ) {
                editor.putInt(key, (int) value);
            } else if (value instanceof Collection) {
                Collection values = (Collection) value;
                Set<String> valuesSet = new HashSet<>();
                for (Object item : values) {
                    if (item != null) {
                        valuesSet.add(item.toString());
                    }
                }
                editor.putStringSet(key, valuesSet);
            }
            // Let the editor apply the edited preferences as usual
            editor.apply();
        } catch (Exception e) {
            // If this doesn't work, try to use String instead.
            getSharedPreferencesEditor()
                    .putString(key, value.toString())
                    .apply();
            Logger.logError(TAG, "Failed to save preference with key " + key + " and value " + value + " . Defaulting to String value! " + e.getMessage(), e);
        }
    }

    // Helper methods for the app

    /**
     * Checks if a device and update method have been set.
     * @return if the user has chosen a device and an update method.
     */
    public boolean checkIfSetupScreenIsFilledIn() {
        return getPreference(PROPERTY_DEVICE_ID, -1L) != -1L && getPreference(PROPERTY_UPDATE_METHOD_ID, -1L) != -1L;
    }


    /**
     * Checks if a user has completed the initial setup screen.
     * This means the user has filled it in and also pressed the "Start app" button at the very last screen.
     * @return if the user has completed the setup screen.
     */
    public boolean checkIfSetupScreenHasBeenCompleted() {
        return checkIfSetupScreenIsFilledIn() && getPreference(PROPERTY_SETUP_DONE, false);
    }

    /**
     * Checks if the update information has been saved before so it can be viewed without a network connection
     * @return true or false.
     */
    public boolean checkIfOfflineUpdateDataIsAvailable() {
        try {
            return containsPreference(PROPERTY_OFFLINE_UPDATE_DOWNLOAD_SIZE)
                    && containsPreference(PROPERTY_OFFLINE_UPDATE_NAME)
                    && containsPreference(PROPERTY_OFFLINE_FILE_NAME);
        } catch(Exception ignored) {
            return false;
        }
    }

    // Helper methods

    private SharedPreferences getSharedPreferences() {
        if(context == null) return null;
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    private SharedPreferences.Editor getSharedPreferencesEditor() {
        if(getSharedPreferences() == null) return null;
        return getSharedPreferences().edit();
    }

}
