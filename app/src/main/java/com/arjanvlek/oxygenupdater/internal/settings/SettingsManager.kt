package com.arjanvlek.oxygenupdater.internal.settings

import android.content.SharedPreferences
import androidx.core.content.edit
import com.arjanvlek.oxygenupdater.utils.Logger.logError
import org.koin.java.KoinJavaComponent.inject

class SettingsManager {

    private val sharedPreferences: SharedPreferences by inject(SharedPreferences::class.java)

    @Suppress("UNCHECKED_CAST")
    fun <T> getPreference(key: String?, defaultValue: T): T = sharedPreferences.let {
        when {
            it.contains(key) -> it.all[key] as T
            else -> defaultValue
        }
    }

    /**
     * Checks if a certain preference is set.
     *
     * @param key Preference Key
     *
     * @return Returns if the given key is stored in the preferences.
     */
    fun containsPreference(key: String?) = sharedPreferences.contains(key)

    /**
     * Deletes a preference
     *
     * @param key Preference Key
     */
    fun deletePreference(key: String?) = sharedPreferences.edit { remove(key) }

    /**
     * Saves a preference to sharedPreferences
     *
     * @param key   Item key to later retrieve the item back
     * @param value Item that needs to be saved in shared preferences.
     */
    fun savePreference(key: String?, value: Any?) = try {
        sharedPreferences.edit {
            when (value) {
                null -> putString(key, null)
                is String -> putString(key, value.toString())
                is Boolean -> putBoolean(key, value)
                is Long -> putLong(key, value)
                is Float -> putFloat(key, value)
                is Int -> putInt(key, value)
                is Collection<*> -> HashSet<String>().let { valuesSet ->
                    value.forEach {
                        if (it != null) {
                            valuesSet.add(it.toString())
                        }
                    }

                    putStringSet(key, valuesSet)
                }
            }
        }
    } catch (e: Exception) {
        logError(TAG, "Failed to save preference with key $key and value $value. Defaulting to String value! ${e.message}", e)

        // If this doesn't work, try to use String instead.
        sharedPreferences.edit {
            putString(key, value.toString())
        }
    }

    /**
     * Checks if a device and update method have been set.
     *
     * @return if the user has chosen a device and an update method.
     */
    fun checkIfSetupScreenIsFilledIn() = getPreference(PROPERTY_DEVICE_ID, -1L) != -1L
            && getPreference(PROPERTY_UPDATE_METHOD_ID, -1L) != -1L

    /**
     * Checks if a user has completed the initial setup screen. This means the user has filled it in
     * and also pressed the "Start app" button at the very last screen.
     *
     * @return if the user has completed the setup screen.
     */
    fun checkIfSetupScreenHasBeenCompleted() = checkIfSetupScreenIsFilledIn()
            && getPreference(PROPERTY_SETUP_DONE, false)

    /**
     * Checks if the update information has been saved before so it can be viewed without a network
     * connection
     *
     * @return true or false.
     */
    fun checkIfOfflineUpdateDataIsAvailable() = try {
        containsPreference(PROPERTY_OFFLINE_UPDATE_DOWNLOAD_SIZE)
                && containsPreference(PROPERTY_OFFLINE_UPDATE_NAME)
                && containsPreference(PROPERTY_OFFLINE_FILE_NAME)
    } catch (ignored: Exception) {
        false
    }

