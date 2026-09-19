package com.pduvall.whtz.domain.model

import kotlinx.serialization.Serializable

/**
 * Immutable snapshot of one player's game. Each zone is an ordered list of card instances
 * (LIBRARY index 0 = top). [printedThisGame] records the instance ids that have been physically
 * printed this game — tracked per individual card, not per card name.
 */
@Serializable
data class GameState(
    val zones: Map<Zone, List<CardInstance>>,
    val printedThisGame: Set<String> = emptySet(),
    /** Instance ids of this game's commander(s) — 1 or 2 (partners). */
    val commanderIds: Set<String> = emptySet(),
    /** Times each commander has been cast from the command zone, driving commander tax. */
    val commanderCastCount: Map<String, Int> = emptyMap(),
) {
    fun zone(zone: Zone): List<CardInstance> = zones[zone].orEmpty()

    fun withZone(zone: Zone, cards: List<CardInstance>): GameState =
        copy(zones = zones + (zone to cards))

    /** Finds which zone a card instance currently sits in, or null if absent. */
    fun locate(instanceId: String): Pair<Zone, CardInstance>? {
        for ((zone, cards) in zones) {
            cards.firstOrNull { it.instanceId == instanceId }?.let { return zone to it }
        }
        return null
    }

    fun hasPrinted(instanceId: String): Boolean = instanceId in printedThisGame

    fun isCommander(instanceId: String): Boolean = instanceId in commanderIds

    /** Commander tax: +{2} per prior cast from the command zone. */
    fun commanderTax(instanceId: String): Int = 2 * (commanderCastCount[instanceId] ?: 0)

    companion object {
        /** A fresh game with the given library and every other zone empty. */
        fun newGame(library: List<CardInstance>): GameState =
            GameState(zones = Zone.entries.associateWith { zone ->
                if (zone == Zone.LIBRARY) library else emptyList()
            })
    }
}
