package com.oxygenupdater.viewmodels

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oxygenupdater.internal.KotlinCallback
import com.oxygenupdater.internal.settings.PrefManager
import com.oxygenupdater.models.Device
import com.oxygenupdater.models.DeviceRequestFilter
import com.oxygenupdater.models.UpdateMethod
import com.oxygenupdater.repositories.ServerRepository
import com.oxygenupdater.utils.Logger.logDebug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * For [com.oxygenupdater.fragments.SettingsFragment]
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
class SettingsViewModel(
    private val serverRepository: ServerRepository
) : ViewModel() {

    private val _enabledDevices = MutableLiveData<List<Device>>()
    private val _updateMethodsForDevice = MutableLiveData<List<UpdateMethod>>()

    private var pendingJob: Job? = null

    private val previousSettingValues = hashMapOf<String, Any?>()

    init {
        previousSettingValues[PrefManager.PROPERTY_DEVICE_ID] = PrefManager.getLong(
            PrefManager.PROPERTY_DEVICE_ID,
            -1L
        )
        previousSettingValues[PrefManager.PROPERTY_UPDATE_METHOD_ID] = PrefManager.getLong(
            PrefManager.PROPERTY_UPDATE_METHOD_ID,
            -1L
        )
        previousSettingValues[PrefManager.PROPERTY_ADVANCED_MODE] = PrefManager.getBoolean(
            PrefManager.PROPERTY_ADVANCED_MODE,
            false
        )
    }

    fun fetchEnabledDevices(): LiveData<List<Device>> = viewModelScope.launch(Dispatchers.IO) {
        serverRepository.fetchDevices(DeviceRequestFilter.ENABLED)?.let {
            _enabledDevices.postValue(it)
        }
    }.let { _enabledDevices }

    fun fetchUpdateMethodsForDevice(
        deviceId: Long
    ): LiveData<List<UpdateMethod>> = viewModelScope.launch(Dispatchers.IO) {
        serverRepository.fetchUpdateMethodsForDevice(deviceId)?.let {
            _updateMethodsForDevice.postValue(it)
        }
    }.let { _updateMethodsForDevice }

    /**
     * A utility function that returns `true` in the callback only if the key
     * corresponds to a preference that should trigger a UI refresh, and if value
     * has actually changed compared to initial/default values: [previousSettingValues].
     *
     * This listener allows for a `1s` delay before notifying, for cases where a
     * setting is changed after a network request (e.g. device or update method).
     *
     * In case preferences are changed in quick succession, any pending notifications
     * are cancelled and the default `1s` delay is reduced to `200ms`. The delay is
     * reduced so that the current notify request can execute ASAP, while still
     * allowing cases where preferences are changed by code logic (e.g. `device` and
     * `device_id` are updated together, but because they are separate statements,
     * there might be a small delay of a few milliseconds due to the asynchronous
     * nature of [SharedPreferences.Editor.apply])
     *
     * @param key the preference key
     * @param callback invoked when the pending runnable is executed.
     * `true`/`false` denote whether the preference's value has changed or not.
     */
    fun hasPreferenceChanged(key: String, callback: KotlinCallback<Boolean>) {
        when (key) {
            PrefManager.PROPERTY_DEVICE_ID,
            PrefManager.PROPERTY_UPDATE_METHOD_ID,
            PrefManager.PROPERTY_ADVANCED_MODE -> {
                val timeout = if (pendingJob != null) {
                    logDebug(TAG, "$key: cancelling previous job and setting timeout to 200ms")
                    // Cancel any pending notifies and reset timeout to 200ms
                    // so that the current notify request can execute ASAP.
                    pendingJob?.cancel("Received new request for '$key'")
                    200L
                } else {
                    logDebug(TAG, "$key: setting timeout to 1s")
                    // Default timeout is 1s, which should reasonably allow for
                    // cases where a setting is changed after on a network request
                    1000L
                }

                pendingJob = viewModelScope.launch(Dispatchers.IO) {
                    delay(timeout)

                    val previousValue = previousSettingValues[key]
                    val currentValue = PrefManager.getPreference(key, previousValue, null)
                    val hasChanged = previousValue != currentValue

                    previousSettingValues[key] = currentValue

                    logDebug(TAG, "$key: $previousValue -> $currentValue; hasChanged: $hasChanged")
                    withContext(Dispatchers.Main) {
                        callback.invoke(hasChanged)
                        // Job's finished, so reset it
                        pendingJob = null
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "SettingsViewModel"
    }
}
