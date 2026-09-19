package com.pduvall.whtz.data.deck

import com.pduvall.whtz.data.local.entity.OracleCardEntity

/**
 * Max copies of a single card we'll put into a game. High ceiling to support very large
 * libraries (cards like Templar Knight / Persistent Petitioners allow any number), while still
 * bounding game state to avoid unbounded memory use.
 */
const val MAX_COPIES_PER_CARD = 50_000

/** A decklist entry resolved to a real card. */
data class ResolvedDeckCard(
    val oracleId: String,
    val name: String,
    val quantity: Int,
    val section: DeckSection,
    val card: OracleCardEntity,
)

data class ResolvedDeck(
    val name: String,
    val cards: List<ResolvedDeckCard>,
)

/** Result of importing a decklist: the resolved deck plus any entries we couldn't match. */
data class DeckImportOutcome(
    val deck: ResolvedDeck,
    val unresolved: List<DeckEntry>,
    val resolvedCardCount: Int,
    val totalCardCount: Int,
    /** Number of entries whose quantity was capped to [MAX_COPIES_PER_CARD]. */
    val cappedCount: Int = 0,
)
