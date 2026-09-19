package com.pduvall.whtz.data.deck

import org.junit.Assert.assertEquals
import org.junit.Test

class CardTypesTest {

    @Test
    fun `legendary creature is a creature`() {
        assertEquals(setOf(CardType.CREATURE), cardTypesOf("Legendary Creature — Goblin Warrior"))
    }

    @Test
    fun `artifact creature has both types`() {
        assertEquals(
            setOf(CardType.ARTIFACT, CardType.CREATURE),
            cardTypesOf("Artifact Creature — Golem"),
        )
    }

    @Test
    fun `basic land is a land and subtypes are ignored`() {
        assertEquals(setOf(CardType.LAND), cardTypesOf("Basic Land — Mountain"))
        assertEquals(setOf(CardType.LAND), cardTypesOf("Land — Island"))
    }

    @Test
    fun `plain instant`() {
        assertEquals(setOf(CardType.INSTANT), cardTypesOf("Instant"))
    }

    @Test
    fun `split faces are both counted`() {
        assertEquals(
            setOf(CardType.INSTANT, CardType.SORCERY),
            cardTypesOf("Instant // Sorcery"),
        )
    }

    @Test
    fun `null or blank yields no types`() {
        assertEquals(emptySet<CardType>(), cardTypesOf(null))
        assertEquals(emptySet<CardType>(), cardTypesOf("   "))
    }

    @Test
    fun `mana value parses cost symbols`() {
        assertEquals(5, manaValueOf("{3}{U}{U}"))
        assertEquals(1, manaValueOf("{X}{R}"))
        assertEquals(2, manaValueOf("{2/W}"))
        assertEquals(0, manaValueOf(null))
        assertEquals(0, manaValueOf(""))
    }
}
