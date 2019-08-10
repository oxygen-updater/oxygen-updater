package com.arjanvlek.oxygenupdater.download

import com.arjanvlek.oxygenupdater.internal.OxygenUpdaterException

/**
 * Oxygen Updater, copyright 2019 Arjan Vlek. File created by arjan.vlek on 05/05/2019.
 */
class UpdateVerificationException(failureReason: String) : OxygenUpdaterException(failureReason) {
    companion object {
        private const val serialVersionUID = -8034425806658367633L
    }
}
