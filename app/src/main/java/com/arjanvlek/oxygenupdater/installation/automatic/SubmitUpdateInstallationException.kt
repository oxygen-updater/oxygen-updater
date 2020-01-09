package com.arjanvlek.oxygenupdater.installation.automatic

import com.arjanvlek.oxygenupdater.internal.OxygenUpdaterException

/**
 * Oxygen Updater, copyright 2019 Arjan Vlek. File created by arjan.vlek on 05/05/2019.
 */
class SubmitUpdateInstallationException(message: String?) : OxygenUpdaterException(message) {
    companion object {
        private const val serialVersionUID = -629237813420662801L
    }
}
