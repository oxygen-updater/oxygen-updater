package com.arjanvlek.oxygenupdater.installation.automatic

import java.io.Serializable

enum class InstallationStatus : Serializable {
    STARTED,
    FINISHED,
    FAILED
}
