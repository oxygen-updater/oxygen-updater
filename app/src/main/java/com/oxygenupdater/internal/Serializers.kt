package com.oxygenupdater.internal

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.listSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer

typealias ForceBoolean = @Serializable(ForceBooleanSerializer::class) Boolean
typealias CsvList = @Serializable(CsvListSerializer::class) List<String>

/** Converts strings/numbers to [Boolean] */
object ForceBooleanSerializer : JsonTransformingSerializer<Boolean>(Boolean.serializer()) {
    override fun transformDeserialize(element: JsonElement) = JsonPrimitive(
        if (element is JsonPrimitive) when (element.content) {
            "true", "1" -> true
            else -> false
        } else false
    )
}

/**
 * Converts comma-separated string to List<String>
 *
 * Used in [com.oxygenupdater.models.Device.productNames]
 */
object CsvListSerializer : KSerializer<List<String>> {

    /** Prefixed with package name to ensure uniqueness */
    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor = SerialDescriptor("com.oxygenupdater.CsvList", listSerialDescriptor<String>())

    override fun serialize(encoder: Encoder, value: List<String>) = encoder.encodeString(value.joinToString(","))
    override fun deserialize(decoder: Decoder) = decoder.decodeString().trim().split(",").map { it.trim() }
}
