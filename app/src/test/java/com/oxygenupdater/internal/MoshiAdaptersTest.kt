package com.oxygenupdater.internal

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MoshiAdaptersTest {

    @Test
    fun `check if BooleanJsonAdapter's fromJson works`() = BooleanJsonAdapter().run {
        assertFalse(fromJson(null))
        assertFalse(fromJson(""))

        assertFalse(fromJson("0"))
        assertFalse(fromJson("false"))

        assertTrue(fromJson("1"))
        assertTrue(fromJson("true"))
    }

    @Test
    fun `check if BooleanJsonAdapter's toJson works`() = BooleanJsonAdapter().run {
        assertFalse(toJson(false))
        assertTrue(toJson(true))
    }

    @Test
    fun `check if CsvListJsonAdapter's fromJson works`() = CsvListJsonAdapter().run {
        assertContentEquals(fromJson(null), listOf())
        assertContentEquals(fromJson(""), listOf(""))

        assertContentEquals(fromJson("1"), listOf("1"))
        assertContentEquals(fromJson("1,2"), listOf("1", "2"))
        assertContentEquals(fromJson("1, 2, 3"), listOf("1", "2", "3"))
    }

    @Test
    fun `check if CsvListJsonAdapter's toJson works`() = CsvListJsonAdapter().run {
        assertEquals("", toJson(listOf()))

        assertEquals("1", toJson(listOf("1")))
        assertEquals("1,2", toJson(listOf("1", "2")))
    }
}
