package com.arjanvlek.oxygenupdater.support;

import android.content.Context;
import android.support.v4.app.Fragment;

import com.arjanvlek.oxygenupdater.R;

import org.joda.time.LocalDateTime;

import java.text.DateFormat;

/**
 * Class used to format the date retrieved from the update server
 */
public class DateTimeFormatter {
    private Context context;
    private Fragment fragment;

    /**
     * Create a new DateTimeFormatter.
     *
     * @param context  Application Context
     * @param fragment Currently active fragment
     */
    public DateTimeFormatter(Context context, Fragment fragment) {
        this.context = context;
        this.fragment = fragment;
    }

    public String formatDateTime(String dateTimeString) {
        try {
            if (dateTimeString == null || dateTimeString.isEmpty()) {
                return fragment.getString(R.string.device_information_unknown);
            }
            LocalDateTime dateTime = LocalDateTime.parse(dateTimeString);
            DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(context);
            String formattedTime = timeFormat.format(dateTime.toDate());

            LocalDateTime today = LocalDateTime.now();
            if ((dateTime.getDayOfMonth() == today.getDayOfMonth()) && dateTime.getMonthOfYear() == today.getMonthOfYear() && dateTime.getYear() == today.getYear()) {
                return formattedTime;
            } else if ((dateTime.getDayOfMonth() + 1) == today.getDayOfMonth() && dateTime.getMonthOfYear() == today.getMonthOfYear() && dateTime.getYear() == today.getYear()) {
                return fragment.getString(R.string.time_yesterday) + " " + fragment.getString(R.string.time_at) + " " + formattedTime;
            } else {
                DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(context);
                String formattedDate = dateFormat.format(dateTime.toDate());
                return formattedDate + " " + fragment.getString(R.string.time_at) + " " + formattedTime;
            }
        } catch (Exception e) {
            Logger.logError("DateTimeFormatter", "Unable to parse date: ", e);
            return dateTimeString;
        }
    }

}
