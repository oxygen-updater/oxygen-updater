package com.arjanvlek.oxygenupdater.internal;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.TypedValue;

import com.arjanvlek.oxygenupdater.BuildConfig;
import com.arjanvlek.oxygenupdater.R;
import com.arjanvlek.oxygenupdater.domain.Device;
import com.arjanvlek.oxygenupdater.domain.SystemVersionProperties;
import com.arjanvlek.oxygenupdater.internal.logger.Logger;

import org.joda.time.LocalDateTime;

import java.text.DateFormat;
import java.util.List;
import java.util.Random;

import java8.util.Optional;
import java8.util.stream.StreamSupport;

import static com.arjanvlek.oxygenupdater.ApplicationData.NO_OXYGEN_OS;

public class Utils {

    private static Random random = new Random();

    /**
     * Originally part of {@link com.google.android.gms.common.util.NumberUtils},
     * removed in later versions
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

    /**
     * Converts DiP units to pixels
     */
    public static int diPToPixels(Activity activity, int numberOfPixels) {
        if (activity != null && activity.getResources() != null) {
            return (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    numberOfPixels,
                    activity.getResources().getDisplayMetrics()
            );
        } else {
            return 0;
        }
    }

    public static boolean checkNetworkConnection(Context context) {
        if (context == null) {
            Logger.logWarning("Utils", new OxygenUpdaterException("CheckNetworkConnection: check skipped due to empty / null context"));
            return false;
        }

        ConnectivityManager connectivityManager = (ConnectivityManager) Utils.getSystemService(context, Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager != null ? connectivityManager.getActiveNetworkInfo() : null;
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    public static synchronized boolean isSupportedDevice(SystemVersionProperties systemVersionProperties, List<Device> devices) {
        String oemFingerPrint = systemVersionProperties.getOemFingerprint();
        String oxygenOsVersion = systemVersionProperties.getOxygenOSVersion();

        boolean firmwareIsSupported =
                oemFingerPrint != null
                        && !oemFingerPrint.equals(NO_OXYGEN_OS)
                        && oemFingerPrint.contains(BuildConfig.SUPPORTED_BUILD_FINGERPRINT_KEYS)
                        && oxygenOsVersion != null
                        && !oxygenOsVersion.equals(NO_OXYGEN_OS);

        if (devices == null || devices.isEmpty()) {
            // To prevent incorrect results on empty server response. This still checks if official ROM is used and if an oxygen os version is found on the device.
            return firmwareIsSupported;
        }

        Optional<Device> supportedDevice = StreamSupport.stream(devices)
                .filter(d -> d.getProductNames() != null && d.getProductNames().contains(systemVersionProperties.getOxygenDeviceName()))
                .findAny();

        return supportedDevice.isPresent() && firmwareIsSupported;
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
            Logger.logError("DateTimeFormatter", String.format("Unable to parse date from input '%s'", dateTimeString), e);
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
