package com.arjanvlek.oxygenupdater.domain

/**
 * @author Adhiraj Singh Chauhan (github.com/adhirajsinghchauhan)
 */
enum class DeviceRequestFilter(val filter: String) {
    ALL("all"),
    ENABLED("enabled"),
    DISABLED("disabled");

    override fun toString(): String {
        return filter
    }
}
