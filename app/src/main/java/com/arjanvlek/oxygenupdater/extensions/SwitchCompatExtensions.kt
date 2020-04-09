package com.arjanvlek.oxygenupdater.extensions

import com.arjanvlek.oxygenupdater.internal.KotlinCallback
import com.arjanvlek.oxygenupdater.internal.settings.SettingsManager
import com.google.android.material.switchmaterial.SwitchMaterial
import org.koin.java.KoinJavaComponent.inject

private val settingsManager by inject(SettingsManager::class.java)

/**
 * Sync a [SwitchMaterial]'s UI state with [android.content.SharedPreferences]
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
fun SwitchMaterial.syncWithSharedPreferences(
    settingName: String,
    defaultValue: Boolean,
    checkedChangeCallback: KotlinCallback<Boolean>? = null
) {
    isChecked = settingsManager.getPreference(settingName, defaultValue)

    setOnCheckedChangeListener { _, isChecked ->
        settingsManager.savePreference(settingName, isChecked)

        checkedChangeCallback?.invoke(isChecked)
    }
}
