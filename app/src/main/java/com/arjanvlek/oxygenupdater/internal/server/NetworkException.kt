package com.arjanvlek.oxygenupdater.internal.server

import com.arjanvlek.oxygenupdater.internal.OxygenUpdaterException

/**
 * Oxygen Updater, copyright 2019 Arjan Vlek. File created by arjan.vlek on 05/05/2019.
 */
class NetworkException(formattedMessage: String) : OxygenUpdaterException(formattedMessage) {
    companion object {
        private const val serialVersionUID = 4758362378351778010L
    }
}
