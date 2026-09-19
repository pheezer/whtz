package com.pduvall.whtz.domain

import com.pduvall.whtz.domain.engine.Command
import com.pduvall.whtz.domain.engine.GameEngine
import com.pduvall.whtz.domain.engine.GameSession
import com.pduvall.whtz.domain.model.CardInstance
import com.pduvall.whtz.domain.model.GameState
import com.pduvall.whtz.domain.model.Zone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameEngineTest {

    private fun inst(id: String, oracle: String = "o-$id") = CardInstance(id, oracle, "Card $id")
    private fun libraryOf(vararg ids: String) = GameState.newGame(ids.map { inst(it) })

    private fun ids(state: GameState, zone: Zone) = state.zone(zone).map { it.instanceId }

    @Test
    fun `new game puts everything in library`() {
        val s = libraryOf("a", "b", "c")
        assertEquals(listOf("a", "b", "c"), ids(s, Zone.LIBRARY))
        assertTrue(s.zone(Zone.HAND).isEmpty())
        assertTrue(s.zone(Zone.BATTLEFIELD).isEmpty())
    }

    @Test
    fun `wheel discards the hand to the graveyard and draws seven`() {
        val libIds = (1..10).map { "l$it" }
        val state = GameState(
            zones = mapOf(
                Zone.LIBRARY to libIds.map { inst(it) },
                Zone.HAND to listOf(inst("x"), inst("y")),
            ),
        )
        val after = GameEngine.reduce(state, Command.Wheel)
        assertEquals(libIds.take(7), ids(after, Zone.HAND))
        assertEquals(listOf("x", "y"), ids(after, Zone.GRAVEYARD))
        assertEquals(libIds.drop(7), ids(after, Zone.LIBRARY))
    }

    @Test
    fun `casting the commander moves it to battlefield and grows the tax`() {
        val state = GameState(
            zones = mapOf(Zone.COMMAND to listOf(inst("cmd"))),
            commanderIds = setOf("cmd"),
        )
        assertEquals(0, state.commanderTax("cmd"))

        val cast1 = GameEngine.reduce(state, Command.CastCommander("cmd"))
        assertEquals(listOf("cmd"), ids(cast1, Zone.BATTLEFIELD))
        assertTrue(cast1.zone(Zone.COMMAND).isEmpty())
        assertEquals(2, cast1.commanderTax("cmd"))

        // Return to the command zone, then cast again → tax climbs to 4.
        val returned = GameEngine.reduce(cast1, Command.MoveCard("cmd", Zone.COMMAND))
        assertEquals(listOf("cmd"), ids(returned, Zone.COMMAND))
        val cast2 = GameEngine.reduce(returned, Command.CastCommander("cmd"))
        assertEquals(4, cast2.commanderTax("cmd"))
    }

    @Test
    fun `draw moves top cards to hand preserving order`() {
        val s = GameEngine.reduce(libraryOf("a", "b", "c", "d"), Command.Draw(2))
        assertEquals(listOf("c", "d"), ids(s, Zone.LIBRARY))
        assertEquals(listOf("a", "b"), ids(s, Zone.HAND))
    }

    @Test
    fun `drawing more than library size is clamped`() {
        val s = GameEngine.reduce(libraryOf("a", "b"), Command.Draw(5))
        assertTrue(s.zone(Zone.LIBRARY).isEmpty())
        assertEquals(listOf("a", "b"), ids(s, Zone.HAND))
    }

    @Test
    fun `mill and exile pull from top of library`() {
        val milled = GameEngine.reduce(libraryOf("a", "b", "c"), Command.MillTop(1))
        assertEquals(listOf("a"), ids(milled, Zone.GRAVEYARD))
        val exiled = GameEngine.reduce(milled, Command.ExileTop(1))
        assertEquals(listOf("b"), ids(exiled, Zone.EXILE))
        assertEquals(listOf("c"), ids(exiled, Zone.LIBRARY))
    }

    @Test
    fun `shuffle preserves the multiset and is deterministic for a seed`() {
        val base = libraryOf("a", "b", "c", "d", "e")
        val once = GameEngine.reduce(base, Command.Shuffle, Random(42))
        val twice = GameEngine.reduce(base, Command.Shuffle, Random(42))
        assertEquals(ids(once, Zone.LIBRARY), ids(twice, Zone.LIBRARY))
        assertEquals(
            setOf("a", "b", "c", "d", "e"),
            once.zone(Zone.LIBRARY).map { it.instanceId }.toSet(),
        )
    }

    @Test
    fun `move card sends it to the target zone`() {
        var s = libraryOf("a", "b", "c")
        s = GameEngine.reduce(s, Command.Draw(1)) // a -> hand
        s = GameEngine.reduce(s, Command.MoveCard("a", Zone.BATTLEFIELD))
        assertEquals(listOf("a"), ids(s, Zone.BATTLEFIELD))
        assertTrue(s.zone(Zone.HAND).isEmpty())
    }

    @Test
    fun `move to top and bottom of library`() {
        var s = libraryOf("a", "b", "c")
        s = GameEngine.reduce(s, Command.Draw(2)) // hand: a,b ; library: c
        s = GameEngine.reduce(s, Command.MoveToTop("b")) // library: b,c
        assertEquals(listOf("b", "c"), ids(s, Zone.LIBRARY))
        s = GameEngine.reduce(s, Command.MoveToBottom("a")) // library: b,c,a
        assertEquals(listOf("b", "c", "a"), ids(s, Zone.LIBRARY))
        assertTrue(s.zone(Zone.HAND).isEmpty())
    }

    @Test
    fun `reorder top rearranges the top N and rejects invalid input`() {
        val s = libraryOf("a", "b", "c", "d")
        val reordered = GameEngine.reduce(s, Command.ReorderTop(listOf("c", "a", "b")))
        assertEquals(listOf("c", "a", "b", "d"), ids(reordered, Zone.LIBRARY))

        // ids that aren't the current top set are ignored (no-op).
        val invalid = GameEngine.reduce(s, Command.ReorderTop(listOf("a", "d")))
        assertEquals(listOf("a", "b", "c", "d"), ids(invalid, Zone.LIBRARY))
    }

    @Test
    fun `mark printed records the oracle id`() {
        val s = GameEngine.reduce(libraryOf("a"), Command.MarkPrinted("oracle-x"))
        assertTrue(s.hasPrinted("oracle-x"))
        assertFalse(s.hasPrinted("oracle-y"))
    }

    @Test
    fun `session undo restores prior state`() {
        val session = GameSession(libraryOf("a", "b", "c"))
        assertFalse(session.canUndo())

        session.apply(Command.Draw(1))
        assertEquals(listOf("a"), session.state.zone(Zone.HAND).map { it.instanceId })
        assertTrue(session.canUndo())

        session.apply(Command.Draw(1))
        assertEquals(2, session.state.zone(Zone.HAND).size)

        session.undo()
        assertEquals(1, session.state.zone(Zone.HAND).size)
        session.undo()
        assertTrue(session.state.zone(Zone.HAND).isEmpty())
        assertEquals(listOf("a", "b", "c"), session.state.zone(Zone.LIBRARY).map { it.instanceId })
        assertFalse(session.canUndo())
    }

    @Test
    fun `revertTo jumps back to a past action and truncates the log`() {
        val session = GameSession(libraryOf("a", "b", "c", "d"))
        session.apply(Command.Draw(1), "Draw 1")
        session.apply(Command.Draw(1), "Draw 1")
        session.apply(Command.Draw(1), "Draw 1")
        assertEquals(3, session.state.zone(Zone.HAND).size)
        assertEquals(4, session.log.size) // "Game start" + 3 draws

        session.revertTo(1)
        assertEquals(listOf("a"), session.state.zone(Zone.HAND).map { it.instanceId })
        assertEquals(2, session.log.size) // "Game start" + first draw
    }
}
