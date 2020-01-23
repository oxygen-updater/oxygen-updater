package com.arjanvlek.oxygenupdater.updateinformation

import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.URLSpan
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logError
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logInfo
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logVerbose
import com.arjanvlek.oxygenupdater.models.FormattableUpdateData
import com.arjanvlek.oxygenupdater.versionformatter.UpdateDataVersionFormatter
import java.util.*

object UpdateDescriptionParser {
    private const val TAG = "UpdateDescriptionParser"
    private const val EMPTY_STRING = ""

    fun parse(updateDescription: String): Spanned {
        val result: SpannableString
        val links: MutableMap<String, String> = HashMap()

        try {
            var modifiedUpdateDescription = EMPTY_STRING

            // First, loop through all lines and modify them were needed.
            // This consists of removing heading symbols, making list items, adding line separators and displaying link texts.
            updateDescription.lines().forEach { currentLine ->
                val element = UpdateDescriptionElement.of(currentLine)
                var modifiedLine = StringBuilder(EMPTY_STRING)

                // If the current line contains the OxygenOS version number, skip it as it will be displayed as the update title.
                if (UpdateDataVersionFormatter.canVersionInfoBeFormatted(LineDetectingUpdateInfo(currentLine)) && element == UpdateDescriptionElement.HEADING_1) {
                    // skip this line, continue to the next line
                    return@forEach
                }

                when (element) {
                    UpdateDescriptionElement.HEADING_3 -> modifiedLine = StringBuilder(currentLine.replace("###", ""))
                    UpdateDescriptionElement.HEADING_2 -> modifiedLine = StringBuilder(currentLine.replace("##", ""))
                    UpdateDescriptionElement.HEADING_1 -> modifiedLine = StringBuilder(currentLine.replace("#", ""))
                    UpdateDescriptionElement.LIST_ITEM -> {
                        modifiedLine = StringBuilder(currentLine.replace("*", "•"))

                        // There could also be multiple OnePlus line separators in this line.
                        // Replace each OnePlus line separator with an actual line separator.
                        currentLine.forEach {
                            if (it == '\\') {
                                modifiedLine.append("\n")
                            }
                        }
                    }
                    UpdateDescriptionElement.LINE_SEPARATORS -> {
                        currentLine.forEach {
                            if (it == '\\') {
                                modifiedLine.append("\n")
                            }
                        }
                    }
                    UpdateDescriptionElement.LINK -> {
                        val linkTitle = currentLine.substring(currentLine.indexOf("[") + 1, currentLine.lastIndexOf("]"))

                        val linkAddress = if (currentLine.contains("(") && currentLine.contains(")")) {
                            currentLine.substring(currentLine.indexOf("(") + 1, currentLine.lastIndexOf(")"))
                        } else if (currentLine.contains("{") && currentLine.contains("}")) {
                            currentLine.substring(currentLine.indexOf("{") + 1, currentLine.lastIndexOf("}"))
                        } else {
                            ""
                        }

                        // We need to save the full URL somewhere, to point the browser to it when clicked...
                        links[linkTitle] = linkAddress

                        // The link title will be displayed. It will also be used to look up the full url when clicked.
                        modifiedLine = StringBuilder(linkTitle)
                    }
                    else -> modifiedLine = StringBuilder(currentLine)
                }

                modifiedUpdateDescription = modifiedUpdateDescription + modifiedLine.toString() + if (element == UpdateDescriptionElement.LINE_SEPARATORS) "" else "\n"
            }

            result = SpannableString(modifiedUpdateDescription)

            // Finally, loop through the modified update description and set formatting attributes for the headers and links.
            modifiedUpdateDescription.lines().forEach { currentLine ->
                if (currentLine.isEmpty()) {
                    // skip this line, continue to the next line
                    return@forEach
                }

                val element = UpdateDescriptionElement.find(currentLine, updateDescription)
                val startPosition = modifiedUpdateDescription.indexOf(currentLine)
                val endPosition = startPosition + currentLine.length

                when (element) {
                    UpdateDescriptionElement.HEADING_1 -> {
                        // Heading 1 should be made bold and pretty large.
                        result.setSpan(RelativeSizeSpan(1.3f), startPosition, endPosition, 0)
                        result.setSpan(StyleSpan(Typeface.BOLD), startPosition, endPosition, 0)
                    }
                    UpdateDescriptionElement.HEADING_2 -> {
                        // Heading 2 should be made bold and a bit larger than normal, but smaller than heading 1.
                        result.setSpan(RelativeSizeSpan(1.1f), startPosition, endPosition, 0)
                        result.setSpan(StyleSpan(Typeface.BOLD), startPosition, endPosition, 0)
                    }
                    UpdateDescriptionElement.HEADING_3 ->
                        // Heading 3 is the same size as normal text but will be displayed in bold.
                        result.setSpan(StyleSpan(Typeface.BOLD), startPosition, endPosition, 0)
                    UpdateDescriptionElement.LINK ->
                        // A link should be made clickable and must be displayed as a hyperlink.
                        result.setSpan(URLSpan(links[currentLine]), startPosition, endPosition, 0)
                    else -> logInfo(TAG, "Case not implemented: $element")
                }
            }
        } catch (e: Exception) {
            // If an error occurred, log it and return the original / unmodified update description
            logError(TAG, "Error parsing update description", e)
            return SpannableString(updateDescription)
        }

        return result
    }

