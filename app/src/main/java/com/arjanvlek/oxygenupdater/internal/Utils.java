package com.arjanvlek.oxygenupdater.internal;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.TypedValue;

import androidx.annotation.Dimension;
import androidx.annotation.NonNull;

import com.arjanvlek.oxygenupdater.BuildConfig;
import com.arjanvlek.oxygenupdater.R;
import com.arjanvlek.oxygenupdater.domain.Device;
import com.arjanvlek.oxygenupdater.domain.DeviceOsSpec;
import com.arjanvlek.oxygenupdater.domain.SystemVersionProperties;

import org.joda.time.LocalDateTime;

import java.text.DateFormat;
import java.util.List;
import java.util.Random;

import static com.arjanvlek.oxygenupdater.ApplicationData.NO_OXYGEN_OS;
import static com.arjanvlek.oxygenupdater.internal.logger.Logger.logError;
import static com.arjanvlek.oxygenupdater.internal.logger.Logger.logWarning;

public class Utils {

	private static Random random = new Random();

	/**
	 * Originally part of {@link com.google.android.gms.common.util.NumberUtils}, removed in later
	 * versions
	 *
	 * @param string the string to test
	 *
	 * @return true if string is a number, false otherwise
	 */
	public static boolean isNumeric(String string) {
		try {
			Long.parseLong(string);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	@SuppressWarnings("unused")
	public static float dpToPx(@NonNull Context context, @Dimension(unit = Dimension.DP) float dp) {
		return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
	}

	@SuppressWarnings("unused")
	public static float spToPx(@NonNull Context context, @Dimension(unit = Dimension.SP) float sp) {
		return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, context.getResources().getDisplayMetrics());
	}

	public static boolean checkNetworkConnection(Context context) {
		if (context == null) {
			logWarning("Utils", new OxygenUpdaterException("CheckNetworkConnection: check skipped due to empty / null context"));
			return false;
		}

		ConnectivityManager connectivityManager = (ConnectivityManager) Utils.getSystemService(context, Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetwork = connectivityManager != null ? connectivityManager.getActiveNetworkInfo() : null;
		return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
	}

	public static synchronized DeviceOsSpec checkDeviceOsSpec(SystemVersionProperties systemVersionProperties, List<Device> devices) {
		String oemFingerPrint = systemVersionProperties.getOemFingerprint();
		String oxygenOsVersion = systemVersionProperties.getOxygenOSVersion();

		boolean firmwareIsSupported = oemFingerPrint != null
				&& !oemFingerPrint.equals(NO_OXYGEN_OS)
				&& oemFingerPrint.contains(BuildConfig.SUPPORTED_BUILD_FINGERPRINT_KEYS)
				&& oxygenOsVersion != null
				&& !oxygenOsVersion.equals(NO_OXYGEN_OS);

		if (devices == null || devices.isEmpty()) {
			// To prevent incorrect results on empty server response. This still checks if official ROM is used and if an oxygen os version is found on the device.
			return firmwareIsSupported ? DeviceOsSpec.SUPPORTED_OXYGEN_OS : DeviceOsSpec.UNSUPPORTED_OS;
		}

		if (firmwareIsSupported) {
			// user's device is definitely running OxygenOS, now onto other checks...
			for (Device device : devices) {
				List<String> productNames = device.getProductNames();

				// find the user's device in the list of devices retrieved from the server
				if (productNames != null && productNames.contains(systemVersionProperties.getOxygenDeviceName())) {
					if (device.isEnabled()) {
						// device found, and is enabled, which means it is supported
						return DeviceOsSpec.SUPPORTED_OXYGEN_OS;
					} else {
						// device found, but is disabled, which means it's a carrier-exclusive
						// because only carrier-exclusive devices are disabled in the database
						return DeviceOsSpec.CARRIER_EXCLUSIVE_OXYGEN_OS;
					}
				}
			}

			// device not found among the server-provided list
			// hence, must be a newly-released OnePlus device that we're yet to add support for
			return DeviceOsSpec.UNSUPPORTED_OXYGEN_OS;
		} else {
			// device isn't running OxygenOS at all
			// note: the device may very well be a OnePlus device, but in this case it's running a custom ROM, which we don't support duh
			return DeviceOsSpec.UNSUPPORTED_OS;
		}
	}

	public static String formatDateTime(Context context, String dateTimeString) {
		try {
			if (dateTimeString == null || dateTimeString.isEmpty()) {
				return context.getString(R.string.device_information_unknown);
			}
			dateTimeString = dateTimeString.replace(" ", "T");
			LocalDateTime dateTime = LocalDateTime.parse(dateTimeString);
			DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(context);
			String formattedTime = timeFormat.format(dateTime.toDate());

			LocalDateTime today = LocalDateTime.now();
			if ((dateTime.getDayOfMonth() == today.getDayOfMonth()) && dateTime.getMonthOfYear() == today.getMonthOfYear() && dateTime.getYear() == today.getYear()) {
				return formattedTime;
			} else if ((dateTime.getDayOfMonth() + 1) == today.getDayOfMonth() && dateTime.getMonthOfYear() == today.getMonthOfYear() && dateTime.getYear() == today.getYear()) {
				return context.getString(R.string.time_yesterday) + " " + context.getString(R.string.time_at) + " " + formattedTime;
			} else {
				DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(context);
				String formattedDate = dateFormat.format(dateTime.toDate());
				return formattedDate + " " + context.getString(R.string.time_at) + " " + formattedTime;
			}
		} catch (Exception e) {
			logError("DateTimeFormatter", String.format("Unable to parse date from input '%s'", dateTimeString), e);
			return dateTimeString;
		}
	}

	public static Object getSystemService(Context context, String serviceName) {
		return context != null ? context.getSystemService(serviceName) : null;
	}

	/**
	 * Min is inclusive and max is exclusive in this case
	 **/
	public static int randomBetween(int min, int max) {
		return random.nextInt(max - min) + min;
	}
}
