package com.finemooll.shelka

import android.app.Application
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.finemooll.shelka.app.AppInitializer
import com.finemooll.shelka.data.asset.AndroidWordAssetDataSource
import com.finemooll.shelka.data.importer.DataStoreWordImportStateStore
import com.finemooll.shelka.data.importer.WordImporter
import com.finemooll.shelka.data.logo.AndroidTeamLogoStorage
import com.finemooll.shelka.data.repository.RoomGameRepository
import com.finemooll.shelka.domain.usecase.CreateGameSessionUseCase
import com.finemooll.shelka.presentation.viewmodel.NewGameViewModelFactory
import com.finemooll.shelka.data.json.DefaultWordValidator
import com.finemooll.shelka.data.json.WordJsonParser
import com.finemooll.shelka.data.local.database.AppDatabase
import com.finemooll.shelka.data.repository.WordRepositoryImpl
import com.finemooll.shelka.domain.repository.WordRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers

private val Application.wordImportDataStore by preferencesDataStore(name = "word_import_state")

class ShelkaApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
        appContainer.appInitializer.start()
    }
}

class AppContainer(application: Application) {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: AppDatabase = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "shelka.db",
    ).build()

    private val wordAssetDataSource = AndroidWordAssetDataSource(application.assets)
    private val wordJsonParser = WordJsonParser()
    private val wordValidator = DefaultWordValidator()
    private val importStateStore = DataStoreWordImportStateStore(application.wordImportDataStore)
    private val wordImporter = WordImporter(
        assetDataSource = wordAssetDataSource,
        parser = wordJsonParser,
        validator = wordValidator,
        database = database,
        stateStore = importStateStore,
    )

    val wordRepository: WordRepository = WordRepositoryImpl(database.wordDao(), wordImporter)
    private val teamLogoStorage = AndroidTeamLogoStorage(application)
    private val gameRepository = RoomGameRepository(database, teamLogoStorage)
    val appInitializer: AppInitializer = AppInitializer(wordRepository, applicationScope)
    val newGameDependencies = NewGameViewModelFactory.Dependencies(
        wordRepository = wordRepository,
        logoStorage = teamLogoStorage,
        createGame = CreateGameSessionUseCase(gameRepository),
        appInitializationState = appInitializer.state,
    )
}
