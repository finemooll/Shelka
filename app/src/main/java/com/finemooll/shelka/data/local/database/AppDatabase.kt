package com.finemooll.shelka.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.finemooll.shelka.data.local.dao.GameHistoryDao
import com.finemooll.shelka.data.local.dao.WordDao
import com.finemooll.shelka.data.local.entity.*

class AppTypeConverters {
    @TypeConverter fun gameStatusToString(value: GameSessionStatus): String = value.name
    @TypeConverter fun stringToGameStatus(value: String): GameSessionStatus = GameSessionStatus.valueOf(value)
    @TypeConverter fun resultStatusToString(value: TurnWordResultStatus): String = value.name
    @TypeConverter fun stringToResultStatus(value: String): TurnWordResultStatus = TurnWordResultStatus.valueOf(value)
}

@Database(
    entities = [WordEntity::class, GameSessionEntity::class, TeamEntity::class, PlayerEntity::class, GameSelectedWordEntity::class, TurnEntity::class, TurnWordResultEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(AppTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
    abstract fun gameHistoryDao(): GameHistoryDao
}
