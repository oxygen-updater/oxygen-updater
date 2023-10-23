package com.oxygenupdater.models

import com.oxygenupdater.ValueClassTestHelper
import org.junit.Test

class DeviceOsSpecTest {

    @Test
    fun `check if values are unique`() = ValueClassTestHelper.ensureUniqueValues<DeviceOsSpec>()
}
