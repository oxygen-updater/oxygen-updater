package com.oxygenupdater.extensions

import com.google.android.material.switchmaterial.SwitchMaterial
import com.oxygenupdater.internal.KotlinCallback
import com.oxygenupdater.internal.settings.PrefManager

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
    isChecked = PrefManager.getBoolean(settingName, defaultValue)

    setOnCheckedChangeListener { _, isChecked ->
        PrefManager.putBoolean(settingName, isChecked)

        checkedChangeCallback?.invoke(isChecked)
    }
}
