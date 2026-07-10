package com.finemooll.shelka.data.importer

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

interface WordImportStateStore { suspend fun isImported(): Boolean; suspend fun setImported(imported: Boolean) }

class DataStoreWordImportStateStore(private val dataStore: DataStore<Preferences>) : WordImportStateStore {
    private val key = booleanPreferencesKey("words_imported")
    override suspend fun isImported(): Boolean = dataStore.data.map { it[key] ?: false }.first()
    override suspend fun setImported(imported: Boolean) { dataStore.edit { it[key] = imported } }
}
