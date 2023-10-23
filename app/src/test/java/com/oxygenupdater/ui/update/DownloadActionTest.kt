package com.oxygenupdater.ui.update

import com.oxygenupdater.ValueClassTestHelper
import kotlin.test.Test

class DownloadActionTest {

    @Test
    fun `check if values are unique`() = ValueClassTestHelper.ensureUniqueValues<DownloadAction>()
}
