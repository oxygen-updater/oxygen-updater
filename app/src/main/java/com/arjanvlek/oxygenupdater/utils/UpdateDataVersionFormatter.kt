package com.arjanvlek.oxygenupdater.utils

import com.arjanvlek.oxygenupdater.models.FormattableUpdateData
import java.io.BufferedReader
import java.io.IOException
import java.io.StringReader
import java.util.*
import java.util.regex.Pattern

object UpdateDataVersionFormatter {

    /**
     * Prefix to check if we can format anything of the update info.
     */
    private const val OS_VERSION_LINE_HEADING = "#"

    /**
     * Expression which is used to obtain the version number from a regular update of OxygenOS
     */
    private val REGULAR_PATTERN = Pattern.compile("(\\d+\\.\\d+(\\.\\d+)*)$")

    /**
     * Expression which indicates that this update is a beta update of OxygenOS.
     * `_\\w`: because sometimes a date string is appended to the version number
     */
    private val BETA_PATTERN = Pattern.compile("(open|beta)_(\\w+)$")

    /**
     * User-readable name for regular versions of OxygenOS
     */
    private const val OXYGEN_OS_REGULAR_DISPLAY_PREFIX = "OxygenOS "

    /**
     * User-readable name for beta versions of OxygenOS
     */
    private const val OXYGEN_OS_BETA_DISPLAY_PREFIX = "OxygenOS Open Beta "

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
            val firstLineLowerCase = firstLine.toLowerCase(Locale.getDefault())
            val betaMatcher = BETA_PATTERN.matcher(firstLineLowerCase)
            val regularMatcher = REGULAR_PATTERN.matcher(firstLineLowerCase)

            when {
                betaMatcher.find() -> OXYGEN_OS_BETA_DISPLAY_PREFIX + betaMatcher.group(2)
                regularMatcher.find() -> OXYGEN_OS_REGULAR_DISPLAY_PREFIX + regularMatcher.group()
                else -> firstLine.replace(OS_VERSION_LINE_HEADING, "")
            }
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
