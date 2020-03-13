package com.arjanvlek.oxygenupdater.internal

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
class BooleanDeserializer : JsonDeserializer<Boolean>() {
    override fun deserialize(parser: JsonParser, context: DeserializationContext?): Boolean {
        return when (parser.text) {
            "true", "1" -> true
            "false", "0" -> false
            else -> false
        }
    }
}

