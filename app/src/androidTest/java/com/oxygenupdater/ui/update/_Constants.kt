package com.oxygenupdater.ui.update

import com.oxygenupdater.internal.settings.KeyUpdateMethod
import com.oxygenupdater.ui.settings.MethodSettingsListConfig

val UpdateMethod = MethodSettingsListConfig.list[0].name!!

val GetPrefStrForUpdateMethod: (key: String, default: String) -> String = { key, default ->
    if (key == KeyUpdateMethod) UpdateMethod else default
}
