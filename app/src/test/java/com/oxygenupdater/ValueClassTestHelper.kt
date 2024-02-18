package com.oxygenupdater

import java.lang.reflect.Modifier
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Helpers for [`@JvmInline`][JvmInline]` value class` tests */
object ValueClassTestHelper {

    inline fun <reified T> ensureUniqueValues(
        prefixClassNameInToStringCheck: Boolean = true,
    ) {
        val clazz = T::class.java
        val declaredFields = clazz.declaredFields

        val valueFieldType = declaredFields.find { it.name == "value" }?.type
        assertNotNull(valueFieldType) // ensure it has a `value` constructor param

        val fields = declaredFields.mapNotNull {
            // We only care about `private static final` fields in value classes
            if (it.modifiers and (Modifier.PRIVATE or Modifier.STATIC or Modifier.FINAL) == 0) return@mapNotNull null

            // Ignore constructor param
            if (it.name == "value") return@mapNotNull null

            // Ignore fields that don't have the same type the constructor param
            if (it.type != valueFieldType) return@mapNotNull null

            it.isAccessible = true; it
        }

        val values = fields.map { it.get(clazz)!! }
        // If empty, there's probably something wrong with our reflection code above
        assertTrue(values.isNotEmpty())
        // Ensure no value is repeated (could be due to a human copy-paste mistake)
        assertEquals(values.distinct().size, values.size)

        // Values are fine, now check their toString() representations
        val constructor = clazz.getDeclaredConstructor(valueFieldType).apply { isAccessible = true }
        // Ensure it's what we expect: "ClassName.FieldName" or "ClassName.value" if `value` is a String
        values.map { constructor.newInstance(it).toString() }.forEachIndexed { index, string ->
            val expected = if (valueFieldType == String::class.java) {
                values[index] as String
            } else fields[index].name

            assertEquals(if (prefixClassNameInToStringCheck) "${clazz.simpleName}.$expected" else expected, string)
        }
    }
}
