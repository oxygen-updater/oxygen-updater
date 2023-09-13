package com.oxygenupdater.models

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

@Immutable
@JvmInline
value class DeviceRequestFilter(val value: String) {
    override fun toString() = value

    companion object {
        @Stable
        val All = DeviceRequestFilter("all")

        @Stable
        val Enabled = DeviceRequestFilter("enabled")

        @Stable
        val Disabled = DeviceRequestFilter("disabled")
    }
}
