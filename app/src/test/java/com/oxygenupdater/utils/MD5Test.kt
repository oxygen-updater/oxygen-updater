package com.oxygenupdater.utils

import kotlin.test.Test
import kotlin.test.assertEquals

class MD5Test {

    @Test
    fun `check if MD5 length is 32 characters`() {
        assertEquals(32, MD5.calculateMD5("").length)
        assertEquals(32, MD5.calculateMD5("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA").length)
    }
}
