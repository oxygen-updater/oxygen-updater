package com.oxygenupdater.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SystemVersionPropertiesTestOnePlus6TGlobal : SystemVersionPropertiesTest() {

    @Test
    fun testSupportedDevice_OxygenOS9012() {
        assertTrue(
            isSupportedDevice(
                "op6t",
                "9.0.12",
                "OnePlus 6T",
                "OnePlus 6T",
                "OnePlus6T",
                "9.0.12",
                "OnePlus6TOxygen_34.O.19_GLO_019_1901231347",
                true
            )
        )

        assertEquals("OnePlus 6T (Global)", getSupportedDevice("op6t", "9.0.12").name)
    }
}
