package com.oxygenupdater.models

import com.oxygenupdater.ValueClassTestHelper
import kotlin.test.Test

class DeviceRequestFilterTest {

    @Test
    fun `check if values are unique`() = ValueClassTestHelper.ensureUniqueValues<DeviceRequestFilter>()
}
