package com.arjanvlek.oxygenupdater.exceptions

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 * @author [Arjan Vlek](https://github.com/arjanvlek)
 */
class NetworkException(formattedMessage: String?) : OxygenUpdaterException(formattedMessage) {
    companion object {
        private const val serialVersionUID = 4758362378351778010L
    }
}
