package com.arjanvlek.oxygenupdater.contribution;

import com.arjanvlek.oxygenupdater.internal.OxygenUpdaterException;

/**
 * Oxygen Updater, copyright 2019 Arjan Vlek. File created by arjan.vlek on 05/05/2019.
 */
@SuppressWarnings("WeakerAccess")
public class ContributorException extends OxygenUpdaterException {

	private static final long serialVersionUID = -8580549379811358747L;

	public ContributorException(String message) {
		super(message);
	}
}
