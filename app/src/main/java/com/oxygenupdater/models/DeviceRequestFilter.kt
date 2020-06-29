package com.oxygenupdater.models

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
enum class DeviceRequestFilter(val filter: String) {
    ALL("all"),
    ENABLED("enabled"),
    DISABLED("disabled");

    override fun toString(): String {
        return filter
    }
}
