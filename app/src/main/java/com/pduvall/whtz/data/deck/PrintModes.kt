package com.pduvall.whtz.data.deck

import com.pduvall.whtz.data.local.entity.OracleCardEntity
import com.pduvall.whtz.data.scryfall.CardFaceDto
import com.pduvall.whtz.domain.model.Zone
import kotlinx.serialization.json.Json

/**
 * One printable/castable mode of a card: which face to render ([faceIndex]; null = the whole
 * single-faced card) and where the physical card ends up after printing ([destination]).
 */
data class PrintMode(
    val label: String,
    val faceIndex: Int?,
    val destination: Zone,
)

/** Graveyard for instants/sorceries, battlefield otherwise. */
private fun zoneForType(typeLine: String?): Zone {
    val types = cardTypesOf(typeLine)
    return if (CardType.INSTANT in types || CardType.SORCERY in types) {
        Zone.GRAVEYARD
    } else {
        Zone.BATTLEFIELD
    }
}

/**
 * The print/cast modes for a card:
 *  - **adventure**: the creature (routed by its type) and the adventure spell (always → exile);
 *  - **transform / modal_dfc / double_faced_token / split**: one mode per face, routed by that
 *    face's type;
 *  - everything else (normal, saga, …): a single mode routed by the card's type.
 * Instants and sorceries route to the graveyard; permanents to the battlefield.
 */
fun OracleCardEntity.printModes(json: Json): List<PrintMode> {
    val faces = cardFacesJson?.let {
        runCatching { json.decodeFromString<List<CardFaceDto>>(it) }.getOrNull()
    }.orEmpty()
    val single = listOf(PrintMode(name, faceIndex = null, destination = zoneForType(typeLine)))

    return when (layout) {
        "adventure" -> if (faces.size < 2) {
            single
        } else {
            listOf(
                PrintMode(faces[0].name ?: name, 0, zoneForType(faces[0].typeLine)),
                PrintMode("${faces[1].name ?: "Adventure"} (Adventure)", 1, Zone.EXILE),
            )
        }

        "transform", "modal_dfc", "double_faced_token", "split" -> if (faces.size < 2) {
            single
        } else {
            faces.mapIndexed { i, face ->
                PrintMode(face.name ?: name, i, zoneForType(face.typeLine))
            }
        }

        else -> single
    }
}
