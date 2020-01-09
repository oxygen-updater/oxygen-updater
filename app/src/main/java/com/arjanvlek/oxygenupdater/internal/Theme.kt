package com.arjanvlek.oxygenupdater.internal

import lombok.Getter

/**
 * Integer values for app and OnePlus' system themes.
 *
 * OnePlus-specific themes are translated into app-specific themes by value.
 *
 * Note:<br></br>
 * These integers are duplicated in `integers.xml` but are specified here to allow easy access and comparison throughout the app.
 * Why: it's easier to do `Theme.LIGHT`, instead of `context.getResources().getInteger(R.integer.R.theme_light_value)`
 *
 * Read through the comments in `integers.xml` to understand why XML values are present as well
 *
 * @author Adhiraj Singh Chauhan (github.com/adhirajsinghchauhan)
 * @see ThemeUtils.OEM_BLACK_MODE
 */
@Getter
internal enum class Theme(private val value: Int) {
    LIGHT(0),
    DARK(1),
    SYSTEM(2),
    AUTO(3);

    companion object {
        /**
         * Returns a theme bases on value. If value isn't between [0, 3], default to [.SYSTEM]
         *
         * @param value the integer value
         *
         * @return [Theme]
         */
        operator fun get(value: Int): Theme {
            return when (value) {
                0 -> LIGHT
                1 -> DARK
                2 -> SYSTEM
                3 -> AUTO
                else -> SYSTEM
            }
        }
    }

}
