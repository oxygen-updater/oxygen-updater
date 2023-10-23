package com.oxygenupdater.ui.update

import com.oxygenupdater.ValueClassTestHelper
import kotlin.test.Test

class DownloadFailureTest {

    @Test
    fun `check if values are unique`() = ValueClassTestHelper.ensureUniqueValues<DownloadFailure>()
}
