package com.pduvall.whtz.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pduvall.whtz.data.local.entity.OracleCardEntity

/** Lightweight projection for filtering + mechanics without loading full rows. */
data class CardMetaRow(
    val oracleId: String,
    val typeLine: String?,
    val rarity: String?,
    val manaCost: String?,
    val allPartsJson: String?,
    val normalImageUrl: String?,
)

@Dao
interface OracleCardDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(cards: List<OracleCardEntity>)

    @Query("SELECT COUNT(*) FROM oracle_cards")
    suspend fun count(): Int

    /** Case-insensitive exact-name lookup; pass an already-lowercased name. */
    @Query("SELECT * FROM oracle_cards WHERE nameLower = :nameLower LIMIT 1")
    suspend fun findByExactName(nameLower: String): OracleCardEntity?

    @Query("SELECT * FROM oracle_cards WHERE oracleId = :oracleId LIMIT 1")
    suspend fun getByOracleId(oracleId: String): OracleCardEntity?

    @Query("SELECT oracleId, typeLine, rarity, manaCost, allPartsJson, normalImageUrl FROM oracle_cards WHERE oracleId IN (:oracleIds)")
    suspend fun getMeta(oracleIds: List<String>): List<CardMetaRow>
}
