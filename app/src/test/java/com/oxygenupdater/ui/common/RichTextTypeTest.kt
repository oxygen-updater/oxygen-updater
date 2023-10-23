package com.oxygenupdater.ui.common

import com.oxygenupdater.ValueClassTestHelper
import kotlin.test.Test

class RichTextTypeTest {

    @Test
    fun `check if values are unique`() = ValueClassTestHelper.ensureUniqueValues<RichTextType>()
}
