package com.arjanvlek.oxygenupdater.utils

import android.app.Activity
import android.content.Context
import android.util.TypedValue
import androidx.annotation.Dimension
import com.arjanvlek.oxygenupdater.BuildConfig
import com.arjanvlek.oxygenupdater.OxygenUpdater
import com.arjanvlek.oxygenupdater.OxygenUpdater.Companion.isNetworkAvailable
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.internal.settings.SettingsManager
import com.arjanvlek.oxygenupdater.models.Device
import com.arjanvlek.oxygenupdater.models.DeviceOsSpec
import com.arjanvlek.oxygenupdater.models.DeviceOsSpec.CARRIER_EXCLUSIVE_OXYGEN_OS
import com.arjanvlek.oxygenupdater.models.DeviceOsSpec.SUPPORTED_OXYGEN_OS
import com.arjanvlek.oxygenupdater.models.DeviceOsSpec.UNSUPPORTED_OS
import com.arjanvlek.oxygenupdater.models.DeviceOsSpec.UNSUPPORTED_OXYGEN_OS
import com.arjanvlek.oxygenupdater.models.SystemVersionProperties
import com.arjanvlek.oxygenupdater.utils.Logger.logError
import com.arjanvlek.oxygenupdater.utils.Logger.logVerbose
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import org.koin.java.KoinJavaComponent.inject
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle
import kotlin.system.exitProcess

@Suppress("unused")
object Utils {

    val SERVER_TIME_ZONE: ZoneId = ZoneId.of("Europe/Amsterdam")
    val USER_TIME_ZONE: ZoneId = ZoneId.systemDefault()

    private const val TAG = "Utils"
    private const val PLAY_SERVICES_RESOLUTION_REQUEST = 9000

    private val systemVersionProperties by inject(SystemVersionProperties::class.java)
    private val settingsManager by inject(SettingsManager::class.java)

    /**
     * Originally part of [com.google.android.gms.common.util.NumberUtils], removed in later versions
     *
     * @param string the string to test
     *
     * @return true if string is a number, false otherwise
     */
    fun isNumeric(string: String) = try {
        string.toLong()
        true
    } catch (e: NumberFormatException) {
        false
    }

    fun dpToPx(context: Context, @Dimension(unit = Dimension.DP) dp: Float) = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp,
        context.resources.displayMetrics
    )

    fun spToPx(context: Context, @Dimension(unit = Dimension.SP) sp: Float) = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        sp,
        context.resources.displayMetrics
    )

    /**
     * Checks if the Google Play Services are installed on the device.
     *
     * @return Returns if the Google Play Services are installed.
     */
    fun checkPlayServices(activity: Activity?, showErrorIfMissing: Boolean): Boolean {
        logVerbose(TAG, "Executing Google Play Services check...")

        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(activity)

        return if (resultCode != ConnectionResult.SUCCESS && showErrorIfMissing) {
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                googleApiAvailability.getErrorDialog(
                    activity,
                    resultCode,
                    PLAY_SERVICES_RESOLUTION_REQUEST
                ).show()
            } else {
                exitProcess(0)
            }

            logVerbose(TAG, "Google Play Services are *NOT* available! Ads and notifications are not supported!")
            false
        } else {
            val result = resultCode == ConnectionResult.SUCCESS

            if (result) {
                logVerbose(TAG, "Google Play Services are available.")
            } else {
                logVerbose(TAG, "Google Play Services are *NOT* available! Ads and notifications are not supported!")
            }

            result
        }
    }

    fun checkNetworkConnection() = isNetworkAvailable.value == true

    fun checkDeviceOsSpec(devices: List<Device>?): DeviceOsSpec {
        val oemFingerPrint: String? = systemVersionProperties.oemFingerprint
        val oxygenOsVersion: String? = systemVersionProperties.oxygenOSVersion

        val firmwareIsSupported = !oemFingerPrint.isNullOrEmpty()
                && oemFingerPrint != OxygenUpdater.NO_OXYGEN_OS
                && oemFingerPrint.contains(BuildConfig.SUPPORTED_BUILD_FINGERPRINT_KEYS)
                && !oxygenOsVersion.isNullOrEmpty()
                && oxygenOsVersion != OxygenUpdater.NO_OXYGEN_OS

        if (devices.isNullOrEmpty()) {
            // To prevent incorrect results on empty server response. This still checks if official ROM is used and if an OxygenOS version is found on the device.
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
                if (it.productNames.contains(systemVersionProperties.oxygenDeviceName)) {
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
        @Suppress("NAME_SHADOWING")
        var dateTimeString = dateTimeString

        return try {
            if (dateTimeString == null || dateTimeString.isEmpty()) {
                return context.getString(R.string.device_information_unknown)
            }

            dateTimeString = dateTimeString.replace(" ", "T")

            val serverDateTime = LocalDateTime.parse(dateTimeString)
                .atZone(SERVER_TIME_ZONE)
                .withSecond(0)
                .withNano(0)

            val serverLocalDate = serverDateTime.toLocalDate()
            val userDateTime = serverDateTime.withZoneSameInstant(USER_TIME_ZONE)
            val formattedTime = userDateTime.toLocalTime().toString()
            val today = LocalDate.now()

            when {
                serverLocalDate.isEqual(today) -> formattedTime
                serverLocalDate.isEqual(today.minusDays(1)) -> context.getString(R.string.time_yesterday) + " " +
                        context.getString(R.string.time_at) + " " +
                        formattedTime
                else -> userDateTime.toLocalDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) + " " +
                        context.getString(R.string.time_at) + " " +
                        formattedTime
            }
        } catch (e: Exception) {
            logError("DateTimeFormatter", String.format("Unable to parse date from input '%s'", dateTimeString), e)
            dateTimeString
        }
    }
}
