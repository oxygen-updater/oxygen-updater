package com.oxygenupdater.utils

import com.oxygenupdater.OxygenUpdater.Companion.NO_OXYGEN_OS
import com.oxygenupdater.internal.DeviceInformationData
import com.oxygenupdater.models.FormattableUpdateData
import java.io.BufferedReader
import java.io.IOException
import java.io.StringReader
import java.util.regex.Pattern

object UpdateDataVersionFormatter {

    /**
     * Prefix to check if we can format anything of the update info.
     */
    private const val OS_VERSION_LINE_HEADING = "#"

    /**
     * Basic semver (`major.minor.patch` only), but modified to include:
     * * an optional "count" number after the "patch" (this is present in 8T builds)
     * * an optional build tag at the end (e.g. `IN21DA` for OP8_IND)
     *
     * @see <a href="https://semver.org/#is-there-a-suggested-regular-expression-regex-to-check-a-semver-string">semver.org</a>
     */
    private val STABLE_PATTERN = Pattern.compile(
        "((?:0|[1-9]\\d*)\\.(?:0|[1-9]\\d*)\\.(?:0|[1-9]\\d*)(?:\\.(?:0|[1-9]\\d*))?(?:\\.(?:[A-Z]{2}\\d{2}[A-Z]{2}))?)$",
        Pattern.CASE_INSENSITIVE
    )

    /**
     * Expression which indicates that this update is a beta update of OxygenOS.
     * `_\\w`: because sometimes a date string is appended to the version number
     */
    private val ALPHA_BETA_DP_PATTERN = Pattern.compile(
        "(open|beta|alpha|dp)_(\\w+)$",
        Pattern.CASE_INSENSITIVE
    )

    /**
     * User-readable name for regular versions of OxygenOS
     */
    const val OXYGEN_OS_PREFIX = "OxygenOS"

    /**
     * User-readable name for beta versions of OxygenOS
     */
    const val BETA_PREFIX = "Open Beta"

    /**
     * User-readable name for alpha versions of OxygenOS
     */
    const val ALPHA_PREFIX = "Closed Beta"

    /**
     * User-readable name for developer preview versions of OxygenOS
     */
    const val DP_PREFIX = "DP"

    /**
     * Checks if the passed update version information contains a version number which can be
     * formatted
     *
     * @param versionInfo Update version information to check
     *
     * @return Whether or not this update version information contains a version number which can be
     * formatted.
     */
    fun canVersionInfoBeFormatted(versionInfo: FormattableUpdateData?) = getFirstLine(versionInfo)
        .trim { it <= ' ' }
        .startsWith(OS_VERSION_LINE_HEADING)

    /**
     * Get the formatted version number of the passed update version information. The output is as
     * following: Regular update: "OxygenOS 4.0.0" Open Beta update: "OxygenOS Open Beta 28" Other
     * update with "(#)OS Version": "OS Version: Windows 10 1709" Update without "(#)OS Version":
     * "V. OnePlus5TOxygen_23_1804231200" Null versionInfo / no internal version number in
     * versionInfo: "OxygenOS System Update"
     *
     * @param versionInfo Update version information to get the formatted version number for.
     *
     * @return Formatted version number for the given Update Version Information.
     */
    fun getFormattedVersionNumber(versionInfo: FormattableUpdateData?): String {
        val firstLine = getFirstLine(versionInfo)

        // we could use the `canVersionInfoBeFormatted(versionInfo)` function defined in this file,
        // but this is a performance/memory optimization.
        // `canVersionInfoBeFormatted(versionInfo)` calls `getFirstLine(versionInfo)`, which is also called to set the value for `firstLine`.
        // `firstLine` is used in the else branch, so it's better to call `getFirstLine(versionInfo)` only once.
        // `getFirstLine(versionInfo)` constructs a `BufferedReader` and a `StringReader`, so this optimization saves some CPU cycles, and some memory as well
        val canVersionInfoBeFormatted = firstLine.trim { it <= ' ' }.startsWith(OS_VERSION_LINE_HEADING)

        return if (!canVersionInfoBeFormatted) {
            if (!versionInfo?.internalVersionNumber.isNullOrBlank()) "V. " + versionInfo!!.internalVersionNumber
            else "OxygenOS System Update"
        } else {
            val alphaBetaMatcher = ALPHA_BETA_DP_PATTERN.matcher(firstLine)
            val regularMatcher = STABLE_PATTERN.matcher(firstLine)

            when {
                alphaBetaMatcher.find() -> if (alphaBetaMatcher.group(1)?.lowercase() == "alpha") {
                    "$OXYGEN_OS_PREFIX $ALPHA_PREFIX ${alphaBetaMatcher.group(2)}"
                } else {
                    "$OXYGEN_OS_PREFIX $BETA_PREFIX ${alphaBetaMatcher.group(2)}"
                }
                regularMatcher.find() -> "$OXYGEN_OS_PREFIX ${regularMatcher.group()}"
                else -> firstLine.replace(OS_VERSION_LINE_HEADING, "")
            }
        }
    }

    /**
     * Used for formatting [com.oxygenupdater.models.SystemVersionProperties.oxygenOSVersion]
     */
    fun getFormattedOxygenOsVersion(version: String): String {
        val alphaBetaDpMatcher = ALPHA_BETA_DP_PATTERN.matcher(version)
        val regularMatcher = STABLE_PATTERN.matcher(version)

        return when {
            version == NO_OXYGEN_OS || version.isBlank() -> NO_OXYGEN_OS
            alphaBetaDpMatcher.find() -> when (alphaBetaDpMatcher.group(1)?.lowercase() ?: "") {
                "alpha" -> "$OXYGEN_OS_PREFIX $ALPHA_PREFIX ${alphaBetaDpMatcher.group(2)}"
                "beta" -> "$OXYGEN_OS_PREFIX $BETA_PREFIX ${alphaBetaDpMatcher.group(2)}"
                "dp" -> "Android ${DeviceInformationData.osVersion} $DP_PREFIX ${alphaBetaDpMatcher.group(2)}"
                else -> "$OXYGEN_OS_PREFIX $BETA_PREFIX ${alphaBetaDpMatcher.group(2)}"
            }
            regularMatcher.find() -> "$OXYGEN_OS_PREFIX ${regularMatcher.group()}"
            else -> "$OXYGEN_OS_PREFIX $version"
        }
    }

    /**
     * Returns the first line of the update description of the given Version Info
     *
     * @param versionInfo Version Info to get the first line of.
     *
     * @return The first line of the update description of the given Version Info, or the empty
     * string if the update description is null or empty.
     */
    private fun getFirstLine(versionInfo: FormattableUpdateData?): String {
        if (versionInfo?.updateDescription.isNullOrBlank()) {
            return ""
        }

        return try {
            BufferedReader(StringReader(versionInfo?.updateDescription)).readLine() ?: ""
        } catch (e: IOException) {
            ""
        }
    }
}
