package com.arjanvlek.oxygenupdater.internal;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.provider.Settings.SettingNotFoundException;
import android.provider.Settings.System;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate.NightMode;

import com.arjanvlek.oxygenupdater.R;
import com.arjanvlek.oxygenupdater.settings.SettingsManager;

import java.util.Calendar;

import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY;
import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO;
import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES;
import static com.arjanvlek.oxygenupdater.internal.Theme.DARK;
import static com.arjanvlek.oxygenupdater.internal.Theme.LIGHT;

/**
 * @author Adhiraj Singh Chauhan (github.com/adhirajsinghchauhan)
 */
@SuppressWarnings("WeakerAccess")
public class ThemeUtils {
	public final static String OEM_BLACK_MODE = "oem_black_mode";

	/**
	 * Translates the theme chosen by the user to the corresponding {@link NightMode}.
	 * <p>Since {@link androidx.appcompat.app.AppCompatDelegate#MODE_NIGHT_AUTO_TIME} is deprecated, we're considering night to last from 19:00 to 06:00</p>
	 * <br>
	 * Cases:
	 * <ul>
	 * <li>Light/Dark: <code>MODE_NIGHT_NO/MODE_NIGHT_YES</code></li>
	 * <li>Auto: <code>MODE_NIGHT_YES</code> if it's night-time. Otherwise, <code>MODE_NIGHT_AUTO_BATTERY</code></li>
	 * <li>System: <code>MODE_NIGHT_FOLLOW_SYSTEM</code> above Android Pie (9.0). Otherwise, MODE_NIGHT_AUTO_BATTERY</code></li>
	 * </ul>
	 *
	 * @param context the context
	 *
	 * @return the {@link NightMode} to apply using {@link androidx.appcompat.app.AppCompatDelegate#setDefaultNightMode}
	 */
	@NightMode
	public static int translateThemeToNightMode(Context context) {
		Integer theme = new SettingsManager(context).getPreference(context.getString(R.string.key_theme_id), context.getResources().getInteger(R.integer.theme_system_id));

		return translateThemeToNightMode(context, Theme.get(theme));
	}

	private static int translateThemeToNightMode(Context context, @NonNull Theme theme) {
		switch (theme) {
			case LIGHT:
				return MODE_NIGHT_NO;
			case DARK:
				return MODE_NIGHT_YES;
			case AUTO:
				int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

				return (hour >= 19 && hour <= 23) || (hour >= 0 && hour <= 6)
						? MODE_NIGHT_YES
						: MODE_NIGHT_AUTO_BATTERY;
			case SYSTEM:
			default:
				Theme translatedTheme = translateOnePlusTheme(context);

				// if the user has chosen Dark theme for their OnePlus device, honor it.
				// otherwise resort to Android-provided modes
				if (translatedTheme == DARK) {
					return MODE_NIGHT_YES;
				} else if (translatedTheme == LIGHT) {
					return MODE_NIGHT_NO;
				} else {
					// Android Pie (9.0) introduced a night mode system flag that could be set in developer options
					return VERSION.SDK_INT >= VERSION_CODES.P && translatedTheme != null
							? MODE_NIGHT_FOLLOW_SYSTEM
							: MODE_NIGHT_AUTO_BATTERY;
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
	public static boolean isNightModeActive(Context context) {
		int nightModeFlags = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
		switch (nightModeFlags) {
			case Configuration.UI_MODE_NIGHT_YES:
				return true;
			case Configuration.UI_MODE_NIGHT_NO:
			case Configuration.UI_MODE_NIGHT_UNDEFINED:
			default:
				return false;
		}
	}

	/**
	 * OxygenOS stores the system theme at <code>content://settings/system/oem_black_mode</code>.
	 * <p>
	 * If the device has an option to change the theme (Settings -> Display -> Theme), this method will return the corresponding {@link Theme}.
	 * <p>
	 * This is how I've mapped OnePlus themes to {@link Theme}:
	 * <ul>
	 * <li>OnePlusLight(0) and OnePlusColorful(2) are both {@link Theme#LIGHT} themes</li>
	 * <li>OnePlusDark is a {@link Theme#DARK} theme</li>
	 * <li>(haha ezpz)</li>
	 * </ul>
	 *
	 * @param context the context
	 *
	 * @return 0 for light, 1 for dark, 2 for colorful/default theme
	 */
	private static Theme translateOnePlusTheme(Context context) {
		ContentResolver resolver = context.getContentResolver();

		try {
			int onePlusTheme = System.getInt(resolver, OEM_BLACK_MODE);

			switch (onePlusTheme) {
				case 0:
					// OnePlusLight is a light theme
					return LIGHT;
				case 1:
					// OnePlusDark is a dark theme
					return DARK;
				case 2:
					// OnePlusColorful is a light theme
					return LIGHT;
			}
		} catch (SettingNotFoundException e) {
			// no-op
		}

		// system doesn't support OnePlus themes yet (it was introduced in later OxygenOS versions),
		// so old devices will probably not have this setting
		return null;
	}
}
