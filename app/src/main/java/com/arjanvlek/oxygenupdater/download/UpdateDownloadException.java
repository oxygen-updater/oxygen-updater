package com.arjanvlek.oxygenupdater.download;

import com.arjanvlek.oxygenupdater.internal.OxygenUpdaterException;

/**
 * Oxygen Updater, copyright 2019 Arjan Vlek. File created by arjan.vlek on 05/05/2019.
 */
@SuppressWarnings("WeakerAccess")
public class UpdateDownloadException extends OxygenUpdaterException {

	private static final long serialVersionUID = -354531343629257282L;

	public UpdateDownloadException(String failureReason) {
		super(failureReason);
	}
}
