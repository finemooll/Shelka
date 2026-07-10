package com.finemooll.shelka.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.finemooll.shelka.data.local.dao.WordDao
import com.finemooll.shelka.data.local.entity.GameSelectedWordEntity
import com.finemooll.shelka.data.local.entity.GameSessionEntity
import com.finemooll.shelka.data.local.entity.GameSessionStatus
import com.finemooll.shelka.data.local.entity.PlayerEntity
import com.finemooll.shelka.data.local.entity.TeamEntity
import com.finemooll.shelka.data.local.entity.TurnEntity
import com.finemooll.shelka.data.local.entity.TurnWordResultEntity
import com.finemooll.shelka.data.local.entity.TurnWordResultStatus
import com.finemooll.shelka.data.local.entity.WordEntity

@Database(
    entities = [WordEntity::class, GameSessionEntity::class, TeamEntity::class, PlayerEntity::class, GameSelectedWordEntity::class, TurnEntity::class, TurnWordResultEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(AppTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
}

class AppTypeConverters {
    @TypeConverter fun toGameSessionStatus(value: String): GameSessionStatus = GameSessionStatus.valueOf(value)
    @TypeConverter fun fromGameSessionStatus(value: GameSessionStatus): String = value.name
    @TypeConverter fun toTurnWordResultStatus(value: String): TurnWordResultStatus = TurnWordResultStatus.valueOf(value)
    @TypeConverter fun fromTurnWordResultStatus(value: TurnWordResultStatus): String = value.name
}
