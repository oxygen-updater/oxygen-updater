package com.oxygenupdater.utils

import com.oxygenupdater.exceptions.UserSettingsCouldNotBeSavedException

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 * @author [Arjan Vlek](https://github.com/arjanvlek)
 */
object SetupUtils {

    fun getAsError(screenName: String?, deviceId: Long?, updateMethodId: Long?): UserSettingsCouldNotBeSavedException {
        val errors = buildString {
            if (!isValid(deviceId)) {
                append(System.lineSeparator())
                    .append("  - Device")
            }

            if (!isValid(updateMethodId)) {
                append(System.lineSeparator())
                    .append("  - Update method")
            }
        }

        return UserSettingsCouldNotBeSavedException("User tried to leave the $screenName before all settings were saved. Missing item(s): $errors")
    }

    private fun isValid(id: Long?) = id != null && id != -1L
}
