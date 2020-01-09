package com.arjanvlek.oxygenupdater.settings.adFreeVersion.util

/**
 * Represents the result of an in-app billing operation. A result is composed of a response code (an
 * integer) and possibly a message (String). You can get those by calling [.getResponse] and
 * [.getMessage], respectively. You can also inquire whether a result is a success or a
 * failure by calling [.success] and [.isFailure].
 */
class IabResult(var response: Int, message: String?) {
    var message: String? = null

    val success: Boolean
        get() = response == IabHelper.BILLING_RESPONSE_RESULT_OK

    val isFailure: Boolean
        get() = !success

    override fun toString(): String {
        return "IabResult: $message"
    }

    init {
        if (message == null || message.trim { it <= ' ' }.isEmpty()) {
            this.message = IabHelper.getResponseDesc(response)
        } else {
            this.message = message + " (response: " + IabHelper.getResponseDesc(response) + ")"
        }
    }
}
