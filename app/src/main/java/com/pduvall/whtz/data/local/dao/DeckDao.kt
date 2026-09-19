package com.pduvall.whtz.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.pduvall.whtz.data.local.entity.DeckCardEntity
import com.pduvall.whtz.data.local.entity.DeckEntity

@Dao
interface DeckDao {

    @Insert
    suspend fun insertDeck(deck: DeckEntity): Long

    @Insert
    suspend fun insertDeckCards(cards: List<DeckCardEntity>)

    @Query("SELECT * FROM decks ORDER BY createdAt DESC")
    suspend fun getDecks(): List<DeckEntity>

    @Query("SELECT * FROM decks WHERE id = :deckId LIMIT 1")
    suspend fun getDeck(deckId: Long): DeckEntity?

    @Query("SELECT * FROM deck_cards WHERE deckId = :deckId")
    suspend fun getDeckCards(deckId: Long): List<DeckCardEntity>

    @Query("DELETE FROM decks WHERE id = :deckId")
    suspend fun deleteDeck(deckId: Long)
}
