package com.arjanvlek.oxygenupdater.internal

import android.content.Context
import android.content.res.Configuration
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import androidx.appcompat.app.AppCompatDelegate.NightMode
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.settings.SettingsManager
import java.util.*

/**
 * @author Adhiraj Singh Chauhan (github.com/adhirajsinghchauhan)
 */
object ThemeUtils {
    const val OEM_BLACK_MODE = "oem_black_mode"

    /**
     * Translates the theme chosen by the user to the corresponding [NightMode].
     *
     * Since [androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_AUTO_TIME] is deprecated, we're considering night to last from 19:00 to 06:00
     * <br></br>
     * Cases:
     *
     *  1. Light/Dark: `MODE_NIGHT_NO/MODE_NIGHT_YES`
     *  1. Auto: `MODE_NIGHT_YES` if it's night-time. Otherwise, `MODE_NIGHT_AUTO_BATTERY`
     *  1. System: `MODE_NIGHT_FOLLOW_SYSTEM` above Android Pie (9.0). Otherwise, MODE_NIGHT_AUTO_BATTERY
     *
     *
     * @param context the context
     *
     * @return the [NightMode] to apply using [androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode]
     */
    @NightMode
    fun translateThemeToNightMode(context: Context): Int {
        val theme = SettingsManager(context).getPreference(context.getString(R.string.key_theme), context.getString(R.string.theme_system))

        return translateThemeToNightMode(context, theme)
    }

    private fun translateThemeToNightMode(context: Context, theme: String): Int {
        when (theme.toLowerCase()) {
            "light" -> return MODE_NIGHT_NO
            "dark" -> return MODE_NIGHT_YES
            "auto" -> {
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

                return if (hour in 19..23 || hour in 0..6)
                    MODE_NIGHT_YES
                else
                    MODE_NIGHT_AUTO_BATTERY
            }
            else -> {
                val onePlusTheme = getOnePlusTheme(context)

                // if the user has chosen Dark theme for their OnePlus device, honor it.
                // otherwise resort to Android-provided modes
                return if (onePlusTheme == OnePlusTheme.DARK.ordinal) {
                    MODE_NIGHT_YES
                } else {
                    // Android Pie (9.0) introduced a night mode system flag that could be set in developer options
                    if (VERSION.SDK_INT >= VERSION_CODES.P && onePlusTheme != -1)
                        MODE_NIGHT_FOLLOW_SYSTEM
                    else
                        MODE_NIGHT_AUTO_BATTERY
                }
            }
        }
    }

    /**
     * Checks night mode flags and returns true if night mode is active
     *
     * @param context the context
     *
     * @return true if night mode is active, else false
     */
    fun isNightModeActive(context: Context): Boolean {
        return when (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }
    }

    /**
     * OxygenOS stores the system theme at `content://settings/system/oem_black_mode`.
     *
     *
     * If the device has an option to change the theme (Settings -> Display -> Theme), this method will return the corresponding int
     *
     * @param context the context
     *
     * @return 0 for light, 1 for dark, 2 for colorful/default theme
     */
    private fun getOnePlusTheme(context: Context): Int {
        val resolver = context.contentResolver

        try {
            return Settings.System.getInt(resolver, OEM_BLACK_MODE)
        } catch (e: SettingNotFoundException) {
            // no-op
        }

        return -1
    }
}
