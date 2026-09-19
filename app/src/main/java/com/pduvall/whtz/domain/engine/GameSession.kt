package com.pduvall.whtz.domain.engine

import com.pduvall.whtz.domain.model.GameState
import kotlin.random.Random

/**
 * Stateful wrapper around [GameEngine] that keeps a labeled history of snapshots. [undo] pops the
 * last entry; [log] exposes the labels (oldest-first) and [revertTo] jumps back to any earlier
 * point, discarding everything after it. Snapshots are cheap because [GameState] is immutable.
 */
class GameSession(
    initial: GameState,
    private val random: Random = Random.Default,
    private val maxUndo: Int = DEFAULT_MAX_UNDO,
) {
    private data class Entry(val label: String, val state: GameState)

    private val history = ArrayDeque<Entry>().apply { addLast(Entry("Game start", initial)) }

    val state: GameState get() = history.last().state

    /** Action labels, oldest-first ("Game start" at index 0). */
    val log: List<String> get() = history.map { it.label }

    fun apply(command: Command, label: String = "") {
        val next = GameEngine.reduce(state, command, random)
        history.addLast(Entry(label, next))
        while (history.size > maxUndo + 1) history.removeFirst()
    }

    fun canUndo(): Boolean = history.size > 1

    fun undo() {
        if (history.size > 1) history.removeLast()
    }

    /** Revert to the state at [index] in [log], discarding every action after it. */
    fun revertTo(index: Int) {
        if (index < 0) return
        while (history.size - 1 > index) history.removeLast()
    }

    companion object {
        const val DEFAULT_MAX_UNDO = 200
    }
}
