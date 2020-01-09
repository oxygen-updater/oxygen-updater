package com.arjanvlek.oxygenupdater.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SystemVersionPropertiesTestOnePlus6 : SystemVersionPropertiesTest() {

    @Test
    fun testSupportedDevice_OxygenOS904() {
        assertTrue(
            isSupportedDevice(
                "op6",
                "9.0.4",
                "OnePlus 6",
                "OnePlus 6",
                "OnePlus6",
                "9.0.4",
                "OnePlus6Oxygen_22.O.29_GLO_029_1901231504",
                true
            )
        )

        assertEquals("OnePlus 6", getSupportedDevice("op6", "9.0.4").name)
    }
}
