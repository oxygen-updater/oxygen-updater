package com.oxygenupdater.extensions

import com.google.android.material.switchmaterial.SwitchMaterial
import com.oxygenupdater.internal.KotlinCallback
import com.oxygenupdater.internal.settings.SettingsManager

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
    isChecked = SettingsManager.getPreference(settingName, defaultValue)

    setOnCheckedChangeListener { _, isChecked ->
        SettingsManager.savePreference(settingName, isChecked)

        checkedChangeCallback?.invoke(isChecked)
    }
}
