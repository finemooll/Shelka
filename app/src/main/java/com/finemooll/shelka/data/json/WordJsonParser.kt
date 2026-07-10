package com.finemooll.shelka.data.json

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement

class WordJsonParseException(message: String, cause: Throwable? = null) : Exception(message, cause)

class WordJsonParser(private val json: Json = Json { ignoreUnknownKeys = false }) {
    fun parse(rawJson: String): List<WordDto> {
        val root = try { json.parseToJsonElement(rawJson) } catch (e: SerializationException) { throw WordJsonParseException("Malformed words JSON", e) }
        if (root !is JsonArray) throw WordJsonParseException("words.json root must be an array")
        return root.mapIndexed { index: Int, element: JsonElement ->
            try { json.decodeFromJsonElement(WordDto.serializer(), element) }
            catch (e: SerializationException) { throw WordJsonParseException("Malformed word object at index $index", e) }
            catch (e: IllegalArgumentException) { throw WordJsonParseException("Malformed word object at index $index", e) }
        }
    }
}
