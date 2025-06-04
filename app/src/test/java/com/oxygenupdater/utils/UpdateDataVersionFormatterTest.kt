package com.oxygenupdater.utils

import android.os.Build
import com.oxygenupdater.models.UpdateData
import com.oxygenupdater.utils.UpdateDataVersionFormatter.getFormattedOsVersion
import com.oxygenupdater.utils.UpdateDataVersionFormatter.getFormattedVersionNumber
import kotlin.test.Test
import kotlin.test.assertEquals

class UpdateDataVersionFormatterTest {

    @Test
    fun `check if version number is formatted correctly`() {
        // Default
        assertEquals("", getFormattedVersionNumber("OxygenOS ", UpdateData(id = 1), ""))
        assertEquals("default", getFormattedVersionNumber("OxygenOS ", UpdateData(id = 1), "default"))

        // Can be extracted from `description`'s first line
        assertEquals("OxygenOS Closed Beta 1", getFormattedVersionNumber("OxygenOS ", UpdateData(id = 1, description = "#Alpha_1")))
        assertEquals("OxygenOS Open Beta 1", getFormattedVersionNumber("OxygenOS ", UpdateData(id = 1, description = "#Open_1")))
        assertEquals("OxygenOS Open Beta 1", getFormattedVersionNumber("OxygenOS ", UpdateData(id = 1, description = "#Beta_1")))
        assertEquals("Android ${Build.VERSION.RELEASE} DP 1", getFormattedVersionNumber("OxygenOS ", UpdateData(id = 1, description = "#DP_1")))
        assertEquals("OxygenOS 1.2.3", getFormattedVersionNumber("OxygenOS ", UpdateData(id = 1, description = "#1.2.3")))
        assertEquals("OxygenOS 1.2.3.4", getFormattedVersionNumber("OxygenOS ", UpdateData(id = 1, description = "#1.2.3.4")))
        assertEquals("OxygenOS 1.2.3.4.AB01CD", getFormattedVersionNumber("OxygenOS ", UpdateData(id = 1, description = "#1.2.3.4.AB01CD")))
        assertEquals("Custom version", getFormattedVersionNumber("OxygenOS ", UpdateData(id = 1, description = "#Custom version")))

        // Fallback to `versionNumber`
        assertEquals("XY1234_11_A.01", getFormattedVersionNumber("OxygenOS ", UpdateData(id = 1, versionNumber = "XY1234_11_A.01")))
    }

    @Test
    fun `check if OS version is formatted correctly`() {
        // Invalid
        assertEquals(Build.UNKNOWN, getFormattedOsVersion(""))
        assertEquals(Build.UNKNOWN, getFormattedOsVersion(" "))

        // Alpha, beta, or developer preview
        assertEquals("Closed Beta 1", getFormattedOsVersion("Alpha_1"))
        assertEquals("Open Beta 1", getFormattedOsVersion("Open_1"))
        assertEquals("Open Beta 1", getFormattedOsVersion("Beta_1"))
        assertEquals("Android ${Build.VERSION.RELEASE} DP 1", getFormattedOsVersion("DP_1"))

        // Stable
        assertEquals("1.2.3", getFormattedOsVersion("1.2.3"))
        assertEquals("1.2.3.4", getFormattedOsVersion("1.2.3.4"))
        assertEquals("1.2.3.4.AB01CD", getFormattedOsVersion("1.2.3.4.AB01CD"))

        // Anything else
        assertEquals("Custom version", getFormattedOsVersion("Custom version"))
    }
}
