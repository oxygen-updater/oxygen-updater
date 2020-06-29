package com.oxygenupdater.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 * @author [Arjan Vlek](https://github.com/arjanvlek)
 */
class SystemVersionPropertiesTestOnePlus7Pro : SystemVersionPropertiesTest() {

    /*
     * Test if European Economic Area (EEA) model is correctly detected by the app.
     * Note: The device *is* detected by App 1.0.0 - 2.3.2, but those legacy versions
     * cannot differentiate the EU version from other regional editions.
     * As nobody is going to use such old OU versions on OP7+, this is acceptable
     */
    @Test
    fun testSupportedDevice_EEAVersion_OxygenOS951GM21BA() {
        val testDataDir = "op7pro/eea"
        val testDataOfVersion = "9.5.1.GM21BA"

        assertTrue(
            isSupportedDevice(
                testDataDir,
                testDataOfVersion,
                "OnePlus7Pro_EEA",
                "OnePlus7Pro_EEA",  // Gets replaced with value of ro.product.name by @hack #3 in SystemVersionProperties.java. But should be: "OnePlus 7 Pro"
                "OnePlus7Pro",
                "9.5.1.GM21BA",
                "OnePlus7ProOxygen_21.E.05_GLO_005_1904250312",
                true
            )
        )

        assertEquals("OnePlus 7 Pro (EU)", getSupportedDevice(testDataDir, testDataOfVersion).name)
    }

    /*
     * Test if International (intl) model is correctly detected by the app.
     * Note: All variants of OP7 Pro are incorrectly detected as this variant by app versions 1.0.0 - 2.3.2
     * However: as nobody is going to use such old OU versions on OP7+, this is acceptable
     */
    @Test
    fun testSupportedDevice_INTLVersion_OxygenOS953GM21AA() {
        val testDataDir = "op7pro/intl"
        val testDataOfVersion = "9.5.3.GM21AA"

        assertTrue(
            isSupportedDevice(
                testDataDir,
                testDataOfVersion,
                "OnePlus7Pro",
                "OnePlus7Pro",  // Gets replaced with value of ro.product.name by @hack #3 in SystemVersionProperties.java. But should be: "OnePlus 7 Pro"
                "OnePlus7Pro",
                "9.5.3.GM21AA",
                "OnePlus7ProOxygen_21.O.07_GLO_007_1905120542",
                true
            )
        )

        assertEquals("OnePlus 7 Pro", getSupportedDevice(testDataDir, testDataOfVersion).name)
    }

    @Test
    fun testSupportedDevice_5GEUVersion_OxygenOS951GM27BA() {
        val testDataDir = "op7pro/eea-5g"
        val testDataOfVersion = "9.5.1.GM27BA"

        assertTrue(
            isSupportedDevice(
                testDataDir,
                testDataOfVersion,
                "OnePlus7ProNR_EEA",
                "OnePlus7ProNR_EEA",  // Gets replaced with value of ro.product.name by @hack #3 in SystemVersionProperties.java. But should be: "OnePlus 7 Pro 5G"
                "OnePlus7ProNR",
                "9.5.1.GM27BA",
                "OnePlus7ProNROxygen_21.E.02_GLO_002_1905122259",
                true
            )
        )

        assertEquals("OnePlus 7 Pro 5G (EU)", getSupportedDevice(testDataDir, testDataOfVersion).name)
    }
}
