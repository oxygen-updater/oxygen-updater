package com.arjanvlek.oxygenupdater.exceptions

/**
 * @author Adhiraj Singh Chauhan (github.com/adhirajsinghchauhan)
 * @author Arjan Vlek (github.com/arjanvlek)
 */
class NetworkException(formattedMessage: String?) : OxygenUpdaterException(formattedMessage) {
    companion object {
        private const val serialVersionUID = 4758362378351778010L
    }
}
