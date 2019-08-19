package com.arjanvlek.oxygenupdater.internal;

import java8.lang.FunctionalInterface;

/**
 * Oxygen Updater - © 2017 Arjan Vlek
 */

@FunctionalInterface
public interface Worker {

	Worker NOOP = () -> {
	};

	void start();
}
