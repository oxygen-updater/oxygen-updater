package com.oxygenupdater.ui.update

import com.oxygenupdater.ValueClassTestHelper
import kotlin.test.Test

class Md5VerificationFailureTest {

    @Test
    fun `check if values are unique`() = ValueClassTestHelper.ensureUniqueValues<Md5VerificationFailure>()
}
