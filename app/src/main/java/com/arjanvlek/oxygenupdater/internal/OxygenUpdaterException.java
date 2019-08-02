package com.arjanvlek.oxygenupdater.internal;

/**
 * Oxygen Updater, copyright 2019 Arjan Vlek. File created by arjan.vlek on 05/05/2019.
 */
public class OxygenUpdaterException extends RuntimeException {

	/**
	 * Errors / warnings which must be logged to Firebase but will *not* cause the app to crash for
	 * the user.
	 *
	 * @param message Warning / Error message to log
	 */
	public OxygenUpdaterException(String message) {
		super(message);
	}
}
