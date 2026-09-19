package com.pduvall.whtz.data.deck

import com.pduvall.whtz.data.local.entity.OracleCardEntity
import com.pduvall.whtz.domain.model.Zone
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class PrintModesTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun card(
        name: String,
        typeLine: String?,
        layout: String = "normal",
        cardFacesJson: String? = null,
    ) = OracleCardEntity(
        oracleId = "o-$name",
        name = name,
        nameLower = name.lowercase(),
        manaCost = null,
        typeLine = typeLine,
        oracleText = null,
        power = null,
        toughness = null,
        layout = layout,
        artCropUrl = null,
        normalImageUrl = null,
        cardFacesJson = cardFacesJson,
        printingId = null,
    )

    @Test
    fun `single instant goes to graveyard`() {
        val modes = card("Lightning Bolt", "Instant").printModes(json)
        assertEquals(1, modes.size)
        assertEquals(null, modes[0].faceIndex)
        assertEquals(Zone.GRAVEYARD, modes[0].destination)
    }

    @Test
    fun `single sorcery goes to graveyard`() {
        val modes = card("Divination", "Sorcery").printModes(json)
        assertEquals(Zone.GRAVEYARD, modes.single().destination)
    }

    @Test
    fun `single creature goes to battlefield`() {
        val modes = card("Grizzly Bears", "Creature — Bear").printModes(json)
        assertEquals(Zone.BATTLEFIELD, modes.single().destination)
    }

    @Test
    fun `saga goes to battlefield as a single mode`() {
        val modes = card("History of Benalia", "Enchantment — Saga", layout = "saga").printModes(json)
        assertEquals(1, modes.size)
        assertEquals(Zone.BATTLEFIELD, modes.single().destination)
    }

    @Test
    fun `adventure offers creature to battlefield and adventure to exile`() {
        val faces = """
            [
              {"name":"Bonecrusher Giant","type_line":"Creature — Giant"},
              {"name":"Stomp","type_line":"Instant — Adventure"}
            ]
        """.trimIndent()
        val modes = card(
            "Bonecrusher Giant",
            "Creature — Giant // Instant — Adventure",
            layout = "adventure",
            cardFacesJson = faces,
        ).printModes(json)

        assertEquals(2, modes.size)
        assertEquals(0, modes[0].faceIndex)
        assertEquals(Zone.BATTLEFIELD, modes[0].destination)
        assertEquals(1, modes[1].faceIndex)
        assertEquals(Zone.EXILE, modes[1].destination)
        assertEquals("Stomp (Adventure)", modes[1].label)
    }

    @Test
    fun `transform offers one mode per face routed by type`() {
        val faces = """
            [
              {"name":"Delver of Secrets","type_line":"Creature — Human Wizard"},
              {"name":"Insectile Aberration","type_line":"Creature — Human Insect"}
            ]
        """.trimIndent()
        val modes = card(
            "Delver of Secrets // Insectile Aberration",
            "Creature — Human Wizard // Creature — Human Insect",
            layout = "transform",
            cardFacesJson = faces,
        ).printModes(json)

        assertEquals(2, modes.size)
        assertEquals(listOf(0, 1), modes.map { it.faceIndex })
        assertEquals(listOf(Zone.BATTLEFIELD, Zone.BATTLEFIELD), modes.map { it.destination })
    }

    @Test
    fun `split halves both go to graveyard`() {
        val faces = """
            [
              {"name":"Fire","type_line":"Instant"},
              {"name":"Ice","type_line":"Instant"}
            ]
        """.trimIndent()
        val modes = card("Fire // Ice", "Instant // Instant", layout = "split", cardFacesJson = faces)
            .printModes(json)
        assertEquals(2, modes.size)
        assertEquals(listOf(Zone.GRAVEYARD, Zone.GRAVEYARD), modes.map { it.destination })
    }
}
