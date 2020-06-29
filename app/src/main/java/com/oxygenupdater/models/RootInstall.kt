package com.oxygenupdater.models

import java.io.Serializable

data class RootInstall(
    var deviceId: Long,
    var updateMethodId: Long,
    var installationStatus: InstallationStatus,
    var installationId: String,
    var timestamp: String,
    var startOsVersion: String,
    var destinationOsVersion: String,
    var currentOsVersion: String,
    var failureReason: String
) : Serializable {

    override fun toString(): String {
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
                '}'
    }

    companion object {
        private const val serialVersionUID = -3101644612957861502L
    }

}
