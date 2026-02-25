package com.oxygenupdater.internal

import com.oxygenupdater.utils.json
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SerializersTest {

    @Test
    fun `check ForceBooleanSerializer functionality`() = ForceBooleanSerializer.run {
        assertFalse(json.decodeFromString(this, "\"\""))

        assertFalse(json.decodeFromString(this, "0"))
        assertFalse(json.decodeFromString(this, "\"0\""))
        assertFalse(json.decodeFromString(this, "\"false\""))

        assertTrue(json.decodeFromString(this, "1"))
        assertTrue(json.decodeFromString(this, "\"1\""))
        assertTrue(json.decodeFromString(this, "\"true\""))
    }

    @Test
    fun `check if CsvListSerializer's deserialize works`() = CsvListSerializer.run {
        assertContentEquals(json.decodeFromString(this, "\"\""), listOf(""))

        assertContentEquals(json.decodeFromString(this, "\"1\""), listOf("1"))
        assertContentEquals(json.decodeFromString(this, "\"1,2\""), listOf("1", "2"))
        assertContentEquals(json.decodeFromString(this, "\"1, 2, 3\""), listOf("1", "2", "3"))
    }

    @Test
    fun `check if CsvListSerializer's serialize works`() = CsvListSerializer.run {
        assertEquals("\"\"", json.encodeToString(this, listOf()))

        assertEquals("\"1\"", json.encodeToString(this, listOf("1")))
        assertEquals("\"1,2\"", json.encodeToString(this, listOf("1", "2")))
    }
}
