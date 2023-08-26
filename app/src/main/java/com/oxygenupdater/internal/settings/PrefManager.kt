package com.oxygenupdater.internal.settings

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import com.oxygenupdater.compose.ui.Theme
import com.oxygenupdater.compose.ui.onboarding.NOT_SET
import com.oxygenupdater.compose.ui.onboarding.NOT_SET_L
import com.oxygenupdater.utils.Logger
import org.koin.java.KoinJavaComponent.getKoin

object PrefManager {

    private val sharedPreferences by getKoin().inject<SharedPreferences>()

    /** @see [SharedPreferences.getString] */
    fun getString(key: String, defValue: String?) = sharedPreferences.getString(key, defValue)

    /** @see [SharedPreferences.Editor.putString] */
    fun putString(key: String, value: String?) = sharedPreferences.edit { putString(key, value) }

    /** @see [SharedPreferences.getStringSet] */
    fun getStringSet(key: String, defValues: Set<String>?): Set<String>? = sharedPreferences.getStringSet(key, defValues)

    /** @see [SharedPreferences.Editor.putStringSet] */
    fun putStringSet(key: String, values: Set<String>?) = sharedPreferences.edit { putStringSet(key, values) }

    /** @see [SharedPreferences.getInt] */
    fun getInt(key: String, defValue: Int) = sharedPreferences.getInt(key, defValue)

    /** @see [SharedPreferences.Editor.putInt] */
    fun putInt(key: String, value: Int) = sharedPreferences.edit { putInt(key, value) }

    fun incrementInt(key: String) = sharedPreferences.edit {
        putInt(key, getInt(key, 0) + 1)
    }

    /** @see [SharedPreferences.getLong] */
    fun getLong(key: String, defValue: Long) = sharedPreferences.getLong(key, defValue)

    /** @see [SharedPreferences.Editor.putLong] */
    fun putLong(key: String, value: Long) = sharedPreferences.edit { putLong(key, value) }

    /** @see [SharedPreferences.getFloat] */
    fun getFloat(key: String, defValue: Float) = sharedPreferences.getFloat(key, defValue)

    /** @see [SharedPreferences.Editor.putFloat] */
    fun putFloat(key: String, value: Float) = sharedPreferences.edit { putFloat(key, value) }

    /** @see [SharedPreferences.getBoolean] */
    fun getBoolean(key: String, defValue: Boolean) = sharedPreferences.getBoolean(key, defValue)

    /** @see [SharedPreferences.Editor.putBoolean] */
    fun putBoolean(key: String, value: Boolean) = sharedPreferences.edit { putBoolean(key, value) }

    @Suppress("UNCHECKED_CAST")
    fun <T> getPreference(
        key: String?,
        typecastValue: T,
        defaultValue: T,
    ) = sharedPreferences.run {
        if (key == null) return@run defaultValue
        when (typecastValue) {
            null, is String -> PrefManager.getString(key, null)
            is Int -> PrefManager.getInt(key, NOT_SET)
            is Long -> PrefManager.getLong(key, NOT_SET_L)
            is Float -> PrefManager.getFloat(key, -1f)
            is Boolean -> PrefManager.getBoolean(key, false)
            is Collection<*> -> PrefManager.getStringSet(key, null)
            else -> if (contains(key)) all[key] else defaultValue
        } as T
    }

    /**
     * Terrible performance, it relies on loading the map of all preferences.
     * Use this only if type isn't known.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getPreference(
        key: String?,
        defaultValue: T,
    ) = sharedPreferences.run {
        if (contains(key)) all[key] as T else defaultValue
    }

    /**
     * Saves a preference to sharedPreferences when its type isn't known
     *
     * @param key   Item key to later retrieve the item back
     * @param value Item that needs to be saved in shared preferences.
     */
    fun putPreference(key: String?, value: Any?) = try {
        sharedPreferences.edit {
            when (value) {
                null, is String -> putString(key, value as String?)
                is Int -> putInt(key, value)
                is Long -> putLong(key, value)
                is Float -> putFloat(key, value)
                is Boolean -> putBoolean(key, value)
                is Collection<*> -> putStringSet(key, buildSet<String> {
                    value.forEach {
                        if (it != null) {
                            add(it.toString())
                        }
                    }
                })
            }
        }
    } catch (e: Exception) {
        Logger.logError(TAG, "Failed to save preference with key $key and value $value. Defaulting to String value! ${e.message}", e)

        // If this doesn't work, try to use String instead.
        sharedPreferences.edit {
            putString(key, value.toString())
        }
    }

