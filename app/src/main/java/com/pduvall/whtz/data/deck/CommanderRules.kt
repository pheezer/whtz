package com.pduvall.whtz.data.deck

import com.pduvall.whtz.data.local.entity.OracleCardEntity

/**
 * Whether a card can legally be a commander: a legendary creature, or any card whose rules text
 * says it "can be your commander" (planeswalker commanders, backgrounds' partners, etc.).
 */
fun OracleCardEntity.isEligibleCommander(): Boolean {
    val type = typeLine.orEmpty()
    val legendaryCreature = type.contains("Legendary", ignoreCase = true) &&
        type.contains("Creature", ignoreCase = true)
    val canBeCommander = oracleText?.contains("can be your commander", ignoreCase = true) == true
    return legendaryCreature || canBeCommander
}
