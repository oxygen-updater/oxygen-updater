package com.arjanvlek.oxygenupdater.updateinformation;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;

import com.arjanvlek.oxygenupdater.internal.logger.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import static android.graphics.Typeface.BOLD;

public class UpdateDescriptionParser {

    private static final String TAG = "UpdateDescriptionParser";

    private enum UpdateDescriptionElement {
        UPDATE_TITLE,
        UPDATE_HEADER,
        EMPTY_LINE,
        LINK,
        TEXT;

        // Finds the element type of a given line of OnePlus formatted text.
        private static UpdateDescriptionElement of(String inputLine) {
            // The empty imput gets parsed as TEXT, because no modification to it is required.
            Logger.logVerbose(TAG, "Input line: " + inputLine);

            if (inputLine == null || inputLine.isEmpty()) {
                Logger.logVerbose(TAG, "Matched type: TEXT");
                return TEXT;
            }

            // Remove all spaces and newline characters from this line.
            inputLine = inputLine.replace('\r', ' ');
            inputLine = inputLine.replace('\n', ' ');
            inputLine = inputLine.replace(" ", "");

            // If the input is exactly one character long, and that character is a backwards slash, return that this should be an empty line.
            char firstChar = inputLine.charAt(0);
            Logger.logVerbose(TAG, "First Char: " + firstChar);

            if (inputLine.length() == 1 && firstChar == '\\') {
                Logger.logVerbose(TAG, "Matched type: EMPTY_LINE");
                return EMPTY_LINE;
            }

            // In all other cases, look at the first two chars.
            char secondChar = inputLine.charAt(1);
            Logger.logVerbose(TAG, "Second Char: " + secondChar);

            if (firstChar == '#' && secondChar == '#') {
                Logger.logVerbose(TAG, "Matched type: UPDATE_HEADER");
                return UPDATE_HEADER;
            } else if (firstChar == '#') {
                Logger.logVerbose(TAG, "Matched type: UPDATE_TITLE");
                return UPDATE_TITLE;
            } else if (firstChar == '\\') {
                Logger.logVerbose(TAG, "Matched type: EMPTY_LINE");
                return EMPTY_LINE;
            } else if (firstChar == '[' && secondChar != '[' && inputLine.contains("]") && inputLine.contains("(") && inputLine.contains(")") && (inputLine.contains("http") || inputLine.contains("www"))) {
                Logger.logVerbose(TAG, "Matched type: LINK");
                return LINK;
            } else {
                Logger.logVerbose(TAG, "Matched type: TEXT");
                return TEXT;
            }
        }

        // Finds the type of element of a modified line by looking it back up in the original text.
        public static UpdateDescriptionElement find(String modifiedLine, String originalText) throws IOException {
            BufferedReader reader = new BufferedReader(new StringReader(originalText));
            String currentLine;

            // As almost all the modifications that are made are substrings, this means the original text should always contain the modified line.
            // Then, the "of" function can be used with the belonging line to lookup the element type of the modified line.
            // The only exception is the empty line, but it's no problem that one is returned as TEXT, because the empty line is not needed for SpannableString.
            while ((currentLine = reader.readLine()) != null) {
                if (currentLine.contains(modifiedLine)) return of(currentLine);
            }

            return TEXT;
        }
    }

    public static Spanned parse(String updateDescription) {
        SpannableString result;

        final Map<String, String> links = new HashMap<>();

        String currentLine;
        try {
            BufferedReader reader = new BufferedReader(new StringReader(updateDescription));
            String modifiedUpdateDescription = "";

            // First, loop through all lines and modify them were needed. This includes placing line separators and html-style links.
            while ((currentLine = reader.readLine()) != null) {
                UpdateDescriptionElement element = UpdateDescriptionElement.of(currentLine);
                String modifiedLine = "";

                switch (element) {
                    case UPDATE_HEADER:
                        modifiedLine = currentLine.substring(2, currentLine.length()); // Remove the double hashtags (##).
                        break;
                    case UPDATE_TITLE:
                        modifiedLine = currentLine.substring(1, currentLine.length()) + "\n"; // Remove the single hash tag and add another line separator to the title.
                    case EMPTY_LINE: // The line starts with a line separator (\). However, there could also be multiple OnePlus line separators in this line. Replace each OnePlus line separator with an actual line separator.
                        char[] chars = currentLine.toCharArray();
                        for (char c : chars) {
                            if (c == '\\') {
                                modifiedLine = modifiedLine + "\n";
                            }
                        }
                        break;
                    case LINK:
                        String linkTitle = currentLine.substring(currentLine.indexOf("[") + 1, currentLine.lastIndexOf("]"));
                        String linkAddress = currentLine.substring(currentLine.indexOf("(") + 1, currentLine.lastIndexOf(")"));

                        // We need to save the full URL somewhere, to point the browser to it when clicked...
                        links.put(linkTitle, linkAddress);

                        // The link title will be displayed. It will also be used to look up the full url when clicked.
                        modifiedLine = linkTitle;
                        break;
                    default:
                        modifiedLine = currentLine;
                }

                modifiedUpdateDescription = modifiedUpdateDescription.concat(modifiedLine + (element.equals(UpdateDescriptionElement.EMPTY_LINE) ? "" : "\n"));
            }

            // Next, loop through the modified update description and place formatting attributes for the title / headers.
            reader = new BufferedReader(new StringReader(modifiedUpdateDescription));
            result = new SpannableString(modifiedUpdateDescription);


            while ((currentLine = reader.readLine()) != null) {
                if (currentLine.isEmpty()) continue;

                UpdateDescriptionElement element = UpdateDescriptionElement.find(currentLine, updateDescription);

                int startPosition = modifiedUpdateDescription.indexOf(currentLine);
                int endPosition = startPosition + currentLine.length();

                switch (element) {
                    case UPDATE_TITLE:
                        // The update header should be made larger and bold.
                        result.setSpan(new RelativeSizeSpan(1.3f), startPosition, endPosition, 0);
                        result.setSpan(new StyleSpan(BOLD), startPosition, endPosition, 0);
                        break;
                    case UPDATE_HEADER:
                        // The update header should be a bit larger than normal, but smaller than the title, and bold.
                        result.setSpan(new RelativeSizeSpan(1.1f), startPosition, endPosition, 0);
                        result.setSpan(new StyleSpan(BOLD), startPosition, endPosition, 0);
                        break;
                    case LINK:
                        // A link should be made clickable and must be displayed as a link.
                        result.setSpan(new FormattedURLSpan(links.get(currentLine)), startPosition, endPosition, 0);
                        break;
                }
            }


        } catch (Exception e) {
            // If an error occurred, log it and return the original / unmodified update description
            Logger.logError(TAG, "Error parsing update description", e);
            return new SpannableString(updateDescription);
        }

        return result;
    }

}
