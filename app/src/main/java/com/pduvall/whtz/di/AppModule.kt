package com.pduvall.whtz.di

import android.content.Context
import androidx.room.Room
import com.pduvall.whtz.data.local.AppDatabase
import com.pduvall.whtz.data.local.MIGRATION_3_4
import com.pduvall.whtz.data.local.MIGRATION_4_5
import com.pduvall.whtz.data.local.dao.DeckDao
import com.pduvall.whtz.data.local.dao.GameStateDao
import com.pduvall.whtz.data.local.dao.OracleCardDao
import com.pduvall.whtz.data.local.dao.RulingDao
import com.pduvall.whtz.data.local.dao.TokenCardDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "whtz.db")
            .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
            // Migrations preserve saved decks; destructive fallback only for uncovered version jumps.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideOracleCardDao(db: AppDatabase): OracleCardDao = db.oracleCardDao()

    @Provides
    fun provideDeckDao(db: AppDatabase): DeckDao = db.deckDao()

    @Provides
    fun provideGameStateDao(db: AppDatabase): GameStateDao = db.gameStateDao()

    @Provides
    fun provideTokenCardDao(db: AppDatabase): TokenCardDao = db.tokenCardDao()

    @Provides
    fun provideRulingDao(db: AppDatabase): RulingDao = db.rulingDao()
}
