package com.arjanvlek.oxygenupdater.models

import java.io.Serializable

enum class InstallationStatus : Serializable {
    STARTED,
    FINISHED,
    FAILED
}
