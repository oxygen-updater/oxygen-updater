package com.arjanvlek.oxygenupdater.versionformatter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateDataVersionFormatter {

	/**
	 * Prefix to check if we can format anything of the update info.
	 */
	private static final String OS_VERSION_LINE_HEADING = "#";


	/**
	 * Expression which is used to obtain the version number from a regular update of OxygenOS
	 */
	private static final Pattern REGULAR_PATTERN = Pattern.compile("(\\d+\\.\\d+(\\.\\d+)*)$");

	/**
	 * Expression which indicates that this update is a beta update of OxygenOS.
	 */
	private static final Pattern BETA_PATTERN = Pattern.compile("(open|beta)_(\\w+)$"); // \\w, because sometimes a date string is appended to the version number


	/**
	 * User-readable name for regular versions of OxygenOS
	 */
	private static final String OXYGEN_OS_REGULAR_DISPLAY_PREFIX = "Oxygen OS ";

	/**
	 * User-readable name for beta versions of OxygenOS
	 */
	private static final String OXYGEN_OS_BETA_DISPLAY_PREFIX = "Oxygen OS Open Beta ";


	/**
	 * Checks if the passed update version information contains a version number which can be
	 * formatted
	 *
	 * @param versionInfo Update version information to check
	 *
	 * @return Whether or not this update version information contains a version number which can be
	 * formatted.
	 */
	public static boolean canVersionInfoBeFormatted(FormattableUpdateData versionInfo) {
		String firstLine = getFirstLine(versionInfo);
		return firstLine != null && firstLine.trim().startsWith(OS_VERSION_LINE_HEADING);
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
	public static String getFormattedVersionNumber(FormattableUpdateData versionInfo) {
		if (!canVersionInfoBeFormatted(versionInfo)) {
			return versionInfo != null
					&& versionInfo.getInternalVersionNumber() != null
					&& !versionInfo.getInternalVersionNumber().equals("") ? "V. " + versionInfo.getInternalVersionNumber() : "Oxygen OS System Update";
		}

		String firstLine = getFirstLine(versionInfo); // Can never be null due to check canVersionInfoBeFormatted
		String firstLineLowerCase = firstLine.toLowerCase();

		Matcher betaMatcher = BETA_PATTERN.matcher(firstLineLowerCase);
		Matcher regularMatcher = REGULAR_PATTERN.matcher(firstLineLowerCase);

		if (betaMatcher.find()) {
			return OXYGEN_OS_BETA_DISPLAY_PREFIX + betaMatcher.group(2);
		} else if (regularMatcher.find()) {
			return OXYGEN_OS_REGULAR_DISPLAY_PREFIX + regularMatcher.group();
		} else {
			return firstLine.replace(OS_VERSION_LINE_HEADING, "");
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
	private static String getFirstLine(FormattableUpdateData versionInfo) {
		if (versionInfo == null || versionInfo.getUpdateDescription() == null || versionInfo.getUpdateDescription().equals("")) {
			return "";
		}

		BufferedReader reader = new BufferedReader(new StringReader(versionInfo.getUpdateDescription()));

		try {
			String line = reader.readLine();
			return line == null ? "" : line;
		} catch (IOException e) {
			return "";
		}
	}
}
