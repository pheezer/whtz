package com.pduvall.whtz.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One Scryfall ruling, keyed by the card's oracle_id (shared across printings). Bundled offline
 * from the Scryfall "rulings" bulk file; the table is fully replaced on each (re)import.
 */
@Entity(
    tableName = "rulings",
    indices = [Index(value = ["oracleId"])],
)
data class RulingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val oracleId: String,
    val source: String?,
    val publishedAt: String?,
    val comment: String,
)
