package com.arjanvlek.oxygenupdater.internal

import com.arjanvlek.oxygenupdater.settings.UserSettingsCouldNotBeSavedException

/**
 * Oxygen Updater, copyright 2018 Arjan Vlek. File created by arjan.vlek on 18-01-18.
 */
object SetupUtils {

    fun getAsError(screenName: String?, deviceId: Long?, updateMethodId: Long?): UserSettingsCouldNotBeSavedException {
        val errors = StringBuilder()

        if (!isValid(deviceId)) {
            errors.append(System.lineSeparator())
                .append("  - Device")
        }

        if (!isValid(updateMethodId)) {
            errors.append(System.lineSeparator())
                .append("  - Update method")
        }

        return UserSettingsCouldNotBeSavedException("User tried to leave the $screenName before all settings were saved. Missing item(s):$errors")
    }

    private fun isValid(id: Long?): Boolean {
        return id != null && id != -1L
    }
}
