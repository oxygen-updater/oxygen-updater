package com.arjanvlek.oxygenupdater.internal

import android.content.Context
import android.net.ConnectivityManager
import android.util.TypedValue
import androidx.annotation.Dimension
import com.arjanvlek.oxygenupdater.ApplicationData.Companion.NO_OXYGEN_OS
import com.arjanvlek.oxygenupdater.BuildConfig
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.domain.Device
import com.arjanvlek.oxygenupdater.domain.SystemVersionProperties
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logError
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logWarning
import java8.util.stream.StreamSupport
import org.joda.time.LocalDateTime
import java.util.*

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
            java.lang.Long.parseLong(string)
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

    fun checkNetworkConnection(context: Context?): Boolean {
        if (context == null) {
            logWarning("Utils", OxygenUpdaterException("CheckNetworkConnection: check skipped due to empty / null context"))
            return false
        }

        val connectivityManager = getSystemService(context, Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        val activeNetwork = connectivityManager?.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting
    }

    @Synchronized
    fun isSupportedDevice(systemVersionProperties: SystemVersionProperties, devices: List<Device>?): Boolean {
        val oemFingerPrint = systemVersionProperties.oemFingerprint
        val oxygenOsVersion = systemVersionProperties.oxygenOSVersion

        val firmwareIsSupported = (oemFingerPrint != NO_OXYGEN_OS && oemFingerPrint.contains(BuildConfig.SUPPORTED_BUILD_FINGERPRINT_KEYS) && oxygenOsVersion != NO_OXYGEN_OS)

        if (devices.isNullOrEmpty()) {
            // To prevent incorrect results on empty server response. This still checks if official ROM is used and if an oxygen os version is found on the device.
            return firmwareIsSupported
        }

        val supportedDevice = StreamSupport.stream(devices)
                .filter { d -> d.productNames != null && d.productNames!!.contains(systemVersionProperties.oxygenDeviceName) }
                .findAny()

        return supportedDevice.isPresent && firmwareIsSupported
    }

    fun formatDateTime(context: Context, dateTimeString: String?): String? {
        var dateTimeString = dateTimeString
        try {
            if (dateTimeString.isNullOrEmpty()) {
                return context.getString(R.string.device_information_unknown)
            }
            dateTimeString = dateTimeString.replace(" ", "T")
            val dateTime = LocalDateTime.parse(dateTimeString)
            val timeFormat = android.text.format.DateFormat.getTimeFormat(context)
            val formattedTime = timeFormat.format(dateTime.toDate())

            val today = LocalDateTime.now()
            return if (dateTime.dayOfMonth == today.dayOfMonth && dateTime.monthOfYear == today.monthOfYear && dateTime.year == today.year) {
                formattedTime
            } else if (dateTime.dayOfMonth + 1 == today.dayOfMonth && dateTime.monthOfYear == today.monthOfYear && dateTime.year == today.year) {
                context.getString(R.string.time_yesterday) + " " + context.getString(R.string.time_at) + " " + formattedTime
            } else {
                val dateFormat = android.text.format.DateFormat.getDateFormat(context)
                val formattedDate = dateFormat.format(dateTime.toDate())
                formattedDate + " " + context.getString(R.string.time_at) + " " + formattedTime
            }
        } catch (e: Exception) {
            logError("DateTimeFormatter", String.format("Unable to parse date from input '%s'", dateTimeString), e)
            return dateTimeString
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
