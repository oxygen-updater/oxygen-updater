package com.oxygenupdater.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.NightMode
import com.oxygenupdater.compose.ui.Theme
import com.oxygenupdater.internal.settings.PrefManager
import java.util.Calendar

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
object ThemeUtils {

    /**
     * Checks night mode flags and returns true if night mode is active
     *
     * @param context the context
     *
     * @return true if night mode is active, else false
     */
    fun isNightModeActive(context: Context) = when (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
        Configuration.UI_MODE_NIGHT_YES -> true
        Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_UNDEFINED -> false
        else -> false
    }

    /**
     * Translates the theme chosen by the user to the corresponding [NightMode].
     *
     * Since [AppCompatDelegate.MODE_NIGHT_AUTO_TIME] is deprecated, we're considering night to last from 19:00 to 06:00
     * <br></br>
     * Cases:
     *
     *  * Light/Dark: `MODE_NIGHT_NO/MODE_NIGHT_YES`
     *  * Auto: `MODE_NIGHT_YES` if it's night-time. Otherwise, `MODE_NIGHT_AUTO_BATTERY`
     *  * System: `MODE_NIGHT_FOLLOW_SYSTEM` above Android Pie (9.0). Otherwise, MODE_NIGHT_AUTO_BATTERY
     *
     * @param context the context
     *
     * @return the [NightMode] to apply using [AppCompatDelegate.setDefaultNightMode]
     */
    @NightMode
    fun translateThemeToNightMode() = when (PrefManager.getInt(PrefManager.PROPERTY_THEME_ID, Theme.System.value)) {
        Theme.Light.value -> AppCompatDelegate.MODE_NIGHT_NO
        Theme.Dark.value -> AppCompatDelegate.MODE_NIGHT_YES
        Theme.Auto.value -> Calendar.getInstance()[Calendar.HOUR_OF_DAY].let { hour ->
            if (hour in 19..23 || hour in 0..6) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
            }
        }
        // includes case for Theme.System as well
        else -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Android Pie (9.0) introduced a night mode system flag that could be set in developer options
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        } else {
            AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
        }
        // Note: v5.7.2 onwards we're not reading OnePlus-specific settings.
        // See https://github.com/oxygen-updater/oxygen-updater/issues/189#issuecomment-1101082561.
    }
}
