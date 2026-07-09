package com.finemooll.shelka.domain.repository

import com.finemooll.shelka.domain.model.Word

interface WordRepository {
    suspend fun getWords(): List<Word>
}
