package com.finemooll.shelka.data.json

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray

class WordJsonParseException(message: String, cause: Throwable? = null) : Exception(message, cause)

interface WordJsonParser {
    @Throws(WordJsonParseException::class)
    fun parse(jsonText: String): List<WordDto>
}

class KotlinxWordJsonParser(
    private val json: Json = Json { ignoreUnknownKeys = false },
) : WordJsonParser {
    override fun parse(jsonText: String): List<WordDto> = try {
        val element = json.parseToJsonElement(jsonText)
        if (element !is JsonArray) {
            throw WordJsonParseException("words.json root must be a JSON array")
        }
        element.jsonArray.mapIndexed { index, item ->
            try {
                json.decodeFromJsonElement<WordDto>(item)
            } catch (cause: SerializationException) {
                throw WordJsonParseException("Malformed word object at index $index", cause)
            } catch (cause: IllegalArgumentException) {
                throw WordJsonParseException("Invalid word object at index $index", cause)
            }
        }
    } catch (cause: WordJsonParseException) {
        throw cause
    } catch (cause: Exception) {
        throw WordJsonParseException("Failed to parse words.json", cause)
    }
}
