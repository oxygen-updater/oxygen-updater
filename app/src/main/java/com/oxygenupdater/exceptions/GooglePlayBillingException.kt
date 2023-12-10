package com.oxygenupdater.exceptions

class GooglePlayBillingException(message: String?) : RuntimeException(message) {
    companion object {
        private const val serialVersionUID = 387133325499415925L
    }
}
