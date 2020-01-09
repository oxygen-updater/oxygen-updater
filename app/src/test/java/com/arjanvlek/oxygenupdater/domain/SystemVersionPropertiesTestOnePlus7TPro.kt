package com.arjanvlek.oxygenupdater.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * @author Adhiraj Singh Chauhan
 */
class SystemVersionPropertiesTestOnePlus7TPro : SystemVersionPropertiesTest() {

    /*
     * Test if European Economic Area (EEA) model is correctly detected by the app.
     * Note: The device *is* detected by App 1.0.0 - 2.3.2, but those legacy versions
     * cannot differentiate the EU version from other regional editions.
     * As nobody is going to use such old OU versions on OP7T+, this is acceptable
     */
    @Test
    fun testSupportedDevice_EEAVersion_OxygenOS100HD01BA() {
        val testDataDir = "op7tpro-eea"
        val testDataOfVersion = "10.0.HD01BA"

        assertTrue(
            isSupportedDevice(
                testDataDir,
                testDataOfVersion,
                "OnePlus7TPro_EEA",  // Gets replaced with value of ro.product.name by @hack #3 in SystemVersionProperties.java. But should be: "OnePlus 7T Pro"
                "OnePlus7TPro_EEA",
                "OnePlus7TPro",
                "10.0.HD01BA",
                "OnePlus7TProOxygen_14.E.01_GLO_001_1909152252",
                true
            )
        )

        assertEquals("OnePlus 7T Pro (EU)", getSupportedDevice(testDataDir, testDataOfVersion).name)
    }

    /*
     * Test if Indian model is correctly detected by the app.
     * Note: All variants of OP7T Pro are incorrectly detected as this variant by app versions 1.0.0 - 2.3.2
     * However: as nobody is going to use such old OU versions on OP7T+, this is acceptable
     */
    @Test
    fun testSupportedDevice_INDIAVersion_OxygenOS1001HD01AA() {
        val testDataDir = "op7tpro-india"
        val testDataOfVersion = "10.0.1.HD01AA"

        assertTrue(
            isSupportedDevice(
                testDataDir,
                testDataOfVersion,
                "OnePlus7TPro_IN",  // Gets replaced with value of ro.product.name by @hack #3 in SystemVersionProperties.java. But should be: "OnePlus 7T Pro"
                "OnePlus7TPro_IN",
                "OnePlus7TPro",
                "10.0.1.HD01AA",
                "OnePlus7TProOxygen_14.I.05_GLO_005_1909252239",
                true
            )
        )

        assertEquals("OnePlus 7T Pro (India)", getSupportedDevice(testDataDir, testDataOfVersion).name)
    }
}
