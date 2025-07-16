package com.oxygenupdater.models

import android.content.Context
import com.oxygenupdater.R
import java.io.Serializable

/**
 * Contains the progress & ETA of a download.
 */
data class TimeRemaining(private val seconds: Long) : Serializable {

    private val hoursRemaining = seconds / 3600
    private val minutesRemaining = seconds / 60
    private val secondsRemaining = seconds % 60

    fun toString(context: Context?): String {
        if (context == null) {
            return ""
        }

        return if (hoursRemaining > 1L) {
            context.getString(R.string.download_progress_text_hours_remaining, hoursRemaining)
        } else if (hoursRemaining == 1L) {
            context.getString(R.string.download_progress_text_one_hour_remaining)
        } else if (hoursRemaining == 0L && minutesRemaining > 1L) {
            context.getString(R.string.download_progress_text_minutes_remaining, minutesRemaining)
        } else if (hoursRemaining == 0L && minutesRemaining == 1L) {
            context.getString(R.string.download_progress_text_one_minute_remaining)
        } else if (hoursRemaining == 0L && minutesRemaining == 0L && secondsRemaining > 10L) {
            context.getString(R.string.download_progress_text_less_than_a_minute_remaining)
        } else if (hoursRemaining == 0L && minutesRemaining == 0L && secondsRemaining <= 10L) {
            context.getString(R.string.download_progress_text_seconds_remaining)
        } else {
            context.getString(R.string.download_progress_text_unknown_time_remaining)
        }
    }
}
