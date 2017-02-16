package com.arjanvlek.oxygenupdater.Model;


import static com.arjanvlek.oxygenupdater.Support.UpdateDownloader.NOT_SET;

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
    }
}
