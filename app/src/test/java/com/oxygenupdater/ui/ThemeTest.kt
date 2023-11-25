package com.oxygenupdater.ui

import com.oxygenupdater.ValueClassTestHelper
import kotlin.test.Test

class ThemeTest {

    @Test
    fun `check if values are unique`() = ValueClassTestHelper.ensureUniqueValues<Theme>()
}
