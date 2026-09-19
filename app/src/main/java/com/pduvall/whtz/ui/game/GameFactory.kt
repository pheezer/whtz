package com.pduvall.whtz.ui.game

import com.pduvall.whtz.data.deck.DeckSection
import com.pduvall.whtz.data.deck.MAX_COPIES_PER_CARD
import com.pduvall.whtz.data.deck.ResolvedDeck
import com.pduvall.whtz.domain.model.CardInstance
import com.pduvall.whtz.domain.model.GameState
import com.pduvall.whtz.domain.model.Zone
import java.util.UUID
import kotlin.random.Random

/** Expands a resolved deck into card instances and lays out the opening zones. */
object GameFactory {
    fun newGame(deck: ResolvedDeck, random: Random = Random.Default): GameState {
        val library = mutableListOf<CardInstance>()
        val command = mutableListOf<CardInstance>()
        for (card in deck.cards) {
            repeat(card.quantity.coerceAtMost(MAX_COPIES_PER_CARD)) {
                val instance = CardInstance(
                    instanceId = UUID.randomUUID().toString(),
                    oracleId = card.oracleId,
                    name = card.name,
                )
                when (card.section) {
                    DeckSection.COMMANDER -> command.add(instance)
                    DeckSection.MAINBOARD -> library.add(instance)
                    // Sideboard / companion / maybeboard are not part of a goldfishing game.
                    else -> Unit
                }
            }
        }
        val shuffledLibrary = library.shuffled(random)
        val zones = Zone.entries.associateWith { zone ->
            when (zone) {
                Zone.LIBRARY -> shuffledLibrary
                Zone.COMMAND -> command
                else -> emptyList()
            }
        }
        return GameState(
            zones = zones,
            commanderIds = command.map { it.instanceId }.toSet(),
        )
    }
}
