package com.arjanvlek.oxygenupdater.installation.automatic

import java.io.Serializable

class RootInstall(var deviceId: Long, var updateMethodId: Long, var installationStatus: InstallationStatus?, private var installationId: String?, private var timestamp: String?, private var startOsVersion: String?, private var destinationOsVersion: String?, private var currentOsVersion: String?, private var failureReason: String?) : Serializable {

    override fun toString(): String {
        return "RootInstall{" +
                "deviceId=" + deviceId +
                ", updateMethodId=" + updateMethodId +
                ", installationStatus=" + installationStatus +
                ", installationId='" + installationId + '\''.toString() +
                ", timestamp=" + timestamp +
                ", startOsVersion='" + startOsVersion + '\''.toString() +
                ", destinationOsVersion='" + destinationOsVersion + '\''.toString() +
                ", currentOsVersion='" + currentOsVersion + '\''.toString() +
                ", failureReason='" + failureReason + '\''.toString() +
                '}'.toString()
    }

    companion object {
        private const val serialVersionUID = -3101644612957861502L
    }
}
