package com.arjanvlek.oxygenupdater.internal.server;

import com.arjanvlek.oxygenupdater.internal.OxygenUpdaterException;

/**
 * Oxygen Updater, copyright 2019 Arjan Vlek. File created by arjan.vlek on 05/05/2019.
 */
public class NetworkException extends OxygenUpdaterException {

	public NetworkException(String formattedMessage) {
		super(formattedMessage);
	}
}
