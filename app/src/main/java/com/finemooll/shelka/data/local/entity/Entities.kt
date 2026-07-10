package com.finemooll.shelka.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class GameSessionStatus { IN_PROGRESS, COMPLETED }
enum class TurnWordResultStatus { CORRECT, SKIPPED, MISTAKE }

@Entity(
    tableName = "words",
    indices = [Index("difficulty"), Index("themeId"), Index(value = ["difficulty", "themeId"])]
)
data class WordEntity(
    @PrimaryKey val id: String,
    val text: String,
    val themeId: String,
    val themeName: String,
    val difficulty: Int,
)

@Entity(tableName = "game_sessions")
data class GameSessionEntity(
    @PrimaryKey val id: String,
    val createdAt: Long,
    val updatedAt: Long,
    val completedAt: Long?,
    val status: GameSessionStatus,
    val wordCount: Int,
    val difficulty: Int,
    val timerSeconds: Int,
    val currentRoundIndex: Int,
)

@Entity(
    tableName = "teams",
    foreignKeys = [ForeignKey(GameSessionEntity::class, ["id"], ["gameId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("gameId")]
)
data class TeamEntity(
    @PrimaryKey val id: String,
    val gameId: String,
    val name: String,
    val logoPath: String?,
    val orderIndex: Int,
)

@Entity(
    tableName = "players",
    foreignKeys = [ForeignKey(TeamEntity::class, ["id"], ["teamId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("teamId")]
)
data class PlayerEntity(
    @PrimaryKey val id: String,
    val teamId: String,
    val name: String,
    val orderIndex: Int,
)

@Entity(
    tableName = "game_selected_words",
    primaryKeys = ["gameId", "wordId"],
    foreignKeys = [
        ForeignKey(GameSessionEntity::class, ["id"], ["gameId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(WordEntity::class, ["id"], ["wordId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("gameId"), Index("wordId")]
)
data class GameSelectedWordEntity(
    val gameId: String,
    val wordId: String,
    val selectedOrder: Int?,
)

@Entity(
    tableName = "turns",
    foreignKeys = [
        ForeignKey(GameSessionEntity::class, ["id"], ["gameId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(TeamEntity::class, ["id"], ["teamId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(PlayerEntity::class, ["id"], ["explainerPlayerId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(PlayerEntity::class, ["id"], ["guesserPlayerId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("gameId"), Index("teamId"), Index("explainerPlayerId"), Index("guesserPlayerId")]
)
data class TurnEntity(
    @PrimaryKey val id: String,
    val gameId: String,
    val roundIndex: Int,
    val teamId: String,
    val explainerPlayerId: String,
    val guesserPlayerId: String,
    val startedAt: Long,
    val endedAt: Long?,
    val editable: Boolean,
)

@Entity(
    tableName = "turn_word_results",
    foreignKeys = [
        ForeignKey(TurnEntity::class, ["id"], ["turnId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(WordEntity::class, ["id"], ["wordId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("turnId"), Index("wordId")]
)
data class TurnWordResultEntity(
    @PrimaryKey val id: String,
    val turnId: String,
    val wordId: String,
    val status: TurnWordResultStatus,
    val shownOrder: Int,
)
