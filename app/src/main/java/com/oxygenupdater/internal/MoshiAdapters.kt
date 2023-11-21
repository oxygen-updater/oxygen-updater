package com.oxygenupdater.internal

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonQualifier
import com.squareup.moshi.ToJson

@Retention(AnnotationRetention.RUNTIME)
@JsonQualifier
annotation class ForceBoolean

/** Converts strings/numbers to [Boolean] */
class BooleanJsonAdapter {
    @FromJson
    @ForceBoolean
    fun fromJson(value: String?) = when (value) {
        "true", "1" -> true
        else -> false
    }

    @ToJson
    fun toJson(@ForceBoolean value: Boolean) = value
}

@Retention(AnnotationRetention.RUNTIME)
@JsonQualifier
annotation class CsvList

/**
 * Converts comma-separated string to List<String>
 *
 * Used in [com.oxygenupdater.models.Device.productNames]
 */
class CsvListJsonAdapter {
    @FromJson
    @CsvList
    fun fromJson(value: String?) = value?.trim()?.split(",")?.map { it.trim() } ?: listOf()

    @ToJson
    fun toJson(@CsvList value: List<String>) = value.joinToString(",")
}
