package com.oxygenupdater.ui.main

import com.oxygenupdater.ValueClassTestHelper
import kotlin.test.Test

class NavTypeTest {

    @Test
    fun `check if values are unique`() = ValueClassTestHelper.ensureUniqueValues<NavType>()
}
