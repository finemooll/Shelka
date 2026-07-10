package com.finemooll.shelka

import android.app.Application
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.finemooll.shelka.data.asset.AndroidWordAssetDataSource
import com.finemooll.shelka.data.json.DefaultWordValidator
import com.finemooll.shelka.data.json.KotlinxWordJsonParser
import com.finemooll.shelka.data.local.database.AppDatabase
import com.finemooll.shelka.data.repository.WordImporter
import com.finemooll.shelka.data.repository.WordRepositoryImpl
import com.finemooll.shelka.domain.repository.WordRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private val Application.wordImportDataStore by preferencesDataStore(name = "word_import")

class ShelkaApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
        applicationScope.launch { appContainer.wordRepository.ensureWordsImported() }
    }
}

class AppContainer(application: Application) {
    private val database: AppDatabase = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "shelka.db",
    ).build()

    private val importer = WordImporter(
        assetDataSource = AndroidWordAssetDataSource(application.assets),
        parser = KotlinxWordJsonParser(),
        validator = DefaultWordValidator(),
        database = database,
        dataStore = application.wordImportDataStore,
    )

    val wordRepository: WordRepository = WordRepositoryImpl(database.wordDao(), importer)
}
