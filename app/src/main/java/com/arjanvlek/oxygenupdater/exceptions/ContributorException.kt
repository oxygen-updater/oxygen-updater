package com.arjanvlek.oxygenupdater.exceptions

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 * @author Arjan Vlek (github.com/arjanvlek)
 */
class ContributorException(message: String?) : OxygenUpdaterException(message) {
    companion object {
        private const val serialVersionUID = -8580549379811358747L
    }
}
