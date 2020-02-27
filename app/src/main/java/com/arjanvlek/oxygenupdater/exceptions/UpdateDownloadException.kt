package com.arjanvlek.oxygenupdater.exceptions

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 * @author Arjan Vlek (github.com/arjanvlek)
 */
class UpdateDownloadException(failureReason: String?) : OxygenUpdaterException(failureReason) {
    companion object {
        private const val serialVersionUID = -354531343629257282L
    }
}
