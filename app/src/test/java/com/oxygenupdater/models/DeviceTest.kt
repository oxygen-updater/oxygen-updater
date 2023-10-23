package com.oxygenupdater.models

import kotlin.test.Test
import kotlin.test.assertTrue

class DeviceTest {

    @Test
    fun `check if image URL is constructed properly`() {
        Device.constructImageUrl("Test").let {
            assertTrue(it.startsWith(Device.ImageUrlPrefix))
            assertTrue(it.endsWith("test" + Device.ImageUrlSuffix))
        }

        Device.constructImageUrl("Test Device (regional qualifier)").let {
            assertTrue(it.startsWith(Device.ImageUrlPrefix))
            assertTrue(it.endsWith("test-device" + Device.ImageUrlSuffix))
        }

        Device.constructImageUrl("Test Device Long NameNumber (regional qualifier) (something else)").let {
            assertTrue(it.startsWith(Device.ImageUrlPrefix))
            assertTrue(it.endsWith("test-device-long-namenumber" + Device.ImageUrlSuffix))
        }
    }
}
