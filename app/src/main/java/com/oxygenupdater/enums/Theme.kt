package com.oxygenupdater.enums

/**
 * Integer values for app and OnePlus' system themes.
 *
 * v5.7.2 onwards, OnePlus-specific themes aren't translated into app themes.
 * See https://github.com/oxygen-updater/oxygen-updater/issues/189#issuecomment-1101082561.
 *
 * Note:<br></br>
 * These integers are duplicated in `integers.xml` but are specified here to allow easy access and comparison throughout the app.
 * Why: it's easier to do `Theme.LIGHT`, instead of `context.getResources().getInteger(R.integer.R.theme_light_value)`
 *
 * Read through the comments in `integers.xml` to understand why XML values are present as well
 *
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
internal enum class Theme(private val value: Int) {

    LIGHT(0),
    DARK(1),
    SYSTEM(2),
    AUTO(3);

    companion object {
        private val map = values().associateBy { it.value }

        /**
         * Returns a theme based on value. If value isn't between [0, 3], default to [SYSTEM]
         *
         * @param value the integer value
         *
         * @return [Theme]
         */
        operator fun get(value: Int): Theme = map[value] ?: SYSTEM
    }
}
