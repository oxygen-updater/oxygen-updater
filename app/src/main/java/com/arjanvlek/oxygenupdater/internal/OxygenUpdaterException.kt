package com.arjanvlek.oxygenupdater.internal

/**
 * Oxygen Updater, copyright 2019 Arjan Vlek. File created by arjan.vlek on 05/05/2019.
 */
open class OxygenUpdaterException
/**
 * Errors / warnings which must be logged to Firebase but will *not* cause the app to crash for
 * the user.
 *
 * @param message Warning / Error message to log
 */(message: String?) : RuntimeException(message) {
    companion object {
        private const val serialVersionUID = -5629187151074427120L
    }
}
