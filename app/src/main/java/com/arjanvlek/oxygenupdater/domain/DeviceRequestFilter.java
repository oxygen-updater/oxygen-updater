package com.arjanvlek.oxygenupdater.domain;

import androidx.annotation.NonNull;

/**
 * @author Adhiraj Singh Chauhan (github.com/adhirajsinghchauhan)
 */
public enum DeviceRequestFilter {
	ALL("all"),
	ENABLED("enabled"),
	DISABLED("disabled");

	private final String filter;

	DeviceRequestFilter(String filter) {
		this.filter = filter;
	}

	public String getFilter() {
		return filter;
	}

	@NonNull
	@Override
	public String toString() {
		return filter;
	}
}
