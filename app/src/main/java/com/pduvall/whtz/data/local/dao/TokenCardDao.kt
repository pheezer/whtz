package com.pduvall.whtz.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pduvall.whtz.data.local.entity.TokenCardEntity

@Dao
interface TokenCardDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tokens: List<TokenCardEntity>)

    @Query("SELECT COUNT(*) FROM tokens")
    suspend fun count(): Int

    /** Substring match on name or type line; pass an already-lowercased query. */
    @Query(
        "SELECT * FROM tokens WHERE nameLower LIKE '%' || :q || '%' " +
            "OR (typeLine IS NOT NULL AND lower(typeLine) LIKE '%' || :q || '%') " +
            "ORDER BY name LIMIT :limit",
    )
    suspend fun search(q: String, limit: Int = 200): List<TokenCardEntity>

    @Query("SELECT * FROM tokens ORDER BY name LIMIT :limit")
    suspend fun browse(limit: Int = 200): List<TokenCardEntity>

    /** Case-insensitive exact-name lookup; pass an already-lowercased name. */
    @Query("SELECT * FROM tokens WHERE nameLower = :nameLower LIMIT 1")
    suspend fun getByName(nameLower: String): TokenCardEntity?
}
