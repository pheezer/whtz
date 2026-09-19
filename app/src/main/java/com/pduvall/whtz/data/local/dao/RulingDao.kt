package com.pduvall.whtz.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.pduvall.whtz.data.local.entity.RulingEntity

@Dao
interface RulingDao {

    @Insert
    suspend fun insertAll(rulings: List<RulingEntity>)

    @Query("DELETE FROM rulings")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM rulings")
    suspend fun count(): Int

    @Query("SELECT * FROM rulings WHERE oracleId = :oracleId ORDER BY publishedAt")
    suspend fun forOracle(oracleId: String): List<RulingEntity>

    /** Of the given oracle ids, those that have at least one ruling (for cheap action gating). */
    @Query("SELECT DISTINCT oracleId FROM rulings WHERE oracleId IN (:oracleIds)")
    suspend fun oracleIdsWithRulings(oracleIds: List<String>): List<String>
}
