package com.pduvall.whtz.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pduvall.whtz.data.local.dao.DeckDao
import com.pduvall.whtz.data.local.dao.GameStateDao
import com.pduvall.whtz.data.local.dao.OracleCardDao
import com.pduvall.whtz.data.local.dao.RulingDao
import com.pduvall.whtz.data.local.dao.TokenCardDao
import com.pduvall.whtz.data.local.entity.DeckCardEntity
import com.pduvall.whtz.data.local.entity.DeckEntity
import com.pduvall.whtz.data.local.entity.GameStateEntity
import com.pduvall.whtz.data.local.entity.OracleCardEntity
import com.pduvall.whtz.data.local.entity.RulingEntity
import com.pduvall.whtz.data.local.entity.TokenCardEntity

@Database(
    entities = [
        OracleCardEntity::class,
        DeckEntity::class,
        DeckCardEntity::class,
        GameStateEntity::class,
        TokenCardEntity::class,
        RulingEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun oracleCardDao(): OracleCardDao
    abstract fun deckDao(): DeckDao
    abstract fun gameStateDao(): GameStateDao
    abstract fun tokenCardDao(): TokenCardDao
    abstract fun rulingDao(): RulingDao
}
