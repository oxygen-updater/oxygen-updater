package com.arjanvlek.oxygenupdater.internal

import com.arjanvlek.oxygenupdater.settings.UserSettingsCouldNotBeSavedException

import java.lang.String.format

/**
 * Oxygen Updater, copyright 2018 Arjan Vlek. File created by arjan.vlek on 18-01-18.
 */
object SetupUtils {

    fun getAsError(screenName: String, deviceId: Long?, updateMethodId: Long?): UserSettingsCouldNotBeSavedException {
        val errors = StringBuilder()

        if (!isValid(deviceId)) {
            errors.append(System.lineSeparator())
            errors.append("  - Device")
        }

        if (!isValid(updateMethodId)) {
            errors.append(System.lineSeparator())
            errors.append("  - Update method")
        }

        return UserSettingsCouldNotBeSavedException(
                format("User tried to leave the %s before all settings were saved. Missing item(s):%s", screenName, errors.toString())
        )
    }

    private fun isValid(id: Long?): Boolean {
        return id != null && id != -1L
    }
}
