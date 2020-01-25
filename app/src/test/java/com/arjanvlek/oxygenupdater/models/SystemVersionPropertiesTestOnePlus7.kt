package com.arjanvlek.oxygenupdater.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Oxygen Updater, copyright 2019 Arjan Vlek. File created by arjan.vlek on 16/05/2019.
 */
class SystemVersionPropertiesTestOnePlus7 : SystemVersionPropertiesTest() {

    /*
     * Test if International (intl) model is correctly detected by the app.
     * Note: All variants of OP7 Pro are incorrectly detected as this variant by app versions 1.0.0 - 2.3.2
     * However: as nobody is going to use such old OU versions on OP7+, this is acceptable
     */
    @Test
    fun testSupportedDevice_INTLVersion_OxygenOS1030GM57AA() {
        val testDataDir = "op7/intl"
        val testDataOfVersion = "10.3.0.GM57AA"

        assertTrue(
            isSupportedDevice(
                testDataDir,
                testDataOfVersion,
                "OnePlus7",
                "OnePlus7",  // Gets replaced with value of ro.product.name by @hack #3 in SystemVersionProperties.java. But should be: "OnePlus 7"
                "OnePlus7",
                "10.3.0.GM57AA",
                "OnePlus7Oxygen_14.P.24_GLO_024_1912142025",
                true
            )
        )

        assertEquals("OnePlus 7", getSupportedDevice(testDataDir, testDataOfVersion).name)
    }
}
