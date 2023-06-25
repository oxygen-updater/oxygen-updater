package com.oxygenupdater.compose.ui.dialogs

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

@Immutable
@JvmInline
value class SheetType(val value: Int) {

    override fun toString() = when (this) {
        None -> "None"
        Device -> "Device"
        Method -> "Method"
        Contributor -> "Contributor"
        ServerMessages -> "ServerMessages"
        Theme -> "Theme"
        Language -> "Language"
        AdvancedMode -> "AdvancedMode"
        else -> "Invalid"
    }

    companion object {
        @Stable
        val None = SheetType(0)

        @Stable
        val Device = SheetType(1)

        @Stable
        val Method = SheetType(2)

        @Stable
        val Contributor = SheetType(3)

        @Stable
        val ServerMessages = SheetType(4)

        @Stable
        val Theme = SheetType(5)

        @Stable
        val Language = SheetType(6)

        @Stable
        val AdvancedMode = SheetType(7)
    }
}
