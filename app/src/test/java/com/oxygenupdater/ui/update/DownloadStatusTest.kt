package com.oxygenupdater.ui.update

import com.oxygenupdater.ValueClassTestHelper
import kotlin.test.Test

class DownloadStatusTest {

    @Test
    fun `check if values are unique`() = ValueClassTestHelper.ensureUniqueValues<DownloadStatus>()
}
