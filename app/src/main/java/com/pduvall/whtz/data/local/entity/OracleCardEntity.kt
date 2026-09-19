package com.pduvall.whtz.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row per Scryfall oracle_id (the game-rules identity of a card, shared across printings).
 * Populated from the Scryfall "Oracle Cards" bulk file. Card art is fetched lazily from the
 * stored URLs and cached on disk; only metadata + URLs live here.
 */
@Entity(
    tableName = "oracle_cards",
    indices = [Index(value = ["nameLower"])],
)
data class OracleCardEntity(
    @PrimaryKey val oracleId: String,
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
    /** JSON array of faces for DFC/split/adventure layouts (name/text/art per face); null otherwise. */
    override val cardFacesJson: String?,
    /** Representative printing id, for resolving a specific art if desired. */
    val printingId: String?,
    /** common / uncommon / rare / mythic / special / bonus; null until the DB is (re)imported. */
    val rarity: String? = null,
    /** JSON array of Scryfall all_parts (token/related links); null until the DB is (re)imported. */
    val allPartsJson: String? = null,
) : PrintableCard
