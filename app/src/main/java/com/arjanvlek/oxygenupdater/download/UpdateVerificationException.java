package com.arjanvlek.oxygenupdater.download;

import com.arjanvlek.oxygenupdater.internal.OxygenUpdaterException;

/**
 * Oxygen Updater, copyright 2019 Arjan Vlek. File created by arjan.vlek on 05/05/2019.
 */
@SuppressWarnings("WeakerAccess")
public class UpdateVerificationException extends OxygenUpdaterException {

	private static final long serialVersionUID = -8034425806658367633L;

	public UpdateVerificationException(String failureReason) {
		super(failureReason);
	}
}
