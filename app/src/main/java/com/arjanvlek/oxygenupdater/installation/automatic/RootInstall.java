package com.arjanvlek.oxygenupdater.installation.automatic;

import java.io.Serializable;

public class RootInstall implements Serializable {

    private long deviceId;
    private long updateMethodId;
    private InstallationStatus installationStatus;
    private String installationId;
    private String timestamp;
    private String startOsVersion;
    private String destinationOsVersion;
    private String currentOsVersion;
    private String failureReason;

    public RootInstall(long deviceId, long updateMethodId, InstallationStatus installationStatus, String installationId, String timestamp, String startOsVersion, String destinationOsVersion, String currentOsVersion, String failureReason) {
        this.deviceId = deviceId;
        this.updateMethodId = updateMethodId;
        this.installationStatus = installationStatus;
        this.installationId = installationId;
        this.timestamp = timestamp;
        this.startOsVersion = startOsVersion;
        this.destinationOsVersion = destinationOsVersion;
        this.currentOsVersion = currentOsVersion;
        this.failureReason = failureReason;
    }

    public long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(long deviceId) {
        this.deviceId = deviceId;
    }

    public long getUpdateMethodId() {
        return updateMethodId;
    }

    public void setUpdateMethodId(long updateMethodId) {
        this.updateMethodId = updateMethodId;
    }

    public InstallationStatus getInstallationStatus() {
        return installationStatus;
    }

    public void setInstallationStatus(InstallationStatus installationStatus) {
        this.installationStatus = installationStatus;
    }

    public String getInstallationId() {
        return installationId;
    }

    public void setInstallationId(String installationId) {
        this.installationId = installationId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getStartOsVersion() {
        return startOsVersion;
    }

    public void setStartOsVersion(String startOsVersion) {
        this.startOsVersion = startOsVersion;
    }

    public String getDestinationOsVersion() {
        return destinationOsVersion;
    }

    public void setDestinationOsVersion(String destinationOsVersion) {
        this.destinationOsVersion = destinationOsVersion;
    }

    public String getCurrentOsVersion() {
        return currentOsVersion;
    }

    public void setCurrentOsVersion(String currentOsVersion) {
        this.currentOsVersion = currentOsVersion;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    @Override
    public String toString() {
        return "RootInstall{" +
                "deviceId=" + deviceId +
                ", updateMethodId=" + updateMethodId +
                ", installationStatus=" + installationStatus +
                ", installationId='" + installationId + '\'' +
                ", timestamp=" + timestamp +
                ", startOsVersion='" + startOsVersion + '\'' +
                ", destinationOsVersion='" + destinationOsVersion + '\'' +
                ", currentOsVersion='" + currentOsVersion + '\'' +
                ", failureReason='" + failureReason + '\'' +
                '}';
    }
}
