package com.arjanvlek.oxygenupdater.download;

import com.arjanvlek.oxygenupdater.internal.OxygenUpdaterException;

/**
 * Oxygen Updater, copyright 2019 Arjan Vlek. File created by arjan.vlek on 05/05/2019.
 */
public class UpdateDownloadException extends OxygenUpdaterException {

	public UpdateDownloadException(String failureReason) {
		super(failureReason);
	}
}
