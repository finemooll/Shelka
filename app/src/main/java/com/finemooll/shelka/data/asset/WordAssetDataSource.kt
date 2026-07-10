package com.finemooll.shelka.data.asset

import android.content.res.AssetManager
import java.io.IOException

class WordAssetReadException(message: String, cause: Throwable) : IOException(message, cause)

interface WordAssetDataSource { suspend fun readWordsJson(): String }

class AndroidWordAssetDataSource(
    private val assetManager: AssetManager,
    private val assetName: String = "words.json",
) : WordAssetDataSource {
    override suspend fun readWordsJson(): String = try {
        assetManager.open(assetName).bufferedReader().use { it.readText() }
    } catch (cause: IOException) {
        throw WordAssetReadException("Failed to read asset '$assetName'", cause)
    }
}
