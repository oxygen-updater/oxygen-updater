package com.oxygenupdater.models

import android.os.Build
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SystemVersionPropertiesTestOnePlus1 : SystemVersionPropertiesTest() {

    @Test
    fun testSupportedDevice_OxygenOS100() {
        // Not supported in app versions 1.2.x - 2.2.x, but has been fixed in App 2.3.x
        assertTrue(
            isSupportedDevice(
                "op1",
                "1.0.0",
                "One",
                Build.UNKNOWN,
                "One",
                "1.0.0",
                "ONEL_12.A.01_001_150403",
                false
            )
        )

        assertEquals("OnePlus One", getSupportedDevice("op1", "1.0.0").name)
    }

    @Test
    fun testSupportedDevice_OxygenOS103() {
        // Not supported in app versions 1.2.x - 2.2.x, but has been fixed in App 2.3.x
        assertTrue(
            isSupportedDevice(
                "op1",
                "1.0.3",
                "One",
                Build.UNKNOWN,
                "One",
                "1.0.3",
                "ONEL_12.A.01_001_150827",
                false
            )
        )

        assertEquals("OnePlus One", getSupportedDevice("op1", "1.0.3").name)
    }

    @Test
    fun testSupportedDevice_OxygenOS214() {
        // Neat OS display version is not present in the firmware, but that's not an issue.
        assertTrue(
            isSupportedDevice(
                "op1",
                "2.1.4",
                "OnePlus",
                "OnePlus",
                "OnePlus",
                "ONELOxygen_12_201601190107",
                "ONELOxygen_12.A.02_GLO_002_201601190107",
                false
            )
        )

        assertEquals("OnePlus One", getSupportedDevice("op1", "2.1.4").name)
    }
}
