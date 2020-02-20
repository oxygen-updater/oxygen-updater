package com.arjanvlek.oxygenupdater.exceptions

/**
 * @author Adhiraj Singh Chauhan (github.com/adhirajsinghchauhan)
 * @author Arjan Vlek (github.com/arjanvlek) 05/05/2019
 */
class UpdateVerificationException(failureReason: String?) : OxygenUpdaterException(failureReason) {
    companion object {
        private const val serialVersionUID = -8034425806658367633L
    }
}
