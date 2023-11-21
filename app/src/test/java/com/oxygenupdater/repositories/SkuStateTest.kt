package com.oxygenupdater.repositories

import com.oxygenupdater.ValueClassTestHelper
import kotlin.test.Test

class SkuStateTest {

    @Test
    fun `check if values are unique`() = ValueClassTestHelper.ensureUniqueValues<BillingRepository.SkuState>()
}
