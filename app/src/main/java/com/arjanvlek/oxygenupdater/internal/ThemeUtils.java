package com.arjanvlek.oxygenupdater.internal;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate.NightMode;

import com.arjanvlek.oxygenupdater.R;
import com.arjanvlek.oxygenupdater.settings.SettingsManager;

import java.util.Calendar;

import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY;
import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO;
import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES;

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
	 * <ol>
	 * <li>Light/Dark: <code>MODE_NIGHT_NO/MODE_NIGHT_YES</code></li>
	 * <li>Auto: <code>MODE_NIGHT_YES</code> if it's night-time. Otherwise, <code>MODE_NIGHT_AUTO_BATTERY</code></li>
	 * <li>System: <code>MODE_NIGHT_FOLLOW_SYSTEM</code> above Android Pie (9.0). Otherwise, MODE_NIGHT_AUTO_BATTERY</code></li>
	 * </ol>
	 *
	 * @param context the context
	 *
	 * @return the {@link NightMode} to apply using {@link androidx.appcompat.app.AppCompatDelegate#setDefaultNightMode}
	 */
	@NightMode
	public static int translateThemeToNightMode(Context context) {
		String theme = new SettingsManager(context).getPreference(context.getString(R.string.key_theme), context.getString(R.string.theme_system));

		return translateThemeToNightMode(context, theme);
	}

	private static int translateThemeToNightMode(Context context, @NonNull String theme) {
		switch (theme.toLowerCase()) {
			case "light":
				return MODE_NIGHT_NO;
			case "dark":
				return MODE_NIGHT_YES;
			case "auto":
				int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

				return (hour >= 19 && hour <= 23) || (hour >= 0 && hour <= 6)
						? MODE_NIGHT_YES
						: MODE_NIGHT_AUTO_BATTERY;
			case "system":
			default:
				int onePlusTheme = getOnePlusTheme(context);

				// if the user has chosen Dark theme for their OnePlus device, honor it.
				// otherwise resort to Android-provided modes
				if (onePlusTheme == OnePlusTheme.DARK) {
					return MODE_NIGHT_YES;
				} else {
					// Android Pie (9.0) introduced a night mode system flag that could be set in developer options
					return VERSION.SDK_INT >= VERSION_CODES.P && onePlusTheme != -1
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
	 * If the device has an option to change the theme (Settings -> Display -> Theme), this method will return the corresponding int
	 *
	 * @param context the context
	 *
	 * @return 0 for light, 1 for dark, 2 for colorful/default theme
	 */
	private static int getOnePlusTheme(Context context) {
		ContentResolver resolver = context.getContentResolver();

		try {
			return Settings.System.getInt(resolver, OEM_BLACK_MODE);
		} catch (SettingNotFoundException e) {
			// no-op
		}

		return -1;
	}
}
