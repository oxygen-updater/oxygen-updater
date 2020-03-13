package com.arjanvlek.oxygenupdater.exceptions

/**
 * Errors/warnings which must be logged to Firebase but will *not* cause the app to crash for
 * the user.
 *
 * @param message Warning / Error message to log
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 * @author [Arjan Vlek](https://github.com/arjanvlek)
 */
open class OxygenUpdaterException(message: String?) : RuntimeException(message) {
    companion object {
        private const val serialVersionUID = -5629187151074427120L
    }
}
