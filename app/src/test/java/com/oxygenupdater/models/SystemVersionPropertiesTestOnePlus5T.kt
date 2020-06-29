package com.oxygenupdater.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SystemVersionPropertiesTestOnePlus5T : SystemVersionPropertiesTest() {

    @Test
    fun testSupportedDevice_OxygenOS474() {
        assertTrue(
            isSupportedDevice(
                "op5t",
                "4.7.4",
                "OnePlus 5T",
                "OnePlus 5T",
                "OnePlus5T",
                "4.7.4",
                "OnePlus5TOxygen_43.O.06_GLO_006_1712062242",
                false
            )
        )

        assertEquals("OnePlus 5T", getSupportedDevice("op5t", "4.7.4").name)
    }
}
