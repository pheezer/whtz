package com.pduvall.whtz.data.deck

import com.pduvall.whtz.data.local.dao.DeckDao
import com.pduvall.whtz.data.local.dao.OracleCardDao
import com.pduvall.whtz.data.local.entity.DeckCardEntity
import com.pduvall.whtz.data.local.entity.DeckEntity
import com.pduvall.whtz.data.scryfall.CardRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** A saved deck reconstructed as an editable name + pasteable decklist text. */
data class EditableDeck(val name: String, val text: String)

@Singleton
class DeckRepository @Inject constructor(
    private val cardRepository: CardRepository,
    private val deckDao: DeckDao,
    private val oracleCardDao: OracleCardDao,
) {
    /** Parses a decklist and resolves each entry (local-first, network fallback for misses). */
    suspend fun importFromText(deckName: String, text: String): DeckImportOutcome =
        withContext(Dispatchers.Default) {
            val parsed = DecklistParser.parse(text)
            val resolved = mutableListOf<ResolvedDeckCard>()
            val unresolved = mutableListOf<DeckEntry>()
            var cappedCount = 0
            for (entry in parsed.entries) {
                val card = cardRepository.resolve(entry.name)
                if (card != null) {
                    val quantity = entry.quantity.coerceAtMost(MAX_COPIES_PER_CARD)
                    if (entry.quantity > MAX_COPIES_PER_CARD) cappedCount++
                    resolved += ResolvedDeckCard(
                        oracleId = card.oracleId,
                        name = card.name,
                        quantity = quantity,
                        section = entry.section,
                        card = card,
                    )
                } else {
                    unresolved += entry
                }
            }
            DeckImportOutcome(
                deck = ResolvedDeck(deckName.ifBlank { "Imported deck" }, resolved),
                unresolved = unresolved,
                resolvedCardCount = resolved.sumOf { it.quantity },
                totalCardCount = parsed.entries.sumOf { it.quantity },
                cappedCount = cappedCount,
            )
        }

    suspend fun getDecks() = deckDao.getDecks()

    suspend fun deleteDeck(deckId: Long) = deckDao.deleteDeck(deckId)

    /** Rebuilds a saved deck's name + pasteable decklist text (one "qty name" line per card). */
    suspend fun loadForEdit(deckId: Long): EditableDeck? = withContext(Dispatchers.Default) {
        val deck = deckDao.getDeck(deckId) ?: return@withContext null
        val text = deckDao.getDeckCards(deckId).joinToString("\n") { "${it.quantity} ${it.name}" }
        EditableDeck(deck.name, text)
    }

    /** Replaces an existing deck's contents (delete + re-save); returns the new row id. */
    suspend fun updateDeck(deckId: Long, deck: ResolvedDeck): Long {
        deckDao.deleteDeck(deckId)
        return saveDeck(deck)
    }

    /** Reconstructs a saved deck (with full card data) for starting a game. */
    suspend fun loadDeck(deckId: Long): ResolvedDeck? = withContext(Dispatchers.Default) {
        val deck = deckDao.getDeck(deckId) ?: return@withContext null
        val cards = deckDao.getDeckCards(deckId).mapNotNull { dc ->
            val card = oracleCardDao.getByOracleId(dc.oracleId) ?: return@mapNotNull null
            ResolvedDeckCard(
                oracleId = dc.oracleId,
                name = dc.name,
                quantity = dc.quantity,
                section = runCatching { DeckSection.valueOf(dc.section) }.getOrDefault(DeckSection.MAINBOARD),
                card = card,
            )
        }
        ResolvedDeck(deck.name, cards)
    }

    suspend fun saveDeck(deck: ResolvedDeck): Long {
        val deckId = deckDao.insertDeck(
            DeckEntity(name = deck.name, createdAt = System.currentTimeMillis()),
        )
        deckDao.insertDeckCards(
            deck.cards.map {
                DeckCardEntity(
                    deckId = deckId,
                    oracleId = it.oracleId,
                    name = it.name,
                    quantity = it.quantity,
                    section = it.section.name,
                )
            },
        )
        return deckId
    }
}
