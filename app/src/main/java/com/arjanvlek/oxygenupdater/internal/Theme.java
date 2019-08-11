package com.arjanvlek.oxygenupdater.internal;

import lombok.Getter;

/**
 * Integer values for app and OnePlus' system themes.
 * <p>
 * OnePlus-specific themes are translated into app-specific themes by value.
 * <p></p>
 * Note:<br>
 * These integers are duplicated in <code>integers.xml</code> but are specified here to allow easy access and comparison throughout the app.
 * Why: it's easier to do <code>Theme.LIGHT</code>, instead of <code>context.getResources().getInteger(R.integer.R.theme_light_value)</code>
 * <p>
 * Read through the comments in <code>integers.xml</code> to understand why XML values are present as well
 *
 * @author Adhiraj Singh Chauhan (github.com/adhirajsinghchauhan)
 * @see ThemeUtils#OEM_BLACK_MODE
 */
@Getter
enum Theme {
	LIGHT(0),
	DARK(1),
	SYSTEM(2),
	AUTO(3);

	private int value;

	Theme(int value) {
		this.value = value;
	}

	/**
	 * Returns a theme bases on value. If value isn't between [0, 3], default to {@link #SYSTEM}
	 *
	 * @param value the integer value
	 *
	 * @return {@link Theme}
	 */
	static Theme get(int value) {
		switch (value) {
			case 0:
				return LIGHT;
			case 1:
				return DARK;
			case 2:
			default:
				return SYSTEM;
			case 3:
				return AUTO;
		}
	}
}
