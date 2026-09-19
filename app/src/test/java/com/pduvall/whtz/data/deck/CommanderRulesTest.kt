package com.pduvall.whtz.data.deck

import com.pduvall.whtz.data.local.entity.OracleCardEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommanderRulesTest {

    private fun card(typeLine: String?, oracleText: String? = null) = OracleCardEntity(
        oracleId = "o",
        name = "X",
        nameLower = "x",
        manaCost = null,
        typeLine = typeLine,
        oracleText = oracleText,
        power = null,
        toughness = null,
        layout = "normal",
        artCropUrl = null,
        normalImageUrl = null,
        cardFacesJson = null,
        printingId = null,
    )

    @Test
    fun `legendary creature is eligible`() {
        assertTrue(card("Legendary Creature — Goblin Warrior").isEligibleCommander())
    }

    @Test
    fun `non-legendary creature is not eligible`() {
        assertFalse(card("Creature — Goblin").isEligibleCommander())
    }

    @Test
    fun `planeswalker that can be your commander is eligible`() {
        assertTrue(
            card(
                typeLine = "Legendary Planeswalker — Teferi",
                oracleText = "Teferi, Temporal Archmage can be your commander.",
            ).isEligibleCommander(),
        )
    }

    @Test
    fun `plain land or null type is not eligible`() {
        assertFalse(card("Basic Land — Mountain").isEligibleCommander())
        assertFalse(card(null).isEligibleCommander())
    }
}
