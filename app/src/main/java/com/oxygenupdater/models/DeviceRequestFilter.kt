package com.oxygenupdater.models

import androidx.compose.runtime.Immutable

@Immutable
@JvmInline
value class DeviceRequestFilter private constructor(val value: String) {

    override fun toString() = "DeviceRequestFilter.$value"

    companion object {
        val All = DeviceRequestFilter("all")
        val Enabled = DeviceRequestFilter("enabled")
        val Disabled = DeviceRequestFilter("disabled")
    }
}
