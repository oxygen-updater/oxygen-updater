package com.oxygenupdater.ui.main

import com.oxygenupdater.ValueClassTestHelper
import kotlin.test.Test

class ChildScreenTest {

    @Test
    fun `check if values are unique`() = ValueClassTestHelper.ensureUniqueValues<ChildScreen>(false)
}
