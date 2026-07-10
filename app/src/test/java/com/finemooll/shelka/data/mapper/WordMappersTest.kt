package com.finemooll.shelka.data.mapper

import com.finemooll.shelka.data.json.WordDto
import org.junit.Assert.assertEquals
import org.junit.Test

class WordMappersTest {
    @Test fun dtoToEntityAndEntityToDomain() {
        val dto = WordDto("id", "text", "theme", "Theme", 2)
        val entity = dto.toEntity()
        assertEquals(dto.id, entity.id)
        val domain = entity.toDomain()
        assertEquals(dto.text, domain.text)
        assertEquals(dto.difficulty, domain.difficulty)
    }
}
