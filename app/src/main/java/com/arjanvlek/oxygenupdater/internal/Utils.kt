package com.arjanvlek.oxygenupdater.internal

import android.content.Context
import android.net.ConnectivityManager
import android.text.format.DateFormat
import android.util.TypedValue
import androidx.annotation.Dimension
import com.arjanvlek.oxygenupdater.ApplicationData
import com.arjanvlek.oxygenupdater.BuildConfig
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.domain.Device
import com.arjanvlek.oxygenupdater.domain.DeviceOsSpec
import com.arjanvlek.oxygenupdater.domain.DeviceOsSpec.CARRIER_EXCLUSIVE_OXYGEN_OS
import com.arjanvlek.oxygenupdater.domain.DeviceOsSpec.SUPPORTED_OXYGEN_OS
import com.arjanvlek.oxygenupdater.domain.DeviceOsSpec.UNSUPPORTED_OS
import com.arjanvlek.oxygenupdater.domain.DeviceOsSpec.UNSUPPORTED_OXYGEN_OS
import com.arjanvlek.oxygenupdater.domain.SystemVersionProperties
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logError
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logWarning
import org.joda.time.LocalDateTime
import java.util.*

@Suppress("unused")
object Utils {

    private val random = Random()

    /**
     * Originally part of [com.google.android.gms.common.util.NumberUtils], removed in later
     * versions
     *
     * @param string the string to test
     *
     * @return true if string is a number, false otherwise
     */
    fun isNumeric(string: String): Boolean {
        return try {
            string.toLong()
            true
        } catch (e: NumberFormatException) {
            false
        }
    }

    fun dpToPx(context: Context, @Dimension(unit = Dimension.DP) dp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics)
    }

    fun spToPx(context: Context, @Dimension(unit = Dimension.SP) sp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, context.resources.displayMetrics)
    }

    @JvmStatic
    fun checkNetworkConnection(context: Context?): Boolean {
        if (context == null) {
            logWarning("Utils", OxygenUpdaterException("CheckNetworkConnection: check skipped due to empty / null context"))
            return false
        }

        val connectivityManager = getSystemService(context, Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        return connectivityManager?.activeNetworkInfo?.isConnectedOrConnecting == true
    }

    @JvmStatic
    @Synchronized
    fun checkDeviceOsSpec(systemVersionProperties: SystemVersionProperties, devices: List<Device>?): DeviceOsSpec {
        val oemFingerPrint: String? = systemVersionProperties.oemFingerprint
        val oxygenOsVersion: String? = systemVersionProperties.oxygenOSVersion

        val firmwareIsSupported = oemFingerPrint != null
                && oemFingerPrint != ApplicationData.NO_OXYGEN_OS
                && oemFingerPrint.contains(BuildConfig.SUPPORTED_BUILD_FINGERPRINT_KEYS)
                && oxygenOsVersion != null
                && oxygenOsVersion != ApplicationData.NO_OXYGEN_OS

        if (devices.isNullOrEmpty()) {
            // To prevent incorrect results on empty server response. This still checks if official ROM is used and if an oxygen os version is found on the device.
            return if (firmwareIsSupported) {
                SUPPORTED_OXYGEN_OS
            } else {
                UNSUPPORTED_OS
            }
        }

        return if (firmwareIsSupported) {
            // user's device is definitely running OxygenOS, now onto other checks...
            devices.forEach {
                // find the user's device in the list of devices retrieved from the server
                if (it.productNames?.contains(systemVersionProperties.oxygenDeviceName) == true) {
                    return if (it.enabled) {
                        // device found, and is enabled, which means it is supported
                        SUPPORTED_OXYGEN_OS
                    } else {
                        // device found, but is disabled, which means it's a carrier-exclusive
                        // because only carrier-exclusive devices are disabled in the database
                        CARRIER_EXCLUSIVE_OXYGEN_OS
                    }
                }
            }

            // device not found among the server-provided list
            // hence, must be a newly-released OnePlus device that we're yet to add support for
            UNSUPPORTED_OXYGEN_OS
        } else {
            // device isn't running OxygenOS at all
            // note: the device may very well be a OnePlus device, but in this case it's running a custom ROM, which we don't support duh
            UNSUPPORTED_OS
        }
    }

    fun formatDateTime(context: Context, dateTimeString: String?): String? {
        var dateTimeString = dateTimeString

        return try {
            if (dateTimeString == null || dateTimeString.isEmpty()) {
                return context.getString(R.string.device_information_unknown)
            }

            dateTimeString = dateTimeString.replace(" ", "T")

            val dateTime = LocalDateTime.parse(dateTimeString)
            val timeFormat = DateFormat.getTimeFormat(context)
            val formattedTime = timeFormat.format(dateTime.toDate())
            val today = LocalDateTime.now()

            if (dateTime.dayOfMonth == today.dayOfMonth && dateTime.monthOfYear == today.monthOfYear && dateTime.year == today.year) {
                formattedTime
            } else if (dateTime.dayOfMonth + 1 == today.dayOfMonth && dateTime.monthOfYear == today.monthOfYear && dateTime.year == today.year) {
                context.getString(R.string.time_yesterday) + " " + context.getString(R.string.time_at) + " " + formattedTime
            } else {
                val dateFormat = DateFormat.getDateFormat(context)
                val formattedDate = dateFormat.format(dateTime.toDate())

                formattedDate + " " + context.getString(R.string.time_at) + " " + formattedTime
            }
        } catch (e: Exception) {
            logError("DateTimeFormatter", String.format("Unable to parse date from input '%s'", dateTimeString), e)
            dateTimeString
        }
    }

    fun getSystemService(context: Context?, serviceName: String): Any? {
        return context?.getSystemService(serviceName)
    }

    /**
     * Min is inclusive and max is exclusive in this case
     */
    fun randomBetween(min: Int, max: Int): Int {
        return random.nextInt(max - min) + min
    }
}
