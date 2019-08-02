package com.arjanvlek.oxygenupdater.internal;

import com.arjanvlek.oxygenupdater.settings.UserSettingsCouldNotBeSavedException;

/**
 * Oxygen Updater, copyright 2018 Arjan Vlek. File created by arjan.vlek on 18-01-18.
 */

public class SetupUtils {

	public static UserSettingsCouldNotBeSavedException getAsError(String screenName, Long deviceId, Long updateMethodId) {
		StringBuilder errors = new StringBuilder();

		if (!isValid(deviceId)) {
			errors.append(System.lineSeparator());
			errors.append("  - Device");
		}

		if (!isValid(updateMethodId)) {
			errors.append(System.lineSeparator());
			errors.append("  - Update method");
		}

		return new UserSettingsCouldNotBeSavedException(String.format("User tried to leave the %s before all settings were saved. Missing item(s):%s", screenName, errors
				.toString()));
	}

	private static boolean isValid(Long id) {
		return id != null && id != -1L;
	}
}