    /**
     * Checks if a certain preference is set.
     *
     * @param key Preference Key
     *
     * @return Returns if the given key is stored in the preferences.
     */
    fun contains(key: String) = sharedPreferences.contains(key)

    /**
     * Deletes/removes a preference
     *
     * @param key Preference Key
     */
    fun remove(key: String) = sharedPreferences.edit { remove(key) }

    /**
     * Checks if a device and update method have been set.
     *
     * @return if the user has chosen a device and an update method.
     */
    fun checkIfSetupScreenIsFilledIn() = getLong(PROPERTY_DEVICE_ID, NOT_SET_L) != NOT_SET_L
            && getLong(PROPERTY_UPDATE_METHOD_ID, NOT_SET_L) != NOT_SET_L

    /**
     * Checks if a user has completed the initial setup screen. This means the user has filled it in
     * and also pressed the "Start app" button at the very last screen.
     *
     * @return if the user has completed the setup screen.
     */
    fun checkIfSetupScreenHasBeenCompleted() = checkIfSetupScreenIsFilledIn()
            && getBoolean(PROPERTY_SETUP_DONE, false)

    private const val TAG = "SettingsManager"

    /**
     * Added in v5.4.0
     *
     * @see [com.oxygenupdater.BuildConfig.VERSION_CODE]
     */
    const val PROPERTY_VERSION_CODE = "version_code"

    // Settings properties
    const val PROPERTY_DEVICE = "device"
    const val PROPERTY_DEVICE_ID = "device_id"
    const val PROPERTY_UPDATE_METHOD = "update_method"
    const val PROPERTY_UPDATE_METHOD_ID = "update_method_id"

    const val PROPERTY_THEME_ID = "theme_id"

    const val PROPERTY_ADVANCED_MODE = "advanced_mode"
    const val PROPERTY_SETUP_DONE = "setup_done"
    const val PROPERTY_SQL_TO_ROOM_MIGRATION_DONE = "sql_to_room_migration_done"
    const val PROPERTY_IGNORE_UNSUPPORTED_DEVICE_WARNINGS = "ignore_unsupported_device_warnings"
    const val PROPERTY_IGNORE_INCORRECT_DEVICE_WARNINGS = "ignore_incorrect_device_warnings"
    const val PROPERTY_DOWNLOAD_BYTES_DONE = "download_bytes_done"

    /**
     * Value cannot be changed - is from older version where it was called 'upload_logs'
     */
    const val PROPERTY_SHARE_ANALYTICS_AND_LOGS = "upload_logs"

    /**
     * Note: contribution was a feature in app v2.7.0 - v5.8.3. v5.9.0 removed it, as it wasn't useful anymore.
     * Post Oppo-merger, OnePlus phones used either Google OTA (doesn't save files anywhere), or Component OTA
     * (saves in a directory accessible only via root). URLs in the latter case couldn't be constructed via names alone.
     *
     * It was brought back in app v5.11.0, because we failed to realize that URLs were saved in a SQLite database.
     */
    const val PROPERTY_CONTRIBUTE = "contribute"
    const val PROPERTY_CONTRIBUTION_COUNT = "contribution_count"

    const val PROPERTY_IS_EU_BUILD = "isEuBuild"

    const val PROPERTY_LAST_APP_UPDATE_CHECKED_DATE = "lastAppUpdateCheckDate"
    const val PROPERTY_FLEXIBLE_APP_UPDATE_IGNORE_COUNT = "flexibleAppUpdateIgnoreCount"

    // Notifications properties
    const val PROPERTY_FIREBASE_TOKEN = "firebase_token"
    const val PROPERTY_NOTIFICATION_TOPIC = "notification_topic"
    const val PROPERTY_NOTIFICATION_DELAY_IN_SECONDS = "notification_delay_in_seconds"

    // IAB properties
    const val PROPERTY_AD_FREE = "34ejrtgalsJKDf;awljker;2k3jrpwosKjdfpio24uj3tp3oiwfjdscPOKj"

    var theme by mutableStateOf(Theme.from(getInt(PROPERTY_THEME_ID, Theme.System.value)))
        internal set
}
