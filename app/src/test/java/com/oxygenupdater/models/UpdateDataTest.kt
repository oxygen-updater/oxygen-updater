package com.oxygenupdater.models

import kotlin.test.Test
import kotlin.test.assertEquals

class UpdateDataTest {

    @Test
    fun `check if build date is extracted properly`() {
        assertEquals(0, UpdateData.getBuildDate(null))
        assertEquals(0, UpdateData.getBuildDate(""))
        assertEquals(0, UpdateData.getBuildDate("_"))
        assertEquals(0, UpdateData.getBuildDate("_a"))

        assertEquals(5, UpdateData.getBuildDate("_5"))
        assertEquals(999999999999, UpdateData.getBuildDate("_999999999999"))
    }
}
