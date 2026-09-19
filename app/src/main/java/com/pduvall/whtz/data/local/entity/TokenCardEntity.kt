package com.pduvall.whtz.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row per unique token (Scryfall `is:token`, deduped by oracle_id). Bundled offline so the
 * token library is searchable without a network round-trip; art is still fetched lazily at print
 * time from [artCropUrl], as with real cards.
 */
@Entity(
    tableName = "tokens",
    indices = [Index(value = ["nameLower"])],
)
data class TokenCardEntity(
    /** Scryfall oracle_id, or the printing id when a token has no oracle_id. */
    @PrimaryKey val id: String,
    override val name: String,
    val nameLower: String,
    override val manaCost: String?,
    override val typeLine: String?,
    override val oracleText: String?,
    override val power: String?,
    override val toughness: String?,
    val layout: String,
    override val artCropUrl: String?,
    val normalImageUrl: String?,
    override val cardFacesJson: String?,
) : PrintableCard
