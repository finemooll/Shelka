package com.finemooll.shelka.data.mapper

import com.finemooll.shelka.data.json.WordDto
import com.finemooll.shelka.data.local.entity.WordEntity
import com.finemooll.shelka.domain.model.Word

fun WordDto.toEntity(): WordEntity = WordEntity(id, text, themeId, themeName, difficulty)
fun WordEntity.toDomain(): Word = Word(id, text, themeId, themeName, difficulty)
