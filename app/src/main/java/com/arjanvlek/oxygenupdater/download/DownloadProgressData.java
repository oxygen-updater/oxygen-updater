package com.arjanvlek.oxygenupdater.download;


import android.content.Context;

import com.arjanvlek.oxygenupdater.R;

import java.io.Serializable;

public class DownloadProgressData implements Serializable {

    private final TimeRemaining timeRemaining;

    private final int progress;

    private final boolean waitingForConnection;

    DownloadProgressData(long numberOfSecondsRemaining, int progress) {
        this(numberOfSecondsRemaining, progress, false);
    }

    DownloadProgressData(long numberOfSecondsRemaining, int progress, boolean waitingForConnection) {
        this.timeRemaining = calculateTimeRemaining((int)numberOfSecondsRemaining);
        this.progress = progress;
        this.waitingForConnection = waitingForConnection;
    }

    public TimeRemaining getTimeRemaining() {
        return timeRemaining;
    }

    public int getProgress() {
        return progress;
    }

    public boolean isWaitingForConnection() {
        return waitingForConnection;
    }

    private TimeRemaining calculateTimeRemaining(int numberOfSecondsRemaining) {
        if(numberOfSecondsRemaining == DownloadService.NOT_SET) {
            return null;
        }

        return new TimeRemaining(numberOfSecondsRemaining / 3600, numberOfSecondsRemaining / 60, numberOfSecondsRemaining % 60);
    }

    @SuppressWarnings("WeakerAccess")
    // Can't be private, because UpdateInformationFragment calls this.
    public class TimeRemaining implements Serializable {

        private final int hoursRemaining;
        private final int minutesRemaining;
        private final int secondsRemaining;

        TimeRemaining(int hoursRemaining, int minutesRemaining, int secondsRemaining) {
            this.hoursRemaining = hoursRemaining;
            this.minutesRemaining = minutesRemaining;
            this.secondsRemaining = secondsRemaining;
        }

        int getHoursRemaining() {
            return hoursRemaining;
        }

        int getMinutesRemaining() {
            return minutesRemaining;
        }

        int getSecondsRemaining() {
            return secondsRemaining;
        }

        public String toString(Context activity) {
            if(activity == null) return "";

            if (getHoursRemaining() > 1) {
                return activity.getString(R.string.download_progress_text_hours_remaining, getProgress(), getHoursRemaining());
            } else if (getHoursRemaining() == 1) {
                return activity.getString(R.string.download_progress_text_one_hour_remaining, getProgress());
            } else if (getHoursRemaining() == 0 && getMinutesRemaining() > 1) {
                return activity.getString(R.string.download_progress_text_minutes_remaining, getProgress(), getMinutesRemaining());
            } else if (getHoursRemaining() == 0 && getMinutesRemaining() == 1) {
                return activity.getString(R.string.download_progress_text_one_minute_remaining, getProgress());
            } else if (getHoursRemaining() == 0 && getMinutesRemaining() == 0 && getSecondsRemaining() > 10) {
                return activity.getString(R.string.download_progress_text_less_than_a_minute_remaining, getProgress());
            } else if (getHoursRemaining() == 0 && getMinutesRemaining() == 0 && getSecondsRemaining() <= 10) {
                return activity.getString(R.string.download_progress_text_seconds_remaining, getProgress());
            } else {
                return activity.getString(R.string.download_progress_text_unknown_time_remaining, getProgress());
            }
        }
    }
}
