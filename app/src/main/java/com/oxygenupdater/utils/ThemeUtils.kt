package com.oxygenupdater.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.NightMode
import com.oxygenupdater.R
import com.oxygenupdater.enums.Theme
import com.oxygenupdater.enums.Theme.AUTO
import com.oxygenupdater.enums.Theme.DARK
import com.oxygenupdater.enums.Theme.LIGHT
import com.oxygenupdater.internal.settings.SettingsManager
import java.util.*

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
    fun translateThemeToNightMode(context: Context) = SettingsManager.getPreference(
        context.getString(R.string.key_theme_id),
        context.resources.getInteger(R.integer.theme_system_id)
    ).let { translateThemeToNightMode(Theme[it]) }

    @NightMode
    private fun translateThemeToNightMode(theme: Theme) = when (theme) {
        LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
        DARK -> AppCompatDelegate.MODE_NIGHT_YES
        AUTO -> Calendar.getInstance()[Calendar.HOUR_OF_DAY].let { hour ->
            if (hour in 19..23 || hour in 0..6) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
            }
        }
        // includes case for Theme.SYSTEM as well
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
