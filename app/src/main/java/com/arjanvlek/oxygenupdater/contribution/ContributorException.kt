package com.arjanvlek.oxygenupdater.contribution

import com.arjanvlek.oxygenupdater.internal.OxygenUpdaterException

/**
 * Oxygen Updater, copyright 2019 Arjan Vlek. File created by arjan.vlek on 05/05/2019.
 */
class ContributorException(message: String?) : OxygenUpdaterException(message) {
    companion object {
        private const val serialVersionUID = -8580549379811358747L
    }
}
