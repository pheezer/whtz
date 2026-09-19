package com.pduvall.whtz.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Single-row table (id always 0) holding the serialized current game so it survives process death. */
@Entity(tableName = "game_state")
data class GameStateEntity(
    @PrimaryKey val id: Int = 0,
    val deckId: Long?,
    val deckName: String,
    /** JSON of domain GameState. */
    val json: String,
    val updatedAt: Long,
)
