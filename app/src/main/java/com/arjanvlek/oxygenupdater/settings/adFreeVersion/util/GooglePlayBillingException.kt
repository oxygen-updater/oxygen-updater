package com.arjanvlek.oxygenupdater.settings.adFreeVersion.util

import com.arjanvlek.oxygenupdater.internal.OxygenUpdaterException

/**
 * Oxygen Updater, copyright 2019 Arjan Vlek. File created by arjan.vlek on 05/05/2019.
 */
class GooglePlayBillingException(message: String?) : OxygenUpdaterException(message) {
    companion object {
        private const val serialVersionUID = 387133325499415924L
    }
}
