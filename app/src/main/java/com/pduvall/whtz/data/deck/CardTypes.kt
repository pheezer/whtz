package com.pduvall.whtz.data.deck

/** The main Magic card types we let players filter by. */
enum class CardType(val label: String) {
    CREATURE("Creature"),
    INSTANT("Instant"),
    SORCERY("Sorcery"),
    ARTIFACT("Artifact"),
    ENCHANTMENT("Enchantment"),
    PLANESWALKER("Planeswalker"),
    LAND("Land"),
    BATTLE("Battle"),
    KINDRED("Kindred"),
}

/** Per-oracle-card metadata used for in-game filtering and mechanics. */
data class CardMeta(
    val types: Set<CardType>,
    val rarity: String?,
    val manaValue: Int = 0,
    val manaCost: String? = null,
    val imageUrl: String? = null,
    val createsTokens: Boolean = false,
    val hasRulings: Boolean = false,
)

/**
 * Extracts the main card types from a Scryfall type line. Types live before the "—" dash
 * (supertypes/subtypes are ignored); split/DFC faces ("A // B") are each considered.
 */
fun cardTypesOf(typeLine: String?): Set<CardType> {
    if (typeLine.isNullOrBlank()) return emptySet()
    val result = LinkedHashSet<CardType>()
    for (face in typeLine.split("//")) {
        val head = face.substringBefore("—")
        for (type in CardType.entries) {
            if (head.contains(type.label, ignoreCase = true)) result += type
        }
        if (head.contains("Tribal", ignoreCase = true)) result += CardType.KINDRED
    }
    return result
}

/**
 * Mana value (converted mana cost) parsed from a Scryfall mana-cost string like "{2}{W}{U}".
 * Numbers add their value; {X}/{Y}/{Z} count 0; hybrid "{2/W}" uses the numeric part (2);
 * colored/hybrid/phyrexian symbols count 1; null/blank (e.g. lands) is 0.
 */
fun manaValueOf(manaCost: String?): Int {
    if (manaCost.isNullOrBlank()) return 0
    var total = 0
    for (match in Regex("\\{([^}]+)\\}").findAll(manaCost)) {
        val symbol = match.groupValues[1]
        val asNumber = symbol.toIntOrNull()
        total += when {
            asNumber != null -> asNumber
            symbol.equals("X", true) || symbol.equals("Y", true) || symbol.equals("Z", true) -> 0
            symbol.contains("/") -> symbol.split("/").firstNotNullOfOrNull { it.toIntOrNull() } ?: 1
            else -> 1
        }
    }
    return total
}