    companion object {
        private const val TAG = "SettingsManager"

        // Settings properties
        const val PROPERTY_DEVICE = "device"
        const val PROPERTY_DEVICE_ID = "device_id"
        const val PROPERTY_UPDATE_METHOD = "update_method"
        const val PROPERTY_UPDATE_METHOD_ID = "update_method_id"
        const val PROPERTY_UPDATE_CHECKED_DATE = "update_checked_date"
        const val PROPERTY_RECEIVE_SYSTEM_UPDATE_NOTIFICATIONS = "receive_system_update_notifications"
        const val PROPERTY_RECEIVE_GENERAL_NOTIFICATIONS = "recive_general_notifications" // Typo can't be fixed due to older versions of the app being released with it.
        const val PROPERTY_RECEIVE_NEWS_NOTIFICATIONS = "receive_news_notifications"
        const val PROPERTY_RECEIVE_NEW_DEVICE_NOTIFICATIONS = "receive_new_device_notifications"
        const val PROPERTY_SHOW_NEWS_MESSAGES = "show_news_messages"
        const val PROPERTY_SHOW_APP_UPDATE_MESSAGES = "show_app_update_messages"
        const val PROPERTY_SHOW_IF_SYSTEM_IS_UP_TO_DATE = "show_if_system_is_up_to_date" // Used between 1.0.0 and 2.4.5. Replaced with ADVANCED_MODE but needed for migrations.
        const val PROPERTY_ADVANCED_MODE = "advanced_mode"
        const val PROPERTY_SETUP_DONE = "setup_done"
        const val PROPERTY_IGNORE_UNSUPPORTED_DEVICE_WARNINGS = "ignore_unsupported_device_warnings"
        const val PROPERTY_DOWNLOAD_BYTES_DONE = "download_bytes_done"
        const val PROPERTY_DOWNLOAD_ID = "download_id"
        const val PROPERTY_SHARE_ANALYTICS_AND_LOGS = "upload_logs" // Value cannot be changed - is from older version where it was called 'upload_logs'
        const val PROPERTY_ADDITIONAL_ZIP_FILE_PATH = "additional_zip_file_path"
        const val PROPERTY_BACKUP_DEVICE = "backupDevice"
        const val PROPERTY_KEEP_DEVICE_ROOTED = "keepDeviceRooted"
        const val PROPERTY_WIPE_CACHE_PARTITION = "wipeCachePartition"
        const val PROPERTY_REBOOT_AFTER_INSTALL = "rebootAfterInstall"
        const val PROPERTY_VERIFY_SYSTEM_VERSION_ON_REBOOT = "verifySystemVersion"
        const val PROPERTY_OLD_SYSTEM_VERSION = "oldSystemVersion"
        const val PROPERTY_TARGET_SYSTEM_VERSION = "targetSystemVersion"
        const val PROPERTY_INSTALLATION_ID = "installationId"
        const val PROPERTY_IS_AUTOMATIC_INSTALLATION_ENABLED = "isAutomaticInstallationEnabled"
        const val PROPERTY_LAST_NEWS_AD_SHOWN = "lastNewsAdShown"
        const val PROPERTY_CONTRIBUTE = "contribute"
        const val PROPERTY_CONTRIBUTION_COUNT = "contribution_count"
        const val PROPERTY_IS_EU_BUILD = "isEuBuild"

        // Offline cache properties
        const val PROPERTY_OFFLINE_ID = "offlineId"
        const val PROPERTY_OFFLINE_UPDATE_NAME = "offlineUpdateName"
        const val PROPERTY_OFFLINE_UPDATE_DOWNLOAD_SIZE = "offlineUpdateDownloadSize"
        const val PROPERTY_OFFLINE_UPDATE_DESCRIPTION = "offlineUpdateDescription"
        const val PROPERTY_OFFLINE_FILE_NAME = "offlineFileName"
        const val PROPERTY_OFFLINE_DOWNLOAD_URL = "offlineDownloadUrl"
        const val PROPERTY_OFFLINE_UPDATE_INFORMATION_AVAILABLE = "offlineUpdateInformationAvailable"
        const val PROPERTY_OFFLINE_IS_UP_TO_DATE = "offlineIsUpToDate"

        // Notifications properties
        const val PROPERTY_FIREBASE_TOKEN = "firebase_token"
        const val PROPERTY_NOTIFICATION_TOPIC = "notification_topic"
        const val PROPERTY_NOTIFICATION_DELAY_IN_SECONDS = "notification_delay_in_seconds"

        // IAB properties
        const val PROPERTY_AD_FREE = "34ejrtgalsJKDf;awljker;2k3jrpwosKjdfpio24uj3tp3oiwfjdscPOKj"
    }

}
