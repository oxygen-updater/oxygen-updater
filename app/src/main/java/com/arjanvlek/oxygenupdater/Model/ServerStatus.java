package com.arjanvlek.oxygenupdater.Model;

import android.content.Context;
import android.support.v4.content.ContextCompat;

import com.arjanvlek.oxygenupdater.BuildConfig;
import com.arjanvlek.oxygenupdater.R;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ServerStatus implements Banner {

    private Status status;
    private String latestAppVersion;

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status != null ? status : Status.UNREACHABLE;
    }

    public String getLatestAppVersion() {
        return latestAppVersion;
    }

    @JsonProperty("latest_app_version")
    public void setLatestAppVersion(String latestAppVersion) {
        this.latestAppVersion = latestAppVersion != null ? latestAppVersion : BuildConfig.VERSION_NAME; // To prevent incorrect app update messages if response is null / invalid
    }

    @Override
    public String getBannerText(Context context) {
        switch(getStatus()) {
            case WARNING:
                return context.getString(R.string.server_status_warning);
            case ERROR:
                return context.getString(R.string.server_status_error);
            case MAINTENANCE:
                return "";
            case OUTDATED:
                return "";
            case UNREACHABLE:
                return context.getString(R.string.server_status_unreachable);
            default: return "";
        }
    }

    @Override
    public int getColor(Context context) {
        switch(getStatus()) {
            case WARNING:
                return ContextCompat.getColor(context, R.color.holo_orange_light);
            case ERROR:
                return ContextCompat.getColor(context, R.color.holo_red_light);
            case MAINTENANCE:
                return 0;
            case OUTDATED:
                return 0;
            case UNREACHABLE:
                return ContextCompat.getColor(context, R.color.holo_red_light);
            default: return 0;
        }
    }

    public enum Status {
        NORMAL, WARNING, ERROR, MAINTENANCE, OUTDATED, UNREACHABLE;

        public boolean isUserRecoverableError() {
            return this.equals(WARNING) || this.equals(ERROR) || this.equals(UNREACHABLE);
        }

        public boolean isNonRecoverableError() {
            return !isUserRecoverableError() && !this.equals(NORMAL);
        }
    }
}
