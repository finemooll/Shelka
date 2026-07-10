package com.finemooll.shelka.data.asset

import android.content.res.AssetManager

interface WordAssetDataSource { suspend fun readWordsJson(): String }

class AndroidWordAssetDataSource(
    private val assetManager: AssetManager,
    private val assetPath: String = "words.json",
) : WordAssetDataSource {
    override suspend fun readWordsJson(): String = try {
        assetManager.open(assetPath).bufferedReader().use { it.readText() }
    } catch (cause: Exception) {
        throw IllegalStateException("Unable to load asset $assetPath", cause)
    }
}
