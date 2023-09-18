package com.oxygenupdater.models

import com.oxygenupdater.BuildConfig
import kotlin.test.Test
import kotlin.test.assertEquals

class ServerStatusTest {

    @Test
    fun `check if shouldShowAppUpdateNotice works as expected`() {
        // Basic tests with the default `currentVersionName`
        shouldShowAppUpdateNotice(false, BuildConfig.VERSION_NAME) // same as current
        shouldShowAppUpdateNotice(false, null)      // received null from the server
        shouldShowAppUpdateNotice(false, "0.0.0")   // received an obviously older version
        shouldShowAppUpdateNotice(true, "99.99.99") // received an obviously newer version

        // More tests with overridden `currentVersionName`
        shouldShowAppUpdateNotice(true, "1.0.0", "0.0.9")
        shouldShowAppUpdateNotice(true, "1.11.10", "1.10.10")
        shouldShowAppUpdateNotice(false, "1.11.10", "2.0.0")

        // Received malformed from the server
        shouldShowAppUpdateNotice(true, "1.1", "1.0.0")
        shouldShowAppUpdateNotice(false, "1", "1.0.0")
        shouldShowAppUpdateNotice(false, "", "1.0.0")
        shouldShowAppUpdateNotice(false, "-1.1.1", "1.0.0")
    }

    private fun shouldShowAppUpdateNotice(
        expectedResult: Boolean,
        latestAppVersion: String?,
        currentVersionName: String = BuildConfig.VERSION_NAME,
    ) {
        val result = ServerStatus(latestAppVersion = latestAppVersion).shouldShowAppUpdateNotice(currentVersionName)
        assertEquals(expectedResult, result, "[ServerStatusTest] latest=$latestAppVersion, current=$currentVersionName")
    }
}
