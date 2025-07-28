package com.oxygenupdater.ui

import androidx.compose.runtime.Immutable
import com.oxygenupdater.R
import com.oxygenupdater.ui.Theme.Companion.Auto
import com.oxygenupdater.ui.Theme.Companion.System

/**
 * Integer values for app and OnePlus' system themes.
 *
 * v5.7.2 onwards, OnePlus-specific themes aren't translated into app themes.
 * See https://github.com/oxygen-updater/oxygen-updater/issues/189#issuecomment-1101082561.
 */
@Immutable
@JvmInline
value class Theme private constructor(val value: Int) {

    val titleResId
        get() = when (this) {
            Light -> R.string.theme_light
            Dark -> R.string.theme_dark
            Auto -> R.string.theme_auto
            else -> R.string.theme_system
        }

    val subtitleResId
        get() = when (this) {
            Light -> R.string.theme_light_subtitle
            Dark -> R.string.theme_dark_subtitle
            Auto -> R.string.theme_auto_subtitle
            else -> R.string.theme_system_subtitle
        }

    val dark
        get() = when (this) {
            Light -> false
            Dark -> true
            /** Caller needs to distinguish between [Auto] and [System] */
            else -> null
        }

    override fun toString() = "Theme." + when (this) {
        Light -> "Light"
        Dark -> "Dark"
        System -> "System"
        Auto -> "Auto"
        else -> "Invalid"
    }

    companion object {
        val Light = Theme(0)
        val Dark = Theme(1)
        val System = Theme(2)
        val Auto = Theme(3)

        fun from(value: Int) = when (value) {
            Light.value -> Light
            Dark.value -> Dark
            Auto.value -> Auto
            else -> System
        }
    }
}
