package com.oxygenupdater.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.oxygenupdater.R

/**
 * Integer values for app and OnePlus' system themes.
 *
 * v5.7.2 onwards, OnePlus-specific themes aren't translated into app themes.
 * See https://github.com/oxygen-updater/oxygen-updater/issues/189#issuecomment-1101082561.
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
@Immutable
sealed class Theme(
    val value: Int,
    @StringRes val titleResId: Int,
    @StringRes val subtitleResId: Int,
) {

    @Stable
    object Light : Theme(0, R.string.theme_light, R.string.theme_light_subtitle)

    @Stable
    object Dark : Theme(1, R.string.theme_dark, R.string.theme_dark_subtitle)

    @Stable
    object System : Theme(2, R.string.theme_system, R.string.theme_system_subtitle)

    @Stable
    object Auto : Theme(3, R.string.theme_auto, R.string.theme_auto_subtitle)

    override fun toString() = when (this) {
        Light -> "Light"
        Dark -> "Dark"
        System -> "System"
        Auto -> "Auto"
    }

    companion object {
        fun from(value: Int) = when (value) {
            Light.value -> Light
            Dark.value -> Dark
            Auto.value -> Auto
            else -> System
        }
    }
}
