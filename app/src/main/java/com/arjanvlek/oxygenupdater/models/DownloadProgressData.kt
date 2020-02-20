package com.arjanvlek.oxygenupdater.models

import android.content.Context
import com.arjanvlek.oxygenupdater.R
import com.arjanvlek.oxygenupdater.services.DownloadService
import java.io.Serializable

/**
 * Contains the progress & ETA of a download.
 */
data class DownloadProgressData(val numberOfSecondsRemaining: Long, val progress: Int, val isWaitingForConnection: Boolean = false) : Serializable {

    val timeRemaining = calculateTimeRemaining(numberOfSecondsRemaining.toInt())

    constructor(numberOfSecondsRemaining: Long, progress: Int) : this(numberOfSecondsRemaining, progress, false)

    private fun calculateTimeRemaining(numberOfSecondsRemaining: Int): TimeRemaining? {
        return if (numberOfSecondsRemaining == DownloadService.NOT_SET.toInt()) {
            null
        } else {
            TimeRemaining(numberOfSecondsRemaining / 3600, numberOfSecondsRemaining / 60, numberOfSecondsRemaining % 60)
        }
    }

    /**
     * Can't be private, because UpdateInformationFragment calls this.
     */
    inner class TimeRemaining internal constructor(val hoursRemaining: Int, val minutesRemaining: Int, val secondsRemaining: Int) : Serializable {

        fun toString(context: Context?): String {
            if (context == null) {
                return ""
            }

            return if (hoursRemaining > 1) {
                context.getString(R.string.download_progress_text_hours_remaining, hoursRemaining)
            } else if (hoursRemaining == 1) {
                context.getString(R.string.download_progress_text_one_hour_remaining)
            } else if (hoursRemaining == 0 && minutesRemaining > 1) {
                context.getString(R.string.download_progress_text_minutes_remaining, minutesRemaining)
            } else if (hoursRemaining == 0 && minutesRemaining == 1) {
                context.getString(R.string.download_progress_text_one_minute_remaining)
            } else if (hoursRemaining == 0 && minutesRemaining == 0 && secondsRemaining > 10) {
                context.getString(R.string.download_progress_text_less_than_a_minute_remaining)
            } else if (hoursRemaining == 0 && minutesRemaining == 0 && secondsRemaining <= 10) {
                context.getString(R.string.download_progress_text_seconds_remaining)
            } else {
                context.getString(R.string.download_progress_text_unknown_time_remaining)
            }
        }
    }

    companion object {
        private const val serialVersionUID = 5271414164650145215L
    }
}
