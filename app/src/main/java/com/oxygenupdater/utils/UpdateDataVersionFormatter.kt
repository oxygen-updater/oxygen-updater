package com.oxygenupdater.utils

import android.os.Build
import com.oxygenupdater.models.UpdateData
import java.util.regex.Pattern

object UpdateDataVersionFormatter {

    /**
     * Prefix to check if we can format anything of the update info.
     */
    private const val OsVersionLineHeading = "#"

    /**
     * Basic semver (`major.minor.patch` only), but modified to include:
     * * an optional "count" number after the "patch" (this is present in 8T builds)
     * * an optional build tag at the end (e.g. `IN21DA` for OP8_IND)
     *
     * @see <a href="https://semver.org/#is-there-a-suggested-regular-expression-regex-to-check-a-semver-string">semver.org</a>
     */
    private val StablePattern = Pattern.compile(
        "((?:0|[1-9]\\d*)\\.(?:0|[1-9]\\d*)\\.(?:0|[1-9]\\d*)(?:\\.(?:0|[1-9]\\d*))?(?:\\.[A-Z]{2}\\d{2}[A-Z]{2})?)$",
        Pattern.CASE_INSENSITIVE
    )

    /**
     * Expression which indicates that this update is a beta update of OxygenOS.
     * `_\\w`: because sometimes a date string is appended to the version number
     */
    private val AlphaBetaDpPattern = Pattern.compile(
        "(open|beta|alpha|dp)_(\\w+)$",
        Pattern.CASE_INSENSITIVE
    )

    /**
     * User-readable name for regular versions of OxygenOS
     */
    private const val OxygenOsPrefix = "OxygenOS"

    /**
     * User-readable name for beta versions of OxygenOS
     */
    private const val BetaPrefix = "Open Beta"

    /**
     * User-readable name for alpha versions of OxygenOS
     */
    private const val AlphaPrefix = "Closed Beta"

    /**
     * User-readable name for developer preview versions of OxygenOS
     */
    private const val DpPrefix = "DP"

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
    fun getFormattedVersionNumber(
        versionInfo: UpdateData?,
        default: String = "OxygenOS System Update",
    ): String {
        val firstLine = versionInfo?.description.run {
            if (isNullOrBlank()) "" else splitToSequence("\r\n", "\n", "\r", limit = 2).firstOrNull() ?: ""
        }

        val canVersionInfoBeFormatted = firstLine.trim().startsWith(OsVersionLineHeading)
        return if (canVersionInfoBeFormatted) {
            val alphaBetaDpMatcher = AlphaBetaDpPattern.matcher(firstLine)
            val stableMatcher = StablePattern.matcher(firstLine)

            when {
                alphaBetaDpMatcher.find() -> when (alphaBetaDpMatcher.group(1)?.lowercase() ?: "") {
                    "alpha" -> "$OxygenOsPrefix $AlphaPrefix ${alphaBetaDpMatcher.group(2)}"
                    "beta" -> "$OxygenOsPrefix $BetaPrefix ${alphaBetaDpMatcher.group(2)}"
                    "dp" -> "Android ${Build.VERSION.RELEASE} $DpPrefix ${alphaBetaDpMatcher.group(2)}"
                    else -> "$OxygenOsPrefix $BetaPrefix ${alphaBetaDpMatcher.group(2)}"
                }

                stableMatcher.find() -> "$OxygenOsPrefix ${stableMatcher.group()}"
                else -> firstLine.replace(OsVersionLineHeading, "")
            }
        } else versionInfo?.versionNumber.run {
            if (isNullOrBlank()) default else this
        }
    }

    /**
     * Used for formatting [com.oxygenupdater.models.SystemVersionProperties.oxygenOSVersion]
     */
    fun getFormattedOxygenOsVersion(version: String) = if (version.isBlank()) Build.UNKNOWN else {
        val alphaBetaDpMatcher = AlphaBetaDpPattern.matcher(version)
        if (alphaBetaDpMatcher.find()) when (alphaBetaDpMatcher.group(1)?.lowercase() ?: "") {
            "alpha" -> "$AlphaPrefix ${alphaBetaDpMatcher.group(2)}"
            "beta" -> "$BetaPrefix ${alphaBetaDpMatcher.group(2)}"
            "dp" -> "Android ${Build.VERSION.RELEASE} $DpPrefix ${alphaBetaDpMatcher.group(2)}"
            else -> "$BetaPrefix ${alphaBetaDpMatcher.group(2)}"
        } else version
    }
}
