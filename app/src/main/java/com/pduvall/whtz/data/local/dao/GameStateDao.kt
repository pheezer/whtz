package com.pduvall.whtz.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pduvall.whtz.data.local.entity.GameStateEntity

@Dao
interface GameStateDao {

    @Query("SELECT * FROM game_state WHERE id = 0 LIMIT 1")
    suspend fun get(): GameStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: GameStateEntity)

    @Query("DELETE FROM game_state")
    suspend fun clear()
}
