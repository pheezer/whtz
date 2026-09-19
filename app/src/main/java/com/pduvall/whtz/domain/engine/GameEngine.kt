package com.pduvall.whtz.domain.engine

import com.pduvall.whtz.domain.model.CardInstance
import com.pduvall.whtz.domain.model.GameState
import com.pduvall.whtz.domain.model.Zone
import kotlin.random.Random

/** Pure reducer: (state, command) -> new state. The only nondeterminism is Shuffle's [random]. */
object GameEngine {

    fun reduce(state: GameState, command: Command, random: Random = Random.Default): GameState =
        when (command) {
            is Command.Draw -> moveTop(state, Zone.LIBRARY, Zone.HAND, command.count)
            is Command.MillTop -> moveTop(state, Zone.LIBRARY, Zone.GRAVEYARD, command.count)
            is Command.ExileTop -> moveTop(state, Zone.LIBRARY, Zone.EXILE, command.count)
            Command.Shuffle -> state.withZone(Zone.LIBRARY, state.zone(Zone.LIBRARY).shuffled(random))
            is Command.MoveToTop -> moveIntoLibrary(state, command.instanceId, toTop = true)
            is Command.MoveToBottom -> moveIntoLibrary(state, command.instanceId, toTop = false)
            is Command.MoveCard -> moveInstance(state, command.instanceId, command.to)
            is Command.ReorderTop -> reorderTop(state, command.orderedInstanceIds)
            is Command.MarkPrinted -> state.copy(printedThisGame = state.printedThisGame + command.oracleId)
            is Command.CastCommander -> castCommander(state, command.instanceId)
            Command.Wheel -> wheel(state)
        }

    private fun wheel(state: GameState): GameState {
        val discarded = state
            .withZone(Zone.GRAVEYARD, state.zone(Zone.GRAVEYARD) + state.zone(Zone.HAND))
            .withZone(Zone.HAND, emptyList())
        return moveTop(discarded, Zone.LIBRARY, Zone.HAND, 7)
    }

    private fun castCommander(state: GameState, instanceId: String): GameState {
        if (state.locate(instanceId) == null) return state
        val onBattlefield = moveInstance(state, instanceId, Zone.BATTLEFIELD)
        val casts = (onBattlefield.commanderCastCount[instanceId] ?: 0) + 1
        return onBattlefield.copy(
            commanderCastCount = onBattlefield.commanderCastCount + (instanceId to casts),
        )
    }

    private fun moveTop(state: GameState, from: Zone, to: Zone, count: Int): GameState {
        val source = state.zone(from)
        val k = count.coerceIn(0, source.size)
        if (k == 0) return state
        val moved = source.take(k)
        return state
            .withZone(from, source.drop(k))
            .withZone(to, state.zone(to) + moved)
    }

    private fun moveInstance(state: GameState, instanceId: String, to: Zone): GameState {
        val (from, instance) = state.locate(instanceId) ?: return state
        val without = state.zone(from).filterNot { it.instanceId == instanceId }
        val afterRemoval = state.withZone(from, without)
        return afterRemoval.withZone(to, afterRemoval.zone(to) + instance)
    }

    private fun moveIntoLibrary(state: GameState, instanceId: String, toTop: Boolean): GameState {
        val (from, instance) = state.locate(instanceId) ?: return state
        val without = state.zone(from).filterNot { it.instanceId == instanceId }
        val afterRemoval = state.withZone(from, without)
        val library = afterRemoval.zone(Zone.LIBRARY)
        val newLibrary = if (toTop) listOf(instance) + library else library + instance
        return afterRemoval.withZone(Zone.LIBRARY, newLibrary)
    }

    private fun reorderTop(state: GameState, orderedInstanceIds: List<String>): GameState {
        val library = state.zone(Zone.LIBRARY)
        val n = orderedInstanceIds.size
        if (n == 0 || n > library.size) return state
        val currentTop: List<CardInstance> = library.take(n)
        if (currentTop.map { it.instanceId }.toSet() != orderedInstanceIds.toSet()) return state
        val byId = currentTop.associateBy { it.instanceId }
        val reordered = orderedInstanceIds.map { byId.getValue(it) }
        return state.withZone(Zone.LIBRARY, reordered + library.drop(n))
    }
}
