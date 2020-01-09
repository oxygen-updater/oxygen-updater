package com.arjanvlek.oxygenupdater.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SystemVersionPropertiesTestOnePlus3T : SystemVersionPropertiesTest() {

    @Test
    fun testSupportedDevice_OxygenOS353() {
        // Neat OS display version is not present in the firmware, but that's not an issue.
        assertTrue(
            isSupportedDevice(
                "op3t",
                "3.5.3",
                "OnePlus 3T",
                "OnePlus 3T",
                "OnePlus3",
                "OnePlus3TOxygen_28_1611222319",
                "OnePlus3TOxygen_28.A.27_GLO_027_1611222319",
                false
            )
        )

        assertEquals("OnePlus 3T", getSupportedDevice("op3t", "3.5.3").name)
    }

    @Test
    fun testSupportedDevice_OxygenOS451() {
        // Neat OS display version is not present in the firmware, but that's not an issue.
        assertTrue(
            isSupportedDevice(
                "op3t",
                "4.5.1",
                "OnePlus 3T",
                "OnePlus 3T",
                "OnePlus3",
                "OnePlus3TOxygen_28_1710122300",
                "OnePlus3TOxygen_28.A.57_GLO_057_1710122300",
                false
            )
        )

        assertEquals("OnePlus 3T", getSupportedDevice("op3t", "4.5.1").name)
    }

    @Test
    fun testSupportedDevice_OxygenOS500() {
        // Neat OS display version is not present in the firmware, but that's not an issue.
        assertTrue(
            isSupportedDevice(
                "op3t",
                "5.0.0",
                "OnePlus 3T",
                "OnePlus 3T",
                "OnePlus3",
                "OnePlus3TOxygen_28_1711160447",
                "OnePlus3TOxygen_28.A.60_GLO_060_1711160447",
                false
            )
        )

        assertEquals("OnePlus 3T", getSupportedDevice("op3t", "5.0.0").name)
    }
}