    private enum class UpdateDescriptionElement {
        // Grammar for OnePlus update descriptions
        HEADING_1,          // #(char*)\n
        HEADING_2,          // ##(char*)\n
        HEADING_3,          // ###(char*)\n
        LINE_SEPARATORS,    // \(\*)\n
        LIST_ITEM,          // *(char*)\n
        LINK,               // [(char*)](space*)((char*))|{(char*)}\n
        TEXT,               // (char*)
        EMPTY;

        companion object {
            // Finds the element type for a given line of OnePlus formatted text.
            fun of(inputLine: String?): UpdateDescriptionElement {
                // The empty string gets parsed as EMPTY
                logVerbose(TAG, "Input line: $inputLine")
                return if (inputLine.isNullOrEmpty()) {
                    logVerbose(TAG, "Matched type: EMPTY")
                    EMPTY
                } else if (inputLine.contains("###")) {
                    logVerbose(TAG, "Matched type: HEADING_3")
                    HEADING_3
                } else if (inputLine.contains("##")) {
                    logVerbose(TAG, "Matched type: HEADING_2")
                    HEADING_2
                } else if (inputLine.contains("#")) {
                    logVerbose(TAG, "Matched type: HEADING_1")
                    HEADING_1
                } else if (inputLine.contains("*")) {
                    logVerbose(TAG, "Matched type: LIST_ITEM")
                    LIST_ITEM
                } else if (inputLine.contains("\\")) {
                    logVerbose(TAG, "Matched type: LINE_SEPARATORS")
                    LINE_SEPARATORS
                } else if (inputLine.contains("[")
                    && inputLine.contains("]")
                    && (inputLine.contains("(") && inputLine.contains(")") || inputLine.contains("{") && inputLine.contains("}"))
                ) {
                    logVerbose(TAG, "Matched type: LINK")
                    LINK
                } else {
                    logVerbose(TAG, "Matched type: TEXT")
                    TEXT
                }
            }

            // Finds the type of element of a modified line by looking it back up in the original text.
            fun find(modifiedLine: String, originalText: String): UpdateDescriptionElement {
                // As almost all the modifications that are made are substrings, this means the original text should always contain the modified line.
                // Then, the "of" function can be used with the belonging line to lookup the element type of the modified line.
                // The only exception is the empty line, but it's no problem that one is returned as TEXT, because the empty line is not needed for SpannableString.
                originalText.lines().forEach { currentLine ->
                    if (currentLine.contains(modifiedLine)) {
                        return of(currentLine)
                    }
                }

                return TEXT
            }
        }
    }

    // Special class which can be used by UpdateDataVersionFormatter lib to check if the current line contains the OS version number, since that must be excluded from the update description itself.
    private class LineDetectingUpdateInfo(currentLine: String) : FormattableUpdateData {
        override val internalVersionNumber: String? = null
        override val updateDescription: String? = currentLine
    }
}
