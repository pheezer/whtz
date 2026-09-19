package com.pduvall.whtz.data.scryfall

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.DecodeSequenceMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeToSequence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScryfallMappingTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test
    fun `single-faced card maps all fields`() {
        val src = """
            {
              "object": "card",
              "id": "print-uuid-123",
              "oracle_id": "oracle-abc",
              "name": "Templar Knight",
              "mana_cost": "{1}{W}",
              "type_line": "Creature — Human Knight",
              "oracle_text": "Vigilance",
              "power": "2",
              "toughness": "2",
              "layout": "normal",
              "image_uris": {
                "art_crop": "https://img/art.jpg",
                "normal": "https://img/normal.jpg"
              },
              "some_unknown_field": 42
            }
        """.trimIndent()

        val entity = json.decodeFromString<CardDto>(src).toEntity(json)
        assertNotNull(entity)
        requireNotNull(entity)
        assertEquals("oracle-abc", entity.oracleId)
        assertEquals("Templar Knight", entity.name)
        assertEquals("templar knight", entity.nameLower)
        assertEquals("{1}{W}", entity.manaCost)
        assertEquals("Creature — Human Knight", entity.typeLine)
        assertEquals("2", entity.power)
        assertEquals("https://img/art.jpg", entity.artCropUrl)
        assertEquals("print-uuid-123", entity.printingId)
        assertNull(entity.cardFacesJson)
    }

    @Test
    fun `double-faced card falls back to first face for art and text`() {
        val src = """
            {
              "object": "card",
              "id": "print-dfc-1",
              "oracle_id": "oracle-dfc",
              "name": "Delver of Secrets // Insectile Aberration",
              "layout": "transform",
              "card_faces": [
                {
                  "name": "Delver of Secrets",
                  "mana_cost": "{U}",
                  "type_line": "Creature — Human Wizard",
                  "oracle_text": "At the beginning of your upkeep, look at the top card...",
                  "power": "1",
                  "toughness": "1",
                  "image_uris": { "art_crop": "https://img/front_art.jpg", "normal": "https://img/front_normal.jpg" }
                },
                {
                  "name": "Insectile Aberration",
                  "type_line": "Creature — Human Insect",
                  "oracle_text": "Flying",
                  "power": "3",
                  "toughness": "2",
                  "image_uris": { "art_crop": "https://img/back_art.jpg", "normal": "https://img/back_normal.jpg" }
                }
              ]
            }
        """.trimIndent()

        val entity = json.decodeFromString<CardDto>(src).toEntity(json)
        requireNotNull(entity)
        assertEquals("oracle-dfc", entity.oracleId)
        assertEquals("transform", entity.layout)
        // Top-level image/text are absent → fall back to the front face.
        assertEquals("{U}", entity.manaCost)
        assertEquals("Creature — Human Wizard", entity.typeLine)
        assertEquals("https://img/front_art.jpg", entity.artCropUrl)
        assertNotNull(entity.cardFacesJson)
        assertTrue(entity.cardFacesJson!!.contains("Insectile Aberration"))
    }

    @Test
    fun `card without oracle_id is skipped`() {
        val src = """{ "object": "card", "id": "x", "name": "Token", "layout": "token" }"""
        assertNull(json.decodeFromString<CardDto>(src).toEntity(json))
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `jsonl stream decodes multiple cards`() {
        // Mirrors the gzipped-JSONL bulk file the importer streams (one card object per line).
        val jsonl = """
            { "object":"card","id":"1","oracle_id":"o1","name":"Alpha","layout":"normal" }
            { "object":"card","id":"2","oracle_id":"o2","name":"Beta","layout":"normal" }
        """.trimIndent()
        val cards = json.decodeToSequence<CardDto>(
            jsonl.byteInputStream(),
            DecodeSequenceMode.WHITESPACE_SEPARATED,
        ).mapNotNull { it.toEntity(json) }.toList()
        assertEquals(2, cards.size)
        assertEquals(listOf("o1", "o2"), cards.map { it.oracleId })
    }

    @Test
    fun `all_parts is stored on the card entity`() {
        val src = """
            {
              "object": "card", "id": "p1", "oracle_id": "o-krenko",
              "name": "Krenko, Mob Boss", "layout": "normal",
              "all_parts": [
                { "component": "token", "name": "Goblin", "type_line": "Token Creature — Goblin", "uri": "https://api.scryfall.com/cards/tok" },
                { "component": "combo_piece", "name": "Krenko, Mob Boss", "type_line": "Legendary Creature — Goblin Warrior", "uri": "https://x" }
              ]
            }
        """.trimIndent()
        val entity = json.decodeFromString<CardDto>(src).toEntity(json)
        requireNotNull(entity)
        assertNotNull(entity.allPartsJson)
        assertTrue(entity.allPartsJson!!.contains("\"component\":\"token\""))
    }

    @Test
    fun `token maps via toTokenEntity`() {
        val src = """
            {
              "object": "card", "id": "tokprint", "oracle_id": "tok-oracle",
              "name": "Goblin", "layout": "token",
              "type_line": "Token Creature — Goblin", "power": "1", "toughness": "1",
              "image_uris": { "art_crop": "https://img/tok.jpg", "normal": "https://img/tokn.jpg" }
            }
        """.trimIndent()
        val token = json.decodeFromString<CardDto>(src).toTokenEntity(json)
        requireNotNull(token)
        assertEquals("tok-oracle", token.id)
        assertEquals("Goblin", token.name)
        assertEquals("goblin", token.nameLower)
        assertEquals("1", token.power)
        assertEquals("https://img/tok.jpg", token.artCropUrl)
    }

    @Test
    fun `token without oracle_id falls back to printing id`() {
        val src = """{ "object": "card", "id": "only-id", "name": "Emblem", "layout": "emblem" }"""
        val token = json.decodeFromString<CardDto>(src).toTokenEntity(json)
        requireNotNull(token)
        assertEquals("only-id", token.id)
    }

    @Test
    fun `ruling dto decodes`() {
        val src = """
            { "object": "ruling", "oracle_id": "o-1", "source": "wotc",
              "published_at": "2024-06-07", "comment": "Does a thing." }
        """.trimIndent()
        val r = json.decodeFromString<RulingDto>(src)
        assertEquals("o-1", r.oracleId)
        assertEquals("wotc", r.source)
        assertEquals("2024-06-07", r.publishedAt)
        assertEquals("Does a thing.", r.comment)
    }
}
