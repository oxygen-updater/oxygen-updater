package com.arjanvlek.oxygenupdater.Model;

import com.arjanvlek.oxygenupdater.BuildConfig;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ServerStatus {

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

    public enum Status {
        NORMAL, WARNING, ERROR, MAINTENANCE, OUTDATED, UNREACHABLE
    }
}
