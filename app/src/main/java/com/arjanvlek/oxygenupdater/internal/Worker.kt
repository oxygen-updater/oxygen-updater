package com.arjanvlek.oxygenupdater.internal

import java8.lang.FunctionalInterface

/**
 * Oxygen Updater - © 2017 Arjan Vlek
 */

@FunctionalInterface
interface Worker {

    fun start()

    companion object {
        val NOOP = object : Worker {
            override fun start() {}
        }
    }
}
