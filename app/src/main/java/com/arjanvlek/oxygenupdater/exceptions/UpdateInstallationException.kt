package com.arjanvlek.oxygenupdater.exceptions

class UpdateInstallationException internal constructor(message: String?) : Exception(message) {
    companion object {
        private const val serialVersionUID = -3093032334815030458L
    }
}
