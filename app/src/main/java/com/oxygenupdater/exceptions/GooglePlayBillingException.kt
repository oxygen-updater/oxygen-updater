package com.oxygenupdater.exceptions

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 * @author [Arjan Vlek](https://github.com/arjanvlek)
 */
class GooglePlayBillingException(message: String?) : RuntimeException(message) {
    companion object {
        private const val serialVersionUID = 387133325499415925L
    }
}
