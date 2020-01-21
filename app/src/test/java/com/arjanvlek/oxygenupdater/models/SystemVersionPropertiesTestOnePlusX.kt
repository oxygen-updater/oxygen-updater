package com.arjanvlek.oxygenupdater.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SystemVersionPropertiesTestOnePlusX : SystemVersionPropertiesTest() {

    @Test
    fun testSupportedDevice_OxygenOS214() {
        // Neat OS display version is not present in the firmware, but that's not an issue.
        assertTrue(
            isSupportedDevice(
                "opx",
                "2.1.4",
                "OnePlus X",
                "OnePlus X",
                "OnePlus",
                "OnePlusOxygen_14_201512161752",
                "OnePlusOxygen_14.A.07_GLO_007_201512161752",
                false
            )
        )

        assertEquals("OnePlus X", getSupportedDevice("opx", "2.1.4").name)
    }

    @Test
    fun testSupportedDevice_OxygenOS223() {
        // Neat OS display version is not present in the firmware, but that's not an issue.
        assertTrue(
            isSupportedDevice(
                "opx",
                "2.2.3",
                "OnePlus X",
                "OnePlus X",
                "OnePlus",
                "OnePlusOxygen_14_201609012031",
                "OnePlusXOxygen_14.A.13_GLO_013_201609012031",
                false
            )
        )

        assertEquals("OnePlus X", getSupportedDevice("opx", "2.2.3").name)
    }

    @Test
    fun testSupportedDevice_OxygenOS314() {
        // Neat OS display version is not present in the firmware, but that's not an issue.
        assertTrue(
            isSupportedDevice(
                "opx",
                "3.1.4",
                "OnePlus X",
                "OnePlus X",
                "OnePlus",
                "OnePlusXOxygen_14_201611071506",
                "OnePlusXOxygen_14.A.19_GLO_019_201611071506",
                false
            )
        )

        assertEquals("OnePlus X", getSupportedDevice("opx", "3.1.4").name)
    }
}
