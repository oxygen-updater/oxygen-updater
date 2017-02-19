package com.arjanvlek.oxygenupdater.Download;


import android.app.Activity;

import com.arjanvlek.oxygenupdater.R;

import static com.arjanvlek.oxygenupdater.Download.UpdateDownloader.NOT_SET;

public class DownloadProgressData {

    private TimeRemaining timeRemaining;

    private int progress;


    public DownloadProgressData(long numberOfSecondsRemaining, int progress) {
        this.timeRemaining = calculateTimeRemaining((int)numberOfSecondsRemaining);
        this.progress = progress;
    }

    public TimeRemaining getTimeRemaining() {
        return timeRemaining;
    }

    public int getProgress() {
        return progress;
    }

    private TimeRemaining calculateTimeRemaining(int numberOfSecondsRemaining) {
        if(numberOfSecondsRemaining == NOT_SET) {
            return null;
        }

        return new TimeRemaining(numberOfSecondsRemaining / 3600, numberOfSecondsRemaining / 60, numberOfSecondsRemaining % 60);
    }

    public class TimeRemaining {

        private int hoursRemaining;
        private int minutesRemaining;
        private int secondsRemaining;

        public TimeRemaining(int hoursRemaining, int minutesRemaining, int secondsRemaining) {
            this.hoursRemaining = hoursRemaining;
            this.minutesRemaining = minutesRemaining;
            this.secondsRemaining = secondsRemaining;
        }

        public int getHoursRemaining() {
            return hoursRemaining;
        }

        public int getMinutesRemaining() {
            return minutesRemaining;
        }

        public int getSecondsRemaining() {
            return secondsRemaining;
        }

        public String toString(Activity activity) {
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
