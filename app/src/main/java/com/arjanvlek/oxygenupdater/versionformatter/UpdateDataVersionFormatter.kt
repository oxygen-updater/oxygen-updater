package com.arjanvlek.oxygenupdater.versionformatter

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
     */
    private val BETA_PATTERN = Pattern.compile("(open|beta)_(\\w+)$") // \\w, because sometimes a date string is appended to the version number
    /**
     * User-readable name for regular versions of OxygenOS
     */
    private const val OXYGEN_OS_REGULAR_DISPLAY_PREFIX = "Oxygen OS "
    /**
     * User-readable name for beta versions of OxygenOS
     */
    private const val OXYGEN_OS_BETA_DISPLAY_PREFIX = "Oxygen OS Open Beta "

    /**
     * Checks if the passed update version information contains a version number which can be
     * formatted
     *
     * @param versionInfo Update version information to check
     *
     * @return Whether or not this update version information contains a version number which can be
     * formatted.
     */
    fun canVersionInfoBeFormatted(versionInfo: FormattableUpdateData?): Boolean {
        return getFirstLine(versionInfo).trim { it <= ' ' }.startsWith(OS_VERSION_LINE_HEADING)
    }

    /**
     * Get the formatted version number of the passed update version information. The output is as
     * following: Regular update: "Oxygen OS 4.0.0" Open Beta update: "Oxygen OS Open Beta 28" Other
     * update with "(#)OS Version": "OS Version: Windows 10 1709" Update without "(#)OS Version":
     * "V. OnePlus5TOxygen_23_1804231200" Null versionInfo / no internal version number in
     * versionInfo: "Oxygen OS System Update"
     *
     * @param versionInfo Update version information to get the formatted version number for.
     *
     * @return Formatted version number for the given Update Version Information.
     */
    fun getFormattedVersionNumber(versionInfo: FormattableUpdateData?): String {
        if (!canVersionInfoBeFormatted(versionInfo)) {
            return if (!versionInfo?.internalVersionNumber.isNullOrBlank()) "V. " + versionInfo!!.internalVersionNumber else "Oxygen OS System Update"
        }

        val firstLine = getFirstLine(versionInfo) // Can never be null due to check canVersionInfoBeFormatted
        val firstLineLowerCase = firstLine.toLowerCase(Locale.getDefault())
        val betaMatcher = BETA_PATTERN.matcher(firstLineLowerCase)
        val regularMatcher = REGULAR_PATTERN.matcher(firstLineLowerCase)

        return when {
            betaMatcher.find() -> OXYGEN_OS_BETA_DISPLAY_PREFIX + betaMatcher.group(2)
            regularMatcher.find() -> OXYGEN_OS_REGULAR_DISPLAY_PREFIX + regularMatcher.group()
            else -> firstLine.replace(OS_VERSION_LINE_HEADING, "")
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
