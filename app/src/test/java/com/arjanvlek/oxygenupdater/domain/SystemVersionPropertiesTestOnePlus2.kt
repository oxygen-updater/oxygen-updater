package com.arjanvlek.oxygenupdater.domain

import com.arjanvlek.oxygenupdater.ApplicationData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SystemVersionPropertiesTestOnePlus2 : SystemVersionPropertiesTest() {

    @Test
    fun testSupportedDevice_OxygenOS200() {
        // Not supported in app versions 1.2.x - 2.2.x, but has been fixed in App 2.3.x
        assertTrue(
            isSupportedDevice(
                "op2",
                "2.0.0",
                "OnePlus2",
                ApplicationData.NO_OXYGEN_OS,
                "OnePlus2",
                "OnePlus2Oxygen_14_1507251956",
                "OnePlus2Oxygen_14.A.02_GLO_002_1507251956",
                false
            )
        )

        assertEquals("OnePlus 2", getSupportedDevice("op2", "2.0.0").name)
    }

    @Test
    fun testSupportedDevice_OxygenOS310() {
        // Neat OS display version is not present in the firmware, but that's not an issue.
        assertTrue(
            isSupportedDevice(
                "op2",
                "3.1.0",
                "OnePlus2",
                "OnePlus2",
                "OnePlus2",
                "OnePlus2Oxygen_14_1608262242",
                "OnePlus2Oxygen_14.A.20_GLO_020_1608262242",
                false
            )
        )

        assertEquals("OnePlus 2", getSupportedDevice("op2", "3.1.0").name)
    }

    @Test
    fun testSupportedDevice_OxygenOS361() {
        // Neat OS display version is not present in the firmware, but that's not an issue.
        assertTrue(
            isSupportedDevice(
                "op2",
                "3.6.1",
                "OnePlus2",
                "OnePlus2",
                "OnePlus2",
                "OnePlus2Oxygen_14_1710240102",
                "OnePlus2Oxygen_14.A.32_GLO_032_1710240102",
                false
            )
        )

        assertEquals("OnePlus 2", getSupportedDevice("op2", "3.6.1").name)
    }
}
