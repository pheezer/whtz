package com.pduvall.whtz.data.deck

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DecklistParserTest {

    @Test
    fun `plain quantity and name`() {
        val r = DecklistParser.parse("4 Lightning Bolt")
        assertEquals(1, r.entries.size)
        assertEquals(DeckEntry(4, "Lightning Bolt"), r.entries[0])
    }

    @Test
    fun `x-suffixed quantity`() {
        val r = DecklistParser.parse("4x Llanowar Elves")
        assertEquals(DeckEntry(4, "Llanowar Elves"), r.entries[0])
    }

    @Test
    fun `missing quantity defaults to one`() {
        val r = DecklistParser.parse("Black Lotus")
        assertEquals(DeckEntry(1, "Black Lotus"), r.entries[0])
    }

    @Test
    fun `set code and collector number`() {
        val r = DecklistParser.parse("4 Llanowar Elves (M19) 314")
        assertEquals(DeckEntry(4, "Llanowar Elves", "M19", "314"), r.entries[0])
    }

    @Test
    fun `foil and etched markers stripped`() {
        val r = DecklistParser.parse(
            """
            3 Sol Ring (CMR) 472 *F*
            1 Arcane Signet *E*
            2 Command Tower (foil)
            """.trimIndent(),
        )
        assertEquals(DeckEntry(3, "Sol Ring", "CMR", "472"), r.entries[0])
        assertEquals(DeckEntry(1, "Arcane Signet"), r.entries[1])
        assertEquals(DeckEntry(2, "Command Tower"), r.entries[2])
    }

    @Test
    fun `double-faced card name with slashes is preserved`() {
        val r = DecklistParser.parse(
            """
            1 Fire // Ice
            4 Delver of Secrets // Insectile Aberration (ISD) 51
            """.trimIndent(),
        )
        assertEquals(DeckEntry(1, "Fire // Ice"), r.entries[0])
        assertEquals("Delver of Secrets // Insectile Aberration", r.entries[1].name)
        assertEquals("ISD", r.entries[1].setCode)
        assertEquals("51", r.entries[1].collectorNumber)
    }

    @Test
    fun `section headers switch the current section`() {
        val r = DecklistParser.parse(
            """
            Deck
            4 Lightning Bolt

            Sideboard
            2 Pyroblast

            Commander
            1 Krenko, Mob Boss
            """.trimIndent(),
        )
        assertEquals(DeckSection.MAINBOARD, r.entries[0].section)
        assertEquals(DeckSection.SIDEBOARD, r.entries[1].section)
        assertEquals(DeckSection.COMMANDER, r.entries[2].section)
    }

    @Test
    fun `SB prefix marks a single sideboard entry`() {
        val r = DecklistParser.parse(
            """
            4 Lightning Bolt
            SB: 2 Pyroblast
            """.trimIndent(),
        )
        assertEquals(DeckSection.MAINBOARD, r.entries[0].section)
        assertEquals(DeckSection.SIDEBOARD, r.entries[1].section)
        assertEquals(DeckEntry(2, "Pyroblast", section = DeckSection.SIDEBOARD), r.entries[1])
    }

    @Test
    fun `comments and blank lines are ignored`() {
        val r = DecklistParser.parse(
            """
            // my burn deck
            # another comment
            4 Lightning Bolt

            """.trimIndent(),
        )
        assertEquals(1, r.entries.size)
        assertEquals("Lightning Bolt", r.entries[0].name)
    }

    @Test
    fun `curly apostrophe is normalized`() {
        val r = DecklistParser.parse("1 Urza’s Saga")
        assertEquals("Urza's Saga", r.entries[0].name)
    }

    @Test
    fun `arena about and name metadata skipped`() {
        val r = DecklistParser.parse(
            """
            About
            Name My Sweet Deck

            Deck
            4 Lightning Bolt
            """.trimIndent(),
        )
        assertEquals(1, r.entries.size)
        assertEquals("Lightning Bolt", r.entries[0].name)
    }

    @Test
    fun `mixed real-world arena list`() {
        val r = DecklistParser.parse(
            """
            Deck
            4 Templar Knight (NEO) 33
            2 Templar Knight
            20 Plains

            Sideboard
            3 Rest in Peace (M20) 30
            """.trimIndent(),
        )
        assertEquals(4, r.entries.size)
        assertEquals(4, r.entries[0].quantity)
        assertEquals("Templar Knight", r.entries[0].name)
        assertEquals("NEO", r.entries[0].setCode)
        assertEquals(DeckSection.SIDEBOARD, r.entries.last().section)
        assertTrue(r.unparseableLines.isEmpty())
    }
}
