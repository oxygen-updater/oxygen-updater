package com.arjanvlek.oxygenupdater.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Oxygen Updater, copyright 2019 Arjan Vlek. File created by arjan.vlek on 16/05/2019.
 */
class SystemVersionPropertiesTestOnePlus7T : SystemVersionPropertiesTest() {

    /*
     * Test if European Economic Area (EEA) model is correctly detected by the app.
     * Note: The device *is* detected by App 1.0.0 - 2.3.2, but those legacy versions
     * cannot differentiate the EU version from other regional editions.
     * As nobody is going to use such old OU versions on OP7T+, this is acceptable
     */
    @Test
    fun testSupportedDevice_EEAVersion_OxygenOS1003HD65BA() {
        val testDataDir = "op7t/eea"
        val testDataOfVersion = "10.0.3.HD65BA"

        assertTrue(
            isSupportedDevice(
                testDataDir,
                testDataOfVersion,
                "OnePlus7T_EEA",
                "OnePlus7T_EEA",  // Gets replaced with value of ro.product.name by @hack #3 in SystemVersionProperties.java. But should be: "OnePlus 7T"
                "OnePlus7T",
                "10.0.3.HD65BA",
                "OnePlus7TOxygen_14.E.05_GLO_005_1909252229",
                true
            )
        )

        assertEquals("OnePlus 7T (EU)", getSupportedDevice(testDataDir, testDataOfVersion).name)
    }

    /*
     * Test if International (intl) model is correctly detected by the app.
     * Note: All variants of OP7T are incorrectly detected as this variant by app versions 1.0.0 - 2.3.2
     * However: as nobody is going to use such old OU versions on OP7T+, this is acceptable
     */
    @Test
    fun testSupportedDevice_INTLVersion_OxygenOS1001HD65AA() {
        val testDataDir = "op7t/intl"
        val testDataOfVersion = "10.0.1.HD65AA"

        assertTrue(
            isSupportedDevice(
                testDataDir,
                testDataOfVersion,
                "OnePlus7T",
                "OnePlus7T",  // Gets replaced with value of ro.product.name by @hack #3 in SystemVersionProperties.java. But should be: "OnePlus 7T"
                "OnePlus7T",
                "10.0.1.HD65AA",
                "OnePlus7TOxygen_14.O.04_GLO_004_1909151202",
                true
            )
        )

        assertEquals("OnePlus 7T", getSupportedDevice(testDataDir, testDataOfVersion).name)
    }
}
